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
import mosaic.bregman.segmentation.SegmentationParameters.IntensityMode;
import mosaic.core.psf.psf;
import mosaic.utils.ArrayOps;
import mosaic.utils.ArrayOps.MinMax;
import net.imglib2.type.numeric.real.DoubleType;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.DatasetTools;


class AnalysePatch {
    // size of original image 
    private final int iSizeOrigX;
    private final int iSizeOrigY;
    private final int iSizeOrigZ;

    // coordinate of patch in original image
    private int iOffsetOrigX;
    private int iOffsetOrigY;
    private int iOffsetOrigZ;
    
    // size of patch (oversampled)
    private int iSizeOverX;
    private int iSizeOverY;
    private int iSizeOverZ;
    
    // interpolated object sizes (interpolated and oversampled)
    private int iSizeOverInterX;
    private int iSizeOverInterY;
    private int iSizeOverInterZ;
    
    private int iInterpolationXY;
    private int iInterpolationZ;
    private int iOversamplingXY;
    private int iOverInterXY; 
    private int iOversamplingZ;
    private int iOverInterZ;
    
    // Input parameters
    final Region iInputRegion;

    private final Tools iLocalTools;
    private final SegmentationParameters iParameters;
    
    final double iMinObjectIntensity;
    final double iIntensityMin;
    final double iIntensityMax;
    private final double iScaledIntensityMin;
    
    private boolean border_attained = false;
    private boolean objectFound = false;
    private double[][][] result;
    
    private final double[][][] iRegionMask;
    
    double cin, cout; 
    private double cout_front;// estimated intensities
    private double min_thresh;
    private final double[][][] iPatch;
    private double rescaled_min_int_all;
    private final double[][][] w3kpatch;
    private double t_high;
    private final double[] clBetaMleIntensities = new double[2];
    
    // Temporary buffers for RSS and computeEnergyPSF_weighted methods
    private final double[][][] temp1;
    private final double[][][] temp2;
    private final double[][][] temp3;

    private final double iRegulariztionPatch;
    private final psf<DoubleType> iPsf;
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
    AnalysePatch(double[][][] aInputImage, Region aInputRegion, SegmentationParameters aParameters, int aOversampling, double[][][] w3kbest, double aRegularization, double aMinObjectIntensity,  psf<DoubleType> aPsf) {
        iSizeOrigX = aInputImage[0].length;
        iSizeOrigY = aInputImage[0][0].length;
        iSizeOrigZ = aInputImage.length;

        iInputRegion = aInputRegion;
        iParameters = aParameters;
        iPsf = aPsf;
        cout = 0;
        cin = 1;

        // Setup patch margins
        // old comment: check that the margin is at least 8 time bigger than the PSF
        int margin = 6;
        int zmargin = 1;// was 2
        final int[] sz_psf = iPsf.getSuggestedImageSize();
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
        computePatchGeometry(aInputRegion, aOversampling, iParameters.interpolation, margin, zmargin);
        
        // create weights mask (binary)
        iRegionMask = generateMask(aInputRegion.rvoronoi, true);// mask for voronoi region into weights

        // create patch image with oversampling
        iPatch = new double[iSizeOverZ][iSizeOverX][iSizeOverY];
        fill_patch(aInputImage, iPatch, iRegionMask, iOversamplingXY, iOversamplingZ, iOffsetOrigX, iOffsetOrigY, iOffsetOrigZ);

        // for testing
        w3kpatch = new double[iSizeOverZ][iSizeOverX][iSizeOverY];
        fill_patch(w3kbest, w3kpatch, iRegionMask, iOversamplingXY, iOversamplingZ, iOffsetOrigX, iOffsetOrigY, iOffsetOrigZ);
        
        // create object (for result)
        result = new double[iSizeOverZ][iSizeOverX][iSizeOverY];

        // create mask
        double[][][] mask = generateMask(aInputRegion, false);

        iLocalTools = new Tools(iSizeOverX, iSizeOverY, iSizeOverZ);
        
        // normalize
        MinMax<Double> minMax = ArrayOps.normalize(iPatch);
        iIntensityMin = minMax.getMin();
        iIntensityMax = minMax.getMax();
        iMinObjectIntensity = aMinObjectIntensity;
        iScaledIntensityMin = ( iMinObjectIntensity - iIntensityMin) / (iIntensityMax - iIntensityMin);
        if (iParameters.intensityMode == IntensityMode.HIGH) {
            ArrayOps.normalize(w3kpatch[0]);
        }

        rescaled_min_int_all = iMinObjectIntensity / 0.99;

        temp1 = new double[iSizeOverZ][iSizeOverX][iSizeOverY];
        temp2 = new double[iSizeOverZ][iSizeOverX][iSizeOverY];
        temp3 = new double[iSizeOverZ][iSizeOverX][iSizeOverY];
        
        iRegulariztionPatch = aRegularization * aOversampling;
        
        // estimate ints
        if (iParameters.intensityMode == IntensityMode.AUTOMATIC) {
            find_best_thresh_and_int(w3kpatch);
        }
        else if (iParameters.intensityMode == IntensityMode.LOW) {
            estimate_int_weighted(mask);
        }
        else if (iParameters.intensityMode == IntensityMode.MEDIUM) {
            estimate_int_clustering(iPatch, 4, false);
        }
        
        clBetaMleIntensities[0] = Math.max(cout, 0);
        clBetaMleIntensities[1] = Math.max(0.75 * (iMinObjectIntensity - iIntensityMin) / (iIntensityMax - iIntensityMin), cin);

        if (iParameters.intensityMode == IntensityMode.HIGH) {
            clBetaMleIntensities[0] = iParameters.defaultBetaMleOut;
            clBetaMleIntensities[1] = 1;
            t_high = cin;
        }

        rescaled_min_int_all = Math.max(0, (iScaledIntensityMin - cout) / (cin - cout));
        if (iParameters.debug) {
            IJ.log(aInputRegion.iLabel + "min all " + rescaled_min_int_all);
        }
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
                estimate_int_weighted(result);
                double tempEnergy = iLocalTools.computeEnergyPSF_weighted(temp1, result, temp2, temp3, iRegionMask, iParameters.ldata, iRegulariztionPatch, iPsf, cout_front, cin, iPatch, iParameters.noiseModel);
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
            IJ.log("fbest" + iInputRegion.iLabel + "min all " + rescaled_min_int_all);
        }
    
