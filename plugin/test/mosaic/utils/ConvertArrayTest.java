package mosaic.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class ConvertArrayTest {

    @Test
    public void testToDouble2D() {
        final float[][] input = new float[][] {{1.0f, 1.5f, 2.0f}, {0.0f, -1.0f, -3.0f}};

        // Tested method
        final double[][] result = ConvertArray.toDouble(input);

        assertEquals(input.length, result.length);
        assertEquals(input[0].length, result[0].length);
        for (int i = 0; i < input.length; ++i) {
            for (int j = 0; j < input[0].length; ++j) {
                assertEquals("(" + i + "," + j + ")", input[i][j], result[i][j], 0.0);
            }
        }
    }

    @Test
    public void testToFloat2D() {
        final double[][] input = new double[][] {{1.0, 1.5, 2.0}, {0.0, -1.0, -3.0}};

        // Tested method
        final float[][] result = ConvertArray.toFloat(input);

        assertEquals(input.length, result.length);
        assertEquals(input[0].length, result[0].length);
        for (int i = 0; i < input.length; ++i) {
            for (int j = 0; j < input[0].length; ++j) {
                assertEquals("(" + i + "," + j + ")", input[i][j], result[i][j], 0.0);
            }
        }
    }

    @Test
    public void testToDouble1D() {
        final float[] input = new float[] {1.0f, 1.5f, 2.0f, 0.0f, -1.0f, -3.0f};

        // Tested method
        final double[] result = ConvertArray.toDouble(input);

        assertEquals(input.length, result.length);
        for (int i = 0; i < input.length; ++i) {
            assertEquals("(" + i + ")", input[i], result[i], 0.0);
        }
    }

    @Test
    public void testToFloat1D() {
        final double[] input = new double[] {1.0, 1.5, 2.0, 0.0, -1.0, -3.0};

        // Tested method
        final float[] result = ConvertArray.toFloat(input);

        assertEquals(input.length, result.length);
        for (int i = 0; i < input.length; ++i) {
            assertEquals("(" + i + ")", input[i], result[i], 0.0);
        }
    }
    
    @Test
    public void testToInt1D() {
        final long[] input = new long[] {0, 1 ,2, -1 ,-2};

        // Tested method
        final int[] result = ConvertArray.toInt(input);
 
        assertEquals(input.length, result.length);
        for (int i = 0; i < input.length; ++i) {
            assertEquals("(" + i + ")", input[i], result[i]);
        }
    }
    
    @Test
    public void testToFloatFrom1Dto2D() {
        final float[] input = new float[] {1.0f, 1.5f, 2.0f, 0.0f, -1.0f, -3.0f};
        final float[][] expected = new float[][] {{1.0f, 0.0f}, {1.5f, -1.0f}, {2.0f, -3.0f}};
        
        // Tested method
        final float[][] result = ConvertArray.toFloat2D(input, 3, 2);
        
        for (int i = 0; i < expected.length; ++i) {
            Assert.assertArrayEquals(expected[i], result[i], 0.0f);
         }
    }
}
