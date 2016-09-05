package mosaic.utils.math;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import mosaic.test.framework.CommonBase;

public class MatlabTest extends CommonBase {

    @Test
    public void testRegularySpacedArray() {
        {
            // Increasing values, step matching all from start to stop including
            final double[] result = {1.0, 3.0, 5.0, 7.0};
            assertArrayEquals(result, Matlab.regularySpacedArray(1, 2, 7), 0.00001);
        }
        {
            // Chosen step value does not match stop
            final double[] result = {1.0, 3.0, 5.0, 7.0};
            assertArrayEquals(result, Matlab.regularySpacedArray(1, 2, 8), 0.00001);
        }
        {
            // Decreasing values, all values matched including stop value
            final double[] result = {8.0, 6.0, 4.0, 2.0};
            assertArrayEquals(result, Matlab.regularySpacedArray(8, -2, 1), 0.00001);
        }
        {
            // Decreasing values, does not match stop stop value
            final double[] result = {7.0, 5.0, 3.0, 1.0};
            assertArrayEquals(result, Matlab.regularySpacedArray(7, -2, 1), 0.00001);
        }
        {
            // Only first value matched
            final double[] result = {5.0};
            assertArrayEquals(result, Matlab.regularySpacedArray(5, 1000, 6), 0.00001);
        }
        {
            // Not possible to generate array for step equal 0
            final double[] result = null;
            assertEquals(result, Matlab.regularySpacedArray(7, 0, 1));
        }
        {
            // Not possible to generate array with positive step
            final double[] result = null;
            assertEquals(result, Matlab.regularySpacedArray(7, 1, 1));
        }
        {
            // Not possible to generate array with negative step
            final double[] result = null;
            assertEquals(result, Matlab.regularySpacedArray(1, -1, 7));
        }
    }

    @Test
    public void testRegularySpacedVector() {
        final Matrix expected = new Matrix(new double [][] {{4, 8, 12}});

        final Matrix result = Matlab.regularySpacedVector(4, 4, 12);

        assertTrue(expected.compare(result, 0.00001));
    }

    @Test
    public void testLinspace() {
        {   // values increasing
            final Matrix expected = new Matrix(new double [][] {{1, 1.75, 2.5, 3.25, 4}});
            final Matrix result = Matlab.linspace(1, 4, 5);
            assertEquals(expected, result);
        }
        {   // same values repeated
            final Matrix expected = new Matrix(new double [][] {{1, 1, 1}});
            final Matrix result = Matlab.linspace(1, 1, 3);
            assertEquals(expected, result);
        }
        {   // values decreasing
            final Matrix expected = new Matrix(new double [][] {{2, 1.5, 1}});
            final Matrix result = Matlab.linspace(2, 1, 3);
            assertEquals(expected, result);
        }
    }

    @Test
    public void testMeshgrid() {
        final Matrix expectedM1 = new Matrix(new double [][] {{1, 2, 3},
                {1, 2, 3}});
        final Matrix expectedM2 = new Matrix(new double [][] {{4, 4, 4},
                {5, 5, 5}});

        // Test several configurations of input vectors.
        // It should give same results regardless of input vectors are row / col type.

        {
            // Input values
            final Matrix rows = Matrix.mkRowVector(new double [] {1, 2, 3});
            final Matrix cols = Matrix.mkColVector(new double[] {4, 5});

            // Tested function
            final Matrix[] result = Matlab.meshgrid(rows, cols);

            assertEquals(expectedM1, result[0]);
            assertEquals(expectedM2, result[1]);
        }
        {
            // Input values
            final Matrix rows = Matrix.mkRowVector(new double [] {1, 2, 3});
            final Matrix cols = Matrix.mkRowVector(new double[] {4, 5});

            // Tested function
            final Matrix[] result = Matlab.meshgrid(rows, cols);

            assertEquals(expectedM1, result[0]);
            assertEquals(expectedM2, result[1]);
        }
        {
            // Input values
            final Matrix rows = Matrix.mkColVector(new double [] {1, 2, 3});
            final Matrix cols = Matrix.mkRowVector(new double[] {4, 5});

            // Tested function
            final Matrix[] result = Matlab.meshgrid(rows, cols);

            assertEquals(expectedM1, result[0]);
            assertEquals(expectedM2, result[1]);
        }
    }

