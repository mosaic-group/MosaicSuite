package mosaic.bregman;


import java.util.concurrent.CountDownLatch;

import ij.IJ;
import mosaic.core.psf.psf;
import net.imglib2.type.numeric.real.DoubleType;


class Tools {
    final private int ni, nj, nz, nlevels;
    
    Tools(int nni, int nnj, int nnz) {
        this(nni, nnj, nnz, 1);
    }
    
    Tools(int nni, int nnj, int nnz, int levels) {
        this.ni = nni;
        this.nj = nnj;
        this.nz = nnz;
        this.nlevels = levels;
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
                            sum += in[colIndex][rowIndex] * kernel[mm][nn];
                        }

                    }
                }
                out[i][j] = sum;
            }
        }
        return;
    }

    static void convolve2Dseparable(double[][] out, double[][] in, int icols, int irows, psf<DoubleType> psf, double temp[][]) {
        convolve2Dseparable(out, in, icols, irows, psf, temp, 0, icols);
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
                        if (colIndex < 0) {
                            colIndex = -colIndex - 1;
                        }
                        if (colIndex > icols - 1) {
                            colIndex = icols - (colIndex - icols) - 1;
                        }
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
                        if (rowIndex < 0) {
                            rowIndex = -rowIndex - 1;
                        }
                        if (rowIndex > irows - 1) {
                            rowIndex = irows - (rowIndex - irows) - 1;
                        }
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
                    // IJ.log(" " +k +" " +i+" "+j);
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

    void dctshift(double[][][] result, double[][][] PSF, int cc, int cr) {
        // check if non square image
        final int cols = PSF[0].length;
        final int rows = PSF[0][0].length;
        final int k = Math.min(cr - 1, Math.min(cc - 1, Math.min(rows - cr, cols - cc)));

        final int frow = cr - k;
        final int lrow = cr + k;
        final int rowSize = lrow - frow + 1;

        final int fcol = cc - k;
        final int lcol = cc + k;
        final int colSize = lcol - fcol + 1;

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    result[z][i][j] = 0;
                }
            }
        }

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < 1 + colSize - cc + fcol - 1; i++) {
                for (int j = 0; j < 1 + rowSize - cr + frow - 1; j++) {
                    result[z][i][j] = PSF[z][cc - fcol + i][cr - frow + j];
                }
            }
        }

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < 1 + colSize - cc + fcol - 2; i++) {
                for (int j = 0; j < 1 + rowSize - cr + frow - 1; j++) {
                    result[z][i][j] += PSF[z][cc - fcol + 1 + i][cr - frow + j];
                }
            }
        }

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < 1 + colSize - cc + fcol - 1; i++) {
                for (int j = 0; j < 1 + rowSize - cr + frow - 2; j++) {
                    result[z][i][j] += PSF[z][cc - fcol + i][cr - frow + 1 + j];
                }
            }
        }

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < 1 + colSize - cc + fcol - 2; i++) {
                for (int j = 0; j < 1 + rowSize - cr + frow - 2; j++) {
                    result[z][i][j] += PSF[z][cc - fcol + 1 + i][cr - frow + 1 + j];
                }
            }
        }

        for (int z = 0; z < nz; z++) {
            for (int i = 2 * k + 1; i < cols; i++) {
                for (int j = 2 * k + 1; j < rows; j++) {
                    result[z][i][j] = 0;
                }
            }
        }
    }

    void dctshift3D(double[][][] result, double[][][] PSF, int cr, int cc, int cs) {

        // check if non square image
        final int cols = PSF[0].length;
        final int rows = PSF[0][0].length;
        final int slices = PSF.length;

        final int k = Math.min(cr - 1, Math.min(cc - 1, Math.min(rows - cr, Math.min(cols - cc, Math.min(cs - 1, slices - cs)))));

        final int frow = cr - k;
        final int lrow = cr + k;
        final int rowSize = lrow - frow + 1;

        final int fcol = cc - k;
        final int lcol = cc + k;
        final int colSize = lcol - fcol + 1;

        final int fslice = cs - k;
        final int lslice = cs + k;
        final int sliceSize = lslice - fslice + 1;

        // z1
        for (int z = 0; z < 1 + sliceSize - cs + fslice - 1; z++) {
            for (int i = 0; i < 1 + colSize - cc + fcol - 1; i++) {
                for (int j = 0; j < 1 + rowSize - cr + frow - 1; j++) {
                    result[z][i][j] = PSF[cs - fslice + z][cc - fcol + i][cr - frow + j];
                }
            }
        }

        for (int z = 0; z < 1 + sliceSize - cs + fslice - 1; z++) {
            for (int i = 0; i < 1 + colSize - cc + fcol - 2; i++) {
                for (int j = 0; j < 1 + rowSize - cr + frow - 1; j++) {
                    result[z][i][j] += PSF[cs - fslice + z][cc - fcol + 1 + i][cr - frow + j];
                }
            }
        }

        for (int z = 0; z < 1 + sliceSize - cs + fslice - 1; z++) {
            for (int i = 0; i < 1 + colSize - cc + fcol - 1; i++) {
                for (int j = 0; j < 1 + rowSize - cr + frow - 2; j++) {
                    result[z][i][j] += PSF[cs - fslice + z][cc - fcol + i][cr - frow + 1 + j];
                }
            }
        }

        for (int z = 0; z < 1 + sliceSize - cs + fslice - 1; z++) {
            for (int i = 0; i < 1 + colSize - cc + fcol - 2; i++) {
                for (int j = 0; j < 1 + rowSize - cr + frow - 2; j++) {
                    result[z][i][j] += PSF[cs - fslice + z][cc - fcol + 1 + i][cr - frow + 1 + j];
                }
            }
        }

        // z 2
        for (int z = 0; z < 1 + sliceSize - cs + fslice - 2; z++) {
            for (int i = 0; i < 1 + colSize - cc + fcol - 1; i++) {
                for (int j = 0; j < 1 + rowSize - cr + frow - 1; j++) {
                    result[z][i][j] += PSF[cs - fslice + 1 + z][cc - fcol + i][cr - frow + j];
                }
            }
        }

        for (int z = 0; z < 1 + sliceSize - cs + fslice - 2; z++) {
            for (int i = 0; i < 1 + colSize - cc + fcol - 2; i++) {
                for (int j = 0; j < 1 + rowSize - cr + frow - 1; j++) {
                    result[z][i][j] += PSF[cs - fslice + 1 + z][cc - fcol + 1 + i][cr - frow + j];
                }
            }
        }

        for (int z = 0; z < 1 + sliceSize - cs + fslice - 2; z++) {
            for (int i = 0; i < 1 + colSize - cc + fcol - 1; i++) {
                for (int j = 0; j < 1 + rowSize - cr + frow - 2; j++) {
                    result[z][i][j] += PSF[cs - fslice + 1 + z][cc - fcol + i][cr - frow + 1 + j];
                }
            }
        }

        for (int z = 0; z < 1 + sliceSize - cs + fslice - 2; z++) {
            for (int i = 0; i < 1 + colSize - cc + fcol - 2; i++) {
                for (int j = 0; j < 1 + rowSize - cr + frow - 2; j++) {
                    result[z][i][j] += PSF[cs - fslice + 1 + z][cc - fcol + 1 + i][cr - frow + 1 + j];
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

    void addtab(double[][][] res, double[][][] m1, double[][][] m2) {
        addtab(res, m1, m2, 0, ni);
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

    void subtab(double[][][] res, double[][][] m1, double[][][] m2) {
        subtab(res, m1, m2, 0, ni);
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

    void copytab(double[][][] res, double[][][] m1) {
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    res[z][i][j] = m1[z][i][j];
                }
            }
        }
    }

    void copytab(float[][][] res, float[][][] m1) {
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    res[z][i][j] = m1[z][i][j];
                }
            }
        }
    }

    private void nllMean(double[][][] res, double[][][] image, double[][][] mu) {
        nllMean(res, image, mu, 0, ni);
    }

    private void nllMean(double[][][] res, double[][][] image, double[][][] mu, int iStart, int iEnd) {
        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    res[z][i][j] = noise(image[z][i][j], mu[z][i][j]);
                }
            }
        }
    }

    void nllMean1(double[][][] res, double[][][] image, double mu) {
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    res[z][i][j] = noise(image[z][i][j], mu);
                }
            }
        }
    }

    private double noise(double im, double mu) {
        double res;
        if (mu < 0) {
            mu = 0.0001;
        }
        if (Analysis.p.noise_model == 0) {// poisson

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

    void createmask(double[][][][] res, double[][][] image, double[] cl) {
        // add 0 and 1 at extremities
        final double[] cltemp = new double[nlevels + 2];
        cltemp[0] = 0;
        cltemp[nlevels + 1] = 1;
        for (int l = 1; l < nlevels + 1; l++) {
            cltemp[l] = cl[l - 1];
        }
        double thr;

        for (int l = 0; l < nlevels; l++) {
            if (nlevels > 2) {
                thr = cl[l];
            }
            else {
                thr = cl[1];// if only two regions only first mask is used
            }
            if (thr == 1) {
                thr = 0.5;// should not have threhold to 1: creates
            }
            // empty mask and wrong behavior in dct3D
            // computation
            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        if (image[z][i][j] >= thr) {
                            // image[z][i][j]<=cltemp[l+2])
                            res[l][z][i][j] = 1;
                        }
                        else {
                            res[l][z][i][j] = 0;
                        }
                    }
                }
            }
        }
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

    void shrink2D(double[][][] res1, double[][][] res2, double[][][] u1, double[][][] u2, double t) {
        shrink2D(res1, res2, u1, u2, t, 0, ni);
    }

    void shrink2D(double[][][] res1, double[][][] res2, double[][][] u1, double[][][] u2, double t, int iStart, int iEnd) {
        double norm = 0;
        double u1tmp, u2tmp;

        for (int z = 0; z < nz; z++) {// todo : shrink3D
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    u1tmp = u1[z][i][j];
                    u2tmp = u2[z][i][j];
                    norm = Math.sqrt(u1tmp * u1tmp + u2tmp * u2tmp);
                    if (norm >= t) {
                        res1[z][i][j] = u1[z][i][j] - t * u1[z][i][j] / norm;
                        res2[z][i][j] = u2[z][i][j] - t * u2[z][i][j] / norm;
                    }
                    else {
                        res1[z][i][j] = 0;
                        res2[z][i][j] = 0;
                    }
                }
            }
        }
    }

    void shrink3D(double[][][] res1, double[][][] res2, double[][][] res3, double[][][] u1, double[][][] u2, double[][][] u3, double t) {
        shrink3D(res1, res2, res3, u1, u2, u3, t, 0, ni);
    }

    void shrink3D(double[][][] res1, double[][][] res2, double[][][] res3, double[][][] u1, double[][][] u2, double[][][] u3, double t, int iStart, int iEnd) {
        double norm = 0;
        double u1tmp, u2tmp, u3tmp;

        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    u1tmp = u1[z][i][j];
                    u2tmp = u2[z][i][j];
                    u3tmp = u3[z][i][j];
                    norm = Math.sqrt(u1tmp * u1tmp + u2tmp * u2tmp + u3tmp * u3tmp);
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

    double computeEnergy(double[][][] speedData, double[][][] mask, double[][][] maskx, double[][][] masky, double ldata, double lreg) {

        fgradx2D(maskx, mask);
        fgrady2D(masky, mask);

        double energyData = 0;
        double energyPrior = 0;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    energyData += speedData[z][i][j] * mask[z][i][j];
                    energyPrior += Math.sqrt(Math.pow(maskx[z][i][j], 2) + Math.pow(masky[z][i][j], 2));
                }
            }
        }

        return ldata * energyData + lreg * energyPrior;
    }

    double computeEnergy3D(double[][][] speedData, double[][][] mask, double[][][] maskx, double[][][] masky, double[][][] maskz, double ldata, double lreg) {

        double energyData = 0;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    energyData += speedData[z][i][j] * mask[z][i][j];
                }
            }
        }

        fgradx2D(maskx, mask);
        fgrady2D(masky, mask);
        fgradz2D(maskz, mask);

        double energyPrior = 0;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    energyPrior += Math.sqrt(Math.pow(maskx[z][i][j], 2) + Math.pow(masky[z][i][j], 2) + Math.pow(maskz[z][i][j], 2));
                }
            }
        }
        final double energy = ldata * energyData + lreg * energyPrior;

        return energy;
    }

    double computeEnergyPSF_weighted(double[][][] speedData, double[][][] mask, double[][][] maskx, double[][][] masky, double[][][] weights, double ldata, double lreg, Parameters p, double c0,
            double c1, double[][][] image) {

        if (p.PSF.isSeparable() == true) {
            Tools.convolve2Dseparable(speedData[0], mask[0], ni, nj, p.PSF, maskx[0], 0, ni);
        }
        else {
            IJ.error("Error: non-separable PSF calculation are not implemented");
            return 0.0;
        }

        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                speedData[0][i][j] = (c1 - c0) * speedData[0][i][j] + c0;
            }
        }
        nllMean(speedData, image, speedData, 0, ni);

        double energyData = 0;
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                energyData += speedData[0][i][j] * weights[0][i][j];
            }
        }

        fgradx2D(maskx, mask, 0, nj);
        fgrady2D(masky, mask, 0, ni);

        double energyPrior = 0;
        double mkx, mky;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
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

    double computeEnergyPSF(double[][][] speedData, double[][][] mask, double[][][] maskx, double[][][] masky, double ldata, double lreg, Parameters p, double c0, double c1, double[][][] image,
            int iStart, int iEnd, int jStart, int jEnd, CountDownLatch Sync8, CountDownLatch Sync9) throws InterruptedException {
        Tools.convolve2Dseparable(speedData[0], mask[0], ni, nj, p.PSF, maskx[0], iStart, iEnd);

        for (int i = iStart; i < iEnd; i++) {
            for (int j = 0; j < nj; j++) {
                speedData[0][i][j] = (c1 - c0) * speedData[0][i][j] + c0;
            }
        }

        // }
        // nllMeanPoisson2(speedData, image, speedData, 1, ldata, iStart, iEnd);
        nllMean(speedData, image, speedData, iStart, iEnd);
        double energyData = 0;
        // for (int z=0; z<nz; z++){
        for (int i = iStart; i < iEnd; i++) {
            for (int j = 0; j < nj; j++) {
                energyData += speedData[0][i][j];
            }
        }
        // }

        Sync8.countDown();
        Sync8.await();
        fgradx2D(maskx, mask, jStart, jEnd);
        fgrady2D(masky, mask, iStart, iEnd);

        Sync9.countDown();
        Sync9.await();

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

    double computeEnergyPSF3D(double[][][] speedData, double[][][] mask, double[][][] temp, double[][][] temp2, double ldata, double lreg, Parameters p, double c0, double c1, double[][][] image,
            int iStart, int iEnd, int jStart, int jEnd, CountDownLatch Sync8, CountDownLatch Sync9, CountDownLatch Sync10) throws InterruptedException {

        Tools.convolve3Dseparable(speedData, mask, ni, nj, nz, p.PSF, temp, iStart, iEnd);

        // for (int z=0; z<nz; z++){
        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    speedData[z][i][j] = (c1 - c0) * speedData[z][i][j] + c0;
                }
            }
        }

        // }
        // nllMeanPoisson2(speedData, image, speedData, 1, ldata, iStart, iEnd);
        nllMean(speedData, image, speedData, iStart, iEnd);
        double energyData = 0;
        for (int z = 0; z < nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < nj; j++) {
                    energyData += speedData[z][i][j];
                }
            }
        }

        if (Sync8 != null) {
            Sync8.countDown();
            Sync8.await();
        }

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

        if (Sync10 != null) {
            Sync10.countDown();
            Sync10.await();
        }
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

        if (Sync9 != null) {
            Sync9.countDown();
            Sync9.await();
        }
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

    double computeEnergyPSF3D_weighted(double[][][] speedData, double[][][] mask, double[][][] temp, double[][][] temp2, double[][][] weights, double ldata, double lreg, Parameters p, double c0,
            double c1, double[][][] image

    ) {
        Tools.convolve3Dseparable(speedData, mask, ni, nj, nz, p.PSF, temp);

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    speedData[z][i][j] = (c1 - c0) * speedData[z][i][j] + c0;
                }
            }
        }

        nllMean(speedData, image, speedData);
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

    void mydivergence(double[][][] res, double[][][] m1, double[][][] m2) {
        mydivergence(res, m1, m2, 0, ni, 0, nj);
    }

    private void mydivergence(double[][][] res, double[][][] m1, double[][][] m2, int iStart, int iEnd, int jStart, int jEnd) {
        bgradxdbc2D(res, m1, jStart, jEnd);
        bgradydbc2D(m1, m2, iStart, iEnd);
        addtab(res, res, m1, iStart, iEnd);

    }

    void mydivergence(double[][][] res, double[][][] m1, double[][][] m2, double[][][] temp, CountDownLatch Sync2, int iStart, int iEnd, int jStart, int jEnd) throws InterruptedException {
        bgradxdbc2D(res, m1, jStart, jEnd);
        bgradydbc2D(temp, m2, iStart, iEnd);
        Sync2.countDown();
        Sync2.await();
        addtab(res, res, temp, iStart, iEnd);
    }

    void mydivergence3D(double[][][] res, double[][][] m1, double[][][] m2, double[][][] m3) {
        mydivergence3D(res, m1, m2, m3, 0, ni, 0, nj);
    }

    void mydivergence3D(double[][][] res, double[][][] m1, double[][][] m2, double[][][] m3, double[][][] temp, CountDownLatch Sync2, int iStart, int iEnd, int jStart, int jEnd)
            throws InterruptedException {
        bgradxdbc2D(res, m1, jStart, jEnd);
        bgradydbc2D(temp, m2, iStart, iEnd);
        if (Sync2 != null) {
            Sync2.countDown();
            Sync2.await();
        }
        addtab(res, res, temp, iStart, iEnd);
        bgradzdbc2D(m1, m3, iStart, iEnd);
        addtab(res, res, m1, iStart, iEnd);
    }

    private void mydivergence3D(double[][][] res, double[][][] m1, double[][][] m2, double[][][] m3, int iStart, int iEnd, int jStart, int jEnd) {
        bgradxdbc2D(res, m1, jStart, jEnd);
        bgradydbc2D(m1, m2, iStart, iEnd);
        addtab(res, res, m1, iStart, iEnd);
        bgradzdbc2D(m1, m3, iStart, iEnd);
        addtab(res, res, m1, iStart, iEnd);
    }

    void max_mask(int[][][] res, double[][][][] mask) {
        double max;
        int index_max;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    max = 0;
                    index_max = 0;
                    for (int l = 0; l < nlevels; l++) {
                        if ((mask[l][z][i][j]) >= max) {
                            max = mask[l][z][i][j];
                            index_max = l;
                        }
                    }
                    res[z][i][j] = index_max;
                }
            }
        }
    }

    // Round y to z-places after comma
    static double round(double y, final int z) {
        // Special tip to round numbers to 10^-z
        final double factor = Math.pow(10,  z);
        y *= factor;
        y = (int) y;
        y /= factor;
        return y;
    }
}
