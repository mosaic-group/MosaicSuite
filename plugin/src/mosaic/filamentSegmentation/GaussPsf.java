package mosaic.filamentSegmentation;

import mosaic.utils.math.Matrix;

class GaussPsf {
    private static double[][] generateKernel(int xl, int yl, double sigma) {
        final double[][] psf = new double[yl][xl];
        final double middlex = (double)(xl-1)/2;
        final double middley = (double)(yl-1)/2;

        // Generate values
        double sum = 0;
        for (int y = 0; y < yl; ++y) {
            for (int x = 0; x < xl; ++x) {
                final double val = Math.exp(-(Math.pow(x-middlex, 2) + Math.pow(y-middley, 2))/(2 * sigma * sigma));
                psf[y][x] = val;
                sum += val;
            }
        }

        // Normalize
        for (int y = 0; y < yl; ++y) {
            for (int x = 0; x < xl; ++x) {
                psf[y][x] /= sum;
            }
        }

        return psf;
    }

    static Matrix generate(int xl, int yl, double sigma) {
        return new Matrix(generateKernel(xl, yl, sigma));
    }
}
