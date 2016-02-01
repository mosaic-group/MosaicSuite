package mosaic.bregman.segmentation;


import java.util.ArrayList;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Resizer;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mosaic.bregman.Parameters;
import mosaic.utils.ArrayOps;
import mosaic.utils.ArrayOps.MinMax;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.DatasetTools;


class AnalysePatch implements Runnable {
    // size of original image 
    private final int iSizeOrigX;
    private final int iSizeOrigY;
    private final int iSizeOrigZ;

    // coordinate of patch in original image
    private int iOffsetOrigX;
    private int iOffsetOrigY;
    private int iOffsetOrigZ;
    
    // size of patch (oversampled)
    private int iSizeOversX;
    private int iSizeOversY;
    private int iSizeOversZ;
    
    // interpolated object sizes (interpolated and oversampled)
    private int iSizeOverInterX;
    private int iSizeOverInterY;
    private int iSizeOverInterZ;
    
    private int iInterpolationXY;
    private int iInterpolationZ;
    
    private int iOversamplingInXY;
    private int iOverInterInXY; 
    private int iOversamplingInZ;
    private int iOverInterInZ;
    
    // Input parameters
    final Region iInputRegion;
    private final ImagePatches iImagePatches;

    private final Tools iLocalTools;
    private final Parameters iParameters;
    
    final double iIntensityMin;
    final double iIntensityMax;
    private final double iScaledIntensityMin;
    
    private boolean border_attained = false;
    private boolean objectFound = false;
    private double[][][] object;
    
    private final double[][][] iRegionMask;
    
    double cin, cout; 
    private double cout_front;// estimated intensities
    private double min_thresh;
    private final double[][][] iPatch;
    private final double[][][] mask;// nregions nslices ni nj
    private double rescaled_min_int_all;
    private final double[][][] w3kpatch;
    private double t_high;
    private ASplitBregmanSolver A_solver;
    private final double[] clBetaMleIntensities = new double[2];
    
    // Temporary buffers for RSS and computeEnergyPSF_weighted methods
    private final double[][][] temp1;
    private final double[][][] temp2;
    private final double[][][] temp3;

    private final double lreg_patch;
    private final double iMinIntensity;
    
