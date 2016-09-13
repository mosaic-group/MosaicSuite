package mosaic.utils.math;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.special.BesselJ;
import org.junit.Test;


public class MathOpsTest {

    @Test
    public void testFactorial() {
        assertEquals(1, MathOps.factorial(0));
        assertEquals(1, MathOps.factorial(1));
        assertEquals(2, MathOps.factorial(2));
        assertEquals(6, MathOps.factorial(3));
        
        // Returns 1 for forbidden input values (no check is done on input).
        assertEquals(1, MathOps.factorial(-1));
    }

    @Test
    public void testBessel0() {
        // Test fast implementation of bessel funcitons with provided in commons-math3 
        // (which are about 3.5x slower and currently this is the only reason to not change
        // bessel0 to it.
        double epsilon = 1e-8;
        assertEquals(BesselJ.value(0, 0), MathOps.bessel0(0), epsilon);
        assertEquals(BesselJ.value(0, 1), MathOps.bessel0(1), epsilon);
        assertEquals(BesselJ.value(0, 1), MathOps.bessel0(-1), epsilon);
        assertEquals(BesselJ.value(0, 10), MathOps.bessel0(10), epsilon);
        assertEquals(BesselJ.value(0, 10), MathOps.bessel0(-10), epsilon);
    }

    @Test
    public void testBessel1() {
        // Test fast implementation of bessel funcitons with provided in commons-math3 
        // (which are about 3.5x slower and currently this is the only reason to not change
        // bessel1 to it.
        double epsilon = 1e-8;
        assertEquals(0.0, MathOps.bessel1(0), epsilon);
        assertEquals(BesselJ.value(1, 0), MathOps.bessel1(0), epsilon);
        assertEquals(BesselJ.value(1, 1), MathOps.bessel1(1), epsilon);
        assertEquals(-BesselJ.value(1, 1), MathOps.bessel1(-1), epsilon);
        assertEquals(BesselJ.value(1, 10), MathOps.bessel1(10), epsilon);
        assertEquals(-BesselJ.value(1, 10), MathOps.bessel1(-10), epsilon);
    }
}
