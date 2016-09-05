package mosaic.bregman.segmentation;


import mosaic.bregman.segmentation.SegmentationParameters.NoiseModel;
import mosaic.core.psf.psf;
import mosaic.utils.ArrayOps;
import mosaic.utils.ArrayOps.MinMax;
import net.imglib2.type.numeric.real.DoubleType;


class SegmentationTools {
    final private int ni, nj, nz;
    
    SegmentationTools(int nni, int nnj, int nnz) {
        ni = nni;
        nj = nnj;
        nz = nnz;
    }

    private static void convolve2Dseparable(double[][] out, double[][] in, int icols, int irows, psf<DoubleType> psf, double temp[][]) {
        convolve2Dseparable(out, in, icols, irows, psf, temp, 0, icols);
    }

    private static void convolve2Dseparable(double[][] out, double[][] in, int icols, int irows, psf<DoubleType> psf, double[][] temp, int iStart, int iEnd) {
        final int[] sz = psf.getSuggestedImageSize();
        int kCenterX = sz[0] / 2;
        int kCenterY = sz[1] / 2;
        final double kernelx[] = psf.getSeparableImageAsDoubleArray(0);
        final double kernely[] = psf.getSeparableImageAsDoubleArray(1);

        for (int i = iStart; i < iEnd; ++i) // columns
        {
            for (int j = 0; j < irows; ++j) // rows
            {
                double sum = 0; // init to 0 before sum
                for (int m = 0; m < sz[0]; ++m) // kernel cols
                {
                    int mm = sz[0] - 1 - m; // col index of flipped kernel

                    // index of input signal, used for checking boundary
                    int colIndex = i + m - kCenterX;
                    int rowIndex = j;

                    if (colIndex >= 0 && colIndex < icols) {
                        sum += in[colIndex][rowIndex] * kernelx[mm];
                    }
                    else {
                        do {
                            if (colIndex < 0) {
                                colIndex = -colIndex - 1;
                            }
                            if (colIndex > icols - 1) {
                                colIndex = icols - (colIndex - icols) - 1;
                            }
                        } while (!(colIndex >= 0 && colIndex < icols));
                        sum += in[colIndex][rowIndex] * kernelx[mm];
                    }

                }
                temp[i][j] = sum;
            }
        }

        // convolve in y (j coordinate), vertical
        for (int i = iStart; i < iEnd; ++i) // columns
        {
            for (int j = 0; j < irows; ++j) // rows
            {
                double sum = 0; // init to 0 before sum
                for (int n = 0; n < sz[1]; ++n) // kernel rows
                {
                    int nn = sz[1] - 1 - n; // row index of flipped kernel

                    // index of input signal, used for checking boundary
                    int colIndex = i;
                    int rowIndex = j + n - kCenterY;

                    if (rowIndex >= 0 && rowIndex < irows) {
                        sum += temp[colIndex][rowIndex] * kernely[nn];
                    }
                    else {
                        do {
                            if (rowIndex < 0) {
                                rowIndex = -rowIndex - 1;
                            }
                            if (rowIndex > irows - 1) {
                                rowIndex = irows - (rowIndex - irows) - 1;
                            }
                        } while(!(rowIndex >= 0 && rowIndex < irows));
                        sum += temp[colIndex][rowIndex] * kernely[nn];
                    }

                }
                out[i][j] = sum;
            }
        }

        return;
    }

    private static void convolve3Dseparable(double[][][] out, double[][][] in, int icols, int irows, int islices, psf<DoubleType> psf, double temp[][][]) {
        convolve3Dseparable(out, in, icols, irows, islices, psf, temp, 0, icols);
    }

