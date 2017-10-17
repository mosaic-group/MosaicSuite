package mosaic.filamentSegmentation;

import mosaic.utils.math.Matrix;

public class GaussPsf {
    /**
     * Generates psf array with coordinates [y][x]
     * @param xl len of x direction
     * @param yl len o y direction
     * @param sigma sigma of Gaussian
     */
    public static double[][] generateKernel(int xl, int yl, double sigma) {
        final double[][] psf = new double[yl][xl];
        final double middlex = (double)(xl-1)/2;
        final double middley = (double)(yl-1)/2;

        // Generate values
        double sum = 0;
        for (int y = 0; y < yl; ++y) {
            for (int x = 0; x < xl; ++x) {
                // e^( - ((x-mx)^2 + (y-my)^2) / 2*sigma^2 ) 
                // skip 1/sqrt(2*pi*sigma^2) since kernel is normalized anyway and this is just constant
                final double val = Math.exp(-(Math.pow(x-middlex, 2) + Math.pow(y-middley, 2))/(2 * sigma * sigma));
                psf[y][x] = val;
                sum += val;
            }
        }

        // Normalize
        if (sum != 0) {
            for (int y = 0; y < yl; ++y) {
                for (int x = 0; x < xl; ++x) {
                    psf[y][x] /= sum;
                }
            }
        }

        return psf;
    }

    public static Matrix generate(int xl, int yl, double sigma) {
        return new Matrix(generateKernel(xl, yl, sigma));
    }
}
