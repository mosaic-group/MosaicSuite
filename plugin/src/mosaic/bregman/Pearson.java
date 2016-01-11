package mosaic.bregman;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

class Pearson {
    private final int ni, nj, nz;
    private final double[][][] imageA;
    private final double[][][] imageB;
    private boolean[][][] cellmaskA;
    private boolean[][][] cellmaskB;

    private final Parameters p;

    public Pearson(ImagePlus ipA, ImagePlus ipB, Parameters par) {
        this.p = par;
        this.ni = p.ni;
        this.nj = p.nj;
        this.nz = p.nz;
        
        if (ipA.getWidth() != ipB.getWidth() || ipA.getHeight() != ipB.getHeight() || ipA.getNSlices() != ipB.getNSlices() || ipA.getBitDepth() != ipB.getBitDepth()) {
            IJ.error("ImageColocalizer expects both images to have the same size and depth");
            throw new RuntimeException();
        }
        
        int osz = (p.nz > 1) ? p.model_oversampling : 1;

        imageA = new double[nz][ni][nj];
        double maxA = initImageAndGetMax(ipA, imageA, osz);
        double tx = (p.usecellmaskX) ? p.thresholdcellmask * maxA : 0; 
        cellmaskA = createBinaryCellMask(tx, ipA, 0, osz);
        
        imageB = new double[nz][ni][nj];
        double maxB = initImageAndGetMax(ipB, imageB, osz);
        double ty = (p.usecellmaskY) ? p.thresholdcellmasky * maxB : 0;
        cellmaskB = createBinaryCellMask(ty, ipB, 1, osz);
    }

    private double initImageAndGetMax(ImagePlus inputImage, double[][][] image, int osz) {
        double max = 0;
        for (int z = 0; z < nz / osz; z++) {
            inputImage.setSlice(z + 1);
            ImageProcessor imp = inputImage.getProcessor();
            for (int i = 0; i < ni / p.model_oversampling; i++) {
                for (int j = 0; j < nj / p.model_oversampling; j++) {
                    image[z][i][j] = imp.getPixelValue(i, j);
                    if (imp.getPixelValue(i, j) > max) {
                        max = imp.getPixelValue(i, j);
                    }
                }
            }
        }
        return max;
    }

    public double[] run() {
        final double[] res = new double[3];

        res[0] = linreg(this.imageA, this.imageB, 0, 0, 0)[2];// without
        res[1] = linreg(this.imageA, this.imageB, 0, 0, 1)[2];// with mask
        res[2] = linreg(this.imageA, this.imageB, 0, 0, 2)[2];// with mask to zero

        return res;
    }

    private double[] linreg(double[][][] Aarray, double[][][] Barray, int TA, int TB, int mask) {// mask == 0 no mask, 1 : mask, 2 : outside zet to zero only
        double sumA = 0;
        double sumB = 0;
        double sumAB = 0;
        double sumsqrA = 0;
        int count = 0;

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    boolean bothMaskSet = cellmaskB[z][i][j] && cellmaskA[z][i][j];
                    final boolean bothArraysSet = Aarray[z][i][j]>=TA && Barray[z][i][j]>=TB;
                    if (bothArraysSet && (mask == 0 || bothMaskSet)) {
                        sumA+=Aarray[z][i][j];
                        sumB+=Barray[z][i][j];
                        sumAB+=Aarray[z][i][j]*Barray[z][i][j];
                        sumsqrA+=Math.pow(Aarray[z][i][j],2);
                    }
                    count++;    
                }
            }
        }

        double Aarraymean = sumA / count;
        double Barraymean = sumB / count;

        double num = 0;
        double den1 = 0;
        double den2 = 0;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    boolean bothMaskSet = cellmaskB[z][i][j] && cellmaskA[z][i][j];
                    final boolean bothArraysSet = Aarray[z][i][j]>=TA && Barray[z][i][j]>=TB;
                    if (bothArraysSet && (mask == 0 || bothMaskSet)) {
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

    private boolean[][][] createBinaryCellMask(double threshold, ImagePlus img, int channel, int osz) {
        final boolean[][][] cellmask = new boolean[nz][ni][nj];

        if (threshold == 0) {
            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        cellmask[z][i][j] = true;
                    }
                }
            }

            return cellmask;
        }

        final ImageStack maska_ims = new ImageStack(ni, nj);
        for (int z = 0; z < nz; z++) {
            img.setSlice(z / osz + 1);
            ImageProcessor imp = img.getProcessor();
            final byte[] maska_bytes = new byte[ni * nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (imp.getPixelValue(i / p.model_oversampling, j / p.model_oversampling) > threshold) {
                        maska_bytes[j * ni + i] = (byte) 255;
                    }
                    else {
                        maska_bytes[j * ni + i] = 0;
                    }

                }
            }
            final ByteProcessor bp = new ByteProcessor(ni, nj);
            bp.setPixels(maska_bytes);
            maska_ims.addSlice("", bp);
        }

        final ImagePlus maska_im = new ImagePlus();
        maska_im.setStack("Cell mask channel " + (channel + 1), maska_ims);
        IJ.run(maska_im, "Invert", "stack");
        IJ.run(maska_im, "Fill Holes", "stack");
        IJ.run(maska_im, "Open", "stack");
        IJ.run(maska_im, "Invert", "stack");

        for (int z = 0; z < nz; z++) {
            maska_im.setSlice(z + 1);
            ImageProcessor imp = maska_im.getProcessor();
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    cellmask[z][i][j] = imp.getPixelValue(i, j) != 0;
                }
            }
        }

        return cellmask;
    }
}
