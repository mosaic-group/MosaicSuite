package mosaic.bregman.solver;


import java.util.concurrent.CountDownLatch;

import mosaic.bregman.solver.SolverParameters.NoiseModel;
import mosaic.core.psf.psf;
import mosaic.utils.ArrayOps;
import net.imglib2.type.numeric.real.DoubleType;


class SolverTools {
    final private int ni, nj, nz;
    
    SolverTools(int nni, int nnj, int nnz) {
        ni = nni;
        nj = nnj;
        nz = nnz;
    }

    // convolution with symmetric boundaries extension
    static void convolve2D(double[][] out, double[][] in, int icols, int irows, psf<DoubleType> psf) {
        // find center position of kernel (half of kernel size)
        final int sz[] = psf.getSuggestedImageSize();
        int kCenterX = sz[0] / 2;
        int kCenterY = sz[1] / 2;
        final double kernel[][] = psf.getImage2DAsDoubleArray();

        for (int i = 0; i < icols; ++i) // columns
        {
            for (int j = 0; j < irows; ++j) // rows
            {
                double sum = 0; // init to 0 before sum
                for (int m = 0; m < sz[0]; ++m) // kernel cols
                {
                    int mm = sz[0] - 1 - m; // col index of flipped kernel

                    for (int n = 0; n < sz[1]; ++n) // kernel rows
                    {
                        int nn = sz[1] - 1 - n; // row index of flipped kernel

                        // index of input signal, used for checking boundary
                        int colIndex = i + m - kCenterX;
                        int rowIndex = j + n - kCenterY;

                        if (rowIndex >= 0 && rowIndex < irows && colIndex >= 0 && colIndex < icols) {
                            sum += in[colIndex][rowIndex] * kernel[mm][nn];
                        }
                        else {
                            do {
                                if (rowIndex < 0) {
                                    rowIndex = -rowIndex - 1;
                                }
                                if (rowIndex > irows - 1) {
                                    rowIndex = irows - (rowIndex - irows) - 1;
                                }

                                if (colIndex < 0) {
                                    colIndex = -colIndex - 1;
                                }
                                if (colIndex > icols - 1) {
                                    colIndex = icols - (colIndex - icols) - 1;
                                }
                            } while  (!(rowIndex >= 0 && rowIndex < irows && colIndex >= 0 && colIndex < icols));
                            sum += in[colIndex][rowIndex] * kernel[mm][nn];
                        }

                    }
                }
                out[i][j] = sum;
            }
        }
        return;
    }

