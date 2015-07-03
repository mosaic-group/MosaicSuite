package mosaic.math;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class MatrixTest extends CommonBase {

    @Test
    public void testResize() {
        int startRow = 0;
        int startCol = 1;
        int step = 2;
        Matrix expected = new Matrix(new double[][] {{ 2,  4},
                                                     {10, 12}});
        
        Matrix input = new Matrix(new double[][] {{ 1,  2,  3,  4}, 
                                                  { 5,  6,  7,  8}, 
                                                  { 9, 10, 11, 12}});
        // Tested method
        Matrix result = input.resize(startRow, startCol, step, step);

        assertEquals(expected, result);
    }
    
    @Test
    public void testProcess() {
        final Matrix input = new Matrix(new double[][] {{ 1,  2,  3,  4}, 
                                                        { 5,  6,  7,  8}, 
                                                        { 9, 10, 11, 12}});

        // Tested method
        input.process(new MFunc() {
            
            @Override
            public double f(double aElement, int aRow, int aCol) {
                assertEquals(input.get(aRow, aCol), aElement, 0.0);
                return aElement;
            }
        });
    }
    
    @Test
    public void testBwconncomp8Connectivity() {
        final Matrix input = new Matrix(new double[][] {{ 0, 1, 0, 1 }, 
                                                        { 1, 0, 0, 0 }, 
                                                        { 0, 0, 1, 1 }});

        // Tested method
        Map<Integer, List<Integer>> result = Matlab.bwconncomp(input, true /* 8 connectivity */ );
       
        assertEquals("Number of connected objects: ", 3, result.size());
        assertArrayEquals("First connected object: ", new Object[] {1, 3}, result.get(2).toArray());
        assertArrayEquals("Second connected object: ", new Object[] {8, 11}, result.get(3).toArray());
        assertArrayEquals("Third connected object: ", new Object[] {9}, result.get(4).toArray());
    }
    
    @Test
    public void testBwconncomp4Connectivity() {
        final Matrix input = new Matrix(new double[][] {{ 0, 1, 0, 1 }, 
                                                        { 1, 0, 0, 0 }, 
                                                        { 0, 0, 1, 1 }});

        // Tested method
        Map<Integer, List<Integer>> result = Matlab.bwconncomp(input, false /* 4 connectivity */ );
       
        assertEquals("Number of connected objects: ", 4, result.size());
        assertArrayEquals("Connected object: ", new Object[] {1}, result.get(2).toArray());
        assertArrayEquals("Connected object: ", new Object[] {3}, result.get(3).toArray());
        assertArrayEquals("Connected object: ", new Object[] {8, 11}, result.get(4).toArray());
        assertArrayEquals("Connected object: ", new Object[] {9}, result.get(5).toArray());
    }

}
