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
}
