package mosaic.math;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class MatrixTest extends CommonBase {

    @Test
    public void testConstructors() {
        {
            int numRows = 3;
            int numCols = 2;
            Matrix test = new Matrix(numRows, numCols);
            assertEquals(test.numRows(), numRows);
            assertEquals(test.numCols(), numCols);
            assertEquals(test.size(), numRows * numCols);
        }
        {
            Matrix test = new Matrix(2, 3);
            test.set(0, 0, 1);
            test.set(0, 1, 2);
            test.set(0, 2, 3);
            test.set(1, 0, 4);
            test.set(1, 1, 5);
            test.set(1, 2, 6);
            
            assertEquals("Matrices should be same", new Matrix(new double[][] {{1, 2, 3}, {4, 5, 6}}), test);
        }
        {
            double[][] xy = {{1, 3, 5}, {2, 4, 6}};
            double[][] yx = {{1, 2}, {3, 4}, {5, 6}};
            assertEquals("Matrices should be same", new Matrix(xy, true), new Matrix(yx, false));
            assertEquals("Matrices should be same", new Matrix(yx), new Matrix(yx, false));
            assertEquals("Matrices should be same", new Matrix(yx).transpose(), new Matrix(yx, true));
        }
        {
            double[][] yx = {{1, 2}, {3, 4}, {5, 6}};
            
            Matrix test1 = new Matrix(yx);
            Matrix test2 = new Matrix(test1);
            
            assertEquals("Matrices should be same", test1, test2);
            
            // Check if matrices are not sharing any data when one created from the other
            test1.set(0, 13);
            assertFalse("Matrices should not be same", test1.equals(test2));
        }
        {
            double[][] yx = {{1, 2}, {3, 4}, {5, 6}};
            
            Matrix test1 = new Matrix(yx);
            Matrix test2 = test1.copy();
            
            assertEquals("Matrices should be same", test1, test2);
            
            // Check if matrices are not sharing any data when one created from the other
            test1.set(0, 13);
            assertFalse("Matrices should not be same", test1.equals(test2));
        }
        {
            double[][] yxDouble = {{1, 2}, {3, 4}, {5, 6}};
            float[][] yxFloat = {{1, 2}, {3, 4}, {5, 6}};
            assertEquals("Matrices should be same", new Matrix(yxDouble, true), new Matrix(yxFloat, true));
            assertEquals("Matrices should be same", new Matrix(yxDouble, false), new Matrix(yxFloat, false));
            assertEquals("Matrices should be same", new Matrix(yxDouble), new Matrix(yxFloat));
        }
    }
    
    @Test 
    public void testGetArray() {
        {
            double[][] expected = {{1, 2}, {3, 4}, {5, 6}};
            
            Matrix test = new Matrix(expected);
            
            assertArrayEquals(expected, Matrix.getArrayYX(test));
            assertArrayEquals(expected, test.getArrayYX());
        }
        {
            float[][] expected = {{1, 2}, {3, 4}, {5, 6}};
            
            Matrix test = new Matrix(expected);
            
            assertArrayEquals(expected, Matrix.getArrayYXasFloats(test));
            assertArrayEquals(expected, test.getArrayYXasFloats());
        }
        {
            double[][] expected = {{1, 2}, {3, 4}, {5, 6}};
            
            Matrix test = new Matrix(expected, true);
            
            assertArrayEquals(expected, Matrix.getArrayXY(test));
            assertArrayEquals(expected, test.getArrayXY());
        }
        {
            float[][] expected = {{1, 2}, {3, 4}, {5, 6}};
            
            Matrix test = new Matrix(expected, true);
            
            assertArrayEquals(expected, Matrix.getArrayXYasFloats(test));
            assertArrayEquals(expected, test.getArrayXYasFloats());
        }
    }
    
    @Test
    public void testMkVectors() {
        {   
            Matrix expected = new Matrix(new double[][] {{1, 2, 3, 4}});
            Matrix test = Matrix.mkRowVector(1, 2, 3, 4);
            
            assertEquals(expected, test);
        }
        {
            Matrix expected = new Matrix(new double[][] {{1}, {2}, {3}, {4}});
            Matrix test = Matrix.mkColVector(1, 2, 3, 4);

            assertEquals(expected, test);
        }
        {
            Matrix expected = Matrix.mkColVector(new double[] {1, 2, 3, 4});
            Matrix test = Matrix.mkColVector(1, 2, 3, 4);

            assertEquals(expected, test);
        }
        {
            Matrix expected = Matrix.mkRowVector(new double[] {1, 2, 3, 4});
            Matrix test = Matrix.mkRowVector(1, 2, 3, 4);

            assertEquals(expected, test);
        }
    }
    
    @Test
    public void testDimensionGetters() {
        {
            Matrix test = Matrix.mkRowVector(1, 2, 3);
            
            assertTrue(test.isRowVector());
            assertFalse(test.isColVector());
        }
        {
            Matrix test = Matrix.mkColVector(1, 2, 3);
            
            assertFalse(test.isRowVector());
            assertTrue(test.isColVector());
        }
        {
            // Special case - one number in 1x1 matrix
            Matrix test = new Matrix(new double[][] {{1}});
            
            assertTrue(test.isRowVector());
            assertTrue(test.isColVector());
        }
        {
            // Special case - empty matrix
            Matrix test = new Matrix(new double[][] {{}});
            
            assertFalse(test.isRowVector());
            assertFalse(test.isColVector());
        }
    }
    
    @Test
    public void testProcess() {
        final Matrix input = new Matrix(new double[][] {{ 1,  2,  3,  4}, 
                                                        { 5,  6,  7,  8}, 
                                                        { 9, 10, 11, 12}});
        final Matrix copy = new Matrix(input);
        
        // Tested method
        input.process(new MFunc() {
            
            @Override
            public double f(double aElement, int aRow, int aCol) {
                assertEquals(input.get(aRow, aCol), aElement, 0.0);
                return 2 * aElement;
            }
        });
        
        assertEquals(copy.scale(2.0), input);
    }
    
    @Test
    public void testProcessNoSet() {
        final Matrix input = new Matrix(new double[][] {{ 1,  2,  3,  4}, 
                                                        { 5,  6,  7,  8}, 
                                                        { 9, 10, 11, 12}});
        final Matrix copy = new Matrix(input);
        
        // Tested method
        input.processNoSet(new MFunc() {
            
            @Override
            public double f(double aElement, int aRow, int aCol) {
                assertEquals(input.get(aRow, aCol), aElement, 0.0);
                return 2 * aElement;
            }
        });
        
        // No change expected
        assertEquals(copy, input);
    }

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
    public void testElementAdd() {
        {
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}, {4,-1}});
            
            Matrix expected = new Matrix(new double [][] {{2, 4}, {3, 7}, {9, 5}});
            
            assertEquals(expected, m1.copy().add(m2));
            assertEquals(expected, m2.add(m1));
        }
        {
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            
            Matrix expected = m1.copy().scale(2);
            
            assertEquals(expected, m1.add(m1));
        }
        {
            // Wrong dimensions
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}});
            
            try {
                m1.copy().add(m2);
                fail("It should throw IllegalArgumentException since matrices have different dimensions");
            }
            catch (IllegalArgumentException e) {
                // It is OK to be here
            }
        }
    }
    
    @Test 
    public void testElementMult() {
        {
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}, {4,-1}});
            
            Matrix expected = new Matrix(new double [][] {{1, 4}, {0,12}, {20, -6}});
            
            assertEquals(expected, m1.copy().elementMult(m2));
            assertEquals(expected, m2.elementMult(m1));
        }
        {
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            
            Matrix expected = new Matrix(new double [][] {{1, 4}, {9,16}, {25, 36}});
            
            assertEquals(expected, m1.elementMult(m1));
        }
        {
            // Wrong dimensions
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}});
            
            try {
                m1.copy().elementMult(m2);
                fail("It should throw IllegalArgumentException since matrices have different dimensions");
            }
            catch (IllegalArgumentException e) {
                // It is OK to be here
            }
        }
    }
    
    @Test 
    public void testElementDiv() {
        {
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            Matrix m2 = new Matrix(new double [][] {{2, 1}, {3, 2}, {2.5, -3}});
            
            Matrix expected = new Matrix(new double [][] {{0.5, 2}, {1, 2}, {2, -2}});
            
            assertEquals(expected, m1.copy().elementDiv(m2));
            assertEquals(expected.inv(), m2.elementDiv(m1));
        }
        {
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            
            Matrix expected = new Matrix(new double [][] {{1, 1}, {1, 1}, {1, 1}});
            
            assertEquals(expected, m1.elementDiv(m1));
        }
        {
            // Wrong dimensions
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}});
            
            try {
                m1.copy().elementDiv(m2);
                fail("It should throw IllegalArgumentException since matrices have different dimensions");
            }
            catch (IllegalArgumentException e) {
                // It is OK to be here
            }
        }
    }
    
    @Test
    public void testScalarOperations() {
        {   // add
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            
            Matrix expected = new Matrix(new double [][] {{2, 3}, {4, 5}, {6, 7}});
            
            assertEquals(expected, m1.add(1.0));
        }
        {   // scale
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            
            Matrix expected = new Matrix(new double [][] {{10, 20}, {30, 40}, {50, 60}});
            
            assertEquals(expected, m1.scale(10.0));
        }
    }
    
    @Test 
    public void testElementSub() {
        {
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}, {4,-1}});
            
            Matrix expected = new Matrix(new double [][] {{0, 0}, {3, 1}, {1, 7}});
            
            assertEquals(expected, m1.copy().sub(m2));
            assertEquals(expected, m2.sub(m1).negative());
        }
        {
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            
            Matrix expected = m1.copy().zeros();
            
            assertEquals(expected, m1.sub(m1));
        }
        {
            // Wrong dimensions
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}});
            
            try {
                m1.copy().sub(m2);
                fail("It should throw IllegalArgumentException since matrices have different dimensions");
            }
            catch (IllegalArgumentException e) {
                // It is OK to be here
            }
        }
    }
    
    @Test 
    public void testMult() {
        {
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            Matrix m2 = new Matrix(new double [][] {{1, 2, 0}, {3, 4,-1}});
            
            Matrix expected = new Matrix(new double [][] {{7, 10, -2}, {15, 22, -4}, {23, 34, -6}});
            
            assertEquals(expected, m1.mult(m2));
        }
        {
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            Matrix m2 = new Matrix(new double [][] {{1, 2, 0}, {3, 4,-1}});
            
            Matrix expected = new Matrix(new double [][] {{7, 10}, {10, 16}});
            
            assertEquals(expected, m2.mult(m1));
        }
        {
            // Wrong dimensions
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            Matrix m2 = new Matrix(new double [][] {{1, 2, 0}, {3, 4,-1}, {1, 1, 1}});
            
            try {
                m1.copy().sub(m2);
                fail("It should throw IllegalArgumentException since matrices have different dimensions");
            }
            catch (IllegalArgumentException e) {
                // It is OK to be here
            }
        }
    }
    
    @Test
    public void testGettersSetters() {
        {   // set/get based on row/col indexes
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            
            for (int r = 0; r <= 2; r++) {
                for (int c = 0; c <= 1; c++) {
                    assertEquals(c + 1 + 2 * r, m1.get(r, c), 0.0);
                    m1.set(r,  c, r * c);
                    assertEquals(r * c, m1.get(r, c), 0.0);
                }
            }
        }
        {   // set/get based on element index
            Matrix m1 = new Matrix(new double [][] {{1, 4}, {2, 5}, {3, 6}});
            
            for (int r = 0; r <= 5; r++) {
                    assertEquals(r + 1, m1.get(r), 0.0);
                    m1.set(r, r * 2);
                    assertEquals(r * 2, m1.get(r), 0.0);
                
            }
        }
    }
    
    @Test
    public void testFillFunctions() { 
        {   
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            
            Matrix expected = new Matrix(new double [][] {{3, 3}, {3, 3}, {3, 3}});
            
            assertEquals(expected, m1.fill(3.0)); 
        }
        {   
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            
            Matrix expected = new Matrix(new double [][] {{1, 1}, {1, 1}, {1, 1}});
            
            assertEquals(expected, m1.ones()); 
        }
        {   
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            
            Matrix expected = new Matrix(new double [][] {{0, 0}, {0, 0}, {0, 0}});
            
            assertEquals(expected, m1.zeros()); 
        }
    }
    
    @Test
    public void testElementWiseMathFunctions() { 
        {   
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            
            Matrix expected = new Matrix(new double [][] {{1, 4}, {9, 16}, {25, 36}});
            
            assertEquals(expected, m1.pow2()); 
        }
        {   
            Matrix m1 = new Matrix(new double [][] {{1, 4}, {9, 16}, {25, 36}});
            
            Matrix expected = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            
            assertEquals(expected, m1.sqrt()); 
        }
        {   
            Matrix m1 = new Matrix(new double [][] {{1, Math.exp(2)}, {Math.exp(3), Math.exp(4)}});
            
            Matrix expected = new Matrix(new double [][] {{0, 2}, {3, 4}});
            
            assertEquals(expected, m1.log()); 
        }
        {   
            Matrix m1 = new Matrix(new double [][] {{1, 2, 0.5}});
            
            Matrix expected = new Matrix(new double [][] {{1, 0.5, 2}});
            
            assertEquals(expected, m1.inv()); 
        }
        {   
            Matrix m1 = new Matrix(new double [][] {{1, 2, 0.5}});
            
            Matrix expected = new Matrix(new double [][] {{-1, -2, -0.5}});
            
            assertEquals(expected, m1.negative()); 
        }
        {   
            Matrix m1 = new Matrix(new double [][] {{1, 2, 0.5}});
            
            assertEquals(3.5, m1.sum(), 0.0); 
        }
    }
    
    @Test
    public void testNormalize() { 
        {
            Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, -4}, {5, 10}});
            
            Matrix expected = new Matrix(new double [][] {{0.1, 0.2}, {0.3, -0.4}, {0.5, 1.0}});
            
            assertEquals(expected, m1.normalize()); 
        }
        {
            Matrix m1 = new Matrix(new double [][] {{-10, 2}, {3, -4}, {5, 6}});
            
            Matrix expected = new Matrix(new double [][] {{-1, 0.2}, {0.3, -0.4}, {0.5, 0.6}});
            
            assertEquals(expected, m1.normalize()); 
        }
    }
    
    @Test
    public void testNormalizeInRange0to1() { 
        {   // (-5, -2, 1, 4, 7, 10) => (0, 0.2, 0.4, 0.6, 0.8, 1)
            Matrix m1 = new Matrix(new double [][] {{1, 7}, {4, -5}, {-2, 10}});
            
            Matrix expected = new Matrix(new double [][] {{0.4, 0.8}, {0.6, 0}, {0.2, 1.0}});
            
            assertEquals(expected, m1.normalizeInRange0to1()); 
        }
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
                                                           { 5,  6, 20,  8}, 
                                                           { 9, 10, 30, 12}});
        
        final Matrix input = new Matrix(new double[][] {{ 1,  2,  3,  4}, 
                                                        { 5,  6,  7,  8}, 
                                                        { 9, 10, 11, 12}});

        // Tested method
        input.insertCol(Matrix.mkColVector(10, 20, 30), 2);
        
        assertEquals(expected, input);
    }
    
    @Test
    public void testCompare() {
        Matrix m1 = new Matrix(new double [][] {{1, 2.0}, {3, 4.0}, {5.0, 6}});
        Matrix m2 = new Matrix(new double [][] {{1, 2.5}, {3, 4.1}, {4.5, 6}});
        
        
        assertTrue(m1.compare(m2, 0.5));
        assertFalse(m1.compare(m2, 0.45));
    }
}
