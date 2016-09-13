package mosaic.bregman.segmentation;


import mosaic.utils.ArrayOps;


/**
 * Regions statistics solver
 * @author i-bird
 */
class RegionStatisticsSolver {

    private final double[][][] Z;
    private final double[][][] W;
    private final double[][][] mu;
    private final int iMaxIter;
    private final int ni, nj, nz;
    private final double iDefaultBetaMleOut; 
    private final double iDefaultBetaMleIn;

    private final double[][][] iWeights;

    double betaMLEin, betaMLEout;
    
    /**
     * Create a region statistic solver
     * @param temp1 buffer of the same size of image for internal calculation
     * @param temp2 buffer of the same size of image for internal calculation
     * @param image The image pixel array
     * @param weights - if null they will be set to 1.0
     * @param max_iter Maximum number of iteration for the Fisher scoring
     * @param aDefaultBetaMleOut
     * @param aDefaultBetaMleIn
     */
    RegionStatisticsSolver(double[][][] temp1, double[][][] temp2, double[][][] image, double[][][] weights, int max_iter, double aDefaultBetaMleOut, double aDefaultBetaMleIn) {
        Z = image;
        W = temp1;
        mu = temp2;
        iMaxIter = max_iter;
        ni = image[0].length;
        nj = image[0][0].length;
        nz = image.length;
        iDefaultBetaMleOut = aDefaultBetaMleOut;
        iDefaultBetaMleIn = aDefaultBetaMleIn;
        
        if (weights == null) {
            iWeights = new double[nz][ni][nj];
            ArrayOps.fill(iWeights, 1);
        }
        else {
            iWeights = weights;
        }
    }

    /**
     * Evaluate the region intensity
     * @param Mask
     */
    void eval(double[][][] KMask) {
        betaMLEout = 0;
        betaMLEin = 0;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (Z[z][i][j] != 0) {
                        W[z][i][j] = iWeights[z][i][j] / Z[z][i][j];
                    }
                    else {
                        W[z][i][j] = 4.50359962737e+15;
                    }
                }
            }
        }

        int iter = 0;
        while (iter++ < iMaxIter) {
            double K11 = 0;
            double K12 = 0;
            double K22 = 0;
            double U1 = 0;
            double U2 = 0;
            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        final double maskVal = KMask[z][i][j];
                        K11 += W[z][i][j] * Math.pow(1 - maskVal, 2);
                        K12 += W[z][i][j] * (1 - maskVal) * maskVal;
                        K22 += W[z][i][j] * maskVal * maskVal;
                        U1 += W[z][i][j] * (1 - maskVal) * Z[z][i][j];
                        U2 += W[z][i][j] * maskVal * Z[z][i][j];
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
                betaMLEout = iDefaultBetaMleOut;
                betaMLEin = iDefaultBetaMleIn;
            }

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
                            W[z][i][j] = 4.50359962737e+15;
                        }
                    }
                }
            }
        }
    }
}
