package mosaic.ia;


import java.util.Arrays;
import java.util.Random;

import org.apache.log4j.Logger;

import ij.IJ;
import mosaic.ia.Potentials.Potential;


class HypothesisTesting {
    private static final Logger logger = Logger.getLogger(HypothesisTesting.class);
    
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

    public TestResult rankTest() {
        double[] T = calculateT();
        double observedT = -1 * Math.signum(iBestPointFound[0]) * iPotential.calculateWithoutEpsilon(iNearestNeighborDistancesXtoY, iBestPointFound).getSumPotential();
        int i = 0;
        while(i < iNumOfMcRuns) {
            if (observedT <= T[i]) break;
            ++i;
        }
        
        logger.debug("minT=" + T[0] + " maxT=" + T[iNumOfMcRuns - 1] + " observedT=" + observedT + " found at rank:" + i + "/" + iNumOfMcRuns);
        
        String s = "";
        if (i > (int) ((1 - iAlpha) * iNumOfMcRuns)) {
            s = "Null hypothesis: No interaction - Rejected, rank: " + i + " out of " + iNumOfMcRuns + " MC runs with alpha= " + iAlpha;
            s += (i == iNumOfMcRuns) ? (" p-value < " + 1.0 / iNumOfMcRuns) : 
                                       (" p-value: " + ((double) iNumOfMcRuns - i) / iNumOfMcRuns);
        }
        else {
            s = "Null hypothesis accepted, rank: " + i + " out of " + iNumOfMcRuns + " MC runs with alpha= " + iAlpha + " p-value: " + ((double) iNumOfMcRuns - i) / iNumOfMcRuns;
        }
        
        logger.debug(s);
        IJ.showMessage(s);
        
        return new TestResult (
                        i,
                        iNumOfMcRuns,
                        iAlpha,
                        ((double) iNumOfMcRuns - i) / iNumOfMcRuns,
                        i > (int) ((1 - iAlpha) * iNumOfMcRuns)
                    );
    }
    
    static public class TestResult {
        TestResult(int aRank, int aMcmcRuns, double aAlpha, double aPvalue, boolean aNullHypothesisRejected) {
            iRank = aRank;
            iMcmcRuns = aMcmcRuns;
            iAlpha = aAlpha;
            iPvalue = aPvalue;
            iNullHypothesisRejected = aNullHypothesisRejected;
        }
        
        int iRank; 
        int iMcmcRuns; 
        double iAlpha; 
        double iPvalue; 
        boolean iNullHypothesisRejected;
        
        @Override
        public String toString() {
            return "Rank=" + iRank + "/" + iMcmcRuns + " alpha=" + iAlpha + " p-value=" + iPvalue + " NullHyptothesisRejected=" + iNullHypothesisRejected;
        }
    }

    private double[] calculateT() {
        double[] distancesSample = new double[iNearestNeighborDistancesXtoY.length];
        double[] T = new double[iNumOfMcRuns];
        
        for (int i = 0; i < iNumOfMcRuns; ++i) {
            sampleDistancesFromContextQd(distancesSample);
            T[i] = -1 * Math.signum(iBestPointFound[0]) * iPotential.calculateWithoutEpsilon(distancesSample, iBestPointFound).getSumPotential();
        }
        
        Arrays.sort(T);
        return T;
    }

    private void sampleDistancesFromContextQd(double[] aSampleOfDistancesFromContextQd) {
        final Random rng = new Random(System.nanoTime());

        for (int i = 0; i < aSampleOfDistancesFromContextQd.length;) {
            double R = rng.nextDouble();
            // to make sure that random value will be in CDF range
            if (R >= iContextQdCdf[0]) {
                aSampleOfDistancesFromContextQd[i] = findDistanceForPropability(R);
                ++i;
            }
        }
    }

    private double findDistanceForPropability(double aProbabilityQd) {
        int i = 0;
        for (i = 0; i < iContextQdCdf.length - 1; ++i) {
            if (aProbabilityQd >= iContextQdCdf[i] && aProbabilityQd < iContextQdCdf[i + 1]) break;
        }
        return linearInterpolation(iContextQdCdf[i], iContextQdCdf[i + 1], iContextQdDistancesGrid[i], iContextQdDistancesGrid[i + 1], aProbabilityQd);
    }
    
    private static double linearInterpolation(double aXmin, double aXmax, double aYmin, double aYmax, double aXpoint) {
        final double a = (aYmin - aYmax) / (aXmin - aXmax);
        final double b = aYmin - a * aXmin;
        return a * aXpoint + b;
    }
}
