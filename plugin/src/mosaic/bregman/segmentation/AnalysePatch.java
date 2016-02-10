package mosaic.bregman.segmentation;


import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

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
import mosaic.utils.ImgUtils;
import mosaic.utils.ArrayOps.MinMax;
import net.imglib2.type.numeric.real.DoubleType;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.DatasetTools;


class AnalysePatch {
    private static final Logger logger = Logger.getLogger(AnalysePatch.class);
    
    // size of original image 
    private final int iSizeOrigX;
    private final int iSizeOrigY;
    private final int iSizeOrigZ;

    // coordinate of patch in original image
    private int iOffsetOrigX;
    private int iOffsetOrigY;
    private int iOffsetOrigZ;
    
    // geometry and scaling of patch
    private int iInterpolationXY;
    private int iInterpolationZ;
    private int iOversamplingXY;
    private int iOversamplingZ;
    private int iOverInterXY; 
    private int iOverInterZ;
    private int iSizeOverX;
    private int iSizeOverY;
    private int iSizeOverZ;
    private int iSizeOverInterX;
    private int iSizeOverInterY;
    private int iSizeOverInterZ;
       
    // Input parameters
    final Region iInputRegion;
    private final SegmentationParameters iParameters;
    private final psf<DoubleType> iPsf;

    private final Tools iLocalTools;
    private final double iRegulariztionPatch;
    
    private final double[][][] iRegionMask;
    private final double[][][] iPatch;
    private final double[][][] w3kpatch;
    private double[][][] result;
    
    private final double iMinObjectIntensity;
    private final double iIntensityMin;
    private final double iIntensityMax;
    final double iNormalizedMinObjectIntensity;
    private double iRescaledMinIntensityAll;
    double cin, cout_front, cout; 
    private final double[] betaMleIntensities = new double[2];
    
    // Temporary buffers for RSS and computeEnergyPSF_weighted methods
    private final double[][][] temp1;
    private final double[][][] temp2;
    private final double[][][] temp3;

