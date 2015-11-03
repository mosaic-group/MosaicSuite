package mosaic.region_competition.energies;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.Settings;


public class OscillationDetection {
    private final ArrayList<Double> iAllSumsAvg;
    private final double iOscillationThreshold;
    
    private static final double Alpha = 0.1; // exponential moving average factor
    private static final int Length = 10; // how many energies calculation back should be taken into account

    private boolean isFirstRound = true;
    private double iSumAvg = 0;

    public OscillationDetection(Settings aSettings) {
        iOscillationThreshold = aSettings.m_OscillationThreshold;
        iAllSumsAvg = new ArrayList<Double>(aSettings.m_MaxNbIterations);
    }

    public boolean DetectOscillations(Collection<ContourParticle> aParticles) {
        final double sumNew = sumAllEnergies(aParticles);
        
        double oldSumAvg = isFirstRound ? sumNew : iSumAvg;
        iSumAvg = Alpha * sumNew + (1 - Alpha) * oldSumAvg;
        iAllSumsAvg.add(iSumAvg);
    
        if (!isFirstRound) {
            final double totstd = calculateStdDev(iAllSumsAvg);
            final int n = iAllSumsAvg.size();
            final int start = Math.max(0, n - Length);
            final double winstd = calculateStdDev(iAllSumsAvg.subList(start, n));

            if (winstd / totstd < iOscillationThreshold) {
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

    private double calculateMean(List<Double> data) {
        double sum = 0.0;
        for (Double d : data) {
            sum += d;
        }

        return sum / data.size();
    }

    private double calculateStdDev(List<Double> data) {
        final double meanValue = calculateMean(data);

        final int n = data.size();
        double sum = 0;
        for (int i = 0; i < n; ++i) {
            sum += Math.pow(meanValue - data.get(i), 2);
        }
        sum = sum / n;
        
        return Math.sqrt(sum);
    }
}