        if (iParameters.debug) {
            IJ.log(" best energy and int " + energy + " t " + threshold + "region" + iInputRegion.iLabel + " cin " + cin + " cout " + cout);
        }
    
        return threshold;
    }

    private void estimate_int_weighted(double[][][] mask) {
        Tools.normalizeAndConvolveMask(temp3, mask, iPsf, temp1, temp2);
        RegionStatisticsSolver RSS = new RegionStatisticsSolver(temp1, temp2, iPatch, iRegionMask, 10, iParameters.defaultBetaMleOut, iParameters.defaultBetaMleIn);
        RSS.eval(temp3 /* convolved mask */);
    
        cout = RSS.betaMLEout;
        cout_front = cout;
        cin = RSS.betaMLEin;
    
        if (iParameters.debug) {
            IJ.log("reg" + iInputRegion.iLabel + "rescaled min int" + iScaledIntensityMin);
            IJ.log(String.format("Photometry patch:%n background %7.2e %n foreground %7.2e", cout, cin));
        }
    }
    // 2 - A_solver.w3kbest, 3, aUpdateCout true
    // 1 - iPatch, 4
    private void estimate_int_clustering(double[][][] aValues, int aNumOfClasters, boolean aUpdateCout) {
        
        final double[] pixel = new double[1];
        int cpt_vals = 0;
        final Dataset data = new DefaultDataset();
        for (int z = 0; z < iSizeOverZ; z++) {
            for (int i = 0; i < iSizeOverX; i++) {
                for (int j = 0; j < iSizeOverY; j++) {
                    pixel[0] = aValues[z][i][j];
                    if (iRegionMask[z][i][j] == 1) {
                        data.add(new DenseInstance(pixel));
                        cpt_vals++;
                    }
                }
            }
        }
    
        if (cpt_vals > 3) {
            final Clusterer km = new KMeans(aNumOfClasters, 100);
            final Dataset[] data2 = km.cluster(data);
            int nk = data2.length;// get number of clusters really found
            
            final double[] levels = new double[nk];
            for (int i = 0; i < nk; i++) {
                final Instance inst = DatasetTools.average(data2[i]);
                levels[i] = inst.value(0);
            }
    
            Arrays.sort(levels);
            int nk_in = 2;
            int nk2 = Math.min(nk_in, nk - 1);
            cin = Math.max(iScaledIntensityMin, levels[nk2]);
            final int nkm1 = Math.max(nk2 - 1, 0);
            cout_front = levels[nkm1];
            cout = levels[0];
            if (aUpdateCout) {
                cout = cout_front;
            }
    
            if (iParameters.debug) {
                IJ.log("rescaled min int" + iScaledIntensityMin);
                IJ.log(String.format("Photometry patch:%n background %7.2e %n foreground %7.2e", cout, cin));
                IJ.log("levels :");
                for (int i = 0; i < nk; i++) {
                    IJ.log("level r" + iInputRegion.iLabel + " " + (i + 1) + " : " + levels[i]);
                }
            }
        }
        else {
            cin = 1;
            cout_front = iParameters.defaultBetaMleOut;
            cout = iParameters.defaultBetaMleOut;
        }
    }

    /**
     * Analyse one Patch
     * Or run SplitBregman segmentation solver on it
     * @return 
     */
    public ArrayList<Region> calculateRegions() {
        // Check the delta beta, if it is bigger than two ignore it, because I cannot warrant stability
        if (Math.abs(clBetaMleIntensities[0] - clBetaMleIntensities[1]) > 2.0) {
            clBetaMleIntensities[0] = iParameters.defaultBetaMleOut;
            clBetaMleIntensities[1] = iParameters.defaultBetaMleIn;
        }
        ASplitBregmanSolver A_solver = (iSizeOverZ > 1)
                ? new ASplitBregmanSolverTwoRegions3DPSF(iParameters, iPatch, w3kpatch, this, clBetaMleIntensities[0], clBetaMleIntensities[1], iRegulariztionPatch, iPsf)
                : new ASplitBregmanSolverTwoRegions2DPSF(iParameters, iPatch, w3kpatch, this, clBetaMleIntensities[0], clBetaMleIntensities[1], iRegulariztionPatch, iPsf);

        A_solver.second_run();

        cout = A_solver.getBetaMLE()[0];
        cin = A_solver.getBetaMLE()[1];

        if (iParameters.intensityMode == IntensityMode.AUTOMATIC || iParameters.intensityMode == IntensityMode.LOW) {
            min_thresh = rescaled_min_int_all * 0.96;
        }
        else {
            min_thresh = 0.25;
        }

        if (iParameters.debug) {
            IJ.log("region" + iInputRegion.iLabel + "minth " + min_thresh);

        }
        double t = 0;
        if (iParameters.intensityMode == IntensityMode.HIGH) {
            estimate_int_clustering(A_solver.w3kbest, 3, true);

            t_high = cin;
            if (iParameters.debug) {
                IJ.log("obj" + iInputRegion.iLabel + " effective t:" + t_high);
            }
            t = t_high - 0.04;
        }
        else {
            t = find_best_thresh(A_solver.w3kbest);
        }

        if (iParameters.debug) {
            IJ.log("best thresh : " + t + "region" + iInputRegion.iLabel);
        }
        set_object(A_solver.w3kbest, t);
        if (iInterpolationXY > 1) {
            result = createInterpolatedObject(A_solver.w3kbest, t);
        }

        // assemble result into full image
        return assemble_patch();
    }

    private void set_object(double[][][] w3kbest, double aThreshold) {
        objectFound = false;
        border_attained = false;
        for (int z = 0; z < iSizeOverZ; z++) {
            for (int i = 0; i < iSizeOverX; i++) {
                for (int j = 0; j < iSizeOverY; j++) {
                    if (w3kbest[z][i][j] > aThreshold && iRegionMask[z][i][j] == 1) {
                        result[z][i][j] = 1;
                        objectFound = true;
                        if (iSizeOverZ <= 1) {
                            if ((i == 0 && iOffsetOrigX != 0) || (i == (iSizeOverX - 1) && (iOffsetOrigX + iSizeOverX / iOversamplingXY) != iSizeOrigX) || 
                                (j == 0 && iOffsetOrigY != 0) || (j == (iSizeOverY - 1) && (iOffsetOrigY + iSizeOverY / iOversamplingXY) != iSizeOrigY)) {
                                border_attained = true;
                            }
                        }
                    }
                    else {
                        result[z][i][j] = 0;
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
        
        iOversamplingXY = aOversampling;
        iSizeOverX = (xmax - xmin) * iOversamplingXY;
        iSizeOverY = (ymax - ymin) * iOversamplingXY;
        
        iInterpolationXY = aInterpolation;
        iSizeOverInterX = iSizeOverX * iInterpolationXY;
        iSizeOverInterY = iSizeOverY * iInterpolationXY;

        iOverInterXY = iOversamplingXY * iInterpolationXY;
        
        if (iSizeOrigZ == 1) {
            iOversamplingZ = 1;
            iSizeOverZ = 1;
            iInterpolationZ = 1;
            iSizeOverInterZ = 1;
            iOverInterZ = 1;
        }
        else {
            iOversamplingZ = aOversampling;
            iSizeOverZ = (zmax - zmin) * iOversamplingZ;
            iInterpolationZ = aInterpolation;
            iSizeOverInterZ = iSizeOverZ * iInterpolationZ;
            iOverInterZ = iOversamplingZ * iInterpolationZ;
        }
    }

    private void fill_patch(double[][][] aSourceImage, double[][][] aOutput, double[][][] aWeights, int aOversamplingXY, int aOversamplingZ, int aOffsetX, int aOffsetY, int aOffsetZ) {
        for (int z = 0; z < iSizeOverZ; z++) {
            for (int i = 0; i < iSizeOverX; i++) {
                for (int j = 0; j < iSizeOverY; j++) {
                    // aWeights are set to 0 or 1.
                    aOutput[z][i][j] = aWeights[z][i][j] * aSourceImage[z / aOversamplingZ + aOffsetZ][i / aOversamplingXY + aOffsetX][j / aOversamplingXY + aOffsetY];
                }
            }
        }
    }

    private double[][][] generateMask(Region r, boolean aCheckBoundaries) {
        double[][][] mask = new double[iSizeOverZ][iSizeOverX][iSizeOverY];

        for (final Pix p : r.iPixels) {
            int rz = iOversamplingXY * (p.pz - iOffsetOrigZ);
            int rx = iOversamplingXY * (p.px - iOffsetOrigX);
            int ry = iOversamplingXY * (p.py - iOffsetOrigY);
            if (aCheckBoundaries && (rz < 0 || rz + iOversamplingZ > iSizeOverZ || rx < 0 || rx + iOversamplingXY > iSizeOverX || ry < 0 || ry + iOversamplingXY > iSizeOverY)) {
                continue;
            }
            
            for (int z = rz; z < rz + iOversamplingZ; z++) {
                for (int i = rx; i < rx + iOversamplingXY; i++) {
                    for (int j = ry; j < ry + iOversamplingXY; j++) {
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
                double tempEnergy = iLocalTools.computeEnergyPSF_weighted(temp1, result, temp2, temp3, iRegionMask, iParameters.ldata, iRegulariztionPatch, iPsf, cout_front, cin, iPatch, iParameters.noiseModel);
                if (tempEnergy < bestEenergy) {
                    bestEenergy = tempEnergy;
                    bestThreshold = currentThr;
                }
            }
        }
        
        return bestThreshold;
    }

    // build local object list
    private ArrayList<Region> assemble_patch() {
        final ImageStack maska_ims = new ImageStack(iSizeOverInterX, iSizeOverInterY);
        for (int z = 0; z < iSizeOverInterZ; z++) {
            final byte[] maska_bytes = new byte[iSizeOverInterX * iSizeOverInterY];
            for (int i = 0; i < iSizeOverInterX; i++) {
                for (int j = 0; j < iSizeOverInterY; j++) {
                    maska_bytes[j * iSizeOverInterX + i] = (result[z][i][j] >= 1) ? (byte)(result[z][i][j]) : 0;
                }
            }
            final ByteProcessor bp = new ByteProcessor(iSizeOverInterX, iSizeOverInterY, maska_bytes);
            maska_ims.addSlice("", bp);
        }
        final ImagePlus maska_im = new ImagePlus("test Mask vo2", maska_ims);

        final FindConnectedRegions fcr = new FindConnectedRegions(maska_im);
        fcr.run(iSizeOverInterX * iSizeOverInterY * iSizeOverInterZ, 0 * iOverInterXY, /* threshold */ 0.5f, iParameters.excludeEdgesZ, iOversamplingXY, iInterpolationXY);

        // add to regions refined with correct indexes
        final ArrayList<Region> foundRegions = fcr.getFoundRegions();
        rescalePixelPositions(foundRegions);

        return foundRegions;
    }

    private void rescalePixelPositions(ArrayList<Region> localList) {
        for (final Region r : localList) {
            for (final Pix p : r.iPixels) {
                p.pz = p.pz + iOffsetOrigZ * iOverInterZ;
                p.px = p.px + iOffsetOrigX * iOverInterXY;
                p.py = p.py + iOffsetOrigY * iOverInterXY;
            }
        }
    }

    private double[][][] createInterpolatedObject(double[][][] aInputData, double aThreshold) {
        // Build non interpolated image
        ImageStack imgStack = new ImageStack(iSizeOverX, iSizeOverY);
        for (int z = 0; z < iSizeOverZ; z++) {
            final float[] pixels = new float[iSizeOverX * iSizeOverY];
            for (int i = 0; i < iSizeOverX; i++) {
                for (int j = 0; j < iSizeOverY; j++) {
                    pixels[j * iSizeOverX + i] = (float) aInputData[z][i][j];
                }
            }
            final FloatProcessor fp = new FloatProcessor(iSizeOverX, iSizeOverY, pixels);
            imgStack.addSlice("", fp);
        }
        ImagePlus interpolatedImg = new ImagePlus("Object x", imgStack);
        
        // Interpolate in Z and XY planes
        if (iSizeOverInterZ != iSizeOverZ) {
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