    AnalysePatch(double[][][] aInputImage, Region aInputRegion, SegmentationParameters aParameters, int aOversampling, double[][][] w3kbest, double aRegularization, double aMinObjectIntensity,  psf<DoubleType> aPsf) {
        iSizeOrigX = aInputImage[0].length;
        iSizeOrigY = aInputImage[0][0].length;
        iSizeOrigZ = aInputImage.length;

        iInputRegion = aInputRegion;
        iParameters = aParameters;
        iPsf = aPsf;

        computePatchGeometry(iInputRegion, aOversampling, iParameters.interpolation);
        iLocalTools = new Tools(iSizeOverX, iSizeOverY, iSizeOverZ);
        
        iRegionMask = generateMask(iInputRegion.rvoronoi, true);
        iPatch = generateFromPatchArea(aInputImage, iRegionMask, iOversamplingXY, iOversamplingZ, iOffsetOrigX, iOffsetOrigY, iOffsetOrigZ);
        w3kpatch = generateFromPatchArea(w3kbest, iRegionMask, iOversamplingXY, iOversamplingZ, iOffsetOrigX, iOffsetOrigY, iOffsetOrigZ);
        result = new double[iSizeOverZ][iSizeOverX][iSizeOverY];
        iRegulariztionPatch = aRegularization * iOversamplingXY;
        iMinObjectIntensity = aMinObjectIntensity;
        
        temp1 = new double[iSizeOverZ][iSizeOverX][iSizeOverY];
        temp2 = new double[iSizeOverZ][iSizeOverX][iSizeOverY];
        temp3 = new double[iSizeOverZ][iSizeOverX][iSizeOverY];

        double[][][] mask = generateMask(iInputRegion, false);
        
        if (iParameters.debug) {
//            ImgUtils.ZXYarrayToImg(iRegionMask, "iRegionMask_" + aInputRegion.iLabel).show();
//            ImgUtils.ZXYarrayToImg(iPatch, "iPatch_" + aInputRegion.iLabel).show();
//            ImgUtils.ZXYarrayToImg(w3kpatch, "w3kpatch_" + aInputRegion.iLabel).show();
//            ImgUtils.ZXYarrayToImg(mask, "mask_" + aInputRegion.iLabel).show();
        }
        
        MinMax<Double> minMax = ArrayOps.normalize(iPatch);
        iIntensityMin = minMax.getMin();
        iIntensityMax = minMax.getMax();
        iNormalizedMinObjectIntensity = (iMinObjectIntensity - iIntensityMin) / (iIntensityMax - iIntensityMin);
        
        iRescaledMinIntensityAll = iMinObjectIntensity / 0.99;
        
        // estimate intensities
        cout = 0;
        cin = 1;
        if (iParameters.intensityMode == IntensityMode.AUTOMATIC) {
            estimateIntensity(w3kpatch);
        }
        else if (iParameters.intensityMode == IntensityMode.LOW) {
            estimateIntensityRSS(mask);
        }
        else if (iParameters.intensityMode == IntensityMode.MEDIUM) {
            estimateIntensityClustering(iPatch, 4, false);
        }
        
        betaMleIntensities[0] = Math.max(cout, 0);
        betaMleIntensities[1] = Math.max(0.75 * iNormalizedMinObjectIntensity, cin);

        if (iParameters.intensityMode == IntensityMode.HIGH) {
            ArrayOps.normalize(w3kpatch);
            betaMleIntensities[0] = iParameters.defaultBetaMleOut;
            betaMleIntensities[1] = 1;
        }

        iRescaledMinIntensityAll = Math.max(0, (iNormalizedMinObjectIntensity - cout) / (cin - cout));
        logger.debug("Photometry for region " + iInputRegion.iLabel + ": out=" + cout + ", outFront=" + cout_front + ", cin=" + cin + ", NormalizedMinObjectIntensity=" + iNormalizedMinObjectIntensity);
    }

    ArrayList<Region> calculateRegions() {
        // Check the delta beta, if it is bigger than two ignore it, because I cannot warrant stability
        if (Math.abs(betaMleIntensities[0] - betaMleIntensities[1]) > 2.0) {
            betaMleIntensities[0] = iParameters.defaultBetaMleOut;
            betaMleIntensities[1] = iParameters.defaultBetaMleIn;
        }
        ASplitBregmanSolver solver = (iSizeOverZ > 1)
                ? new ASplitBregmanSolver3D(iParameters, iPatch, w3kpatch, this, betaMleIntensities[0], betaMleIntensities[1], iRegulariztionPatch, iPsf)
                : new ASplitBregmanSolver2D(iParameters, iPatch, w3kpatch, this, betaMleIntensities[0], betaMleIntensities[1], iRegulariztionPatch, iPsf);
    
        solver.second_run();
    
        cin = solver.getBetaMleIn();
    
        double threshold = 0;
        if (iParameters.intensityMode == IntensityMode.HIGH) {
            estimateIntensityClustering(solver.w3kbest, 3, true);
            threshold = cin - 0.04;
        }
        else {
            double minThreshold = (iParameters.intensityMode == IntensityMode.MEDIUM) ? 0.25 : iRescaledMinIntensityAll * 0.96;
            threshold = findBestThreshold(solver.w3kbest, minThreshold);
        }
        logger.debug("Best found threshold: " + threshold + " in region: " + iInputRegion.iLabel);
        
        generateThresholdedObject(solver.w3kbest, threshold);
        if (iInterpolationXY > 1) {
            result = createInterpolatedObject(solver.w3kbest, threshold);
        }
    
        // assemble result into full image
        return generateRegions();
    }

