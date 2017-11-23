package mosaic.utils.math;

import java.util.Arrays;

import org.apache.commons.math3.random.RandomGenerator;


/**
 * Extremely simple Discrete Distribution class taking only pmf as input.
 * Returns via sample() method index of input pmf basing on probability.
 * 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class IndexedDiscreteDistribution {
    protected final RandomGenerator iRng;
    private final double[] iCdf;

    public IndexedDiscreteDistribution(final RandomGenerator rng, double[] pmf) {
        iRng = rng;
        iCdf = StatisticsUtils.calculateCdfFromPmf(pmf, true);
    }

    /**
     * Sample from distribution
     */
    public int sample() {
        final double val = iRng.nextDouble();

        int idx = Arrays.binarySearch(iCdf, val);
        if (idx < 0) {
            // When not exact value is found -idx - 1 is returned pointing to possible inserting
            // location => convert it back
            idx = -idx - 1;
        }
        return idx;
    }
}
