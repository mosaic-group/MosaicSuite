package mosaic.bregman;


import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;


class Pearson {
    private final int iWidth;
    private final int iHeight;
    private final int iDepth;
    private final double[][][] imageA;
    private boolean[][][] cellmaskA;
    private final double[][][] imageB;
    private boolean[][][] cellmaskB;


    public Pearson(ImagePlus ipA, ImagePlus ipB, Parameters iParameters) {
        iWidth = iParameters.ni;
        iHeight = iParameters.nj;
        iDepth = iParameters.nz;
        
        if (ipA.getWidth() != ipB.getWidth() || ipA.getHeight() != ipB.getHeight() || ipA.getNSlices() != ipB.getNSlices() || ipA.getBitDepth() != ipB.getBitDepth()) {
            IJ.error("ImageColocalizer expects both images to have the same size and depth");
            throw new RuntimeException();
        }
        
        imageA = new double[iDepth][iWidth][iHeight];
        double maxA = initImageAndGetMax(ipA, imageA);
        double tx = (iParameters.usecellmaskX) ? iParameters.thresholdcellmask * maxA : -1; 
        cellmaskA = Tools.createBinaryCellMask(tx, ipA, 0, iDepth, iWidth, iHeight, false);
        
        imageB = new double[iDepth][iWidth][iHeight];
        double maxB = initImageAndGetMax(ipB, imageB);
        double ty = (iParameters.usecellmaskY) ? iParameters.thresholdcellmasky * maxB : -1;
        cellmaskB = Tools.createBinaryCellMask(ty, ipB, 1, iDepth, iWidth, iHeight, false);
    }

    private double initImageAndGetMax(ImagePlus inputImage, double[][][] image) {
        double max = 0;
        for (int z = 0; z < iDepth; z++) {
            inputImage.setSlice(z + 1);
            ImageProcessor imp = inputImage.getProcessor();
            for (int i = 0; i < iWidth; i++) {
                for (int j = 0; j < iHeight; j++) {
                    image[z][i][j] = imp.getPixelValue(i, j);
                    if (imp.getPixelValue(i, j) > max) {
                        max = imp.getPixelValue(i, j);
                    }
                }
            }
        }
        return max;
    }

    /**
     * @return Two value array with calculated Pearson correlation: [0] - without mask, [1] - with mask
     */
    public double[] run() {
        return new double[] {linreg(imageA, imageB, 0, 0, false)[2], linreg(imageA, imageB, 0, 0, true)[2]};
    }

    private double[] linreg(double[][][] Aarray, double[][][] Barray, int TA, int TB, boolean aUseMask) {
        double sumA = 0;
        double sumB = 0;
        double sumAB = 0;
        double sumsqrA = 0;

        for (int z = 0; z < iDepth; z++) {
            for (int i = 0; i < iWidth; i++) {
                for (int j = 0; j < iHeight; j++) {
                    boolean bothMaskSet = cellmaskB[z][i][j] && cellmaskA[z][i][j];
                    final boolean bothArraysSet = Aarray[z][i][j]>=TA && Barray[z][i][j]>=TB;
                    if (bothArraysSet && (!aUseMask || bothMaskSet)) {
                        sumA += Aarray[z][i][j];
                        sumB += Barray[z][i][j];
                        sumAB += Aarray[z][i][j] * Barray[z][i][j];
                        sumsqrA += Math.pow(Aarray[z][i][j], 2);
                    }
                }
            }
        }

        int count = iDepth * iWidth * iHeight;
        double Aarraymean = sumA / count;
        double Barraymean = sumB / count;

        double num = 0;
        double den1 = 0;
        double den2 = 0;
        for (int z = 0; z < iDepth; z++) {
            for (int i = 0; i < iWidth; i++) {
                for (int j = 0; j < iHeight; j++) {
                    boolean bothMaskSet = cellmaskB[z][i][j] && cellmaskA[z][i][j];
                    final boolean bothArraysSet = Aarray[z][i][j]>=TA && Barray[z][i][j]>=TB;
                    if (bothArraysSet && (!aUseMask || bothMaskSet)) {
                        num += (Aarray[z][i][j] - Aarraymean) * (Barray[z][i][j] - Barraymean);
                        den1 += Math.pow((Aarray[z][i][j] - Aarraymean), 2);
                        den2 += Math.pow((Barray[z][i][j] - Barraymean), 2);
                    }
                }
            }
        }

        final double[] coeff = new double[6];
        // 0:a, 1:b, 2:corr coeff, 3: num, 4: den1, 5: den2
        coeff[0] = (count * sumAB - sumA * sumB) / (count * sumsqrA - Math.pow(sumA, 2));
        coeff[1] = (sumsqrA * sumB - sumA * sumAB) / (count * sumsqrA - Math.pow(sumA, 2));
        coeff[2] = num / (Math.sqrt(den1 * den2));
        coeff[3] = num;
        coeff[4] = den1;
        coeff[5] = den2;
        return coeff;
    }
}
