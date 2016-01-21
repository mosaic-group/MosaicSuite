package mosaic.bregman;


import mosaic.utils.ArrayOps;
import mosaic.utils.ArrayOps.MinMax;


/**
 * Regions statistics solver
 * @author i-bird
 */
class RegionStatisticsSolver {

    private final double[][][] Z;
    private final double[][][] W;
    private final double[][][] mu;
    private final int iMaxIter;
    private final Parameters iParams;

    private double[][][] iWeights;
    private final double[][][] KMask;

    public double betaMLEin, betaMLEout;
    private int ni, nj, nz;
    
    /**
     * Create a region statistic solver
     *
     * @param temp1 buffer of the same size of image for internal calculation
     * @param temp2 buffer of the same size of image for internal calculation
     * @param temp3 buffer of the same size of image for internal calculation
     * @param image The image pixel array
     * @param weights - if null they will be set to 1.0
     * @param max_iter Maximum number of iteration for the Fisher scoring
     * @param p
     */
    public RegionStatisticsSolver(double[][][] temp1, double[][][] temp2, double[][][] temp3, double[][][] image, double[][][] weights, int max_iter, Parameters p) {
        iParams = p;
        Z = image;
        W = temp1;
        mu = temp2;
        KMask = temp3;
        iMaxIter = max_iter;
        ni = iParams.ni;
        nj = iParams.nj;
        nz = iParams.nz;
        
        iWeights = weights;
        if (iWeights == null) {
            iWeights = new double[nz][ni][nj];
            ArrayOps.fill(iWeights, 1);
        }
    }

    /**
     * Evaluate the region intensity
     * @param Mask
     */
    public void eval(double[][][] Mask) {
        // normalize Mask
        scale_mask(W, Mask);

        // Convolve the mask
        if (nz == 1) {
            Tools.convolve2Dseparable(KMask[0], W[0], ni, nj, Analysis.p.PSF, mu[0]);
        }
        else {
            Tools.convolve3Dseparable(KMask, W, ni, nj, nz, Analysis.p.PSF, mu);
        }

        betaMLEout = 0;
        betaMLEin = 0;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (Z[z][i][j] != 0) {
                        W[z][i][j] = iWeights[z][i][j] / Z[z][i][j];
                    }
                    else {
                        W[z][i][j] = 4.50359962737e+15;// 1e4;
                    }
                }
            }
        }

        int iter = 0;
        while (iter < iMaxIter) {
            double K11 = 0;
            double K12 = 0;
            double K22 = 0;
            double U1 = 0;
            double U2 = 0;
            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        K11 += W[z][i][j] * Math.pow(1 - KMask[z][i][j], 2);
                        K12 += W[z][i][j] * (1 - KMask[z][i][j]) * KMask[z][i][j];
                        K22 += W[z][i][j] * (KMask[z][i][j]) * KMask[z][i][j];
                        U1 += W[z][i][j] * (1 - KMask[z][i][j]) * Z[z][i][j];
                        U2 += W[z][i][j] * (KMask[z][i][j]) * Z[z][i][j];
                    }
                }
            }

            // detK = K11*K22-K12^2;
            // betaMLE_out = ( K22*U1-K12*U2)/detK;
            // betaMLE_in = (-K12*U1+K11*U2)/detK;
            double detK = K11 * K22 - Math.pow(K12, 2);
            if (detK != 0) {
                betaMLEout = (K22 * U1 - K12 * U2) / detK;
                betaMLEin = (-K12 * U1 + K11 * U2) / detK;
            }
            else {
                betaMLEout = iParams.betaMLEoutdefault;
                betaMLEin = iParams.betaMLEindefault;
            }

            // mu update
            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        mu[z][i][j] = (betaMLEin - betaMLEout) * KMask[z][i][j] + betaMLEout;
                    }
                }
            }

            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        if (mu[z][i][j] != 0) {
                            W[z][i][j] = iWeights[z][i][j] / mu[z][i][j];
                        }
                        else {
                            W[z][i][j] = 4.50359962737e+15;// 10000;//Double.MAX_VALUE;
                        }
                    }
                }
            }

            iter++;
        }
    }

    private void scale_mask(double[][][] ScaledMask, double[][][] Mask) {
        MinMax<Double> minMax = ArrayOps.findMinMax(Mask);
        ArrayOps.normalize(Mask, ScaledMask, minMax.getMin(), minMax.getMax());
    }
}
