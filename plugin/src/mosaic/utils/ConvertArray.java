package mosaic.utils;

import java.util.List;

/**
 * Helper class for converting between Java's primitives
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class ConvertArray {

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
     * Converts 3D array from short to double
     * @param aArray 3D array of short
     * @return 3D array of doubles
     */
    public static double[][][] toDouble(short[][][] aArray) {
        final int d = aArray.length; 
        final int h = aArray[0].length; 
        final int w = aArray[0][0].length;
        final double[][][] result = new double[d][h][w];
        for (int z = 0; z < d; ++z) {
            for (int y = 0; y < h; ++y) {
                for (int x = 0; x < w; ++x) {
                    result[z][y][x] = aArray[z][y][x];
                }
            }
        }
        return result;
    }
    
    /**
     * Converts 3D array from float to double
     * @param aArray 3D array of floats
     * @return 3D array of doubles
     */
    public static double[][][] toDouble(float[][][] aArray) {
        final int d = aArray.length; final int h = aArray[0].length; final int w = aArray[0][0].length;
        final double[][][] result = new double[d][h][w];
        for (int z = 0; z < d; ++z) {
            for (int y = 0; y < h; ++y) {
                for (int x = 0; x < w; ++x) {
                    result[z][y][x] = aArray[z][y][x];
                }
            }
        }
        return result;
    }
    
    /**
     * Converts 3D array from double to float
     * @param aArray 3D array of floats
     * @return 3D array of doubles
     */
    public static float[][][] toFloat(double[][][] aArray) {
        final int d = aArray.length; final int h = aArray[0].length; final int w = aArray[0][0].length;
        final float[][][] result = new float[d][h][w];
        for (int z = 0; z < d; ++z) {
            for (int y = 0; y < h; ++y) {
                for (int x = 0; x < w; ++x) {
                    result[z][y][x] = (float)aArray[z][y][x];
                }
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
    
    /**
     * Converts 1D array from float to double
     * @param aArray 1D array of floats
     * @return 1D array of doubles
     */
     static public double[] toDouble(List<Double> aList) {
        final int h = aList.size();
        final double[] result = new double[h];
        for (int y = 0; y < h; ++y) {
            result[y] = aList.get(y);
        }
        return result;
    }
    
    /**
     * Converts 1D array from long to int (there is no safety check performed)
     * @param aArray 1D array of long values
     * @return 1D array of int
     */
    public static int[] toInt(long[] aArray) {
        final int len = aArray.length;
        final int [] result = new int[len];
        for (int i = 0; i < len; ++i) {
            result[i] = (int)aArray[i];
        }
        return result;
    }
    
    /**
     * Converts 1D array from short to int (there is no safety check performed)
     * @param aArray 1D array of short values
     * @return 1D array of int
     */
    public static int[] toInt(short[] aArray) {
        final int len = aArray.length;
        final int [] result = new int[len];
        for (int i = 0; i < len; ++i) {
            result[i] = aArray[i];
        }
        return result;
    }
    
    /**
     * Converts 1D array from byte to int (there is no safety check performed)
     * @param aArray 1D array of byte values
     * @return 1D array of int
     */
    public static int[] toInt(byte[] aArray) {
        final int len = aArray.length;
        final int [] result = new int[len];
        for (int i = 0; i < len; ++i) {
            result[i] = aArray[i];
        }
        return result;
    }
    
    /**
     * Converts 1D array from float to int (there is no safety check performed)
     * @param aArray 1D array of float values
     * @return 1D array of int
     */
    public static int[] toInt(float[] aArray) {
        final int len = aArray.length;
        final int [] result = new int[len];
        for (int i = 0; i < len; ++i) {
            result[i] = (int)aArray[i];
        }
        return result;
    }
    
    /**
     * Converts 1D array from int to short (there is no safety check performed)
     * @param aArray 1D array of int values
     * @return 1D array of short
     */
    public static short[] toShort(int[] aArray) {
        final int len = aArray.length;
        final short [] result = new short[len];
        for (int i = 0; i < len; ++i) {
            result[i] = (short) aArray[i];
        }
        return result;
    }
    
    /**
     * Converts 1D array of floats to 2D [width][height] float array column major with provided dimensions
     * @param aArray 1D array of floats
     * @param aWidth
     * @param aHeight
     * @return 2D array of floats [aWidth][aHeight]
     */
    public static float[][] toFloat2D(float[] aArray, int aWidth, int aHeight) {
        if (aArray.length != aWidth * aHeight) 
            throw new RuntimeException("Wrong Dimensions: " + aArray.length + " vs " + aWidth + "/" + aHeight);
        final float [][] result = new float[aWidth][aHeight];
        for (int x = 0; x < aWidth; ++x) {
            for (int y = 0; y < aHeight; ++y) {
                result[x][y] = aArray[y * aWidth + x];
            }
        }
        return result;
    }
    
    /**
     * Converts 1D array of floats to 2D [width][height] double array column major with provided dimensions
     * @param aArray 1D array of doubles
     * @param aWidth
     * @param aHeight
     * @return 2D array of doubles [aWidth][aHeight]
     */
    public static double[][] toDouble2D(double[] aArray, int aWidth, int aHeight) {
        if (aArray.length != aWidth * aHeight) 
            throw new RuntimeException("Wrong Dimensions: " + aArray.length + " vs " + aWidth + "/" + aHeight);
        final double [][] result = new double[aWidth][aHeight];
        for (int x = 0; x < aWidth; ++x) {
            for (int y = 0; y < aHeight; ++y) {
                result[x][y] = aArray[y * aWidth + x];
            }
        }
        return result;
    }
}
