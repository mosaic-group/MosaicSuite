package mosaic.region_competition.DRS;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.util.Pair;
import org.junit.Test;

import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.test.framework.CommonBase;


public class AlgorithmDRSTest extends CommonBase {

    @Test
    public void testGenerateDiscreteDistribution() {
        // -------------------  Regular Image with large intensity values
        IntensityImage ii = new IntensityImage(new int[] {2, 3});
        ii.set(0, 1);
        ii.set(2, 2);
        
        assertEquals(6, AlgorithmDRS.generateDiscreteDistribution(ii, new Rng()).getPmf().size());
        
        // ------------------- Image with too low intensities
        ii.set(0, 0);
        ii.set(2, 1e-10f);
        
        EnumeratedDistribution<Integer> distr = AlgorithmDRS.generateDiscreteDistribution(ii, new Rng());
        assertEquals(6, distr.getPmf().size());
        
        // We should have flat distribution
        for (Pair<Integer, Double> p : distr.getPmf()) {
            assertEquals(1.0 / 6, p.getSecond(), 1e-6); // 1/6 same normalized probability for all pixels
        }
        
        // Check if all pixels of image are changed to 1.0
        Iterator<Point> ri = new SpaceIterator(ii.getDimensions()).getPointIterator();
        while (ri.hasNext()) {
            final Point point = ri.next();
            assertEquals(1.0, ii.get(point), 1e-6);
        }
    }
}