    void estimateIntensity(double[][][] w3kbest) {
        double bestEnergy = Double.MAX_VALUE;
        double cinbest = cin;
        double coutbest = cout;
        
        for (double thr = 0.95; thr > iRescaledMinIntensityAll * 0.96; thr -= 0.02) {
            if (generateThresholdedObject(w3kbest, thr)) {
                estimateIntensityRSS(result);
                double tempEnergy = iLocalTools.computeEnergyPSF_weighted(temp1, result, temp2, temp3, iRegionMask, iParameters.ldata, iRegulariztionPatch, iPsf, cout, cin, iPatch, iParameters.noiseModel);
                if (tempEnergy < bestEnergy) {
                    bestEnergy = tempEnergy;
                    cinbest = cin;
                    coutbest = cout;
                }
            }
        }

        cin = cinbest;
        cout = coutbest;
        cout_front = cout;

        iRescaledMinIntensityAll = Math.max(0, (iNormalizedMinObjectIntensity - cout) / (cin - cout));
        if (iParameters.debug) {
            IJ.log("fbest" + iInputRegion.iLabel + "min all " + iRescaledMinIntensityAll);
            IJ.log(" best energy and int " + bestEnergy + "region" + iInputRegion.iLabel + " cin " + cin + " cout " + cout);
        }
    }

    /**
     * Here is calculated the best threshold for the patches
     *
     * @param w3kbest the mask
     * @return the best threshold based on energy calculation
     */
    private double findBestThreshold(double[][][] w3kbest, double aMinThreshold) {
        double bestEenergy = Double.MAX_VALUE;
        double bestThreshold = 0.75;
        
        for (double currentThr = 1; currentThr > aMinThreshold; currentThr -= 0.02) {
            if (generateThresholdedObject(w3kbest, currentThr)) {
                double tempEnergy = iLocalTools.computeEnergyPSF_weighted(temp1, result, temp2, temp3, iRegionMask, iParameters.ldata, iRegulariztionPatch, iPsf, cout_front, cin, iPatch, iParameters.noiseModel);
                if (tempEnergy < bestEenergy) {
                    bestEenergy = tempEnergy;
                    bestThreshold = currentThr;
                }
            }
        }
        
        return bestThreshold;
    }

    private boolean generateThresholdedObject(double[][][] w3kbest, double aThreshold) {
        boolean objectFound = false;
        boolean borderAttained = false;
        for (int z = 0; z < iSizeOverZ; z++) {
            for (int i = 0; i < iSizeOverX; i++) {
                for (int j = 0; j < iSizeOverY; j++) {
                    if (w3kbest[z][i][j] > aThreshold && iRegionMask[z][i][j] == 1) {
                        result[z][i][j] = 1;
                        objectFound = true;
                        if (iSizeOverZ <= 1) {
                            if ((i == 0 && iOffsetOrigX != 0) || (i == (iSizeOverX - 1) && (iOffsetOrigX + iSizeOverX / iOversamplingXY) != iSizeOrigX) || 
                                (j == 0 && iOffsetOrigY != 0) || (j == (iSizeOverY - 1) && (iOffsetOrigY + iSizeOverY / iOversamplingXY) != iSizeOrigY)) {
                                borderAttained = true;
                            }
                        }
                    }
                    else {
                        result[z][i][j] = 0;
                    }
                }
            }
        }
        return objectFound && !borderAttained;
    }