    static void convolve2Dseparable(double[][] out, double[][] in, int icols, int irows, psf<DoubleType> psf, double[][] temp, int iStart, int iEnd) {
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

    static void convolve3Dseparable(double[][][] out, double[][][] in, int icols, int irows, int islices, psf<DoubleType> psf, double temp[][][]) {
        convolve3Dseparable(out, in, icols, irows, islices, psf, temp, 0, icols);
    }

    static void convolve3Dseparable(double[][][] out, double[][][] in, int icols, int irows, int islices, psf<DoubleType> psf, double temp[][][], int iStart, int iEnd) {
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
                            if (colIndex < 0) {
                                colIndex = -colIndex - 1;
                            }
                            if (colIndex > icols - 1) {
                                colIndex = icols - (colIndex - icols) - 1;
                            }
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
                            if (rowIndex < 0) {
                                rowIndex = -rowIndex - 1;
                            }
                            if (rowIndex > irows - 1) {
                                rowIndex = irows - (rowIndex - irows) - 1;
                            }
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
                            if (sliceIndex < 0) {
                                sliceIndex = Math.min(islices - 1, -sliceIndex - 1);
                            }
                            if (sliceIndex > islices - 1) {
                                sliceIndex = Math.max(0, islices - (sliceIndex - islices) - 1);
                            }
                            sum += temp[sliceIndex][colIndex][rowIndex] * kernelz[ll];
                        }
                    }
                    out[k][i][j] = sum;
                }
            }
        }

        return;
    }

    void dctshift(double[][] result, double[][] PSF, int cc, int cr) {
        // check if non square image
        final int cols = PSF.length;
        final int rows = PSF[0].length;
        final int k = Math.min(cr - 1, Math.min(cc - 1, Math.min(rows - cr, cols - cc)));

        ArrayOps.fill(result, 0);
        for (int rowShift = 0; rowShift <= 1; rowShift++) 
        for (int colShift = 0; colShift <= 1; colShift++)
        {
            for (int i = 0; i < 1 + k - colShift; i++) {
                for (int j = 0; j < 1 + k - rowShift; j++) {
                    result[i][j] += PSF[k + i + colShift][k + j + rowShift];
                }
            }
        }

        for (int i = 2 * k + 1; i < cols; i++) {
            for (int j = 2 * k + 1; j < rows; j++) {
                result[i][j] = 0;
            }
        }
    }

    void dctshift3D(double[][][] result, double[][][] PSF, int cr, int cc, int cs) {
        final int slices = PSF.length;
        final int cols = PSF[0].length;
        final int rows = PSF[0][0].length;

        final int k = Math.min(cr - 1, Math.min(cc - 1, Math.min(rows - cr, Math.min(cols - cc, Math.min(cs - 1, slices - cs)))));

        ArrayOps.fill(result, 0);
        for (int sliShift = 0; sliShift <= 1; sliShift++)
        for (int rowShift = 0; rowShift <= 1; rowShift++) 
        for (int colShift = 0; colShift <= 1; colShift++)
        {
            for (int z = 0; z <  1 + k - sliShift; z++) {
                for (int i = 0; i < 1 + k - colShift; i++) {
                    for (int j = 0; j < 1 + k - rowShift; j++) {
                        result[z][i][j] += PSF[k + z + sliShift][k + i + colShift][k + j + rowShift];
                    }
                }
            }
        }

        for (int z = 2 * k + 1; z < slices; z++) {
            for (int i = 2 * k + 1; i < cols; i++) {
                for (int j = 2 * k + 1; j < rows; j++) {
                    result[z][i][j] = 0;
                }
            }
        }
    }
    
    void addtab(double[][][] res, double[][][] m1, double[][][] m2, int iStart, int iEnd) {
        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    res[z][i][j] = m1[z][i][j] + m2[z][i][j];
                }
            }
        }
    }

    void subtab(double[][][] res, double[][][] m1, double[][][] m2, int iStart, int iEnd) {
        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    res[z][i][j] = m1[z][i][j] - m2[z][i][j];
                }
            }
        }
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

    void fgradz2D(double[][][] res, double[][][] im) {
        fgradz2D(res, im, 0, ni);
    }

    void fgradz2D(double[][][] res, double[][][] im, int tStart, int tEnd) {
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

    void fgradx2D(double[][][] res, double[][][] im) {
        fgradx2D(res, im, 0, nj);
    }

    void fgradx2D(double[][][] res, double[][][] im, int tStart, int tEnd) {
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

    void fgrady2D(double[][][] res, double[][][] im) {
        fgrady2D(res, im, 0, ni);
    }

    // if x and y do same chunk : possibility to remove one synchronization
    // after each gradient computation
    void fgrady2D(double[][][] res, double[][][] im, int tStart, int tEnd) {
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

    private void bgradxdbc2D(double[][][] res, double[][][] im, int tStart, int tEnd) {
        for (int z = 0; z < nz; z++) {
            for (int i = 1; i < ni - 1; i++) {
                for (int j = tStart; j < tEnd; j++) {
                    res[z][i][j] = -im[z][i - 1][j] + im[z][i][j];
                }
            }

            for (int j = tStart; j < tEnd; j++) {
                // dirichlet boundary right
                res[z][ni - 1][j] = -im[z][ni - 2][j];
                // dirichlet boundary left
                res[z][0][j] = im[z][0][j];
            }
        }
    }

    private void bgradzdbc2D(double[][][] res, double[][][] im, int tStart, int tEnd) {

        for (int z = 1; z < nz - 1; z++) {
            for (int i = tStart; i < tEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    res[z][i][j] = -im[z - 1][i][j] + im[z][i][j];
                }
            }
        }

        // bottom slice dirichlet
        for (int i = tStart; i < tEnd; i++) {
            for (int j = 0; j < nj; j++) {
                // dirichlet boundary left
                res[0][i][j] = im[0][i][j];
            }
        }

        // upper slice dirichlet
        for (int i = tStart; i < tEnd; i++) {
            for (int j = 0; j < nj; j++) {
                // dirichlet boundary right
                res[nz - 1][i][j] = -im[nz - 2][i][j];
            }
        }
    }

    private void bgradydbc2D(double[][][] res, double[][][] im, int tStart, int tEnd) {
        for (int z = 0; z < nz; z++) {
            for (int i = tStart; i < tEnd; i++) {
                for (int j = 1; j < nj - 1; j++) {
                    res[z][i][j] = -im[z][i][j - 1] + im[z][i][j];
                }
            }
            // dirich boundary
            for (int i = tStart; i < tEnd; i++) {
                res[z][i][nj - 1] = 0;// ??

                // dirichlet boundary top
                res[z][i][nj - 1] = -im[z][i][nj - 2];
                // dirichlet boundary top
                res[z][i][0] = im[z][i][0];
            }
        }
    }

    void shrink2D(double[][][] res1, double[][][] res2, double[][][] u1, double[][][] u2, double t, int iStart, int iEnd) {
        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    double u1tmp = u1[z][i][j];
                    double u2tmp = u2[z][i][j];
                    double norm = Math.sqrt(u1tmp * u1tmp + u2tmp * u2tmp);
                    if (norm >= t) {
                        res1[z][i][j] = u1tmp - t * u1tmp / norm;
                        res2[z][i][j] = u2tmp - t * u2tmp / norm;
                    }
                    else {
                        res1[z][i][j] = 0;
                        res2[z][i][j] = 0;
                    }
                }
            }
        }
    }

    void shrink3D(double[][][] res1, double[][][] res2, double[][][] res3, double[][][] u1, double[][][] u2, double[][][] u3, double t, int iStart, int iEnd) {
        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    double u1tmp = u1[z][i][j];
                    double u2tmp = u2[z][i][j];
                    double u3tmp = u3[z][i][j];
                    double norm = Math.sqrt(u1tmp * u1tmp + u2tmp * u2tmp + u3tmp * u3tmp);
                    if (norm >= t) {
                        res1[z][i][j] = u1tmp - t * u1tmp / norm;
                        res2[z][i][j] = u2tmp - t * u2tmp / norm;
                        res3[z][i][j] = u3tmp - t * u3tmp / norm;
                    }
                    else {
                        res1[z][i][j] = 0;
                        res2[z][i][j] = 0;
                        res3[z][i][j] = 0;
                    }
                }
            }
        }
    }

    double computeEnergyPSF(double[][][] speedData, double[][][] mask, double[][][] maskx, double[][][] masky, double ldata, double lreg, psf<DoubleType> aPsf, double c0, double c1, double[][][] image,
            int iStart, int iEnd, int jStart, int jEnd, CountDownLatch Sync8, CountDownLatch Sync9, NoiseModel aNoiseModel) throws InterruptedException {
        SolverTools.convolve2Dseparable(speedData[0], mask[0], ni, nj, aPsf, maskx[0], iStart, iEnd);

        for (int i = iStart; i < iEnd; i++) {
            for (int j = 0; j < nj; j++) {
                speedData[0][i][j] = (c1 - c0) * speedData[0][i][j] + c0;
            }
        }

        nllMean(speedData, image, speedData, iStart, iEnd, aNoiseModel);
        double energyData = 0;
        for (int i = iStart; i < iEnd; i++) {
            for (int j = 0; j < nj; j++) {
                energyData += speedData[0][i][j];
            }
        }

        synchronizedWait(Sync8);
        fgradx2D(maskx, mask, jStart, jEnd);
        fgrady2D(masky, mask, iStart, iEnd);

        synchronizedWait(Sync9);
        
        double energyPrior = 0;
        double mkx, mky;
        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    mkx = maskx[z][i][j];
                    mky = masky[z][i][j];
                    energyPrior += Math.sqrt(mkx * mkx + mky * mky);
                }
            }
        }

        final double energy = ldata * energyData + lreg * energyPrior;

        return energy;
    }

    double computeEnergyPSF3D(double[][][] speedData, double[][][] mask, double[][][] temp, double[][][] temp2, double ldata, double lreg, psf<DoubleType> aPsf, double c0, double c1, double[][][] image,
            int iStart, int iEnd, int jStart, int jEnd, CountDownLatch Sync8, CountDownLatch Sync9, CountDownLatch Sync10, NoiseModel aNoiseModel) throws InterruptedException {

        SolverTools.convolve3Dseparable(speedData, mask, ni, nj, nz, aPsf, temp, iStart, iEnd);

        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    speedData[z][i][j] = (c1 - c0) * speedData[z][i][j] + c0;
                }
            }
        }

        nllMean(speedData, image, speedData, iStart, iEnd, aNoiseModel);
        double energyData = 0;
        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    energyData += speedData[z][i][j];
                }
            }
        }

        synchronizedWait(Sync8);

        fgradx2D(temp, mask, jStart, jEnd);
        double tmp;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = jStart; j < jEnd; j++) {
                    tmp = temp[z][i][j];
                    temp2[z][i][j] = tmp * tmp;
                }
            }
        }

        synchronizedWait(Sync10);
        
        fgrady2D(temp, mask, iStart, iEnd);
        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    tmp = temp[z][i][j];
                    temp2[z][i][j] += tmp * tmp;
                }
            }
        }
        fgradz2D(temp, mask, iStart, iEnd);
        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    tmp = temp[z][i][j];
                    temp2[z][i][j] += tmp * tmp;
                }
            }
        }

        synchronizedWait(Sync9);
        
        double energyPrior = 0;
        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    energyPrior += Math.sqrt(temp2[z][i][j]);
                }
            }
        }

        final double energy = ldata * energyData + lreg * energyPrior;
        return energy;
    }

    void mydivergence(double[][][] res, double[][][] m1, double[][][] m2, double[][][] temp, CountDownLatch Sync2, int iStart, int iEnd, int jStart, int jEnd) throws InterruptedException {
        bgradxdbc2D(res, m1, jStart, jEnd);
        bgradydbc2D(temp, m2, iStart, iEnd);
        synchronizedWait(Sync2);
        addtab(res, res, temp, iStart, iEnd);
    }

    void mydivergence3D(double[][][] res, double[][][] m1, double[][][] m2, double[][][] m3, double[][][] temp, CountDownLatch Sync2, int iStart, int iEnd, int jStart, int jEnd) throws InterruptedException {
        bgradxdbc2D(res, m1, jStart, jEnd);
        bgradydbc2D(temp, m2, iStart, iEnd);
        synchronizedWait(Sync2);
        addtab(res, res, temp, iStart, iEnd);
        bgradzdbc2D(m1, m3, iStart, iEnd);
        addtab(res, res, m1, iStart, iEnd);
    }

    static void synchronizedWait(final CountDownLatch aSyncLatch) throws InterruptedException {
        if (aSyncLatch != null) {
            aSyncLatch.countDown();
            aSyncLatch.await();
        }
    }

    // Round y to z-places after comma
    static double round(double y, final int z) {
        final double factor = Math.pow(10,  z);
        y *= factor;
        y = (int) y;
        y /= factor;
        return y;
    }
}
