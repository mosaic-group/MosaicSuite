package mosaic.region_competition.DRS;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.regions.DRS.AlgorithmDRS;
import mosaic.regions.DRS.Rng;
import mosaic.test.framework.CommonBase;
import mosaic.utils.math.IndexedDiscreteDistribution;


public class AlgorithmDRSTest extends CommonBase {

    @Test
    public void testGenerateDiscreteDistribution() {
        // -------------------  Regular Image with large intensity values
        IntensityImage ii = new IntensityImage(new int[] {2, 3});
        ii.set(2, 1);
        IndexedDiscreteDistribution dd = AlgorithmDRS.generateDiscreteDistribution(ii, new Rng());
        for (int i = 0; i < 10; ++i) {
            // we should get only 2 as sampled index since other probabilities in image are equal 0
            assertEquals(2, dd.sample());
        }
    }
}
