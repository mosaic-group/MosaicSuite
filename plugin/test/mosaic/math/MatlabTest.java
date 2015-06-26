package mosaic.math;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MatlabTest {

    @Test
    public void testRegularySpacedArray() {
        {
            // Increasing values, step matching all from start to stop including
            double[] result = {1.0, 3.0, 5.0, 7.0};
            assertArrayEquals(result, Matlab.regularySpacedArray(1, 2, 7), 0.00001);
        }
        {   
            // Chosen step value does not match stop
            double[] result = {1.0, 3.0, 5.0, 7.0};
            assertArrayEquals(result, Matlab.regularySpacedArray(1, 2, 8), 0.00001);
        }    
        {
            // Decreasing values, all values matched including stop value
            double[] result = {8.0, 6.0, 4.0, 2.0};
            assertArrayEquals(result, Matlab.regularySpacedArray(8, -2, 1), 0.00001);
        }    
        {
            // Decreasing values, does not match stop stop value
            double[] result = {7.0, 5.0, 3.0, 1.0};
            assertArrayEquals(result, Matlab.regularySpacedArray(7, -2, 1), 0.00001);
        }    
        {
            // Only first value matched
            double[] result = {5.0};
            assertArrayEquals(result, Matlab.regularySpacedArray(5, 1000, 6), 0.00001);
        }   
        {
            // Not possible to generate array for step equal 0
            double[] result = null;
            assertEquals(result, Matlab.regularySpacedArray(7, 0, 1));
        } 
        {
            // Not possible to generate array with positive step
            double[] result = null;
            assertEquals(result, Matlab.regularySpacedArray(7, 1, 1));
        } 
        {
            // Not possible to generate array with negative step
            double[] result = null;
            assertEquals(result, Matlab.regularySpacedArray(1, -1, 7));
        }
    }
    
    @Test
    public void testRegularySpacedVector() {
        Matrix expected = new Matrix(new double [][] {{4, 8, 12}});
        
        Matrix result = Matlab.regularySpacedVector(4, 4, 12);
        
        assertTrue(expected.compare(result, 0.00001));
    }
    
    @Test
    public void test() {
        Matrix m = new Matrix(new double [][] {{1, 2, 3 , 4, 5, 6 ,7, 8, 9, 10}});
        Matrix r = Matlab.imresize(m, 2);
        System.out.println(r);
        
    }
}
