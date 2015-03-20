package mosaic.particleTracker;


/**
 * Calculates linear least squares for given pairs (x, y)
 */
public class LeastSquares {
    private double iBeta;  // slope of line
    private double iAlpha; // Y-axis intercept value
    
    /**
     * Provide class with input data (pairs (x, y) for which linear least square should be 
     * calculated. aX and aY should be same length.
     * 
     * @param aX
     * @param aY
     */
    public LeastSquares calculate(double[] aX, double[] aY) {
        if (aX.length != aY.length) {
            throw new IllegalArgumentException("Both arrays should have same number of elements [" + aX.length + " vs. " + aY.length + "]");
        }
        
        final double SX = Sum(aX);
        final double SY = Sum(aY);
        final double SXX = SumMultiplied(aX, aX);
        final double SXY = SumMultiplied(aX, aY);
        final double n = aX.length;
        
        iBeta = (SXY - SX*SY/n) / (SXX-SX*SX/n);
        iAlpha = (SY - iBeta * SX) / n; 
        
        return this;
    }
    
    /**
     * @return the y-axis intercept
     */
    public double getAlpha() { return iAlpha; }
    
    /**
     * @return the slope
     */
    public double getBeta()  { return iBeta; }
    
    /**
     * Calculates sum of all elements in aValues
     * 
     * @param aValues
     * @return sum of all elements
     */
    private double Sum(final double[] aValues) {
        double sum = 0;
        for (int i = 0; i < aValues.length; ++i) {
            sum += aValues[i];
        }
        
        return sum;
    }
    
    /**
     * Calculates sum of aX[0]*aY[0] + ... + aX[n]*aY[n] (product of two vectors aX and aY)
     * Both - aX and aY - should be same length.
     * 
     * @param aX 
     * @param aY
     * @return product of aX and aY
     */
    private double SumMultiplied(final double[] aX, final double[] aY) {
        double sum = 0;
        for (int i = 0; i < aX.length; ++i) {
            sum += aX[i] * aY[i];
        }
        
        return sum;
    }
}
