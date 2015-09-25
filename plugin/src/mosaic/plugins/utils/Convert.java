package mosaic.plugins.utils;

/**
 * Helper class for converting between Java's primitives
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class Convert {

    /**
     * Converts 2D array from double to float
     * @param aArray 2D array of doubles
     * @return 2D array of floats
     */
    public static float[][] toFloat(double[][] aArray) {
        final int h = aArray.length; final int w = aArray[0].length;
        final float [][] result = new float[h][w];
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                result[y][x] = (float)aArray[y][x];
            }
        }
        return result;
    }

    /**
     * Converts 2D array from float to double
     * @param aArray 2D array of floats
     * @return 2D array of doubles
     */
    public static double[][] toDouble(float[][] aArray) {
        final int h = aArray.length; final int w = aArray[0].length;
        final double [][] result = new double[h][w];
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                result[y][x] = aArray[y][x];
            }
        }
        return result;
    }

    /**
     * Converts 1D array from double to float
     * @param aArray 1D array of doubles
     * @return 1D array of floats
     */
    public static float[] toFloat(double[] aArray) {
        final int len = aArray.length;
        final float [] result = new float[len];
        for (int i = 0; i < len; ++i) {
            result[i] = (float)aArray[i];
        }
        return result;
    }

    /**
     * Converts 1D array from float to double
     * @param aArray 1D array of floats
     * @return 1D array of doubles
     */
    public static double[] toDouble(float[] aArray) {
        final int h = aArray.length;
        final double[] result = new double[h];
        for (int y = 0; y < h; ++y) {
            result[y] = aArray[y];
        }
        return result;
    }
}
