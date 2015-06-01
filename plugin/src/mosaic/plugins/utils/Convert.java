package mosaic.plugins.utils;


public class Convert {
	public static float[][] toFloat(double[][] aArray) {
		int h = aArray.length; int w = aArray[0].length;
		float [][] result = new float[h][w];
		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				result[y][x] = (float)aArray[y][x];
			}
		}
		return result;
	}
	public static double[][] toDouble(float[][] aArray) {
		int h = aArray.length; int w = aArray[0].length;
		double [][] result = new double[h][w];
		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				result[y][x] = (double)aArray[y][x];
			}
		}
		return result;
	}
}
