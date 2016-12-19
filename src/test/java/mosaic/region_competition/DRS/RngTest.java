package mosaic.region_competition.DRS;

import static org.junit.Assert.*;

import org.junit.Test;


public class RngTest {

    @Test
    public void testRng() {
        // Compare with itk MersenneTwister output
        Rng rng = new Rng();
        assertEquals(rng.GetIntegerVariate(10), 6);
        assertEquals(rng.GetUniformVariate(2, 5), 4.71738, 0.00001);
        assertEquals(rng.GetVariate(), 0.835009, 0.00001);
        
        // Default value of seed should be 5489 (as in standard boost / c++ impl).
        rng = new Rng(5489);
        assertEquals(rng.GetIntegerVariate(10), 6);
        assertEquals(rng.GetUniformVariate(2, 5), 4.71738, 0.00001);
        assertEquals(rng.GetVariate(), 0.835009, 0.00001);
    }
}
