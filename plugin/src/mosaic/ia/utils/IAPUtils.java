package mosaic.ia.utils;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.util.Arrays;

import javax.vecmath.Point3d;

import weka.estimators.KernelEstimator;

public class IAPUtils {

	public static double[] calculateCDF(double[] qofD) {
		double[] QofD = new double[qofD.length];
		double sum = 0;

		for (int i = 0; i < qofD.length; i++) {
			QofD[i] = sum + qofD[i];
			sum = sum + qofD[i];
		}

		System.out.println("QofD before norm:" + QofD[QofD.length - 1]);
		for (int i = 0; i < qofD.length; i++) {
			QofD[i] = QofD[i] / QofD[QofD.length - 1];
		}
		System.out.println("QofD after norm:" + QofD[QofD.length - 1]);

		return QofD;
	}

	
	public static double[] normalize(double[] array) {

		double sum = 0;
		double[] retarray = new double[array.length];
		for (int i = 0; i < array.length; i++)
			sum = sum + array[i];
		for (int i = 0; i < array.length; i++)
			retarray[i] = array[i] / sum;
		return retarray;
	}
	
	public static double[] calcMinMaxXYZ(Point3d[] points) // returns array with
															// 6 fieldds,
															// minx,miny,minz,maxx,maxy,maxz
	{
		double minx = Float.MAX_VALUE, miny = Float.MAX_VALUE, minz = Float.MAX_VALUE, maxx = Float.MIN_VALUE, maxy = Float.MIN_VALUE, maxz = Float.MIN_VALUE;
		double[] temp = new double[3];
		for (int i = 0; i < points.length; i++) {
			points[i].get(temp);
			if (temp[0] < minx)
				minx = temp[0];
			if (temp[1] < miny)
				miny = temp[1];
			if (temp[2] < minz)
				minz = temp[2];
			if (temp[0] > maxx)
				maxx = temp[0];
			if (temp[1] > maxy)
				maxy = temp[1];
			if (temp[2] > maxz)
				maxz = temp[2];
		}
		double[] minMax = { minx, miny, minz, maxx, maxy, maxz };
		return minMax;
	}

	public static float[][][] imageTo3Darray(ImagePlus image) {

		ImageStack is = image.getStack();
		ImageProcessor imageProc;// ,mgp;

		float[][][] image3d = new float[is.getSize()][is.getWidth()][is
				.getHeight()];

		for (int k = 0; k < is.getSize(); k++) {
			imageProc = is.getProcessor(k + 1);
			// mgp=mgs.getProcessor(k+1);

			image3d[k] = imageProc.getFloatArray();
		}

		/*
		 * for(int i=0;i<is.getSize();i++) { for(int j=0;j<is.getWidth();j++) {
		 * for(int k=0;k<is.getHeight();k++)
		 * System.out.print(image3d[i][j][k]+" ");
		 * 
		 * System.out.println("\n"); } System.out.println("\n"); }
		 */
		return image3d;

	}

	public static double[] getMinMaxMeanD(double[] D) {
		double[] minMax = new double[3];
		minMax[0] = Double.MAX_VALUE; // min
		minMax[1] = Double.MIN_VALUE;// max
		minMax[2] = 0;// mean
		for (int i = 0; i < D.length; i++) {
			if (D[i] < minMax[0])
				minMax[0] = D[i];
			if (D[i] > minMax[1])
				minMax[1] = D[i];
			minMax[2] = minMax[2] + D[i];
		}
		minMax[2] = minMax[2] / D.length;
		return minMax;
	}

	public static double linearInterpolation(double yl, double xl, double yr,
			double xr, double x) {
		double m = (yl - yr) / (xl - xr);
		double c = yl - m * xl;
		return m * x + c;

	}

	public static KernelEstimator createkernelDensityEstimator(
			double[] distances, double weight) {
		double precision = 100d;
		KernelEstimator ker = new KernelEstimator(1 / precision);
		System.out.println("Weight:" + weight);
		for (int i = 0; i < distances.length; i++)
			ker.addValue(distances[i], weight); // weight is important, since
												// bandwidth is calculated with
												// it:
												// http://stackoverflow.com/questions/3511012/how-ist-the-bandwith-calculated-in-weka-kernelestimator-class
		// depending on the changes to the grid, this might have to be changed.
		// System.out.println("Added values to kernel:"+ke.getNumKernels());
/*
		System.out.println("Standard deviation of sample: " + IAPUtils.calcStandDev(distances));
		
		System.out.println("Standard deviation Kernel: " + ker.getStdDev());
		System.out.println("Silverman's bandwidth: " + (1.06 * IAPUtils.calcStandDev(distances)
				* Math.pow(distances.length, -.2)));
		System.out.println("Length:" + distances.length);*/
		
		return ker;
	}

	
	public static double calcSilvermanBandwidth(double [] distances)
	{
		 double q1 = getPercentile(distances, 0.25);
		 double q3 = getPercentile(distances, 0.75);
		 double silBandwidth=.9 * Math.min(IAPUtils.calcStandDev(distances),(q3-q1)/1.34)
					* Math.pow(distances.length, -.2);
		System.out.println("Silverman's bandwidth: " + silBandwidth);
		
		return silBandwidth;
	}
	
