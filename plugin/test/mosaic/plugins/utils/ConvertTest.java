package mosaic.plugins.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class ConvertTest {

    @Test
    public void testToDouble2D() {
        float[][] input = new float[][] {{1.0f, 1.5f, 2.0f}, {0.0f, -1.0f, -3.0f}};
        
        // Tested method
        double[][] result = Convert.toDouble(input);
        
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
        double[][] input = new double[][] {{1.0, 1.5, 2.0}, {0.0, -1.0, -3.0}};
        
        // Tested method
        float[][] result = Convert.toFloat(input);
        
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
        float[] input = new float[] {1.0f, 1.5f, 2.0f, 0.0f, -1.0f, -3.0f};
        
        // Tested method
        double[] result = Convert.toDouble(input);
        
        assertEquals(input.length, result.length);
        for (int i = 0; i < input.length; ++i) {
            assertEquals("(" + i + ")", input[i], result[i], 0.0);
        }
    }
    
    @Test
    public void testToFloat1D() {
        double[] input = new double[] {1.0, 1.5, 2.0, 0.0, -1.0, -3.0};
        
        // Tested method
        float[] result = Convert.toFloat(input);
        
        assertEquals(input.length, result.length);
        for (int i = 0; i < input.length; ++i) {
            assertEquals("(" + i + ")", input[i], result[i], 0.0);
        }
    }
}