    /**
     * Create patches
     *
     * @param aInputImage Image
     * @param aInputRegion Region
     * @param aParameters Paramenters for split Bregman
     * @param aOversampling level of overlampling
     * @param aChannel ?
     * @param regionsf ?
     * @param aImagePatches ?
     */
    AnalysePatch(double[][][] aInputImage, Region aInputRegion, Parameters aParameters, int aOversampling, ImagePatches aImagePatches, double[][][] w3kbest, double aLreg, double aMinIntensity) {
        iSizeOrigX = aInputImage[0].length;
        iSizeOrigY = aInputImage[0][0].length;
        iSizeOrigZ = aInputImage.length;

        iInputRegion = aInputRegion;
        iParameters = aParameters;
        iImagePatches = aImagePatches;
        
        cout = 0;
        cin = 1;

        // Setup patch margins
        // old comment: check that the margin is at least 8 time bigger than the PSF
        int margin = 6;
        int zmargin = 1;// was 2
        final int[] sz_psf = iParameters.PSF.getSuggestedImageSize();
        if (sz_psf[0] > margin) {
            margin = sz_psf[0];
        }
        if (sz_psf[1] > margin) {
            margin = sz_psf[1];
        }
        if (sz_psf.length > 2 && sz_psf[2] > margin) { // TODO: shouldn't compare with zmargin?
            zmargin = sz_psf[2];
        }

        // compute patch geometry
        computePatchGeometry(aInputRegion, aOversampling, aParameters.interpolation, margin, zmargin);
        
        // create weights mask (binary)
        iRegionMask = generateMask(aInputRegion.rvoronoi, true);// mask for voronoi region into weights

        // create patch image with oversampling
        iPatch = new double[iSizeOversZ][iSizeOversX][iSizeOversY];
        fill_patch(aInputImage, iPatch, iRegionMask, iOversamplingInXY, iOversamplingInZ, iOffsetOrigX, iOffsetOrigY, iOffsetOrigZ);

        // for testing
        w3kpatch = new double[iSizeOversZ][iSizeOversX][iSizeOversY];
        fill_patch(w3kbest, w3kpatch, iRegionMask, iOversamplingInXY, iOversamplingInZ, iOffsetOrigX, iOffsetOrigY, iOffsetOrigZ);
        
        // create object (for result)
        object = new double[iSizeOversZ][iSizeOversX][iSizeOversY];

        // create mask
        mask = generateMask(aInputRegion, false);

        iLocalTools = new Tools(iSizeOversX, iSizeOversY, iSizeOversZ);
        
        // normalize
        MinMax<Double> minMax = ArrayOps.normalize(iPatch);
        iIntensityMin = minMax.getMin();
        iIntensityMax = minMax.getMax();
        iMinIntensity = aMinIntensity;
        iScaledIntensityMin = ( iMinIntensity - iIntensityMin) / (iIntensityMax - iIntensityMin);
        if (iParameters.mode_intensity == 3) {
            ArrayOps.normalize(w3kpatch[0]);
        }

        rescaled_min_int_all = iMinIntensity / 0.99; // first val with ~3% margin
        // (15 % compensated in find_best_t_and_int...)

        temp1 = new double[iSizeOversZ][iSizeOversX][iSizeOversY];
        temp2 = new double[iSizeOversZ][iSizeOversX][iSizeOversY];
        temp3 = new double[iSizeOversZ][iSizeOversX][iSizeOversY];
        
        lreg_patch = aLreg * aOversampling;
        
        // estimate ints
        if (iParameters.mode_intensity == 0) {
            find_best_thresh_and_int(w3kpatch);
        }
        else if (iParameters.mode_intensity == 1) {
            estimate_int_weighted(mask);
        }
        else if (iParameters.mode_intensity == 2) {
            estimate_int_clustering(1);// (-1 to correct for old numbering)
        }
        
        clBetaMleIntensities[0] = Math.max(cout, 0);
        clBetaMleIntensities[1] = Math.max(0.75 * (iMinIntensity - iIntensityMin) / (iIntensityMax - iIntensityMin), cin);

        if (iParameters.mode_intensity == 3) {
            clBetaMleIntensities[0] = iParameters.betaMLEoutdefault;
            clBetaMleIntensities[1] = 1;
            t_high = cin;
        }

        rescaled_min_int_all = Math.max(0, (iScaledIntensityMin - cout) / (cin - cout)); //cin
        if (iParameters.debug) {
            IJ.log(aInputRegion.value + "min all " + rescaled_min_int_all);
        }
    }

    /**
     * Analyse one Patch
     * Or run SplitBregman segmentation solver on it
     */
    @Override
    public void run() {
        // Check the delta beta, if it is bigger than two ignore it, because
        // I cannot warrant stability
        if (Math.abs(clBetaMleIntensities[0] - clBetaMleIntensities[1]) > 2.0) {
            // reset
            clBetaMleIntensities[0] = iParameters.betaMLEoutdefault;
            clBetaMleIntensities[1] = iParameters.betaMLEindefault;
        }
        
        if (iSizeOversZ > 1) {
            A_solver = new ASplitBregmanSolverTwoRegions3DPSF(iParameters, iPatch, w3kpatch, this, clBetaMleIntensities[0], clBetaMleIntensities[1], lreg_patch, iMinIntensity);
        }
        else {
            A_solver = new ASplitBregmanSolverTwoRegions2DPSF(iParameters, iPatch, w3kpatch, this, clBetaMleIntensities[0], clBetaMleIntensities[1], lreg_patch, iMinIntensity);
        }

        try {
            A_solver.second_run();

            cout = A_solver.getBetaMLE()[0];
            cin = A_solver.getBetaMLE()[1];

            final int ll = iParameters.mode_intensity;
            if (ll == 0 || ll == 1) {
                min_thresh = rescaled_min_int_all * 0.96;
            }
            else {
                min_thresh = 0.25;
            }

            if (iParameters.debug) {
                IJ.log("region" + iInputRegion.value + "minth " + min_thresh);

            }
            double t = 0;
            if (iParameters.mode_intensity != 3) {
                t = find_best_thresh(A_solver.w3kbest);
            }

            if (iParameters.mode_intensity == 3)// mode high
            {
                estimate_int_clustering(2);// (-1

                t_high = cin;
                if (iParameters.debug) {
                    IJ.log("obj" + iInputRegion.value + " effective t:" + t_high);
                }
                t = t_high - 0.04;
            }

            if (iParameters.debug) {
                IJ.log("best thresh : " + t + "region" + iInputRegion.value);
            }
            set_object(A_solver.w3kbest, t);
            if (iInterpolationXY > 1) {
                object = createInterpolatedObject(A_solver.w3kbest, t);
            }

            // assemble result into full image
            assemble_patch();
        }
        catch (final InterruptedException ex) {
        }
    }