    /**
     * Compute the geometry of the patch
     * @param aInputRegion Region
     * @param aOversampling level of oversampling
     */
    private void computePatchGeometry(Region aInputRegion, int aOversampling, int aInterpolation) {
        Pix[] mm = aInputRegion.getMinMaxCoordinates();
        Pix min = mm[0]; Pix max = mm[1];
        int xmin = min.px;
        int ymin = min.py;
        int zmin = min.pz;
        int xmax = max.px;
        int ymax = max.py;
        int zmax = max.pz;
        
        // Adjust patch coordinates with margin and fit it into min/max coordinates
        // old comment: check that the margin is at least 8 time bigger than the PSF
        int aMarginXY = 6;
        int aMarginZ = 1;// was 2
        final int[] sz_psf = iPsf.getSuggestedImageSize();
        if (sz_psf[0] > aMarginXY) {
            aMarginXY = sz_psf[0];
        }
        if (sz_psf[1] > aMarginXY) {
            aMarginXY = sz_psf[1];
        }
        if (sz_psf.length > 2 && sz_psf[2] > aMarginZ) {
            aMarginZ = sz_psf[2];
        }
        
        xmin = Math.max(0, xmin - aMarginXY);
        xmax = Math.min(iSizeOrigX, xmax + aMarginXY + 1);
        ymin = Math.max(0, ymin - aMarginXY);
        ymax = Math.min(iSizeOrigY, ymax + aMarginXY + 1);
        zmin = Math.max(0, zmin - aMarginZ);
        zmax = Math.min(iSizeOrigZ, zmax + aMarginZ + 1);

        iOffsetOrigX = xmin;
        iOffsetOrigY = ymin;
        iOffsetOrigZ = zmin;
        
        iOversamplingXY = aOversampling;
        iInterpolationXY = aInterpolation;
        if (iSizeOrigZ == 1) {
            iOversamplingZ = 1;
            iInterpolationZ = 1;
        }
        else {
            iOversamplingZ = aOversampling;
            iInterpolationZ = aInterpolation;
        }
        iOverInterXY = iOversamplingXY * iInterpolationXY;
        iOverInterZ = iOversamplingZ * iInterpolationZ;
        
        iSizeOverX = (xmax - xmin) * iOversamplingXY;
        iSizeOverY = (ymax - ymin) * iOversamplingXY;
        iSizeOverZ = (zmax - zmin) * iOversamplingZ;
        
        iSizeOverInterX = iSizeOverX * iInterpolationXY;
        iSizeOverInterY = iSizeOverY * iInterpolationXY;
        iSizeOverInterZ = iSizeOverZ * iInterpolationZ;
    }

    private double[][][] generateFromPatchArea(double[][][] aSourceImage, double[][][] aWeights, int aOversamplingXY, int aOversamplingZ, int aOffsetX, int aOffsetY, int aOffsetZ) {
        double[][][] result = new double[iSizeOverZ][iSizeOverX][iSizeOverY];
        for (int z = 0; z < iSizeOverZ; z++) {
            for (int i = 0; i < iSizeOverX; i++) {
                for (int j = 0; j < iSizeOverY; j++) {
                    // aWeights are set to 0 or 1.
                    result[z][i][j] = aWeights[z][i][j] * aSourceImage[z / aOversamplingZ + aOffsetZ][i / aOversamplingXY + aOffsetX][j / aOversamplingXY + aOffsetY];
                }
            }
        }
        return result;
    }

