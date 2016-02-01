package mosaic.bregman;


import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/** 
 * Calculates sample Pearson correlation coefficient from provided images.
 */
class SamplePearsonCorrelationCoefficient {
    private final int iWidth;
    private final int iHeight;
    private final int iDepth;
    private final double[][][] iImageA;
    private boolean[][][] iMaskA;
    private final double[][][] iImageB;
    private boolean[][][] iMaskB;

    public SamplePearsonCorrelationCoefficient(ImagePlus aImgA, ImagePlus aImgB, boolean aMaskImgA, double aMaskThresholdA, boolean aMaskImgB, double aMaskThresholdB) {
        if (aImgA.getWidth() != aImgB.getWidth() || aImgA.getHeight() != aImgB.getHeight() || aImgA.getNSlices() != aImgB.getNSlices() || aImgA.getBitDepth() != aImgB.getBitDepth()) {
            IJ.error("ImageColocalizer expects both images to have the same size and depth");
            throw new RuntimeException();
        }

        iWidth = aImgA.getWidth();
        iHeight = aImgA.getHeight();
        iDepth = aImgA.getNSlices();
        
        iImageA = new double[iDepth][iWidth][iHeight];
        double maxA = initImageAndGetMax(aImgA, iImageA);
        double tx = aMaskImgA ? aMaskThresholdA * maxA : -1; 
        iMaskA = Analysis.createBinaryCellMask(tx, aImgA, 0, iDepth, iWidth, iHeight, null);
        
        iImageB = new double[iDepth][iWidth][iHeight];
        double maxB = initImageAndGetMax(aImgB, iImageB);
        double ty = aMaskImgB ? aMaskThresholdB * maxB : -1;
        iMaskB = Analysis.createBinaryCellMask(ty, aImgB, 1, iDepth, iWidth, iHeight, null);
    }

    /**
     * Converts ImagePlus to 3D array and finds maximum value in image.
     * @param aImage
     * @param aImageOutputArray
     * @return
     */
    private double initImageAndGetMax(ImagePlus aImage, double[][][] aImageOutputArray) {
        double max = 0;
        for (int z = 0; z < iDepth; z++) {
            aImage.setSlice(z + 1);
            ImageProcessor imp = aImage.getProcessor();
            for (int i = 0; i < iWidth; i++) {
                for (int j = 0; j < iHeight; j++) {
                    final float value = imp.getPixelValue(i, j);
                    aImageOutputArray[z][i][j] = value;
                    if (value > max) max = value;
                }
            }
        }
        return max;
    }

    /**
     * @return Two value array with calculated Pearson correlation: [0] - without mask, [1] - with mask
     */
    public double[] run() {
        return new double[] {calculateCoefficient(iImageA, iImageB, iMaskA, iMaskB, 0, 0, false), 
                             calculateCoefficient(iImageA, iImageB, iMaskA, iMaskB, 0, 0, true) };
    }

    /**
     * Check for details:
     * https://en.wikipedia.org/wiki/Pearson_product-moment_correlation_coefficient
     */
    private double calculateCoefficient(double[][][] aA, double[][][] aB, boolean [][][] aMaskA, boolean[][][] aMaskB, int aThresholdA, int aThresholdB, boolean aUseMask) {
        double sumA = 0;
        double sumB = 0;
        for (int z = 0; z < iDepth; z++) {
            for (int i = 0; i < iWidth; i++) {
                for (int j = 0; j < iHeight; j++) {
                    if (checkCondition(aA, aB, aMaskA, aMaskB, aThresholdA, aThresholdB, aUseMask, z, i, j)) {
                        sumA += aA[z][i][j];
                        sumB += aB[z][i][j];
                    }
                }
            }
        }

        int count = iDepth * iWidth * iHeight;
        double Aarraymean = sumA / count;
        double Barraymean = sumB / count;

        double num = 0;
        double denA = 0;
        double denB = 0;
        for (int z = 0; z < iDepth; z++) {
            for (int i = 0; i < iWidth; i++) {
                for (int j = 0; j < iHeight; j++) {
                    if (checkCondition(aA, aB, aMaskA, aMaskB, aThresholdA, aThresholdB, aUseMask, z, i, j)) {
                        num += (aA[z][i][j] - Aarraymean) * (aB[z][i][j] - Barraymean);
                        denA += Math.pow((aA[z][i][j] - Aarraymean), 2);
                        denB += Math.pow((aB[z][i][j] - Barraymean), 2);
                    }
                }
            }
        }

        return num / (Math.sqrt(denA * denB));
    }

    private boolean checkCondition(double[][][] aA, double[][][] aB, boolean[][][] aMaskA, boolean[][][] aMaskB, int aThresholdA, int aThresholdB, boolean aUseMask, int z, int i, int j) {
        boolean bothMaskSet = aMaskB[z][i][j] && aMaskA[z][i][j];
        final boolean bothArraysSet = aA[z][i][j] >= aThresholdA && aB[z][i][j] >= aThresholdB;
        final boolean checkCondition = bothArraysSet && (!aUseMask || bothMaskSet);
        return checkCondition;
    }
}
