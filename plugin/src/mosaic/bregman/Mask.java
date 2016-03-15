package mosaic.bregman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import mosaic.bregman.segmentation.Pix;
import mosaic.bregman.segmentation.Region;
import mosaic.utils.ArrayOps.MinMax;
import mosaic.utils.ImgUtils;

public class Mask {
    private static final Logger logger = Logger.getLogger(Mask.class);
    
    private double iGlobalMin;
    private double iGlobalMax;
    private Map<Integer, boolean[][][]> iCellMasks;
    private boolean[][][] iOoverallCellMask;
    
    public Mask(double aGlobalMin, double aGlobalMax) {
        iGlobalMin = aGlobalMin;
        iGlobalMax = aGlobalMax;
        iCellMasks = new HashMap<Integer, boolean[][][]>();
    }
    
    void generateMasks(int aChannel, final ImagePlus aImage, double aThreshold) {
        logger.info("Generating mask for channel: " + aChannel);
        double minNorm = iGlobalMin;
        double maxNorm = iGlobalMax;
        if (iGlobalMax == 0) {
            MinMax<Double> mm = ImgUtils.findMinMax(aImage);
            minNorm = mm.getMin();
            maxNorm = mm.getMax();
        }
        iCellMasks.put(aChannel, generateMask(aImage, minNorm, maxNorm, aThreshold));
    }
    
    private boolean[][][] generateMask(final ImagePlus aImage, double aNormalizationMin, double aNormalizationMax, double aMaskThreshold) {
        ImagePlus mask = Mask.createBinaryCellMask(aImage, "Cell mask", aMaskThreshold * (aNormalizationMax - aNormalizationMin) + aNormalizationMin);
        return ImgUtils.imgToZXYbinaryArray(mask);
    }
    
    void computeOverallMask(int nz, int ni, int nj) {
        iOoverallCellMask = new boolean[nz][ni][nj];

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    boolean value = true;
                    for (boolean[][][] mask : iCellMasks.values()) {
                        value = value && mask[z][i][j];
                    }
                    iOoverallCellMask[z][i][j] = value;
                }
            }
        }
    }
    
    List<List<Region>> applyMask(List<List<Region>> aInputRegions, int aScale) {
        List<List<Region>> maskedRegionList = new ArrayList<List<Region>>();
        for (int channel = 0; channel < aInputRegions.size(); channel++) {
            final ArrayList<Region> maskedRegion = new ArrayList<Region>();
            for (Region r : aInputRegions.get(channel)) {
                if (isInside(r, aScale)) {
                    maskedRegion.add(r);
                }
            }
            maskedRegionList.add(maskedRegion);
        }
        return maskedRegionList;
    }
    
    private boolean isInside(Region r, int aScale) {
        final double InsideThreshold = 0.1;
        final int factor2 = aScale;
        int fz2 = (iOoverallCellMask.length > 1) ? factor2 : 1;

        double size = r.iPixels.size();
        int inside = 0;
        for (Pix px : r.iPixels) {
            if (iOoverallCellMask[px.pz / fz2][px.px / factor2][px.py / factor2]) {
                inside++;
            }
        }
        return ((inside / size) > InsideThreshold);
    }

    public static ImagePlus createBinaryCellMask(ImagePlus aInputImage, String aTitle, double aThreshold) {
        int ni = aInputImage.getWidth();
        int nj = aInputImage.getHeight();
        int nz = aInputImage.getNSlices();
        final ImageStack maskStack = new ImageStack(ni, nj);
        for (int z = 0; z < nz; z++) {
            aInputImage.setSlice(z + 1);
            ImageProcessor ip = aInputImage.getProcessor();
            final byte[] mask = new byte[ni * nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (ip.getPixelValue(i, j) > aThreshold) {
                        mask[j * ni + i] = (byte) 255;
                    }
                }
            }
            final ByteProcessor bp = new ByteProcessor(ni, nj);
            bp.setPixels(mask);
            maskStack.addSlice("", bp);
        }
        final ImagePlus maskImg = new ImagePlus(aTitle, maskStack);
        IJ.run(maskImg, "Invert", "stack");
        
        // "Fill Holes" is using Prefs.blackBackground global setting. We need false here.
        boolean tempBlackbackground = ij.Prefs.blackBackground;
        ij.Prefs.blackBackground = false;
        IJ.run(maskImg, "Fill Holes", "stack");
        ij.Prefs.blackBackground = tempBlackbackground;
        
        IJ.run(maskImg, "Open", "stack");
        IJ.run(maskImg, "Invert", "stack");
        maskImg.changes = false;
        
        return maskImg;
    }
}
