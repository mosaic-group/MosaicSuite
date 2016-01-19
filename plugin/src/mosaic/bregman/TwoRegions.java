package mosaic.bregman;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.ImageProcessor;
import mosaic.core.detection.Particle;
import mosaic.core.imageUtils.MaskOnSpaceMapper;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.core.imageUtils.masks.BallMask;
import mosaic.core.psf.GaussPSF;
import mosaic.utils.Debug;
import mosaic.utils.ArrayOps.MinMax;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvColumnConfig;
import net.imglib2.type.numeric.real.DoubleType;


/**
 * Class that process the first Split bregman segmentation and refine with patches
 * @author Aurelien Ritz
 */
class TwoRegions implements Runnable {
    protected final double[][][] image;// 3D image
    protected final double[][][][] mask;// nregions nslices ni nj

    protected final Parameters p;

    protected final int ni, nj, nz;// 3 dimensions
    protected final int nl;
    protected final int channel;
    protected final CountDownLatch DoneSignal;

    protected final Tools LocalTools;
    protected double min, max;
    
    protected MasksDisplay md;
    
    public TwoRegions(ImagePlus img, Parameters params, CountDownLatch DoneSignal, int channel) {
        if (img.getBitDepth() == 32) {
            IJ.log("Error converting float image to short");
        }

        this.p = params;
        this.DoneSignal = DoneSignal;
        this.channel = channel;
        
        this.nl = p.nlevels;
        this.ni = p.ni;
        this.nj = p.nj;
        this.nz = p.nz;

        LocalTools = new Tools(ni, nj, nz, nl);

        image = new double[nz][ni][nj];
        mask = new double[nl][nz][ni][nj];

        /* Search for maximum and minimum value, normalization */
        if (Analysis.norm_max == 0) {
            MinMax<Double> mm = Tools.findMinMax(img);
            min = mm.getMin();
            max = mm.getMax();
        }
        else {
            min = Analysis.norm_min;
            max = Analysis.norm_max;
        }

        if (p.usecellmaskX && channel == 0) {
            Analysis.cellMaskABinary = Tools.createBinaryCellMask(Analysis.p.thresholdcellmask * (max - min) + min, img, channel, nz, ni, nj, true);
        }
        if (p.usecellmaskY && channel == 1) {
            Analysis.cellMaskBBinary = Tools.createBinaryCellMask(Analysis.p.thresholdcellmasky * (max - min) + min, img, channel, nz, ni, nj, true);
        }

        max = 0;
        min = Double.POSITIVE_INFINITY;

        for (int z = 0; z < nz; z++) {
            img.setSlice(z + 1);
            ImageProcessor imp = img.getProcessor();
            
            if (p.removebackground) {
                final BackgroundSubtracter bs = new BackgroundSubtracter();
                bs.rollingBallBackground(imp, p.size_rollingball, false, false, false, true, true);
            }

            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    image[z][i][j] = imp.getPixel(i, j);
                    if (image[z][i][j] > max) {
                        max = image[z][i][j];
                    }
                    if (image[z][i][j] < min) {
                        min = image[z][i][j];
                    }
                }
            }
        }

        /* Again overload the parameter after background subtraction */
        if (Analysis.norm_max != 0) {
            max = Analysis.norm_max;
            if (p.removebackground) {
                // if we are removing the background we have no idea which is the minumum across 
                // all the movie so let be conservative and put min = 0.0 for sure cannot be < 0
                min = 0.0;
            }
            else {
                min = Analysis.norm_min;
            }
        }

        if (p.livedisplay && p.removebackground) {
            final ImagePlus back = img.duplicate();
            back.setTitle("Background reduction channel " + (channel + 1));
            back.changes = false;
            back.setDisplayRange(min, max);
            back.show();
        }

        // normalize the image
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    image[z][i][j] = (image[z][i][j] - min) / (max - min);
                    if (image[z][i][j] < 0.0) {
                        image[z][i][j] = 0.0;
                    }
                    else if (image[z][i][j] > 1.0) {
                        image[z][i][j] = 1.0;
                    }
                }
            }
        }

        if (p.nlevels == 2 || p.nlevels == 1) {
            p.cl[0] = p.betaMLEoutdefault;
            p.cl[1] = p.betaMLEindefault;
        }

        LocalTools.createmask(mask, image, p.cl);
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
    @Override
    public void run() {
        // This store the output mask
        md = new MasksDisplay(ni, nj, nz);

        ASplitBregmanSolver A_solver = null;
        p.cl[0] = p.betaMLEoutdefault;
        p.cl[1] = p.betaMLEindefault;
        
        // TODO: This causes troubles when soft mask is calculated for 2 channel images. Should be investigated. Why it is 1 not 2 or just taken from img?
        //       It is temporarily changed to 2 but it is unknown if has any efect on segmentaiton output.
        p.nlevels = 1;
        // IJ.log(String.format("Photometry default:%n backgroung %7.2e %n foreground %7.2e", p.cl[0],p.cl[1]));
    
        if (p.nz > 1) {
            final GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(3, DoubleType.class);
            final DoubleType[] var = new DoubleType[3];
            var[0] = new DoubleType(p.sigma_gaussian);
            var[1] = new DoubleType(p.sigma_gaussian);
            var[2] = new DoubleType(p.sigma_gaussian / p.zcorrec);
            psf.setVar(var);
            p.PSF = psf;

            A_solver = new ASplitBregmanSolverTwoRegions3DPSF(p, image, mask, md, channel, null);
        }
        else {
            final GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(2, DoubleType.class);
            final DoubleType[] var = new DoubleType[2];
            var[0] = new DoubleType(p.sigma_gaussian);
            var[1] = new DoubleType(p.sigma_gaussian);
            psf.setVar(var);
            p.PSF = psf;

            A_solver = new ASplitBregmanSolverTwoRegionsPSF(p, image, mask, md, channel, null);
        }

        if (Analysis.p.patches_from_file == null) {
            try {
                A_solver.first_run();
            }
            catch (final InterruptedException ex) {
            }
        }
        else {
            // Here we have patches
            // Load particles
            final CSV<Particle> csv = new CSV<Particle>(Particle.class);

            csv.setCSVPreferenceFromFile(Analysis.p.patches_from_file);
            Vector<Particle> pt = csv.Read(Analysis.p.patches_from_file, new CsvColumnConfig(Particle.ParticleDetection_map, Particle.ParticleDetectionCellProcessor));

            // Get the particle related inly to one frames
            final Vector<Particle> pt_f = getPart(pt, Analysis.frame - 1);

            // create a mask Image
            final double img[][][] = new double[p.nz][p.ni][p.nj];
            drawParticles(img, A_solver.w3kbest[0], pt_f, (int) 3.0);

            A_solver.regions_intensity_findthresh(img);
        }

        mergeSoftMask(A_solver);

        if (channel == 0) {
            Analysis.setMaskaTworegions(A_solver.w3kbest[0]);
            float[][][] RiN = new float[p.nz][p.ni][p.nj];
            LocalTools.copytab(RiN, A_solver.Ri[0]);
            float[][][] RoN = new float[p.nz][p.ni][p.nj];
            LocalTools.copytab(RoN, A_solver.Ro[0]);

            final ArrayList<Region> regions = A_solver.regionsvoronoi;

            if (p.findregionthresh) {
                Analysis.compute_connected_regions_a(255 * p.thresh);
            }
            else {
                Analysis.compute_connected_regions_a(255 * p.thresh);
            }

            if (Analysis.p.refinement) {
                Analysis.SetRegionsObjsVoronoi(Analysis.regionslist[0], regions, RiN);
                IJ.showStatus("Computing segmentation  " + 55 + "%");
                IJ.showProgress(0.55);

                final ImagePatches ipatches = new ImagePatches(p, Analysis.regionslist[0], image, channel, A_solver.w3kbest[0], min, max);
                A_solver = null;
                ipatches.run();
                Analysis.regionslist[0] = ipatches.getRegionsList();
                Analysis.regions[0] = ipatches.getRegions();
            }

            // Here we solved the patches and the regions that come from the patches
            // we rescale the intensity to the original one
            for (final Region r : Analysis.regionslist[0]) {
                r.intensity = r.intensity * (max - min) + min;
            }

            // Well we did not finished yet at this stage you can have several artifact produced by the patches
            // for example one region can be segmented partially by two patches, this mean that at least in theory
            // you should repatch (this produce a finer decomposition) again and rerun the second stage until each
            // patches has one and only one region.
            // The method eventually converge because it always produce finer decomposition, in the opposite case you stop
            // The actual patching algorithm use
            // A first phase Split-Bregman segmentation + Threasholding + Voronoi (unfortunately only 2D, for 3D a 2D
            // Maximum projection is computed)
            // The 2D Maximal projection unfortunately complicate
            // even more the things, and produce even more artefatcs in particular for big PSF with big margins
            // patch.
            //
            // IMPROVEMENT:
            //
            // 1) 3D Voronoi (ImageLib2 Voronoi like segmentation)
            // 2) Good result has been achieved using Particle tracker for Patch positioning.
            // 3) Smart patches given the first phase we cut the soft membership each cut produce a segmentation
            // that include the previous one, going to zero this produce a tree graph. the patches are positioned
            // on the leaf ( other algorithms can be implemented to analyze this graph ... )
            //
            // (Temporarily we fix in this way)
            // Save the old intensity label image as an hashmap (to save memory)
            // run find connected region to recompute the regions again
            // recompute the statistics using the old intensity label image

            final HashMap<Integer, Float> lblInt = new HashMap<Integer, Float>();

            for (final Region r : Analysis.regionslist[0]) {
                for (final Pix p : r.pixels) {
                    final Integer id = p.px + p.py * ni + p.pz * ni * nj;
                    lblInt.put(id, (float) r.intensity);
                }
            }

            // we run find connected regions
            final LabelImage img = new LabelImage(Analysis.regions[0]);
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
                        r.pixels.add(new Pix(p.iCoords[2], p.iCoords[0], p.iCoords[1]));
                        r_list.put(lbl, r);
                    }
                    else {
                        r.pixels.add(new Pix(p.iCoords[2], p.iCoords[0], p.iCoords[1]));
                    }
                }
            }

            // Now we run Object properties on this regions list
            final int osxy = p.oversampling2ndstep * p.interpolation;
            final int sx = p.ni * p.oversampling2ndstep * p.interpolation;
            final int sy = p.nj * p.oversampling2ndstep * p.interpolation;
            int sz = 1;
            int osz = 1;
            if (p.nz == 1) {
                sz = 1;
                osz = 1;
            }
            else {
                sz = p.nz * p.oversampling2ndstep * p.interpolation;
                osz = p.oversampling2ndstep * p.interpolation;
            }

            ImagePatches.assemble(r_list.values(), Analysis.regions[0]);

            for (final Region r : r_list.values()) {
                final ObjectProperties obj = new ObjectProperties(image, r, sx, sy, sz, p, osxy, osz, Analysis.regions[0]);
                obj.run();
            }
        }
        else {
            Analysis.setMaskbTworegions(A_solver.w3kbest[0]);
            float[][][] RiN = new float[p.nz][p.ni][p.nj];
            LocalTools.copytab(RiN, A_solver.Ri[0]);
            float[][][] RoN = new float[p.nz][p.ni][p.nj];
            LocalTools.copytab(RoN, A_solver.Ro[0]);

            final ArrayList<Region> regions = A_solver.regionsvoronoi;

            if (p.findregionthresh) {
                Analysis.compute_connected_regions_b(255 * p.thresh);
            }
            else {
                Analysis.compute_connected_regions_b(255 * p.thresh);
            }

            if (Analysis.p.refinement) {
                Analysis.SetRegionsObjsVoronoi(Analysis.regionslist[1], regions, RiN);
                IJ.showStatus("Computing segmentation  " + 55 + "%");
                IJ.showProgress(0.55);

                final ImagePatches ipatches = new ImagePatches(p, Analysis.regionslist[1], image, channel, A_solver.w3kbest[0], min, max);
                A_solver = null;
                ipatches.run();
                Analysis.regionslist[1] = ipatches.getRegionsList();
                Analysis.regions[1] = ipatches.getRegions();
            }

            // Here we solved the patches and the regions that come from the patches
            // we rescale the intensity to the original one
            for (final Region r : Analysis.regionslist[1]) {
                r.intensity = r.intensity * (max - min) + min;
            }
        }

        // correct the level number
        p.nlevels = 2;
        DoneSignal.countDown();
    }

    /**
     * Merge the soft mask
     * @param A_solver the solver used to produce the soft mask
     */
    private void mergeSoftMask(ASplitBregmanSolver A_solver) {
        System.out.println("============ mergeSoftMask" + channel );
        System.out.println(Debug.getArrayDims(out_soft_mask));
        System.out.println(Debug.getArrayDims(A_solver.w3k));
        
        // TODO: Added temporarily to since soft mask for channel 2 is not existing ;
        if (channel > 0) return;
        
        if (p.dispSoftMask) {
                out_soft_mask[channel] = md.generateImgFromArray(A_solver.w3k[channel], "Mask" + ((channel == 0) ? "X" : "Y"));
        }
    }
}