    private void set_object(double[][][] w3kbest, double aThreshold) {
        objectFound = false;
        border_attained = false;
        for (int z = 0; z < iSizeOversZ; z++) {
            for (int i = 0; i < iSizeOversX; i++) {
                for (int j = 0; j < iSizeOversY; j++) {
                    if (w3kbest[z][i][j] > aThreshold && iRegionMask[z][i][j] == 1) {
                        object[z][i][j] = 1;
                        objectFound = true;
                        if (iSizeOversZ <= 1) {
                            if ((i == 0 && iOffsetOrigX != 0) || (i == (iSizeOversX - 1) && (iOffsetOrigX + iSizeOversX / iOversamplingInXY) != iSizeOrigX) || 
                                (j == 0 && iOffsetOrigY != 0) || (j == (iSizeOversY - 1) && (iOffsetOrigY + iSizeOversY / iOversamplingInXY) != iSizeOrigY)) {
                                border_attained = true;
                            }
                        }
                    }
                    else {
                        object[z][i][j] = 0;
                    }
                }
            }
        }
    }

    /**
     * Compute the geometry of the patch
     * @param aInputRegion Region
     * @param aOversampling level of oversampling
     */
    private void computePatchGeometry(Region aInputRegion, int aOversampling, int aInterpolation, int aMarginXY, int aMarginZ) {
        Pix[] mm = aInputRegion.getMinMaxCoordinates();
        Pix min = mm[0]; Pix max = mm[1];
        int xmin = min.px;
        int ymin = min.py;
        int zmin = min.pz;
        int xmax = max.px;
        int ymax = max.py;
        int zmax = max.pz;
        
        // Adjust patch coordinates with margin and fit it into min/max coordinates
        xmin = Math.max(0, xmin - aMarginXY);
        xmax = Math.min(iSizeOrigX, xmax + aMarginXY + 1);
        ymin = Math.max(0, ymin - aMarginXY);
        ymax = Math.min(iSizeOrigY, ymax + aMarginXY + 1);
        if (iSizeOrigZ > 1) {
            zmin = Math.max(0, zmin - aMarginZ);
            zmax = Math.min(iSizeOrigZ, zmax + aMarginZ + 1);
        }

        iOffsetOrigX = xmin;
        iOffsetOrigY = ymin;
        iOffsetOrigZ = zmin;
        
        iOversamplingInXY = aOversampling;
        iSizeOversX = (xmax - xmin) * iOversamplingInXY;
        iSizeOversY = (ymax - ymin) * iOversamplingInXY;
        
        iInterpolationXY = aInterpolation;
        iSizeOverInterX = iSizeOversX * iInterpolationXY;
        iSizeOverInterY = iSizeOversY * iInterpolationXY;

        iOverInterInXY = iOversamplingInXY * iInterpolationXY;
        
        if (iSizeOrigZ == 1) {
            iOversamplingInZ = 1;
            iSizeOversZ = 1;
            iInterpolationZ = 1;
            iSizeOverInterZ = 1;
            iOverInterInZ = 1;
        }
        else {
            iOversamplingInZ = aOversampling;
            iSizeOversZ = (zmax - zmin) * iOversamplingInZ;
            iInterpolationZ = aInterpolation;
            iSizeOverInterZ = iSizeOversZ * iInterpolationZ;
            iOverInterInZ = iOversamplingInZ * iInterpolationZ;
        }
    }

