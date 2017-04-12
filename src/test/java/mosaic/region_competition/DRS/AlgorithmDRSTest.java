package mosaic.region_competition.DRS;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
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
        
        Pair<Double, EnumeratedDistribution<Integer>> distPair1 = AlgorithmDRS.generateDiscreteDistribution(ii);
        assertEquals(3.0, distPair1.getFirst(), 1e-6);
        assertEquals(6, distPair1.getSecond().getPmf().size());
        
        // ------------------- Image with too low intensities
        ii.set(0, 0);
        ii.set(2, 1e-10f);
        
        Pair<Double, EnumeratedDistribution<Integer>> distPair2 = AlgorithmDRS.generateDiscreteDistribution(ii);
        assertEquals(6.0, distPair2.getFirst(), 1e-6); // expected number of pixels * 1.0
        assertEquals(6, distPair2.getSecond().getPmf().size());

        // We should have flat distribution
        for (Pair<Integer, Double> p : distPair2.getSecond().getPmf()) {
            assertEquals(1.0 / 6, p.getSecond(), 1e-6); // 1/6 same normalized probability for all pixels
        }
        
        // Check if all pixels of image are changed to 1.0
        Iterator<Point> ri = new SpaceIterator(ii.getDimensions()).getPointIterator();
        while (ri.hasNext()) {
            final Point point = ri.next();
            assertEquals(1.0, ii.get(point), 1e-6);
        }
    }

    @Test
    public void ttt() {
        LabelImage li = new LabelImage(new int[] {5, 5});
        for (int i = 0; i < li.getSize(); ++i) li.setLabel(i, i + 1);
        li.save("/tmp/5x5.tif");
    }
    
}
