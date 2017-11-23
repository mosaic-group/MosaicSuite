package mosaic.utils.math;

import java.util.Arrays;

import edu.mines.jtk.util.ArrayMath;

public class StatisticsUtils {
    
    public static class MinMaxMean {
        public double min;
        public double max;
        public double mean;
        
        public MinMaxMean(double aMin, double aMax, double aMean) {min = aMin; max = aMax; mean = aMean;}
    }
    
    /**
     * @return MinMaxMean of provided data array
     */
    public static MinMaxMean getMinMaxMean(double[] aValues) {
        if (aValues == null || aValues.length == 0) return null;
        
        double min = Double.MAX_VALUE; 
        double max = -Double.MAX_VALUE;
        double mean = 0;
        
        for (double value : aValues) {
            if (value < min) min = value;
            if (value > max) max = value;
            mean += value;
        }
        mean = mean / aValues.length;
        
        return new MinMaxMean(min, max, mean);
    }
    
    /**
     * Calculates a CDF from given PDF
     * @param aPmf - input PMF
     * @param aNormalize - should values be normalized?
     * @return CDF
     */
    public static double[] calculateCdfFromPmf(double[] aPmf, boolean aNormalize) {
        final double[] cdf = new double[aPmf.length];
        
        double sum = 0;
        for (int i = 0; i < aPmf.length; i++) {
            sum += aPmf[i];
            cdf[i] = sum;
        }

        if (aNormalize) {
            normalizeDiscreteCdf(cdf, false);
        }
        
        return cdf;
    }

    /**
     * Normalizes CDF distribution (last maximum value will be 1.0)
     * @param aCdf - input distribution 
     * @param aGenerateNewContainer - should new container be created or changes should be done "in place"
     * @return normalized CDF
     */
    public static double[] normalizeDiscreteCdf(final double[] aCdf,  boolean aGenerateNewContainer) {
        final double[] result = aGenerateNewContainer ? new double[aCdf.length] : aCdf;
        
        double maximumValue = aCdf[aCdf.length - 1];
        for (int i = 0; i < aCdf.length; i++) {
            result[i] = aCdf[i] / maximumValue;
        }
        
        return result;
    }
    
    /**
     * Normalizes PMF distribution to sum/integrate to 1.0
     * @param aPmf - input distribution 
     * @param aGenerateNewContainer - should new container be created or changes should be done "in place"
     * @return normalized PDF
     */
    public static double[] normalizePmf(double[] aPmf, boolean aGenerateNewContainer) {
        final double[] result = aGenerateNewContainer ? new double[aPmf.length] : aPmf;
        
        double sum = 0;
        for (double value : aPmf) {
            sum += value;
        }
        for (int i = 0; i < aPmf.length; i++) {
            result[i] = aPmf[i] / sum;
        }
        return result;
    }
    
    /**
     * Calculates interpolated value of Y for provided aXpoint
     * @param aXmin
     * @param aXmax
     * @param aYmin
     * @param aYmax
     * @param aXpoint
     * @return
     */
    private static double linearInterpolation(double aXmin, double aXmax, double aYmin, double aYmax, double aXpoint) {
        final double a = (aYmin - aYmax) / (aXmin - aXmax);
        final double b = aYmin - a * aXmin;
        return a * aXpoint + b;
    }
    
    /**
     * Calculated CDF(aX) for given discretized PDF and its grid values. If aX is out-of boundary then
     * value is 0 if it's smaller that smallest grid value and 1.0 if larger than maximum of grid values.
     * @param aX
     * @param aPdf - discretized continous distribution
     * @param aGridValues
     * @return value of CDF(aX)
     */
    public static double calculateCdfAtPoint(double aX, double[] aPdf, double[] aGridValues) {
        double sum = 0.0;
        if (aX > aGridValues[0]) {
            for (int i = 0; i < aGridValues.length - 1; ++i) {
                if (aX < aGridValues[i + 1]) {
                    double pdfAtX = linearInterpolation(aGridValues[i], aGridValues[i + 1], aPdf[i], aPdf[i + 1], aX);
                    sum += (aPdf[i] + pdfAtX) / 2 * (aX - aGridValues[i]);
                    break;
                }
                sum += (aPdf[i]  + aPdf[i + 1]) / 2 * (aGridValues[i + 1] - aGridValues[i]);
            }
        }
        return sum;
    }
    