    private void fill_patch(double[][][] aSourceImage, double[][][] aOutput, double[][][] aWeights, int aOversamplingXY, int aOversamplingZ, int aOffsetX, int aOffsetY, int aOffsetZ) {
        for (int z = 0; z < iSizeOversZ; z++) {
            for (int i = 0; i < iSizeOversX; i++) {
                for (int j = 0; j < iSizeOversY; j++) {
                    // aWeights are set to 0 or 1.
                    aOutput[z][i][j] = aWeights[z][i][j] * aSourceImage[z / aOversamplingZ + aOffsetZ][i / aOversamplingXY + aOffsetX][j / aOversamplingXY + aOffsetY];
                }
            }
        }
    }

    private void estimate_int_weighted(double[][][] mask) {
        Tools.normalizeAndConvolveMask(temp3, mask, iParameters.PSF, temp1, temp2);
        RegionStatisticsSolver RSS = new RegionStatisticsSolver(temp1, temp2, iPatch, iRegionMask, 10, iParameters.betaMLEoutdefault, iParameters.betaMLEindefault);
        RSS.eval(temp3 /* convolved mask */);

        cout = RSS.betaMLEout;
        cout_front = cout;
        cin = RSS.betaMLEin;

        if (iParameters.debug) {
            IJ.log("reg" + iInputRegion.value + "rescaled min int" + iScaledIntensityMin);
            IJ.log(String.format("Photometry patch:%n background %7.2e %n foreground %7.2e", cout, cin));
        }
    }

    private void estimate_int_clustering(int level) {
        int nk = (level == 1) ? 4 : 3;
        int nk_in = 2;
        
        final double[] pixel = new double[1];

        int cpt_vals = 0;
        final Dataset data = new DefaultDataset();
        for (int z = 0; z < iSizeOversZ; z++) {
            for (int i = 0; i < iSizeOversX; i++) {
                for (int j = 0; j < iSizeOversY; j++) {
                    pixel[0] = (level == 1) ? iPatch[z][i][j] : A_solver.w3kbest[z][i][j];
                    if (iRegionMask[z][i][j] == 1) {
                        data.add(new DenseInstance(pixel));
                        cpt_vals++;
                    }
                }
            }
        }

        if (cpt_vals > 3) {
            final Clusterer km = new KMeans(nk, 100);
            final Dataset[] data2 = km.cluster(data);
            nk = data2.length;// get number of clusters really found
            
            final double[] levels = new double[nk];
            for (int i = 0; i < nk; i++) {
                final Instance inst = DatasetTools.average(data2[i]);
                levels[i] = inst.value(0);
            }

            Arrays.sort(levels);
            int nk2 = Math.min(nk_in, nk - 1);
            cin = Math.max(iScaledIntensityMin, levels[nk2]);
            final int nkm1 = Math.max(nk2 - 1, 0);
            cout_front = levels[nkm1];
            cout = levels[0];
            if (level == 2) {
                cout = cout_front;
            }

            if (iParameters.debug) {
                IJ.log("rescaled min int" + iScaledIntensityMin);
                IJ.log(String.format("Photometry patch:%n background %7.2e %n foreground %7.2e", cout, cin));
                IJ.log("levels :");
                for (int i = 0; i < nk; i++) {
                    IJ.log("level r" + iInputRegion.value + " " + (i + 1) + " : " + levels[i]);
                }
            }
        }
        else {
            cin = 1;
            cout_front = iParameters.betaMLEoutdefault;
            cout = iParameters.betaMLEoutdefault;
        }
    }