    private static void convolve3Dseparable(double[][][] out, double[][][] in, int icols, int irows, int islices, psf<DoubleType> psf, double temp[][][], int iStart, int iEnd) {
        int i, j, k, m, n, l, mm, nn, ll;
        int kCenterX, kCenterY, kCenterZ; // center index of kernel
        double sum; // temp accumulation buffer
        int rowIndex, colIndex, sliceIndex;

        final int[] sz = psf.getSuggestedImageSize();
        final double kernelx[] = psf.getSeparableImageAsDoubleArray(0);
        final double kernely[] = psf.getSeparableImageAsDoubleArray(1);
        final double kernelz[] = psf.getSeparableImageAsDoubleArray(2);

        kCenterX = sz[0] / 2;
        kCenterY = sz[1] / 2;
        kCenterZ = sz[2] / 2;

        // convolve in x (i coordinate), horizontal
        for (k = 0; k < islices; ++k) // columns
        {
            for (i = iStart; i < iEnd; ++i) // columns
            {
                for (j = 0; j < irows; ++j) // rows
                {
                    sum = 0; // init to 0 before sum
                    for (m = 0; m < sz[0]; ++m) // kernel cols
                    {
                        mm = sz[0] - 1 - m; // col index of flipped kernel

                        // index of input signal, used for checking boundary
                        colIndex = i + m - kCenterX;
                        rowIndex = j;

                        if (colIndex >= 0 && colIndex < icols) {
                            sum += in[k][colIndex][rowIndex] * kernelx[mm];
                        }
                        else {
                            do {
                                if (colIndex < 0) {
                                    colIndex = -colIndex - 1;
                                }
                                if (colIndex > icols - 1) {
                                    colIndex = icols - (colIndex - icols) - 1;
                                }
                            } while(!(colIndex >= 0 && colIndex < icols));
                            sum += in[k][colIndex][rowIndex] * kernelx[mm];
                        }

                    }
                    out[k][i][j] = sum;
                }
            }
        }

        // convolve in y (j coordinate), vertical
        for (k = 0; k < islices; ++k) // columns
        {
            for (i = iStart; i < iEnd; ++i) // columns
            {
                for (j = 0; j < irows; ++j) // rows
                {
                    sum = 0; // init to 0 before sum
                    for (n = 0; n < sz[1]; ++n) // kernel rows
                    {
                        nn = sz[1] - 1 - n; // row index of flipped kernel

                        // index of input signal, used for checking boundary
                        colIndex = i;
                        rowIndex = j + n - kCenterY;

                        if (rowIndex >= 0 && rowIndex < irows) {
                            sum += out[k][colIndex][rowIndex] * kernely[nn];
                        }
                        else {
                            do {
                                if (rowIndex < 0) {
                                    rowIndex = -rowIndex - 1;
                                }
                                if (rowIndex > irows - 1) {
                                    rowIndex = irows - (rowIndex - irows) - 1;
                                }
                            } while(!(rowIndex >= 0 && rowIndex < irows));
                            sum += out[k][colIndex][rowIndex] * kernely[nn];
                        }

                    }

                    temp[k][i][j] = sum;
                }
            }
        }

        // convolve in z (k coordinate), slices
        for (k = 0; k < islices; ++k) // columns
        {
            for (i = iStart; i < iEnd; ++i) // columns
            {
                for (j = 0; j < irows; ++j) // rows
                {
                    sum = 0; // init to 0 before sum
                    for (l = 0; l < sz[2]; ++l) // kernel slices
                    {
                        ll = sz[2] - 1 - l; // row index of flipped kernel

                        // index of input signal, used for checking boundary
                        colIndex = i;
                        rowIndex = j;
                        sliceIndex = k + l - kCenterZ;

                        if (sliceIndex >= 0 && sliceIndex < islices) {
                            sum += temp[sliceIndex][colIndex][rowIndex] * kernelz[ll];
                        }
                        else {
                            do {
                                if (sliceIndex < 0) {
                                    sliceIndex = Math.min(islices - 1, -sliceIndex - 1);
                                }
                                if (sliceIndex > islices - 1) {
                                    sliceIndex = Math.max(0, islices - (sliceIndex - islices) - 1);
                                }
                            } while(!(sliceIndex >= 0 && sliceIndex < islices));
                            sum += temp[sliceIndex][colIndex][rowIndex] * kernelz[ll];
                        }
                    }
                    out[k][i][j] = sum;
                }
            }
        }

        return;
    }

