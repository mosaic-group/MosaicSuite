package mosaic.filamentSegmentation;

import mosaic.utils.math.Matrix;

public class GaussPsf {
	public static double[][] generateKernel(int xl, int yl, double sigma) {
		double[][] psf = new double[yl][xl];
		double middlex = (double)(xl-1)/2;
		double middley = (double)(yl-1)/2;
		
		// Generate values
		double sum = 0;
		for (int y = 0; y < yl; ++y) 
			for (int x = 0; x < xl; ++x) {
			double val = Math.exp(-(Math.pow(x-middlex, 2) + Math.pow(y-middley, 2))/(2 * sigma * sigma));
			psf[y][x] = val;
			sum += val;
		}
		
		// Normalize
		for (int y = 0; y < yl; ++y) {
			for (int x = 0; x < xl; ++x) {
			    psf[y][x] /= sum;
			}
		}
		
		return psf;
	}
	
	public static Matrix generate(int xl, int yl, double sigma) {
		return new Matrix(generateKernel(xl, yl, sigma));
	}
}