    private double[][][] generateMask(Region r, boolean aCheckBoundaries) {
        double[][][] mask = new double[iSizeOversZ][iSizeOversX][iSizeOversY];

        for (final Pix p : r.pixels) {
            int rz = iOversamplingInXY * (p.pz - iOffsetOrigZ);
            int rx = iOversamplingInXY * (p.px - iOffsetOrigX);
            int ry = iOversamplingInXY * (p.py - iOffsetOrigY);
            if (aCheckBoundaries && (rz < 0 || rz + iOversamplingInZ > iSizeOversZ || rx < 0 || rx + iOversamplingInXY > iSizeOversX || ry < 0 || ry + iOversamplingInXY > iSizeOversY)) {
                continue;
            }
            
            for (int z = rz; z < rz + iOversamplingInZ; z++) {
                for (int i = rx; i < rx + iOversamplingInXY; i++) {
                    for (int j = ry; j < ry + iOversamplingInXY; j++) {
                        mask[z][i][j] = 1;
                    }
                }
            }
        }
        return mask;
    }

    /**
     * Here is calculated the best threshold for the patches
     *
     * @param w3kbest the mask
     * @return the best threshold based on energy calculation
     */
    private double find_best_thresh(double[][][] w3kbest) {
        double bestEenergy = Double.MAX_VALUE;
        double bestThreshold = 0.75;
        
        for (double currentThr = 1; currentThr > min_thresh; currentThr -= 0.02) {
            set_object(w3kbest, currentThr);

            if (objectFound && !border_attained) {
                double tempEnergy = iLocalTools.computeEnergyPSF_weighted(temp1, object, temp2, temp3, iRegionMask, iParameters.ldata, lreg_patch, iParameters.PSF, cout_front, cin, iPatch, iParameters.noise_model);
                if (tempEnergy < bestEenergy) {
                    bestEenergy = tempEnergy;
                    bestThreshold = currentThr;
                }
            }
        }
        
        return bestThreshold;
    }

    double find_best_thresh_and_int(double[][][] w3kbest) {
        double energy = Double.MAX_VALUE;
        double threshold = 0.75;
        double cin_previous = cin;
        double cout_previous = cout;
        double cinbest = 1;
        double coutbest = 0.0001;
        
        for (double thr = 0.95; thr > rescaled_min_int_all * 0.96; thr -= 0.02) {
            set_object(w3kbest, thr);
            if (objectFound && !border_attained) {
                estimate_int_weighted(object);
                double tempEnergy = iLocalTools.computeEnergyPSF_weighted(temp1, object, temp2, temp3, iRegionMask, iParameters.ldata, lreg_patch, iParameters.PSF, cout_front, cin, iPatch, iParameters.noise_model);
                if (tempEnergy < energy) {
                    energy = tempEnergy;
                    threshold = thr;
                    cinbest = cin;
                    coutbest = cout;
                }
            }
            else {
                cin = 1;
                cout_front = 0;
            }
        }

        cin = cinbest;
        cout = coutbest;
        cout_front = cout;

        if (!objectFound) {
            cin = cin_previous;
            cout = cout_previous;
        }

        rescaled_min_int_all = Math.max((iScaledIntensityMin - cout) / (cin - cout), 0);
        if (iParameters.debug) {
            IJ.log("fbest" + iInputRegion.value + "min all " + rescaled_min_int_all);
        }

        if (iParameters.debug) {
            IJ.log(" best energy and int " + energy + " t " + threshold + "region" + iInputRegion.value + " cin " + cin + " cout " + cout);
        }

        return threshold;
    }

