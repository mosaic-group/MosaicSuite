package mosaic.utils.math;

import static org.junit.Assert.assertEquals;

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

}
