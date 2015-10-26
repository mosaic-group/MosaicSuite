package mosaic.region_competition.energies;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mosaic.core.image.Point;
import mosaic.region_competition.Algorithm;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.Settings;


public class OscillationDetection {
    private final Algorithm algorithm;
    private final double alpha = 0.1; // exponential moving average factor
    private ArrayList<Double> sums;
    private double sumAvg;
    private ArrayList<Double> sumsAvg;
    private boolean isFirstRound;
    private final int length = 10;
    private final double threshold;

    public OscillationDetection(Algorithm algo, Settings settings) {
        this.algorithm = algo;
        
        threshold = settings.m_OscillationThreshold;
        initMembeers(settings.m_MaxNbIterations);
    }

    private void initMembeers(int maxIt) {
        sums = new ArrayList<Double>(maxIt);
        sumsAvg = new ArrayList<Double>(maxIt);
        sumAvg = 0;
        isFirstRound = true;
    }
    
    public boolean DetectOscillations(HashMap<Point, ContourParticle> m_Candidates) {
        boolean result = false;
        final double sumNew = SumAllEnergies(m_Candidates);
    
        sums.add(sumNew);
    
        double oldSumAvg = sumAvg;
        if (isFirstRound) {
            oldSumAvg = sumNew;
        }
    
        final double newSumAvg = alpha * sumNew + (1 - alpha) * oldSumAvg;
        sumsAvg.add(newSumAvg);
        sumAvg = newSumAvg;
    
        final double totstd = std(sumsAvg);
        final int n = sumsAvg.size();
        final int start = Math.max(0, n - length);
        final double winstd = std(sumsAvg.subList(start, n));
    
        double fac = 1;
        if (!isFirstRound) {
            fac = winstd / totstd;
        }
    
        debug("sum=" + sumNew + " sumAvg=" + sumAvg);
        debug("fac=" + fac);
    
        isFirstRound = false;
    
        if (fac < threshold) {
            result = true;
            debug("***NEW Oscillation detected***");
            algorithm.m_AcceptedPointsFactor /= 2.0;
        }
        return result;
    }

    private double SumAllEnergies(HashMap<Point, ContourParticle> aContainer) {
        double vTotalEnergyDiff = 0;

        for (final ContourParticle vPointIterator : aContainer.values()) {
            vTotalEnergyDiff += vPointIterator.energyDifference;
        }
        return vTotalEnergyDiff;
    }

    private double mean(List<Double> data) {
        double sum = 0.0;
        for (Double d : data) {
            sum += d;
        }

        return sum / data.size();
    }

    private double std(List<Double> data) {
        final double m = mean(data);

        final int n = data.size();
        double sum = 0;
        for (int i = 0; i < n; i++) {
            final double d = m - data.get(i);
            sum += (d * d);
        }
        sum = sum / n;
        
        return Math.sqrt(sum);
    }

    private static void debug(@SuppressWarnings("unused") String s) {
        //System.out.println(s);
    }
}
