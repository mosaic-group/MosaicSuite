package mosaic.bregman;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;


class Pearson {

    private final int width, height, nbSlices, depth;
    private boolean[][][] cellmaskA;
    private boolean[][][] cellmaskB;
    private final int ni, nj, nz;
    private final Parameters p;
    private int osz;
    private double sumA, sumB, sumAB, sumsqrA, Aarraymean, Barraymean;
    private double maxA, maxB;
    private final double[][][] imageA;
    private final double[][][] imageB;

    public Pearson(ImagePlus ipA, ImagePlus ipB, Parameters p) {
        this.p = p;
        this.ni = p.ni;
        this.nj = p.nj;
        this.nz = p.nz;
        this.width = ipA.getWidth();
        this.height = ipA.getHeight();
        this.nbSlices = ipA.getNSlices();
        this.depth = ipA.getBitDepth();

        imageA = new double[nz][ni][nj];
        imageB = new double[nz][ni][nj];

        if (this.width != ipB.getWidth() || this.height != ipB.getHeight() || this.nbSlices != ipB.getNSlices() || this.depth != ipB.getBitDepth()) {
            IJ.error("ImageColocalizer expects both images to have the same size and depth");
            return;
        }

        if (p.nz > 1) {
            osz = p.model_oversampling;
        }
        else {
            osz = 1;
        }
        final int os = p.model_oversampling;

        ImageProcessor imp;
        for (int z = 0; z < nz / osz; z++) {
            ipA.setSlice(z + 1);
            imp = ipA.getProcessor();
            for (int i = 0; i < ni / os; i++) {
                for (int j = 0; j < nj / os; j++) {
                    imageA[z][i][j] = imp.getPixel(i, j);
                    if (imp.getPixel(i, j) > maxA) {
                        maxA = imp.getPixel(i, j);
                    }
                }
            }
        }

        for (int z = 0; z < nz / osz; z++) {
            ipB.setSlice(z + 1);
            imp = ipB.getProcessor();
            for (int i = 0; i < ni / os; i++) {
                for (int j = 0; j < nj / os; j++) {
                    imageB[z][i][j] = imp.getPixel(i, j);
                    if (imp.getPixel(i, j) > maxB) {
                        maxB = imp.getPixel(i, j);
                    }
                }
            }
        }

        double tx, ty;
        if (Analysis.p.usecellmaskX) {
            tx = Analysis.p.thresholdcellmask * maxA;
        }
        else {
            tx = 0;
        }
        if (Analysis.p.usecellmaskY) {
            ty = Analysis.p.thresholdcellmasky * maxB;
        }
        else {
            ty = 0;
        }
        cellmaskA = createBinaryCellMask(tx, ipA, 0, osz);
        cellmaskB = createBinaryCellMask(ty, ipB, 1, osz);

    }

    public double[] run() {
        final double[] res = new double[3];

        res[0] = linreg(this.imageA, this.imageB, 0, 0, 0)[2];// without
        res[1] = linreg(this.imageA, this.imageB, 0, 0, 1)[2];// with mask
        res[2] = linreg(this.imageA, this.imageB, 0, 0, 2)[2];// with mask to zero

        return res;
    }

    private double[] linreg(double[][][] Aarray, double[][][] Barray, int TA, int TB, int mask) {// mask == 0 no mask, 1 : mask, 2 : outside zet to zero only
        double num = 0;
        double den1 = 0;
        double den2 = 0;
        final double[] coeff = new double[6];
        int count = 0;

        sumA = 0;
        sumB = 0;
        sumAB = 0;
        sumsqrA = 0;
        Aarraymean = 0;
        Barraymean = 0;

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    boolean cond;
                    if (mask == 1) {
                        cond = (Aarray[z][i][j] >= TA && Barray[z][i][j] >= TB && cellmaskB[z][i][j] && cellmaskA[z][i][j]);
                    }
                    else {
                        cond = (Aarray[z][i][j] >= TA && Barray[z][i][j] >= TB);
                    }
                    if (cond) {
                        if (mask == 2 && (!cellmaskB[z][i][j] || !cellmaskA[z][i][j])) {
                            count++;
                        }
                        else {
                            sumA += Aarray[z][i][j];
                            sumB += Barray[z][i][j];
                            sumAB += Aarray[z][i][j] * Barray[z][i][j];
                            sumsqrA += Math.pow(Aarray[z][i][j], 2);
                            count++;
                        }
                    }

                }
            }
        }

        Aarraymean = sumA / count;
        Barraymean = sumB / count;

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    boolean cond;
                    if (mask == 1) {
                        cond = (Aarray[z][i][j] >= TA && Barray[z][i][j] >= TB && cellmaskB[z][i][j] && cellmaskA[z][i][j]);
                    }
                    else {
                        cond = (Aarray[z][i][j] >= TA && Barray[z][i][j] >= TB);
                    }
                    if (cond) {
                        if (mask == 2 && (!cellmaskB[z][i][j] || !cellmaskA[z][i][j])) {

                        }
                        else {
                            num += (Aarray[z][i][j] - Aarraymean) * (Barray[z][i][j] - Barraymean);
                            den1 += Math.pow((Aarray[z][i][j] - Aarraymean), 2);
                            den2 += Math.pow((Barray[z][i][j] - Barraymean), 2);
                        }
                    }
                }
            }
        }

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

        final ImagePlus maska_im = new ImagePlus();
        final ImageStack maska_ims = new ImageStack(ni, nj);
        ImageProcessor imp;

        for (int z = 0; z < nz; z++) {
            img.setSlice(z / osz + 1);
            imp = img.getProcessor();
            final byte[] maska_bytes = new byte[ni * nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (imp.getPixel(i / p.model_oversampling, j / p.model_oversampling) > threshold) {
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

        maska_im.setStack("Cell mask channel " + (channel + 1), maska_ims);

        IJ.run(maska_im, "Invert", "stack");
        //
        IJ.run(maska_im, "Fill Holes", "stack");
        IJ.run(maska_im, "Open", "stack");

        IJ.run(maska_im, "Invert", "stack");

        for (int z = 0; z < nz; z++) {
            maska_im.setSlice(z + 1);
            imp = maska_im.getProcessor();
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    cellmask[z][i][j] = imp.getPixel(i, j) > 254;
                }
            }
        }

        return cellmask;
    }
}
