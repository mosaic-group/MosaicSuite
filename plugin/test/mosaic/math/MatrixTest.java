package mosaic.math;

import static org.junit.Assert.assertEquals;
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
    public void testInsertRow() {
        final Matrix expected = new Matrix(new double[][] {{ 1,  2,  3,  4}, 
                                                           {10, 20, 30, 40}, 
                                                           { 9, 10, 11, 12}});
        
        final Matrix input = new Matrix(new double[][] {{ 1,  2,  3,  4}, 
                                                        { 5,  6,  7,  8}, 
                                                        { 9, 10, 11, 12}});

        // Tested method
        input.insertRow(Matrix.mkRowVector(10, 20, 30, 40), 1);
        
        assertEquals(expected, input);
    }
    
    @Test
    public void testInsertCol() {
        final Matrix expected = new Matrix(new double[][] {{ 1,  2, 10,  4}, 
                                                           {5 ,  6, 20,  8}, 
                                                           { 9, 10, 30, 12}});
        
        final Matrix input = new Matrix(new double[][] {{ 1,  2,  3,  4}, 
                                                        { 5,  6,  7,  8}, 
                                                        { 9, 10, 11, 12}});

        // Tested method
        input.insertCol(Matrix.mkColVector(10, 20, 30), 2);
        
        assertEquals(expected, input);
    }
}
