package mosaic.regions.energies;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mosaic.regions.RC.ContourParticle;


public class OscillationDetection {
    
    // Settings
    private final double iOscillationThreshold;
    private static final double AverageFactor = 0.1; // exponential moving average factor
    private static final int LengthOfLastResultsWindow = 10; // how many energies calculation back should be taken into account

    // Internal data
    private final ArrayList<Double> iAllSumsAvg;
    private double iSumAvg = 0;
    private boolean isFirstRound = true;

    public OscillationDetection(double aOscillationThreshold, int aMaxNumOfIterations) {
        iOscillationThreshold = aOscillationThreshold; //aSettings.m_OscillationThreshold;
        iAllSumsAvg = new ArrayList<Double>(aMaxNumOfIterations); //aSettings.m_MaxNbIterations);
    }

    /**
     * @return returns true if oscillation detected
     */
    public boolean DetectOscillations(Collection<ContourParticle> aParticles) {
        final double sumNew = sumAllEnergies(aParticles);
        
        double oldSumAvg = isFirstRound ? sumNew : iSumAvg;
        iSumAvg = AverageFactor * sumNew + (1 - AverageFactor) * oldSumAvg;
        iAllSumsAvg.add(iSumAvg);
    
        if (!isFirstRound) {
            final double totstd = calculateStdDev(iAllSumsAvg);
            final int n = iAllSumsAvg.size();
            final int start = Math.max(0, n - LengthOfLastResultsWindow);
            final double winstd = calculateStdDev(iAllSumsAvg.subList(start, n));
            
            if ((winstd / totstd) < iOscillationThreshold) {
                // new oscillation detected
                return true;
            }
        }
        isFirstRound = false;
        
        return false;
    }

    private double sumAllEnergies(Collection<ContourParticle> aContainer) {
        double totalEnergyDiff = 0;
        for (final ContourParticle vPointIterator : aContainer) {
            totalEnergyDiff += vPointIterator.energyDifference;
        }
        
        return totalEnergyDiff;
    }

    /**
     * Calculates a mean of given data
     * @param data
     * @return
     */
    private double calculateMean(List<Double> aData) {
        double sum = 0.0;
        for (double d : aData) {
            sum += d;
        }

        return sum / aData.size();
    }

    /**
     * Calculates standard deviation of given data
     * @param data
     * @return
     */
    private double calculateStdDev(List<Double> aData) {
        final double meanValue = calculateMean(aData);

        final int n = aData.size();
        double sum = 0;
        for (int i = 0; i < n; ++i) {
            sum += Math.pow(meanValue - aData.get(i), 2);
        }
        sum = sum / n;
        
        return Math.sqrt(sum);
    }
}
