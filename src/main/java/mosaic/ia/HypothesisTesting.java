package mosaic.ia;


import java.util.Arrays;
import java.util.Random;

import ij.IJ;
import mosaic.ia.Potentials.Potential;
import mosaic.utils.math.StatisticsUtils;
import mosaic.utils.math.StatisticsUtils.MinMaxMean;


class HypothesisTesting {
    // Input Distributions
    private final double[] iContextQdCdf;
    private final double[] iContextQdDistancesGrid;
    private final double[] iNearestNeighborDistancesXtoY;
    
    // Potential Calculator
    private final double[] iBestPointFound;
    private final Potential iPotential;
    
    // Monte-Carlo params
    private final int iNumOfMcRuns;
    private final double iAlpha;

    public HypothesisTesting(double[] aContextQdCdf, double[] aContextQdDistancesGrid, double[] aNearestNeighborDistancesXtoY, double[] aBestPointFound, Potential aPotential, int aNumOfMcRuns, double aAlpha) {
        iContextQdCdf = aContextQdCdf;
        iContextQdDistancesGrid = aContextQdDistancesGrid;
        iNearestNeighborDistancesXtoY = aNearestNeighborDistancesXtoY;
        
        iBestPointFound = aBestPointFound;
        iPotential = aPotential;
        
        iNumOfMcRuns = aNumOfMcRuns;
        iAlpha = aAlpha;
    }

    public boolean rankTest() {
        double[] DRand = new double[iNearestNeighborDistancesXtoY.length];
        double[] T = new double[iNumOfMcRuns];
        calculateT(DRand, T);
        
        double Tob = -1 * iPotential.calculateWithoutEpsilon(iNearestNeighborDistancesXtoY, iBestPointFound).getSumPotential();
        int i = 0;
        for (i = 0; i < iNumOfMcRuns; i++) {
            if (Tob <= T[i]) {
                break;
            }
        }
        
        MinMaxMean mmm = StatisticsUtils.getMinMaxMean(T);
        System.out.println("MinT: " + mmm.min + " maxT: " + mmm.max);
        System.out.println("T obs: " + Tob + " found at rank: " + i);
        
        if (i > (int) ((1 - iAlpha) * iNumOfMcRuns)) {
            if ((iNumOfMcRuns - i) == 0) {
                System.out.println("Null hypothesis rejected, rank: " + i + " out of " + iNumOfMcRuns + " p-value: " + 1.0 / iNumOfMcRuns);
                IJ.showMessage("Null hypothesis: No interaction - Rejected, rank: " + i + " out of " + iNumOfMcRuns + "MC runs with alpha= " + iAlpha + " p-value < " + 1.0 / iNumOfMcRuns);
            }
            else {
                System.out.println("Null hypothesis rejected, rank: " + i + " out of " + iNumOfMcRuns + " p-value: " + ((double) iNumOfMcRuns - i) / iNumOfMcRuns);
                IJ.showMessage("Null hypothesis: No interaction - Rejected, rank: " + i + " out of " + iNumOfMcRuns + "MC runs with alpha= " + iAlpha + " p-value: " + ((double) iNumOfMcRuns - i) / iNumOfMcRuns);
            }
            return true;
        }
        IJ.showMessage("Null hypothesis accepted, rank: " + i + " out of " + iNumOfMcRuns + " MC runs with alpha= " + iAlpha + " p-value: " + ((double) iNumOfMcRuns - i) / iNumOfMcRuns);
        System.out.println("Null hypothesis: No interaction - Accepted, rank: " + i + " out of " + iNumOfMcRuns + " MC runs with alpha= " + iAlpha + " p-value: " + ((double) iNumOfMcRuns - i) / iNumOfMcRuns);
        return false;
    }

    private void calculateT(double[] DRand, double[] T) {
        for (int i = 0; i < iNumOfMcRuns; i++) {
            generateRandomDistances(DRand);
            T[i] = -1 *  iPotential.calculateWithoutEpsilon(DRand, iBestPointFound).getSumPotential();
        }
        Arrays.sort(T);
    }

    private void generateRandomDistances(double[] DRand) {
        final Random rn = new Random(System.nanoTime());

        for (int i = 0; i < iNearestNeighborDistancesXtoY.length;) {
            double R = rn.nextDouble();
            // to make sure that random value will be in CDF range
            if (R >= iContextQdCdf[0]) {
                DRand[i] = findDistance(R);
                i++;
            }
        }
    }

    private double findDistance(double R) {
        int i;
        for (i = 0; i < iContextQdCdf.length - 1; i++) {
            if (R >= iContextQdCdf[i] && R < iContextQdCdf[i + 1]) {
                break;
            }
        }
        return linearInterpolation(iContextQdCdf[i], iContextQdCdf[i + 1], iContextQdDistancesGrid[i], iContextQdDistancesGrid[i + 1], R);
    }
    
    private static double linearInterpolation(double aXmin, double aXmax, double aYmin, double aYmax, double aXpoint) {
        final double a = (aYmin - aYmax) / (aXmin - aXmax);
        final double b = aYmin - a * aXmin;
        return a * aXpoint + b;
    }
}
