package mosaic.utils.math;

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
            final int numRows = 3;
            final int numCols = 2;
            final Matrix test = new Matrix(numRows, numCols);
            assertEquals(test.numRows(), numRows);
            assertEquals(test.numCols(), numCols);
            assertEquals(test.size(), numRows * numCols);
        }
        {
            final Matrix test = new Matrix(2, 3);
            test.set(0, 0, 1);
            test.set(0, 1, 2);
            test.set(0, 2, 3);
            test.set(1, 0, 4);
            test.set(1, 1, 5);
            test.set(1, 2, 6);

            assertEquals("Matrices should be same", new Matrix(new double[][] {{1, 2, 3}, {4, 5, 6}}), test);
        }
        {
            final double[][] xy = {{1, 3, 5}, {2, 4, 6}};
            final double[][] yx = {{1, 2}, {3, 4}, {5, 6}};
            assertEquals("Matrices should be same", new Matrix(xy, true), new Matrix(yx, false));
            assertEquals("Matrices should be same", new Matrix(yx), new Matrix(yx, false));
            assertEquals("Matrices should be same", new Matrix(yx).transpose(), new Matrix(yx, true));
        }
        {
            final double[][] yx = {{1, 2}, {3, 4}, {5, 6}};

            final Matrix test1 = new Matrix(yx);
            final Matrix test2 = new Matrix(test1);

            assertEquals("Matrices should be same", test1, test2);

            // Check if matrices are not sharing any data when one created from the other
            test1.set(0, 13);
            assertFalse("Matrices should not be same", test1.equals(test2));
        }
        {
            final double[][] yx = {{1, 2}, {3, 4}, {5, 6}};

            final Matrix test1 = new Matrix(yx);
            final Matrix test2 = test1.copy();

            assertEquals("Matrices should be same", test1, test2);

            // Check if matrices are not sharing any data when one created from the other
            test1.set(0, 13);
            assertFalse("Matrices should not be same", test1.equals(test2));
        }
        {
            final double[][] yxDouble = {{1, 2}, {3, 4}, {5, 6}};
            final float[][] yxFloat = {{1, 2}, {3, 4}, {5, 6}};
            assertEquals("Matrices should be same", new Matrix(yxDouble, true), new Matrix(yxFloat, true));
            assertEquals("Matrices should be same", new Matrix(yxDouble, false), new Matrix(yxFloat, false));
            assertEquals("Matrices should be same", new Matrix(yxDouble), new Matrix(yxFloat));
        }
    }

    @Test
    public void testGetArray() {
        {
            final double[][] expected = {{1, 2}, {3, 4}, {5, 6}};

            final Matrix test = new Matrix(expected);

            assertArrayEquals(expected, Matrix.getArrayYX(test));
            assertArrayEquals(expected, test.getArrayYX());
        }
        {
            final float[][] expected = {{1, 2}, {3, 4}, {5, 6}};

            final Matrix test = new Matrix(expected);

            assertArrayEquals(expected, Matrix.getArrayYXasFloats(test));
            assertArrayEquals(expected, test.getArrayYXasFloats());
        }
        {
            final double[][] expected = {{1, 2}, {3, 4}, {5, 6}};

            final Matrix test = new Matrix(expected, true);

            assertArrayEquals(expected, Matrix.getArrayXY(test));
            assertArrayEquals(expected, test.getArrayXY());
        }
        {
            final float[][] expected = {{1, 2}, {3, 4}, {5, 6}};

            final Matrix test = new Matrix(expected, true);

            assertArrayEquals(expected, Matrix.getArrayXYasFloats(test));
            assertArrayEquals(expected, test.getArrayXYasFloats());
        }
        {
            final double[][] input = {{1, 2}, {3, 4}, {5, 6}, {7, 8}};
            final double[] expected = {2, 4, 6, 8};

            final Matrix test = new Matrix(input);

            assertArrayEquals(expected, test.getArrayColumn(1), 0.0);
        }
        {
            final double[][] input = {{1, 2}, {3, 4}, {5, 6}, {7, 8}};
            final double[] expected = {5, 6};

            final Matrix test = new Matrix(input);

            assertArrayEquals(expected, test.getArrayRow(2), 0.0);
        }
    }

    @Test
    public void testgetColumnRow() {
        final Matrix input = new Matrix(new double[][] {{1, 2}, {3, 4}, {5, 6}, {7, 8}});
        final Matrix expectedRow = Matrix.mkRowVector(7, 8);
        final Matrix expectedCol = Matrix.mkColVector(1, 3, 5, 7);

        assertEquals(expectedRow, input.getRow(3));
        assertEquals(expectedCol, input.getColumn(0));
    }

    @Test
    public void testMkVectors() {
        {
            final Matrix expected = new Matrix(new double[][] {{1, 2, 3, 4}});
            final Matrix test = Matrix.mkRowVector(1, 2, 3, 4);

            assertEquals(expected, test);
        }
        {
            final Matrix expected = new Matrix(new double[][] {{1}, {2}, {3}, {4}});
            final Matrix test = Matrix.mkColVector(1, 2, 3, 4);

            assertEquals(expected, test);
        }
        {
            final Matrix expected = Matrix.mkColVector(new double[] {1, 2, 3, 4});
            final Matrix test = Matrix.mkColVector(1, 2, 3, 4);

            assertEquals(expected, test);
        }
        {
            final Matrix expected = Matrix.mkRowVector(new double[] {1, 2, 3, 4});
            final Matrix test = Matrix.mkRowVector(1, 2, 3, 4);

            assertEquals(expected, test);
        }
    }

    @Test
    public void testDimensionGetters() {
        {
            final Matrix test = Matrix.mkRowVector(1, 2, 3);

            assertTrue(test.isRowVector());
            assertFalse(test.isColVector());
        }
        {
            final Matrix test = Matrix.mkColVector(1, 2, 3);

            assertFalse(test.isRowVector());
            assertTrue(test.isColVector());
        }
        {
            // Special case - one number in 1x1 matrix
            final Matrix test = new Matrix(new double[][] {{1}});

            assertTrue(test.isRowVector());
            assertTrue(test.isColVector());
        }
        {
            // Special case - empty matrix
            final Matrix test = new Matrix(new double[][] {{}});

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
        {
            final int startRow = 0;
            final int startCol = 1;
            final int step = 2;
            final Matrix expected = new Matrix(new double[][] {{ 2,  4},
                    {10, 12}});

            final Matrix input = new Matrix(new double[][] {{ 1,  2,  3,  4},
                    { 5,  6,  7,  8},
                    { 9, 10, 11, 12}});
            // Tested method
            final Matrix result = input.resize(startRow, startCol, step, step);

            assertEquals(expected, result);
        }
        {
            final int startRow = 0;
            final int startCol = 0;
            final int step = 1;

            final Matrix input = new Matrix(new double[][] {{ 1,  2,  3,  4},
                    { 5,  6,  7,  8},
                    { 9, 10, 11, 12}});
            // Tested method
            final Matrix result = input.resize(startRow, startCol, step, step);

            assertEquals(result, input);
        }
    }

    @Test
    public void testElementAdd() {
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            final Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}, {4,-1}});

            final Matrix expected = new Matrix(new double [][] {{2, 4}, {3, 7}, {9, 5}});

            assertEquals(expected, m1.copy().add(m2));
            assertEquals(expected, m2.add(m1));
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            final Matrix expected = m1.copy().scale(2);

            assertEquals(expected, m1.add(m1));
        }
        {
            // Wrong dimensions
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            final Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}});

            try {
                m1.copy().add(m2);
                fail("It should throw IllegalArgumentException since matrices have different dimensions");
            }
            catch (final IllegalArgumentException e) {
                // It is OK to be here
            }
        }
    }

    @Test
    public void testElementMult() {
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            final Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}, {4,-1}});

            final Matrix expected = new Matrix(new double [][] {{1, 4}, {0,12}, {20, -6}});

            assertEquals(expected, m1.copy().elementMult(m2));
            assertEquals(expected, m2.elementMult(m1));
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            final Matrix expected = new Matrix(new double [][] {{1, 4}, {9,16}, {25, 36}});

            assertEquals(expected, m1.elementMult(m1));
        }
        {
            // Wrong dimensions
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            final Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}});

            try {
                m1.copy().elementMult(m2);
                fail("It should throw IllegalArgumentException since matrices have different dimensions");
            }
            catch (final IllegalArgumentException e) {
                // It is OK to be here
            }
        }
        {
            final Matrix m1 = new Matrix(new double [][] {
                    {0.814723686393179, 0.913375856139019, 0.278498218867048},
                    {0.905791937075619, 0.632359246225410, 0.546881519204984},
                    {0.126986816293506, 0.097540404999410, 0.957506835434298},
            });
            final Matrix m2 = new Matrix(new double [][] {
                    {0.964888535199277, 0.957166948242946, 0.141886338627215},
                    {0.157613081677548, 0.485375648722841, 0.421761282626275},
                    {0.970592781760616, 0.800280468888800, 0.915735525189067},
            });
            final Matrix expected = new Matrix(new double [][] {
                    {0.786117544356069, 0.874253180819373, 0.039515092589246},
                    {0.142764658561164, 0.306931779362545, 0.230653450984500},
                    {0.123252487273238, 0.078059681048531, 0.876823024818548},
            });
            assertTrue(expected.compare(m1.elementMult(m2), 0.000000000000001));
        }
    }

    @Test
    public void testElementDiv() {
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            final Matrix m2 = new Matrix(new double [][] {{2, 1}, {3, 2}, {2.5, -3}});

            final Matrix expected = new Matrix(new double [][] {{0.5, 2}, {1, 2}, {2, -2}});

            assertEquals(expected, m1.copy().elementDiv(m2));
            assertEquals(expected.inv(), m2.elementDiv(m1));
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            final Matrix expected = new Matrix(new double [][] {{1, 1}, {1, 1}, {1, 1}});

            assertEquals(expected, m1.elementDiv(m1));
        }
        {
            // Wrong dimensions
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            final Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}});

            try {
                m1.copy().elementDiv(m2);
                fail("It should throw IllegalArgumentException since matrices have different dimensions");
            }
            catch (final IllegalArgumentException e) {
                // It is OK to be here
            }
        }
    }

    @Test
    public void testScalarOperations() {
        {   // add
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            final Matrix expected = new Matrix(new double [][] {{2, 3}, {4, 5}, {6, 7}});

            assertEquals(expected, m1.add(1.0));
        }
        {   // sub
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            final Matrix expected = new Matrix(new double [][] {{0, 1}, {2, 3}, {4, 5}});

            assertEquals(expected, m1.sub(1.0));
        }
        {   // scale
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            final Matrix expected = new Matrix(new double [][] {{10, 20}, {30, 40}, {50, 60}});

            assertEquals(expected, m1.scale(10.0));
        }
    }

    @Test
    public void testElementSub() {
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            final Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}, {4,-1}});

            final Matrix expected = new Matrix(new double [][] {{0, 0}, {3, 1}, {1, 7}});

            assertEquals(expected, m1.copy().sub(m2));
            assertEquals(expected, m2.sub(m1).negative());
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            final Matrix expected = m1.copy().zeros();

            assertEquals(expected, m1.sub(m1));
        }
        {
            // Wrong dimensions
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            final Matrix m2 = new Matrix(new double [][] {{1, 2}, {0, 3}});

            try {
                m1.copy().sub(m2);
                fail("It should throw IllegalArgumentException since matrices have different dimensions");
            }
            catch (final IllegalArgumentException e) {
                // It is OK to be here
            }
        }
    }

    @Test
    public void testMult() {
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            final Matrix m2 = new Matrix(new double [][] {{1, 2, 0}, {3, 4,-1}});

            final Matrix expected = new Matrix(new double [][] {{7, 10, -2}, {15, 22, -4}, {23, 34, -6}});

            assertEquals(expected, m1.mult(m2));
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            final Matrix m2 = new Matrix(new double [][] {{1, 2, 0}, {3, 4,-1}});

            final Matrix expected = new Matrix(new double [][] {{7, 10}, {10, 16}});

            assertEquals(expected, m2.mult(m1));
        }
        {
            // Wrong dimensions
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});
            final Matrix m2 = new Matrix(new double [][] {{1, 2, 0}, {3, 4,-1}, {1, 1, 1}});

            try {
                m1.copy().sub(m2);
                fail("It should throw IllegalArgumentException since matrices have different dimensions");
            }
            catch (final IllegalArgumentException e) {
                // It is OK to be here
            }
        }
    }

    @Test
    public void testGettersSetters() {
        {   // set/get based on row/col indexes
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            for (int r = 0; r <= 2; r++) {
                for (int c = 0; c <= 1; c++) {
                    assertEquals(c + 1 + 2 * r, m1.get(r, c), 0.0);
                    m1.set(r,  c, r * c);
                    assertEquals(r * c, m1.get(r, c), 0.0);
                }
            }
        }
        {   // set/get based on element index
            final Matrix m1 = new Matrix(new double [][] {{1, 4}, {2, 5}, {3, 6}});

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
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            final Matrix expected = new Matrix(new double [][] {{3, 3}, {3, 3}, {3, 3}});

            assertEquals(expected, m1.fill(3.0));
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            final Matrix expected = new Matrix(new double [][] {{1, 1}, {1, 1}, {1, 1}});

            assertEquals(expected, m1.ones());
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            final Matrix expected = new Matrix(new double [][] {{0, 0}, {0, 0}, {0, 0}});

            assertEquals(expected, m1.zeros());
        }
    }

    @Test
    public void testElementWiseMathFunctions() {
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            final Matrix expected = new Matrix(new double [][] {{1, 4}, {9, 16}, {25, 36}});

            assertEquals(expected, m1.pow2());
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 4}, {9, 16}, {25, 36}});

            final Matrix expected = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            assertEquals(expected, m1.sqrt());
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, Math.exp(2)}, {Math.exp(3), Math.exp(4)}});

            final Matrix expected = new Matrix(new double [][] {{0, 2}, {3, 4}});

            assertEquals(expected, m1.log());
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2, 0.5}});

            final Matrix expected = new Matrix(new double [][] {{1, 0.5, 2}});

            assertEquals(expected, m1.inv());
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2, 0.5}});

            final Matrix expected = new Matrix(new double [][] {{-1, -2, -0.5}});

            assertEquals(expected, m1.negative());
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2, 0.5}});

            assertEquals(3.5, m1.sum(), 0.0);
        }
    }

    @Test
    public void testNormalize() {
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, -4}, {5, 10}});

            final Matrix expected = new Matrix(new double [][] {{0.1, 0.2}, {0.3, -0.4}, {0.5, 1.0}});

            assertEquals(expected, m1.normalize());
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{-10, 2}, {3, -4}, {5, 6}});

            final Matrix expected = new Matrix(new double [][] {{-1, 0.2}, {0.3, -0.4}, {0.5, 0.6}});

            assertEquals(expected, m1.normalize());
        }
    }

    @Test
    public void testNormalizeInRange0to1() {
        {   // (-5, -2, 1, 4, 7, 10) => (0, 0.2, 0.4, 0.6, 0.8, 1)
            final Matrix m1 = new Matrix(new double [][] {{1, 7}, {4, -5}, {-2, 10}});

            final Matrix expected = new Matrix(new double [][] {{0.4, 0.8}, {0.6, 0}, {0.2, 1.0}});

            assertEquals(expected, m1.normalizeInRange0to1());
        }
    }

    @Test
    public void testInsertRow() {
        {
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
        {
            // Wrong dimensions
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            try {
                m1.insertRow(Matrix.mkRowVector(1, 2, 3), 1);
                fail("It should throw IllegalArgumentException since matrices have different dimensions");
            }
            catch (final IllegalArgumentException e) {
                // It is OK to be here
            }
        }
    }

    @Test
    public void testInsertCol() {
        {
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
        {
            // Wrong dimensions
            final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}, {5, 6}});

            try {
                m1.insertCol(Matrix.mkColVector(1, 2, 3, 4, 5), 1);
                fail("It should throw IllegalArgumentException since matrices have different dimensions");
            }
            catch (final IllegalArgumentException e) {
                // It is OK to be here
            }
        }
    }

    @Test
    public void testInsert() {
        final Matrix expected = new Matrix(new double[][] {{ 1,  2, 3,  4},
                { 5,  6, 66, 77},
                { 9, 10, 88, 99}});

        final Matrix input = new Matrix(new double[][] {{ 1,  2,  3,  4},
                { 5,  6,  7,  8},
                { 9, 10, 11, 12}});

        // Tested method
        input.insert(new Matrix(new double[][] {{66, 77}, {88, 99}}), 1,2);
        assertEquals(expected, input);
    }

    @Test
    public void testCompareEquals() {
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2.0}, {3, 4.0}, {5.0, 6}});
            final Matrix m2 = new Matrix(new double [][] {{1, 2.5}, {3, 4.1}, {4.5, 6}});

            assertTrue(m1.compare(m2, 0.5));
            assertFalse(m1.compare(m2, 0.45));
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2.0}, {3, 4.0}, {5.0, 6}});
            assertTrue(m1.equals(m1));
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2.0}, {3, 4.0}, {5.0, 6}});
            assertFalse(m1.equals(null));
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2.0}, {3, 4.0}, {5.0, 6}});
            assertFalse(m1.equals("Hello world"));
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2.0}, {3, 4.0}, {5.0, 6}});
            final Matrix m2 = new Matrix(new double [][] {{1, 2.5}, {3, 4.1}});

            assertFalse(m1.equals(m2));
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2.0}, {3, 4.0}, {5.0, 6}});
            final Matrix m2 = new Matrix(new double [][] {{1, 2.5, 3}, {3, 4.1, 5}, {1, 2, 3}});

            assertFalse(m1.equals(m2));
        }
        {
            final Matrix m1 = new Matrix(new double [][] {{1, 2.0}, {3, 4.0}, {5.0, 6}});
            final Matrix m2 = m1.copy();

            assertTrue(m1.equals(m2));
        }
    }

    @Test
    public void testGetData() {
        final Matrix m1 = new Matrix(new double [][] {{1, 2}, {3, 4}});
        final double[] data = m1.getData();

        // Internal data is kept rows-wise
        for (int i = 0; i < 4; ++i) {
            assertEquals((double)i + 1, data[i], 0.0);
        }
    }
}
