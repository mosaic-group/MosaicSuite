package mosaic.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import mosaic.test.framework.CommonBase;
import mosaic.utils.ArrayOps.MinMax;


public class ArrayOpsTest extends CommonBase {

    @Test
    public void testFindMinMaxFloat2D() {
        final ArrayOps.MinMax<Float> expected = new ArrayOps.MinMax<Float>(-1f, 5f);

        final float[][] input = new float[][] {{ 0, 3},
                                               {-1, 1},
                                               { 2, 5}};

        // Tested function
        final ArrayOps.MinMax<Float> result = ArrayOps.findMinMax(input);

        assertEquals("Min", expected.getMin(), result.getMin(), 0.001f);
        assertEquals("Max", expected.getMax(), result.getMax(), 0.001f);
    }
    
    @Test
    public void testFindMinMaxDouble2D() {
        final ArrayOps.MinMax<Double> expected = new ArrayOps.MinMax<Double>(-1.0, 5.0);

        final double[][] input = new double[][] {{ 0, 3},
                                                 {-1, 1},
                                                 { 2, 5}};

        // Tested function
        final ArrayOps.MinMax<Double> result = ArrayOps.findMinMax(input);

        assertEquals("Min", expected.getMin(), result.getMin(), 0.001);
        assertEquals("Max", expected.getMax(), result.getMax(), 0.001);
    }
    
    @Test
    public void testFindMinMaxDouble3D() {
        final ArrayOps.MinMax<Double> expected = new ArrayOps.MinMax<Double>(-2.0, 9.0);

        final double[][][] input = new double[][][] {{{ 0, 3},
                                                      {-1, 9},
                                                      { 2, 5}},
                                                 
                                                     {{ 0, 3},
                                                      {-2, 1},
                                                      { 2, 5}}};

        // Tested function
        final ArrayOps.MinMax<Double> result = ArrayOps.findMinMax(input);

        assertEquals("Min", expected.getMin(), result.getMin(), 0.001);
        assertEquals("Max", expected.getMax(), result.getMax(), 0.001);
    }
    
    @Test
    public void testFindMinMaxFloat1D() {
        final ArrayOps.MinMax<Float> expected = new ArrayOps.MinMax<Float>(-1f, 5f);

        final float[] input = new float[] { 0, 3, -1, 1, 2, 5};

        // Tested function
        final ArrayOps.MinMax<Float> result = ArrayOps.findMinMax(input);

        assertEquals("Min", expected.getMin(), result.getMin(), 0.001f);
        assertEquals("Max", expected.getMax(), result.getMax(), 0.001f);
    }
    
    @Test
    public void testFindMinMaxDouble1D() {
        final ArrayOps.MinMax<Double> expected = new ArrayOps.MinMax<Double>(-1.0, 5.0);

        final double[] input = new double[] { 0, 3, -1, 1, 2, 5};

        // Tested function
        final ArrayOps.MinMax<Double> result = ArrayOps.findMinMax(input);

        assertEquals("Min", expected.getMin(), result.getMin(), 0.001);
        assertEquals("Max", expected.getMax(), result.getMax(), 0.001);
    }
    
    @Test
    public void testNormalizeFloat2D() {
        final float[][] expected = new float[][] {{0.25f, 1}, {0, 0.5f}, {0.75f, 0.25f}};

        final float[][] input = new float[][] {{0, 3}, {-1, 1}, {2, 0}};

        // Tested function
        ArrayOps.normalize(input);

        compareArrays(expected, input);
    }

    @Test
    public void testNormalizeDouble2D() {
        final double[][] expected = new double[][] {{0.25f, 1}, {0, 0.5f}, {0.75f, 0.25f}};

        final double[][] input = new double[][] {{0, 3}, {-1, 1}, {2, 0}};

        // Tested function
        ArrayOps.normalize(input);

        compareArrays(expected, input);
    }
    
    @Test
    public void testNormalizeDouble3D() {
        final double[][][] expected = new double[][][] {{{0.25}, {1}}, {{0}, {0.5}}, {{0.75}, {0.25}}};
        final MinMax<Double> expectedMinMax = new MinMax<Double>(-1.0, 3.0);
        
        final double[][][] input = new double[][][] {{{0}, {3}}, {{-1}, {1}}, {{2}, {0}}};

        // Tested function
        MinMax<Double> result = ArrayOps.normalize(input);

        compareArrays(expected, input);
        assertEquals("Min", expectedMinMax.getMin(), result.getMin(), 0.001);
        assertEquals("Max", expectedMinMax.getMax(), result.getMax(), 0.001);
    }
    
    @Test
    public void testNormalizeFloat1D() {
        final float[] expected = new float[] {0.25f, 1, 0, 0.5f, 0.75f, 0.25f};

        final float[] input = new float[] {0, 3, -1, 1, 2, 0};

        // Tested function
        ArrayOps.normalize(input);

        assertArrayEquals(expected, input, 1e-9f);
    }

    @Test
    public void testNormalizeDouble1D() {
        final double[] expected = new double[] {0.25, 1, 0, 0.5, 0.75, 0.25};

        final double[] input = new double[] {0, 3, -1, 1, 2, 0};

        // Tested function
        ArrayOps.normalize(input);

        assertArrayEquals(expected, input, 1e-9);
    }
    
    @Test
    public void testConvertRange() {
        final float[][] expected = new float[][] {{0, 3}, {-1, 1}, {2, 0}};

        final float[][] input = new float[][] {{0.25f, 1}, {0, 0.5f}, {0.75f, 0.25f}};

        // Tested function
        ArrayOps.convertRange(input, 3 - (-1), -1);

        compareArrays(expected, input);
    }
    
    @Test
    public void testFillArrayShort3D() {
        short[][][] input = new short[22][33][44];
        ArrayOps.fill(input, (short)5);
        for (int i = 0; i < input.length; ++i)
            for (int j = 0; j < input[0].length; ++j)
                for (int z = 0; z < input[0][0].length; ++z)
                    assertEquals((short)5, input[i][j][z]);
    }
    
    @Test
    public void testFillArrayFloat3D() {
        float[][][] input = new float[22][33][44];
        ArrayOps.fill(input, 5.1f);
        for (int i = 0; i < input.length; ++i)
            for (int j = 0; j < input[0].length; ++j)
                for (int z = 0; z < input[0][0].length; ++z)
                    assertEquals(5.1f, input[i][j][z], 1e-9);
    }
    
    @Test
    public void testFillArrayDouble3D() {
        double[][][] input = new double[22][33][44];
        ArrayOps.fill(input, 1.3);
        for (int i = 0; i < input.length; ++i)
            for (int j = 0; j < input[0].length; ++j)
                for (int z = 0; z < input[0][0].length; ++z)
                    assertEquals(1.3, input[i][j][z], 1e-9);
    }
}
