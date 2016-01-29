package mosaic.bregman;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mosaic.core.detection.Particle;
import mosaic.core.imageUtils.MaskOnSpaceMapper;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.core.imageUtils.masks.BallMask;
import mosaic.core.psf.GaussPSF;
import mosaic.utils.ArrayOps;
import mosaic.utils.ArrayOps.MinMax;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvColumnConfig;
import net.imglib2.type.numeric.real.DoubleType;


/**
 * Class that process the first Split bregman segmentation and refine with patches
 * @author Aurelien Ritz
 */
class TwoRegions {
    private final Parameters iParameters;
    private final int iChannel;
    private final int ni, nj, nz;
    private final Tools iLocalTools;
    private final double[][][] iImage;
    private final double[][][] iMask;
    private double min, max;
    
    private final ImagePlus iAImg = new ImagePlus(); 
    private final ImagePlus iBImg = new ImagePlus();
    
    
    public TwoRegions(ImagePlus aInputImg, Parameters aParameters, int aChannel) {
        iParameters = aParameters;
        iChannel = aChannel;
        
        ni = aInputImg.getWidth();
        nj = aInputImg.getHeight();
        nz = aInputImg.getNSlices();
        iLocalTools = new Tools(ni, nj, nz);
        iImage = new double[nz][ni][nj];
        iMask = new double[nz][ni][nj];

        
        for (int z = 0; z < nz; z++) {
            aInputImg.setSlice(z + 1);
            ImageProcessor imp = aInputImg.getProcessor();
            
            if (iParameters.removebackground) {
                final BackgroundSubtracter bs = new BackgroundSubtracter();
                bs.rollingBallBackground(imp, iParameters.size_rollingball, false, false, false, true, true);
            }

            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    iImage[z][i][j] = imp.getPixel(i, j);
                }
            }
        }
        MinMax<Double> mm = ArrayOps.findMinMax(iImage);
        max = mm.getMax();
        min = mm.getMin();
        
        if (iParameters.livedisplay && iParameters.removebackground) {
            final ImagePlus noBackgroundImg = aInputImg.duplicate();
            noBackgroundImg.setTitle("Background reduction channel " + (iChannel + 1));
            noBackgroundImg.changes = false;
            noBackgroundImg.setDisplayRange(min, max);
            noBackgroundImg.show();
        }

        /* Overload min/max after background subtraction */
        if (Analysis.norm_max != 0) {
            max = Analysis.norm_max;
            // if we are removing the background we have no idea which is the minumum across 
            // all the movie so let be conservative and put min = 0.0 for sure cannot be < 0
            min = (iParameters.removebackground) ? 0.0 : Analysis.norm_min;
        }
        ArrayOps.normalize(iImage, iImage, min, max);

        iLocalTools.createmask(iMask, iImage, iParameters.betaMLEindefault);
    }

    /**
     * Create a sphere of radius r, used to force patches around the spheres that you draw
     *
     * @param out image
     * @param pt vector of particles
     * @param radius of the sphere
     */
    private void drawParticles(double[][][] out, double[][][] mask, Vector<Particle> pt, int radius) {
        // Iterate on all particles

        final int sz[] = new int[3];
        sz[1] = out[0][0].length;
        sz[0] = out[0].length;
        sz[2] = out.length;

        // Create a circle Mask and an iterator
        float[] spac = new float[] {1.0f, 1.0f, 1.0f};
        final BallMask cm = new BallMask(radius, 2 * radius + 1, spac);
        final MaskOnSpaceMapper rg_m = new MaskOnSpaceMapper(cm, sz);

        for (Particle ptt : pt) {
            // Draw the sphere
            rg_m.setMiddlePoint(new Point((int) (ptt.iX), (int) (ptt.iY), (int) (ptt.iZ)));

            while (rg_m.hasNext()) {
                final Point p = rg_m.nextPoint();

                if (p.iCoords[0] < sz[0] && p.iCoords[0] >= 0 && p.iCoords[1] < sz[1] && p.iCoords[1] >= 0 && p.iCoords[2] < sz[2] && p.iCoords[2] >= 0) {
                    out[p.iCoords[2]][p.iCoords[0]][p.iCoords[1]] = 255.0f;
                    mask[p.iCoords[2]][p.iCoords[0]][p.iCoords[1]] = 1.0f;
                }
            }
        }
    }

    public final ImagePlus out_soft_mask[] = new ImagePlus[2];

    /**
     * Get the particles related to one frame
     *
     * @param part particle vector
     * @param frame frame number
     * @return a vector with particles related to one frame
     */
    private Vector<Particle> getPart(Vector<Particle> part, int frame) {
        final Vector<Particle> pp = new Vector<Particle>();

        // get the particle related to one frame
        for (Particle p : part) {
            if (p.getFrame() == frame) {
                pp.add(p);
            }
        }

        return pp;
    }

    /**
     * Run the split Bregman + patch refinement
     */
    public void run() {
        double minIntensity = (iChannel == 0) ? iParameters.min_intensity : iParameters.min_intensityY;

        // ========================      Prepare PSF
        int psfDims = (nz > 1) ? 3 : 2;
        final GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(psfDims, DoubleType.class);
        final DoubleType[] var = new DoubleType[psfDims];
        var[0] = new DoubleType(iParameters.sigma_gaussian);
        var[1] = new DoubleType(iParameters.sigma_gaussian);
        if (psfDims == 3) var[2] = new DoubleType(iParameters.sigma_gaussian / iParameters.zcorrec);
        psf.setVar(var);
        // Prepare PSF for futher use (It prevents from multiphreading problems so it must be like that).
        psf.getSeparableImageAsDoubleArray(0);
        iParameters.PSF = psf;
        
        // ========================      Solver
        ASplitBregmanSolver A_solver = (nz > 1) 
            ? new ASplitBregmanSolverTwoRegions3DPSF(iParameters, iImage, iMask, null, iParameters.betaMLEoutdefault, iParameters.betaMLEindefault, iParameters.lreg_[iChannel], minIntensity)
            : new ASplitBregmanSolverTwoRegions2DPSF(iParameters, iImage, iMask, null, iParameters.betaMLEoutdefault, iParameters.betaMLEindefault, iParameters.lreg_[iChannel], minIntensity);
        
        // ========================     First phase      
        if (Analysis.iParameters.patches_from_file == null) {
            try {
                IJ.showStatus("Computing segmentation");
                IJ.showProgress(0.0);
                A_solver.first_run();
                
                if (iParameters.livedisplay) {
                    display2regions(A_solver.w3kbest, "Mask", iChannel);
                }
            }
            catch (final InterruptedException ex) {
            }
        }
        else {
            final CSV<Particle> csv = new CSV<Particle>(Particle.class);
            csv.setCSVPreferenceFromFile(Analysis.iParameters.patches_from_file);
            Vector<Particle> pt = csv.Read(Analysis.iParameters.patches_from_file, new CsvColumnConfig(Particle.ParticleDetection_map, Particle.ParticleDetectionCellProcessor));

            // Get the particle related inly to one frames
            final Vector<Particle> pt_f = getPart(pt, Analysis.frame - 1);

            // create a mask Image
            final double img[][][] = new double[nz][ni][nj];
            drawParticles(img, A_solver.w3kbest, pt_f, (int) 3.0);

            A_solver.regions_intensity_findthresh(img);
        }

        if (iParameters.dispSoftMask) {
            out_soft_mask[iChannel] = generateImgFromArray(A_solver.w3k, "Mask" + ((iChannel == 0) ? "X" : "Y"));
        }

        // ========================      Compute segmentation
        Analysis.setMask(A_solver.w3kbest, iChannel);
        float[][][] RiN = new float[nz][ni][nj];
        iLocalTools.copytab(RiN, A_solver.Ri);
        
        final ArrayList<Region> regions = A_solver.regionsvoronoi;
        Analysis.compute_connected_regions(iChannel);
        
        if (Analysis.iParameters.refinement) {
            Analysis.SetRegionsObjsVoronoi(Analysis.getRegionslist(iChannel), regions, RiN);
            IJ.showStatus("Computing segmentation  " + 55 + "%");
            IJ.showProgress(0.55);
        
            final ImagePatches ipatches = new ImagePatches(iParameters, Analysis.getRegionslist(iChannel), iImage, A_solver.w3kbest, min, max, iParameters.lreg_[iChannel], minIntensity);
            ipatches.run();
            IJ.log(ipatches.getRegionsList().size() + " objects found in " + ((iChannel == 0) ? "X" : "Y") + ".");
            Analysis.setRegionslist(ipatches.getRegionsList(), iChannel);
            Analysis.setRegions(ipatches.getRegions(), iChannel);
        }
        
        // Here we solved the patches and the regions that come from the patches
        // we rescale the intensity to the original one
        for (final Region r1 : Analysis.getRegionslist(iChannel)) {
            r1.intensity = r1.intensity * (max - min) + min;
        }
        
        //  ========================      Postprocessing phase
        // Well we did not finished yet at this stage you can have several artifact produced by the patches
        // for example one region can be segmented partially by two patches, this mean that at least in theory
        // you should repatch (this produce a finer decomposition) again and rerun the second stage until each
        // patches has one and only one region. The method eventually converge because it always produce finer 
        // decomposition, in the opposite case you stop.
        // The actual patching algorithm use a first phase Split-Bregman segmentation + Threasholding + Voronoi 
        // (unfortunately only 2D, for 3D a 2D Maximum projection is computed)
        // The 2D Maximal projection unfortunately complicate even more the things, and produce even more artifacts,
        // in particular for big PSF with big margins patch.
        //
        // IMPROVEMENT:
        // 1) 3D Voronoi (ImageLib2 Voronoi like segmentation)
        // 2) Good result has been achieved using Particle tracker for Patch positioning.
        // 3) Smart patches given the first phase we cut the soft membership each cut produce a segmentation
        // that include the previous one, going to zero this produce a tree graph. the patches are positioned
        // on the leaf ( other algorithms can be implemented to analyze this graph ... )
        //
        // (Temporarily we fix in this way)
        // Save the old intensity label image as an hashmap (to save memory) run find connected region to recompute 
        // the regions again. Recompute the statistics using the old intensity label image.

        // we run find connected regions
        final LabelImage img = new LabelImage(Analysis.getRegions(0));
        img.connectedComponents();

        final HashMap<Integer, Region> r_list = new HashMap<Integer, Region>();

        // Run on all pixels of the label to add pixels to the regions
        final Iterator<Point> rit = new SpaceIterator(img.getDimensions()).getPointIterator();
        while (rit.hasNext()) {
            final Point p = rit.next();
            final int lbl = img.getLabel(p);
            if (lbl != 0) {
                // foreground
                Region r = r_list.get(lbl);
                if (r == null) {
                    r = new Region(lbl, 0);
                    r_list.put(lbl, r);
                }
                r.pixels.add(new Pix(p.iCoords[2], p.iCoords[0], p.iCoords[1]));
            }
        }

        // Now we run Object properties on this regions list
        final int osxy = iParameters.oversampling2ndstep * iParameters.interpolation;
        final int sx = ni * osxy;
        final int sy = nj * osxy;
        int sz = (nz == 1) ? 1 : nz * osxy;
        int osz = (nz == 1) ? 1 : osxy;

        ImagePatches.assemble(r_list.values(), Analysis.getRegions(0));

        for (final Region r : r_list.values()) {
            final ObjectProperties obj = new ObjectProperties(iImage, r, sx, sy, sz, iParameters, osxy, osz, Analysis.getRegions(0));
            obj.run();
        }
    }

    /**
     * Display the soft membership
     *
     * @param array 3D array of double
     * @param s String of the image
     * @param channel channel
     */
    void display2regions(double[][][] array, String s, int channel) {
        final ImageStack ims = convertArrayToImageProcessor(array);

        if (channel == 0) {
            iAImg.setStack(s + " X", ims);
            iAImg.resetDisplayRange();
            iAImg.show();
        }
        else {
            iBImg.setStack(s + " Y", ims);
            iBImg.resetDisplayRange();
            iBImg.show();
        }
    }

    private ImageStack convertArrayToImageProcessor(double[][][] array) {
        final ImageStack ims = new ImageStack(ni, nj);
        for (int z = 0; z < nz; z++) {
            final byte[] temp = new byte[ni * nj];
            for (int j = 0; j < nj; j++) {
                for (int i = 0; i < ni; i++) {
                    temp[j * ni + i] = (byte) ((int) (255 * array[z][i][j]));
                }
            }
            final ImageProcessor bp = new ByteProcessor(ni, nj);
            bp.setPixels(temp);
            ims.addSlice("", bp);
        }
        return ims;
    }
    
    /**
     * Display the soft membership
     *
     * @param aImgArray 3D array with image data [z][x][y]
     * @param aTitle Title of the image.
     * @return the generated ImagePlus
     */
    private ImagePlus generateImgFromArray(double[][][] aImgArray, String aTitle) {
        int iWidth = aImgArray[0].length;
        int iHeigth = aImgArray[0][0].length;
        int iDepth = aImgArray.length;
        final ImageStack stack = new ImageStack(iWidth, iHeigth);
    
        for (int z = 0; z < iDepth; z++) {
            final float[][] pixels = new float[iWidth][iHeigth];
            for (int i = 0; i < iWidth; i++) {
                for (int j = 0; j < iHeigth; j++) {
                    pixels[i][j] = (float) aImgArray[z][i][j];
                }
            }
            stack.addSlice("", new FloatProcessor(pixels));
        }
    
        final ImagePlus img = new ImagePlus(aTitle, stack);
        img.changes = false;
        
        return img;
    }
}
