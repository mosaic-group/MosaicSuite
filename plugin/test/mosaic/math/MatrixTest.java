package mosaic.math;

import static org.junit.Assert.*;
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

}