    @Test
    public void testImfiltersymmetricVectorFilter() {
        final Matrix expected = new Matrix(new double [][] {{-1, 2, -1},
                                                            {-1, 2, -1}});

        // Input values
        final Matrix img = new Matrix(new double[][] {{ 1, 2, 1},
                                                      { 2, 3, 2}});

        final Matrix filter = Matrix.mkRowVector(new double[] {-1, 2, -1});

        // Tested function
        final Matrix result = Matlab.imfilterSymmetric(img, filter);

        assertEquals(expected, result);
    }

    @Test
    public void testImfiltersymmetricMatrixFilter() {
        final Matrix expected = new Matrix(new double [][] {{2, -1, 2},
                                                            {0, -3, 0}});

        // Input values
        final Matrix img = new Matrix(new double[][] {{ 1, 2, 1},
                                                      { 2, 3, 2}});

        final Matrix filter = new Matrix(new double[][] {{0,  1,  0},
                                                         {1, -4,  1},
                                                         {0,  1,  0}});

        // Tested function
        final Matrix result = Matlab.imfilterSymmetric(img, filter);

        assertEquals(expected, result);
    }

    @Test
    public void testImfilterConvMatrixFilter() {
        final Matrix expected = new Matrix(new double [][] {{20, 34, 30},
                {44, 67, 54}});

        // Input values
        final Matrix img = new Matrix(new double[][] {{ 1, 2, 1},
                { 2, 3, 2}});

        final Matrix filter = new Matrix(new double[][] {{1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}});

        // Tested function
        final Matrix result = Matlab.imfilterConv(img, filter);

        assertEquals(expected, result);
    }

    @Test
    public void testImfilterConvMatrixFilter2() {
        final Matrix expected = new Matrix(new double [][] {{60, 86, 63},
                {79, 97, 66}});

        // Input values
        final Matrix img = new Matrix(new double[][] {{ 1, 2, 3},
                { 4, 5, 6}});

        final Matrix filter = new Matrix(new double[][] {{3, 5}, {7, 11}});

        // Tested function
        final Matrix result = Matlab.imfilterConv(img, filter);

        assertEquals(expected, result);
    }

    @Test
    public void testImresizeScale() {
        final Matrix expected = new Matrix(new double[][] {{0.933855685131195, 1.283345481049561, 1.926202623906705, 2.500000000000000, 3.073797376093295, 3.716654518950438, 4.066144314868804},
                {0.933855685131195, 1.283345481049561, 1.926202623906705, 2.500000000000000, 3.073797376093295, 3.716654518950438, 4.066144314868804}});


        final Matrix input = new Matrix(new double [][] {{1, 2, 3 , 4}});

        // Tested function
        final Matrix result = Matlab.imresize(input, (double)7/4);

        assertTrue(expected.compare(result, 1e-14));
    }

    @Test
    public void testImresizeDims() {
        final Matrix expected = new Matrix(new double[][] {{0.933855685131195, 1.283345481049561, 1.926202623906705, 2.500000000000000, 3.073797376093295, 3.716654518950438, 4.066144314868804}});


        final Matrix input = new Matrix(new double [][] {{1, 2, 3 , 4}});

        // Tested function
        final Matrix result = Matlab.imresize(input, 7, 1);

        assertTrue(expected.compare(result, 1e-14));
    }

    @Test
    public void testBwconncomp8Connectivity() {
        final Matrix input = new Matrix(new double[][] {{ 0, 1, 0, 1 },
                { 1, 0, 0, 0 },
                { 0, 0, 1, 1 }});

        // Tested method
        final Map<Integer, List<Integer>> result = Matlab.bwconncomp(input, true /* 8 connectivity */ );

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
        final Map<Integer, List<Integer>> result = Matlab.bwconncomp(input, false /* 4 connectivity */ );

        assertEquals("Number of connected objects: ", 4, result.size());
        assertArrayEquals("Connected object: ", new Object[] {1}, result.get(2).toArray());
        assertArrayEquals("Connected object: ", new Object[] {3}, result.get(3).toArray());
        assertArrayEquals("Connected object: ", new Object[] {8, 11}, result.get(4).toArray());
        assertArrayEquals("Connected object: ", new Object[] {9}, result.get(5).toArray());
    }

    @Test
    public void testLogical() {
        final Matrix input = Matrix.mkRowVector(0, 1, 0.5, 0.501, 2);

        // Tested method
        final Matrix result = Matlab.logical(input, 0.5);

        assertEquals(Matrix.mkRowVector(0, 1, 0, 1, 1), result);
    }
}
