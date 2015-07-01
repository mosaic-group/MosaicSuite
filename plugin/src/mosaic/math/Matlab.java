package mosaic.math;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;


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
	
	   /**
     * Generates spaced array (as in Matlab double start:step:stop operation)
     * @param aStart
     * @param aStep
     * @param aNoOfSteps
     * @return
     */
    public static double[] regularySpacedArray(double aStart, double aStep, double aStop) {
        if (aStep == 0 || !((aStart > aStop) ^ (aStep > 0))) return null;
        
        int noOfSteps = (int)((aStop - aStart)/aStep) + 1;
        double[] result = new double[noOfSteps];
        
        double val = aStart;
        for (int i = 0; i < noOfSteps; ++i) {
            result[i] = val;
            val += aStep;
        }
        
        return result;
    }
    
	/**
     * Generates spaced vector (as in Matlab double start:step:stop operation)
     * @param aMin
     * @param aMax
     * @param aStep
     * @return
     */
    public static Matrix regularySpacedVector(double aMin, double aStep, double aMax) {
        return Matrix.mkRowVector(regularySpacedArray(aMin, aStep, aMax)); 
    }
    
    /**
     * Generates two matrices as Matlab's command 'meshgrid'
     * @param aVector1 - row values
     * @param aVector2 - col values
     * @return
     */
	public static Matrix[] meshgrid(Matrix aVector1, Matrix aVector2) {
	    // Adjust data but do not change users input - if needed make a copy.
	    if (aVector1.isColVector()) aVector1 = aVector1.copy().transpose();
		if (aVector2.isRowVector()) aVector2 = aVector2.copy().transpose();
		
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
		Matrix result = new Matrix(aImg.numRows(), aImg.numCols());
		
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
	
	/**
	 * Implementation of 'imresize' Matlab function for bicubic interpolation
	 * @param aM Input image
	 * @param scale scale of image
	 * @return scaled image
	 */
    public static Matrix imresize(Matrix aM, double scale) {
        int w = aM.numCols(); int h = aM.numRows();
        int nw = (int) Math.ceil(w * scale); int nh = (int)Math.ceil(h * scale);
        
        return imresize(aM, nw, nh);
    }
    
    /**
     * Implementation of 'imresize' Matlab function for bicubic interpolation
     * @param aM Input image
     * @param aNewWidth
     * @param aNewHeight
     * @return scaled image
     */
    public static Matrix imresize(Matrix aM, int aNewWidth, int aNewHeight) {
        // TODO: This is temporary implementation with IJ functionality
        //       It is not fully compatibile with Matlab's output - should be investigated.
        //       and decided if it is to be changed or not. 
        //       Notice: Matlab's implementation is little bit strange and does not scale good at the beginning and and of 
        //       resized image.
        float[][] input = aM.getArrayYXasFloats();
        ImageProcessor ip = new FloatProcessor(input);
        ip.setInterpolationMethod(FloatProcessor.BICUBIC);
        ImageProcessor ip2 = ip.resize(aNewHeight, aNewWidth);
        float[][] output = ip2.getFloatArray();
        
        Matrix result = new Matrix(output);
        
        
        return result;
    }
}