    static void copytab(double[][][] res, double[][][] m1) {
        int nz = res.length;
        int ni = res[0].length;
        int nj = res[0][0].length;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    res[z][i][j] = m1[z][i][j];
                }
            }
        }
    }

    private void nllMean(double[][][] res, double[][][] image, double[][][] mu, NoiseModel aNoiseModel) {
        nllMean(res, image, mu, 0, ni, aNoiseModel);
    }

    private void nllMean(double[][][] res, double[][][] image, double[][][] mu, int iStart, int iEnd, NoiseModel aNoiseModel) {
        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    res[z][i][j] = noise(image[z][i][j], mu[z][i][j], aNoiseModel);
                }
            }
        }
    }

    private double noise(double im, double mu, NoiseModel aNoiseModel) {
        double res;
        if (mu < 0) {
            mu = 0.0001;
        }
        if (aNoiseModel == NoiseModel.POISSON) {
            if (im != 0) {
                res = (im * Math.log(im / mu) + mu - im);
            }
            else {
                res = mu;
            }

            if (mu == 0) {
                res = im;
            }
        }
        else// gauss
        {
            res = (im - mu) * (im - mu);
        }

        return res;
    }

    private void fgradz2D(double[][][] res, double[][][] im) {
        fgradz2D(res, im, 0, ni);
    }

    private void fgradz2D(double[][][] res, double[][][] im, int tStart, int tEnd) {
        for (int z = 0; z < nz - 1; z++) {
            for (int i = tStart; i < tEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    res[z][i][j] = im[z + 1][i][j] - im[z][i][j];
                }
            }
        }

        // von neumann boundary topslice
        for (int i = tStart; i < tEnd; i++) {
            for (int j = 0; j < nj; j++) {
                res[nz - 1][i][j] = 0;
            }
        }

    }

    private void fgradx2D(double[][][] res, double[][][] im) {
        fgradx2D(res, im, 0, nj);
    }

    private void fgradx2D(double[][][] res, double[][][] im, int tStart, int tEnd) {
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni - 1; i++) {
                for (int j = tStart; j < tEnd; j++) {
                    res[z][i][j] = im[z][i + 1][j] - im[z][i][j];
                }
            }

            // von neumann boundary right
            for (int j = tStart; j < tEnd; j++) {
                res[z][ni - 1][j] = 0;
            }
        }

    }

    private void fgrady2D(double[][][] res, double[][][] im) {
        fgrady2D(res, im, 0, ni);
    }

    // if x and y do same chunk : possibility to remove one synchronization
    // after each gradient computation
    private void fgrady2D(double[][][] res, double[][][] im, int tStart, int tEnd) {
        for (int z = 0; z < nz; z++) {
            for (int i = tStart; i < tEnd; i++) {
                for (int j = 0; j < nj - 1; j++) {
                    res[z][i][j] = im[z][i][j + 1] - im[z][i][j];
                }
            }
            // von neumann boundary bottom
            for (int i = tStart; i < tEnd; i++) {
                res[z][i][nj - 1] = 0;
            }
        }
    }

    double computeEnergyPSF_weighted(double[][][] speedData, double[][][] mask, double[][][] maskx, double[][][] masky, double[][][] weights, double ldata, double lreg, psf<DoubleType> aPsf, double c0, double c1, double[][][] image, NoiseModel aNoiseModel) {
        return (nz == 1) ? computeEnergyPSF2D_weighted(speedData, mask, maskx, masky, weights, ldata, lreg, aPsf, c0, c1, image, aNoiseModel) :
                           computeEnergyPSF3D_weighted(speedData, mask, maskx, masky, weights, ldata, lreg, aPsf, c0, c1, image, aNoiseModel);
    }
            
    private double computeEnergyPSF2D_weighted(double[][][] speedData, double[][][] mask, double[][][] maskx, double[][][] masky, double[][][] weights, double ldata, double lreg, psf<DoubleType> aPsf, double c0,
            double c1, double[][][] image, NoiseModel aNoiseModel) {
        if (aPsf.isSeparable() == true) {
            SegmentationTools.convolve2Dseparable(speedData[0], mask[0], ni, nj, aPsf, maskx[0], 0, ni);
        }
        else {
            throw new RuntimeException("Error: non-separable PSF calculation are not implemented");
        }

        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                speedData[0][i][j] = (c1 - c0) * speedData[0][i][j] + c0;
            }
        }
        nllMean(speedData, image, speedData, 0, ni, aNoiseModel);

        double energyData = 0;
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                energyData += speedData[0][i][j] * weights[0][i][j];
            }
        }

        fgradx2D(maskx, mask, 0, nj);
        fgrady2D(masky, mask, 0, ni);

        double energyPrior = 0;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    double mkx = maskx[z][i][j];
                    double mky = masky[z][i][j];
                    energyPrior += Math.sqrt(mkx * mkx + mky * mky);
                }
            }
        }

        final double energy = ldata * energyData + lreg * energyPrior;
        return energy;
    }

    private double computeEnergyPSF3D_weighted(double[][][] speedData, double[][][] mask, double[][][] temp, double[][][] temp2, double[][][] weights, double ldata, double lreg, psf<DoubleType> aPsf, double c0, double c1, double[][][] image, NoiseModel aNoiseModel) {
        SegmentationTools.convolve3Dseparable(speedData, mask, ni, nj, nz, aPsf, temp);
    
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    speedData[z][i][j] = (c1 - c0) * speedData[z][i][j] + c0;
                }
            }
        }
    
        nllMean(speedData, image, speedData, aNoiseModel);
        double energyData = 0;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    energyData += speedData[z][i][j] * weights[z][i][j];
                }
            }
        }
    
        fgradx2D(temp, mask);
        double tmp;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    tmp = temp[z][i][j];
                    temp2[z][i][j] = tmp * tmp;
                }
            }
        }
    
        fgrady2D(temp, mask);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    tmp = temp[z][i][j];
                    temp2[z][i][j] += tmp * tmp;
                }
            }
        }
    
        fgradz2D(temp, mask);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    tmp = temp[z][i][j];
                    temp2[z][i][j] += tmp * tmp;
                }
            }
        }
    
        double energyPrior = 0;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    energyPrior += Math.sqrt(temp2[z][i][j]);
                }
            }
        }
    
        final double energy = ldata * energyData + lreg * energyPrior;
        return energy;
    }

    // Round y to z-places after comma
    static double round(double y, final int z) {
        final double factor = Math.pow(10,  z);
        y *= factor;
        y = (int) y;
        y /= factor;
        return y;
    }
    
    static double[][][] normalizeAndConvolveMask(double[][][] aResult, double[][][] Mask, psf<DoubleType> aPsf, double[][][] aTempBuf1, double[][][] aTempBuf2) {
        // normalize Mask
        scale_mask(aTempBuf1, Mask);

        // Convolve the mask
        if (Mask.length == 1) {
            SegmentationTools.convolve2Dseparable(aResult[0], aTempBuf1[0], Mask[0].length, Mask[0][0].length, aPsf, aTempBuf2[0]);
        }
        else {
            SegmentationTools.convolve3Dseparable(aResult, aTempBuf1, Mask[0].length, Mask[0][0].length, Mask.length, aPsf, aTempBuf2);
        }
        
        return aResult;
    }
    
    static private void scale_mask(double[][][] ScaledMask, double[][][] Mask) {
        MinMax<Double> minMax = ArrayOps.findMinMax(Mask);
        ArrayOps.normalize(Mask, ScaledMask, minMax.getMin(), minMax.getMax());
    }
}
