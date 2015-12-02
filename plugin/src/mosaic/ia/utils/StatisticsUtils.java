package mosaic.ia.utils;

public class StatisticsUtils {
    /**
     * @return double[] {min, max, mean} of provided data array
     */
    public static double[] getMinMaxMean(double[] aValues) {
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
        
        return new double[] {min, max, mean};
    }
}
