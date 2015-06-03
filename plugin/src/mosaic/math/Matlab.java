package mosaic.math;


public class Matlab {
	/**
	 * Generates linear spaced numbers in array (as in Matlab)
	 * @param aMin
	 * @param aMax
	 * @param aNoOfSteps
	 * @return
	 */
	public static double[] linspaceArray(double aMin, double aMax, int aNoOfSteps) {
		if (aNoOfSteps < 1) return null;
		
		double[] result = new double[aNoOfSteps];
		if (aNoOfSteps > 1) {
			double step = (aMax - aMin) / (aNoOfSteps - 1);
			for (int i = 0; i < aNoOfSteps -1; ++i) {
				result[i] = aMin + i * step;
			}
		}
		
		result[aNoOfSteps - 1] = aMax;
		
		return result;
	}
	
	/**
	 * Generates linear spaced vector (as in Matlab)
	 * @param aMin
	 * @param aMax
	 * @param aNoOfSteps
	 * @return
	 */
	public static Matrix linspace(double aMin, double aMax, int aNoOfSteps) {
		return Matrix.mkRowVector(linspaceArray(aMin, aMax, aNoOfSteps)); 
	}
	
	public static Matrix[] meshgrid(Matrix aVector1, Matrix aVector2) {
		aVector2.transpose();
		int r = aVector2.numRows(); int c = aVector1.numCols();
		
		Matrix m1 = new Matrix(r, c);
		Matrix m2 = new Matrix(r, c);

		for (int i = 0; i < r; ++i) m1.insert(aVector1, i,  0);
		for (int i = 0; i < c; ++i) m2.insert(aVector2, 0, i);
		
		return new Matrix[] {m1, m2};
	}
	
	/**
	 * Implementation of matlab's imfilter for 'symmetric' boundary options
	 * @param aImg - input image
	 * @param aFilter - filter to be used
	 * @return - filtered image (aImg is not changed)
	 */
	public static Matrix imfilterSymmetric(Matrix aImg, Matrix aFilter) {
		Matrix result = new Matrix(aImg);
		
		int filterRows = aFilter.numRows(); 
		int filterCols = aFilter.numCols();
		int filterRowMiddle = ((filterRows + 1) / 2) - 1;
		int filterColMiddle = ((filterCols + 1) / 2) - 1;
		double[][] filter = aFilter.getArrayYX();
		int imageRows = aImg.numRows(); 
		int imageCols = aImg.numCols(); 
		double[][] image = aImg.getArrayYX();

		for (int r = 0; r < imageRows; ++r) {
			for (int c = 0; c < imageCols; ++c) {
				// (r,c) - element of image to be calculated
				double sum = 0.0;
				for (int fr = 0; fr < filterRows; ++fr) {
					for (int fc = 0; fc < filterCols; ++fc) {
						// Calculate image coordinates for (fr, fc) filter element
						// This is symmetric filter so values outside the bounds of the array 
						// are computed by mirror-reflecting the array across the array border.
						int imr = r - filterRowMiddle + fr;
						int imc = c - filterColMiddle + fc;
						do {
							// Intentionally not if..else(d). For very long filters
							// imr/imc can be smaller that 0 and after falling into one 
							// case they can be immediatelly bigger than imageRows/Cols
							// that is also a reason for loop here since it may require 
							// several loops. (Usually filters are smaller than image so it 
							// is not a case but still left here to fully comply with Matlab
							// version).
							if (imr < 0) imr = -imr -1;
							if (imr >= imageRows) imr = (imageRows-1) - (imr - (imageRows-1) - 1);
							if (imc < 0) imc = -imc -1;
							if (imc >= imageCols) imc = (imageCols-1) - (imc - (imageCols-1) - 1);
						} while (!(imr >= 0 && imr < imageRows && imc >= 0 && imc < imageCols)); 

						// After finding coordinates just compute next part of filter sum.
						sum += image[imr][imc] * filter[fr][fc];
					}
				}
				result.set(r, c, sum);
			}
		}

		return result;
	}
}