	public static double calcWekaWeights(double [] distances)
	{
		 
		 
		 double [] minmaxmean= IAPUtils.getMinMaxMeanD(distances);
		 double range=minmaxmean[1]-minmaxmean[0];
		 double bw=calcSilvermanBandwidth(distances);
		  
		return ((1.0d/distances.length)*(range/bw)*(range/bw));
	}
	
	public static double calcStandDev(double [] distances) {

		double sum = 0.0;
		for (double a : distances)
			sum += a;
		double mean = sum / distances.length;

		double temp = 0;
		for (double a : distances)
			temp += (mean - a) * (mean - a);
		double var= temp/(distances.length-1);

		return Math.sqrt(var);
	}
	
	public static double calcStandDev(float[] distances) {
		double sum = 0.0;
		for (float a : distances)
			sum += a;
		double mean = sum / distances.length;

		double temp = 0;
		for (float a : distances)
			temp += (mean - a) * (mean - a);
		double var= temp/(distances.length-1);

		return Math.sqrt(var);
	}

	public static KernelEstimator createkernelDensityEstimator(
			float[] distances, double weight) {
		double precision = 100d;
		KernelEstimator ker = new KernelEstimator(1 / precision);
		System.out.println("Weight:" + weight);
		for (int i = 0; i < distances.length; i++)
			ker.addValue(distances[i], weight); // weight is important, since
												// bandwidth is calculated with
												// it:
												// http://stackoverflow.com/questions/3511012/how-ist-the-bandwith-calculated-in-weka-kernelestimator-class
		// depending on the changes to the grid, this might have to be changed.
		// System.out.println("Added values to kernel:"+ke.getNumKernels());
	/*	System.out.println("Standard deviation of sample: " + IAPUtils.calcStandDev(distances));
		
		System.out.println("Standard deviation Kernel: " + ker.getStdDev());
		System.out.println("Silverman's bandwidth: " + (1.06 * IAPUtils.calcStandDev(distances)
				* Math.pow(distances.length, -.2)));
		System.out.println("Length:" + distances.length);*/
		return ker;
	}
	
	
	 public static double MACHEPS = 2E-16;
	 //to update machine epsilon
	 public static void updateMacheps() {
	  MACHEPS = 1.0d;
	  do
	   MACHEPS /= 2.0d;
	  while (1 + MACHEPS / 2 != 1);
	  System.out.println("Machine epsilon: "+MACHEPS);
	 }
	 
	
	 
		/**
		 * Returns an estimate of the <code>p</code>th percentile of the values
		 * in the <code>values</code> array. Taken from commons-math.
		 */
		public static final double getPercentile(final double[] values, final double p) {

			final int size = values.length;
			if ((p > 1) || (p <= 0)) {
				throw new IllegalArgumentException("invalid quantile value: " + p);
			}
			if (size == 0) {
				return Double.NaN;
			}
			if (size == 1) {
				return values[0]; // always return single value for n = 1
			}
			double n = size;
			double pos = p * (n + 1);
			double fpos = Math.floor(pos);
			int intPos = (int) fpos;
			double dif = pos - fpos;
			double[] sorted = new double[size];
			System.arraycopy(values, 0, sorted, 0, size);
			Arrays.sort(sorted);

			if (pos < 1) {
				return sorted[0];
			}
			if (pos >= n) {
				return sorted[size - 1];
			}
			double lower = sorted[intPos - 1];
			double upper = sorted[intPos];
			return lower + dif * (upper - lower);
		}
		
		/**
		 * Return the optimal bin number for a histogram of the data given in array, using the 
		 * Freedman and Diaconis rule (bin_space = 2*IQR/n^(1/3)).
		 * Inspired from Fiji TMUtils.java
		 */
		public static  int getOptimBins(double[] values, int minBinNumber, int maxBinNumber) {
			 int size = values.length;
			 double q1 = getPercentile(values, 0.25);
			 double q3 = getPercentile(values, 0.75);
			 double interQRange = q3 - q1;
			 double binWidth = 2 * interQRange * Math.pow(size, -0.33);
			 double[] range = IAPUtils.getMinMaxMeanD(values);
			
			int noBins = (int) ( (range[1]-range[0]) / binWidth + 1 );
			if (noBins > maxBinNumber)
				noBins = maxBinNumber;
			else if (noBins < minBinNumber)
				noBins = minBinNumber;
			return  noBins;
		}
		
	



}
