package mosaic.ia.utils;

public class StatisticsUtils {
    
    public static class MinMaxMean {
        public double min;
        public double max;
        public double mean;
        
        public MinMaxMean(double aMin, double aMax, double aMean) {min = aMin; max = aMax; mean = aMean;}
    }
    
    /**
     * @return double[] {min, max, mean} of provided data array
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
     * Calculates a PDF from given CDF
     * @param aPdf - input PDF
     * @param aNormalize - should values be normalized?
     * @return CDF
     */
    public static double[] calculateCdf(double[] aPdf, boolean aNormalize) {
        final double[] cdf = new double[aPdf.length];
        
        double sum = 0;
        for (int i = 0; i < aPdf.length; i++) {
            sum += aPdf[i];
            cdf[i] = sum;
        }

        if (aNormalize) {
            normalizeCdf(cdf, false);
        }
        
        return cdf;
    }

    /**
     * Normalizes CDF distribution (last maximum value will be 1.0)
     * @param aCdf - input distribution 
     * @param aGenerateNewContainer - should new container be created or changes should be done "in place"
     * @return normalized CDF
     */
    private static double[] normalizeCdf(final double[] aCdf,  boolean aGenerateNewContainer) {
        final double[] result = aGenerateNewContainer ? new double[aCdf.length] : aCdf;
        
        double maximumValue = aCdf[aCdf.length - 1];
        System.out.println("CDF before normalization:" + maximumValue);
        for (int i = 0; i < aCdf.length; i++) {
            result[i] = aCdf[i] / maximumValue;
        }
        System.out.println("CDF after normalization:" + result[result.length - 1]);
        
        return result;
    }
    
    /**
     * Normalizes PDF distribution to sum/integrate to 1.0
     * @param aPdf - input distribution 
     * @param aGenerateNewContainer - should new container be created or changes should be done "in place"
     * @return normalized PDF
     */
    public static double[] normalizePdf(double[] aPdf, boolean aGenerateNewContainer) {
        final double[] result = aGenerateNewContainer ? new double[aPdf.length] : aPdf;
        
        double sum = 0;
        for (double value : aPdf) {
            sum += value;
        }
        for (int i = 0; i < aPdf.length; i++) {
            result[i] = aPdf[i] / sum;
        }
        
        return result;
    }
}