    private double[][][] generateMask(Region aRegion, boolean aCheckBoundaries) {
        double[][][] mask = new double[iSizeOverZ][iSizeOverX][iSizeOverY];

        for (final Pix p : aRegion.iPixels) {
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

    // build local object list
    private ArrayList<Region> generateRegions() {
        final ImageStack is = new ImageStack(iSizeOverInterX, iSizeOverInterY);
        for (int z = 0; z < iSizeOverInterZ; z++) {
            final byte[] pixels = new byte[iSizeOverInterX * iSizeOverInterY];
            for (int i = 0; i < iSizeOverInterX; i++) {
                for (int j = 0; j < iSizeOverInterY; j++) {
                    pixels[j * iSizeOverInterX + i] = (result[z][i][j] >= 1) ? (byte)(result[z][i][j]) : 0;
                }
            }
            final ByteProcessor bp = new ByteProcessor(iSizeOverInterX, iSizeOverInterY, pixels);
            is.addSlice("", bp);
        }
        final ImagePlus img = new ImagePlus("8-bit result", is);

        // find regions
        final FindConnectedRegions fcr = new FindConnectedRegions(img);
        fcr.run(iSizeOverInterX * iSizeOverInterY * iSizeOverInterZ, 0 * iOverInterXY, /* threshold */ 0.5f, iParameters.excludeEdgesZ, iOversamplingXY, iInterpolationXY);
        final ArrayList<Region> foundRegions = fcr.getFoundRegions();
        
        // rescale Pixel positions
        for (final Region r : foundRegions) {
            for (final Pix p : r.iPixels) {
                p.pz = p.pz + iOffsetOrigZ * iOverInterZ;
                p.px = p.px + iOffsetOrigX * iOverInterXY;
                p.py = p.py + iOffsetOrigY * iOverInterXY;
            }
        }

        return foundRegions;
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
        double[][][] interpolatedResult = new double[iSizeOverInterZ][iSizeOverInterX][iSizeOverInterY];
        for (int z = 0; z < iSizeOverInterZ; z++) {
            interpolatedImg.setSlice(z + 1);
            ImageProcessor imp = interpolatedImg.getProcessor();
            for (int i = 0; i < iSizeOverInterX; i++) {
                for (int j = 0; j < iSizeOverInterY; j++) {
                    if (imp.getPixelValue(i, j) > aThreshold && iRegionMask[z / iInterpolationZ][i / iInterpolationXY][j / iInterpolationXY] == 1) {
                        interpolatedResult[z][i][j] = 1;
                    }
                    else {
                        interpolatedResult[z][i][j] = 0;
                    }
                }
            }
        }

        return interpolatedResult;
    }

    private void estimateIntensityRSS(double[][][] mask) {
        Tools.normalizeAndConvolveMask(temp3, mask, iPsf, temp1, temp2);
        RegionStatisticsSolver RSS = new RegionStatisticsSolver(temp1, temp2, iPatch, iRegionMask, 10, iParameters.defaultBetaMleOut, iParameters.defaultBetaMleIn);
        RSS.eval(temp3 /* convolved mask */);
    
        cout = RSS.betaMLEout;
        cout_front = cout;
        cin = RSS.betaMLEin;
    }

    private void estimateIntensityClustering(double[][][] aValues, int aNumOfClasters, boolean aUpdateCout) {
        final Dataset data = new DefaultDataset();
        final double[] pixel = new double[1];
        for (int z = 0; z < iSizeOverZ; z++) {
            for (int i = 0; i < iSizeOverX; i++) {
                for (int j = 0; j < iSizeOverY; j++) {
                    if (iRegionMask[z][i][j] == 1) {
                        pixel[0] = aValues[z][i][j];
                        data.add(new DenseInstance(pixel));
                    }
                }
            }
        }
        
        if (data.size() > aNumOfClasters) {
            final Dataset[] dataSet = new KMeans(aNumOfClasters, 100).cluster(data);
    
            int numOfClustersFound = dataSet.length;
            final double[] levels = new double[numOfClustersFound];
            for (int i = 0; i < numOfClustersFound; i++) {
                final Instance inst = DatasetTools.average(dataSet[i]);
                levels[i] = inst.value(0);
            }
            Arrays.sort(levels);
            
            final int inIndex = Math.min(2, numOfClustersFound - 1);
            cin = Math.max(iNormalizedMinObjectIntensity, levels[inIndex]);
            final int outFrontIndex = Math.max(inIndex - 1, 0);
            cout_front = levels[outFrontIndex];
            cout = (aUpdateCout) ? cout_front : levels[0];
    
            logger.debug("Region: " + iInputRegion.iLabel + ", Cluster levels: " + Arrays.toString(levels));
        }
        else {
            cin = 1;
            cout_front = iParameters.defaultBetaMleOut;
            cout = iParameters.defaultBetaMleOut;
        }
    }
}