    /**
     * Normalize discretized PDF on given grid
     * @param aPdf
     * @param aGridValues
     * @param aGenerateNewContainer
     * @return
     */
    public static double[] normalizePdf(double[] aPdf, double[] aGridValues, boolean aGenerateNewContainer) {
        final double[] result = aGenerateNewContainer ? new double[aPdf.length] : aPdf;
        
        double sum = calculateCdfAtPoint(aGridValues[aGridValues.length - 1], aPdf, aGridValues);
        for (int i = 0; i < result.length; ++i) {
            result[i] = aPdf[i] / sum;
        }
        
        return result;
    }
    
    
    /**
     * Calcualtes CDF from provided PDF and grid on which PDF is defined.
     * @param aPdf
     * @param grid
     * @return
     */
    public static double[] calculateCdfFromPdf(double[] aPdf, double[] grid) {
        final double[] cdf = new double[aPdf.length];
    
        cdf[0] = 0;
        for (int i = 0; i < aPdf.length - 1; ++i) {
            cdf[i+1] = (aPdf[i]  + aPdf[i + 1]) / 2 * (grid[i + 1] - grid[i]) + cdf[i];
        }
        for (int i = 0; i < cdf.length; ++i) cdf[i] /= cdf[cdf.length - 1];
        return cdf;
    }
    
    /**
     * @return sample variance of provided data
     */
    public static double calcSampleVariance(double[] aValues) {
        double mean = getMinMaxMean(aValues).mean;

        double sum = 0;
        for (final double a : aValues) {
            sum += Math.pow(mean - a, 2);
        }
        
        return sum / (aValues.length - 1);
    }
    
    /**
     * @return standard deviation of provided data (with sample variance)
     */
    public static double calcStandardDev(double[] aValues) {
        return Math.sqrt(calcSampleVariance(aValues));
    }
    
    /**
     * Returns an estimate (linear interpolation) of the p'th percentile of the provided values
     * If aPercentile = 0, it returns first element of sorted(aValues) - as Matlab does.
     */
    public static final double getPercentile(final double[] aValues, final double aPercentile) {
        final int size = aValues.length;
        if ((aPercentile > 1) || (aPercentile < 0)) {
            throw new IllegalArgumentException("Invalid quantile value: " + aPercentile);
        }
        else if (size == 0) {
            return Double.NaN;
        }
        else if (size == 1) {
            return aValues[0];
        }
        
        final double realPos = aPercentile * (size - 1);
        final int intPos = (int) realPos;

        final double[] sorted = aValues.clone();
        Arrays.sort(sorted);

        if (intPos == size - 1) {
            return sorted[size - 1];
        }
        
        final double diff = realPos - intPos;
        final double lower = sorted[intPos];
        final double upper = sorted[intPos + 1];
        // return linear interpolation based on diff value
        return lower + diff * (upper - lower);
    }
    
    /**
     * Calculates sample Pearson correlation coefficient for provided sample values
     */
    public static double samplePearsonCorrelationCoefficient(double[] aX, double[] aY) {
        double xMean = ArrayMath.sum(aX)/aX.length;
        double yMean = ArrayMath.sum(aY)/aY.length;
 
        double numerator = 0;
        double denA = 0;
        double denB = 0;
        for (int j = 0; j < aX.length; j++) {
            numerator += (aX[j] - xMean) * (aY[j] - yMean);
            denA += Math.pow((aX[j] - xMean), 2);
            denB += Math.pow((aY[j] - yMean), 2);
        }

        return numerator / (Math.sqrt(denA * denB));
    }
}