    // build local object list
    private void assemble_patch() {
        final ImageStack maska_ims = new ImageStack(iSizeOverInterX, iSizeOverInterY);
        for (int z = 0; z < iSizeOverInterZ; z++) {
            final byte[] maska_bytes = new byte[iSizeOverInterX * iSizeOverInterY];
            for (int i = 0; i < iSizeOverInterX; i++) {
                for (int j = 0; j < iSizeOverInterY; j++) {
                    maska_bytes[j * iSizeOverInterX + i] = (object[z][i][j] >= 1) ? (byte)(object[z][i][j]) : 0;
                }
            }
            final ByteProcessor bp = new ByteProcessor(iSizeOverInterX, iSizeOverInterY, maska_bytes);
            maska_ims.addSlice("", bp);
        }
        final ImagePlus maska_im = new ImagePlus("test Mask vo2", maska_ims);

        final double thr = 0.5;
        final FindConnectedRegions fcr = new FindConnectedRegions(maska_im);
        fcr.run(iSizeOverInterX * iSizeOverInterY * iSizeOverInterZ, 0 * iOverInterInXY, (float)thr, iParameters.exclude_z_edges, iParameters.subpixel, iParameters.oversampling2ndstep, iParameters.interpolation);// min size was 5

        // add to list with critical section
        iImagePatches.addRegionsToList(fcr.getFoundRegions());

        // add to regions refined with correct indexes
        assemble(fcr.getFoundRegions());
    }

    private void assemble(ArrayList<Region> localList) {
        for (final Region r : localList) {
            final ArrayList<Pix> pixelsTemp = new ArrayList<Pix>(r.pixels.size());
            for (final Pix p : r.pixels) {
                pixelsTemp.add(new Pix(p.pz + iOffsetOrigZ * iOverInterInZ, p.px + iOffsetOrigX * iOverInterInXY, p.py + iOffsetOrigY * iOverInterInXY));
            }
            r.pixels = pixelsTemp;
        }
    }

    private double[][][] createInterpolatedObject(double[][][] aInputData, double aThreshold) {
        // Build non interpolated image
        ImageStack imgStack = new ImageStack(iSizeOversX, iSizeOversY);
        for (int z = 0; z < iSizeOversZ; z++) {
            final float[] pixels = new float[iSizeOversX * iSizeOversY];
            for (int i = 0; i < iSizeOversX; i++) {
                for (int j = 0; j < iSizeOversY; j++) {
                    pixels[j * iSizeOversX + i] = (float) aInputData[z][i][j];
                }
            }
            final FloatProcessor fp = new FloatProcessor(iSizeOversX, iSizeOversY, pixels);
            imgStack.addSlice("", fp);
        }
        ImagePlus interpolatedImg = new ImagePlus("Object x", imgStack);
        
        // Interpolate in Z and XY planes
        if (iSizeOverInterZ != iSizeOversZ) {
            interpolatedImg = new Resizer().zScale(interpolatedImg, iSizeOverInterZ, ImageProcessor.BILINEAR);
        }
        final ImageStack imgStackInter = new ImageStack(iSizeOverInterX, iSizeOverInterY);
        for (int z = 0; z < iSizeOverInterZ; z++) {
            interpolatedImg.setSliceWithoutUpdate(z + 1);
            interpolatedImg.getProcessor().setInterpolationMethod(ImageProcessor.BILINEAR);
            imgStackInter.addSlice("", interpolatedImg.getProcessor().resize(iSizeOverInterX, iSizeOverInterY, true));
        }
        interpolatedImg.setStack(imgStackInter);

        // put thresholded data into interpolated object 
        double[][][] interpolated_object = new double[iSizeOverInterZ][iSizeOverInterX][iSizeOverInterY];
        for (int z = 0; z < iSizeOverInterZ; z++) {
            interpolatedImg.setSlice(z + 1);
            ImageProcessor imp = interpolatedImg.getProcessor();
            for (int i = 0; i < iSizeOverInterX; i++) {
                for (int j = 0; j < iSizeOverInterY; j++) {
                    if (imp.getPixelValue(i, j) > aThreshold && iRegionMask[z / iInterpolationZ][i / iInterpolationXY][j / iInterpolationXY] == 1) {
                        interpolated_object[z][i][j] = 1;
                    }
                    else {
                        interpolated_object[z][i][j] = 0;
                    }
                }
            }
        }

        return interpolated_object;
    }
}
