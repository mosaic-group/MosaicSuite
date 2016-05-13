package mosaic.utils.math;

import java.util.Arrays;


/**
 * This class implements cubic smoothing spline which is comparable to
 * Matlab's 'csaps(x, y, p)' command.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 *
 */
public class CubicSmoothingSpline {
    private final Polynomial[] iSplines;
    private final double[] iX;
    private double[] iY;
    private final double[] iWeights;
    private double iSmoothingParameter;

    public enum FittingStrategy {
        /**
         * In this strategy generated spline has every provided point in such window:
         * maxError/2 < getValue(x[i]) - y[i] <= maxError
         */
        MaxSinglePointValue,
        
        /**
         * In this strategy generated spline has average value of all errors for provided points is in this window:
         * maxError/2 < sumForEvery_i(getValue(x[i]) - y[i]) / numOfPoints <= maxError
         */
        AverageValue
    }
    
    
    /**
     * Creates smoothing spline. Note: Input parameters are not checked for validity!
     * @param aXvalues Increasing numbers representing x-values
     * @param aYvalues y-values corresponding to x-values (aXvalues nad aValues must be same size)
     * @param aStrategy - strategy for fitting of spline
     * @param aMaxErrorValue - maximum error value to be used in strategy
     */
    public CubicSmoothingSpline(double[] aXvalues, double[] aYvalues, FittingStrategy aStrategy, double aMaxErrorValue) {
        // In a case we have only two points provided value close to zero is chosen (it must be > 0)
        // In any other case we start with smoothing value for first iteration of algorithm = 0.5
        this(aXvalues, aYvalues, (aXvalues.length == 2) ? 1e-52 : 0.5, null);
        
        // When we dealing with only two points there is nothing more to do.
        if (aXvalues.length == 2) return;
        
        int step = 1;
        double direction = 1;
        
        if (aStrategy == FittingStrategy.AverageValue) { aMaxErrorValue = aMaxErrorValue * aXvalues.length; }
        
        // Continue till machine epsilon which is around 2^-52
        while (step <= 52) {
            double currentError = 0;
            for (int i = 0; i < aXvalues.length; i++) {
                double diff = Math.abs(getValue(aXvalues[i]) - aYvalues[i]);
                if (aStrategy == FittingStrategy.MaxSinglePointValue) {
                    if (currentError < diff) currentError = diff;
                    // No need to continue if first point with diff bigger then possible error is found
                    if (currentError > aMaxErrorValue) break;
                }
                else if (aStrategy == FittingStrategy.AverageValue) {
                    currentError += diff;
                }
            }
            
            if (currentError > 0.99 * aMaxErrorValue && currentError <= aMaxErrorValue) break;
            direction = (currentError <= aMaxErrorValue) ? -1 : 1;
            step++;
            iSmoothingParameter += direction * Math.pow(2, -step);
            
            // recalculate spline with new smoothing paramter
            resolve(iX, iY, iSplines, iSmoothingParameter, iWeights);
        }
        System.out.println("SM / T: " + iSmoothingParameter + " " + step);
    }
    
    public CubicSmoothingSpline(double[] aXvalues, double[] aYvalues, FittingStrategy aStrategy, double aMaxErrorValue, double aMaxTolerance) {
        this(aXvalues, aYvalues, aStrategy, aMaxErrorValue);
        System.out.println("CubicSmoothing: " + iSmoothingParameter + " " + aMaxErrorValue + " " + aMaxTolerance);
        iY = aYvalues.clone();
        double[] deltaY = new double[iY.length];
        int iter = 0 ;
        double currentError = 0;
        do {
            currentError = 0;
            for (int i = 0; i < aXvalues.length; i++) {
                deltaY[i] = aYvalues[i] - getValue(aXvalues[i]);
                if (Math.abs(deltaY[i]) > currentError) currentError = Math.abs(deltaY[i]);
            }
            
            if (currentError < aMaxTolerance) { System.out.println("Found at: " + currentError);break; }
            
            for (int i = 0; i < aXvalues.length; i++) {
                iY[i] += 2 * deltaY[i];
            }
            
            // recalculate spline with new smoothing paramter
            resolve(iX, iY, iSplines, iSmoothingParameter, iWeights);
            iter++;
        } while (iter < 100000); 
        System.out.println("MAX ERR: " + currentError + " " + (iter));
    }
    
    /**
     * Creates smoothing spline. Note: Input parameters are not checked for validity!
     * @param aXvalues Increasing numbers representing x-values
     * @param aYvalues y-values corresponding to x-values (aXvalues nad aValues must be same size)
     * @param aSmoothingParameter - smoothing parameter in range (0, 1]. For 1 it produces exact interpolation.
     */
    public CubicSmoothingSpline(double[] aXvalues, double[] aYvalues, double aSmoothingParameter) {
        this(aXvalues, aYvalues, aSmoothingParameter, null);
    }

    /**
     * Creates smoothing spline. Note: Input parameters are not checked for validity!
     * @param aXvalues Increasing numbers representing x-values
     * @param aYvalues y-values corresponding to x-values (aXvalues nad aValues must be same size)
     * @param aSmoothingParameter - smoothing parameter in range (0, 1]. For 1 it produces exact interpolation.
     * @param aWeights - weights applied to each (x,y) pair. If null then default value '1' is used for every point.
     */
    public CubicSmoothingSpline(double[] aXvalues, double[] aYvalues, double aSmoothingParameter, double[] aWeights) {
        if (aWeights == null) {
            iWeights = new double[aXvalues.length];
            for (int i = 0; i < iWeights.length; ++i) {
                iWeights[i] = 1;
            }
        }
        else {
            iWeights = aWeights;
        }

        iX = aXvalues;
        iY = aYvalues;
        iSplines = new Polynomial[iX.length - 1];
        iSmoothingParameter = aSmoothingParameter;

        resolve(iX, iY, iSplines, iSmoothingParameter, iWeights);
    }

    /**
     * Gets value from cubic smoothing spline at given point.
     * If values are outside initial x-values then boundary polynomials are used to
     * extrapolate values. (first generated polynomial if aX < aXvalues[0] and last
     * in opposite case).
     * @param aX
     * @return value in point aX
     */
    public double getValue(double aX) {
        int idx = findSplineIndex(aX);

        return iSplines[idx].getValue(aX - iX[idx]);
    }

    /**
     * Gets cubic smoothing spline at given point together with shift that must be applied. Example:
     * Spline sp = getSplineForValue(x);
     * double valueAtX = sp.equation.getValue(x - sp.shift);
     * 
     * If values are outside initial x-values then boundary polynomials are used to
     * extrapolate values. (first generated polynomial if aX < aXvalues[0] and last
     * in opposite case).
     * @param aX
     * @return Spline class with data
     */
    static public class Spline {
        Spline(Polynomial aPolynomial, double aShift) { equation = aPolynomial; shift = aShift; }
        public Polynomial equation;
        public double shift;
    }
    
    public Spline getSplineForValue(double aX) {
        int idx = findSplineIndex(aX);

        return new Spline(iSplines[idx], iX[idx]);
    }


    private int findSplineIndex(double aX) {
        int idx = Arrays.binarySearch(iX, aX);

        // Handle not exact values (possible insertion points)
        // -(idx + 1) handles output from binarySearch in case not exact match,
        // additional -1 points to the earlier element.
        if (idx < 0) {
            idx = -(idx + 1) - 1;
        }
        if (idx < 0)
        {
            idx = 0; // Special case for "inserting point" equal -1 (0 in array)
        }
        if (idx >= iX.length - 1) {
            idx--;
        }
        return idx;
    }
    
    public double getSmoothingParameter() { 
        return iSmoothingParameter;
    }
    
    /**
     * Returns number of knots (x values)
     */
    public int getNumberOfKNots() {
        return iX.length;
    }

    /**
     * Returns knots (x values)
     */
    public double[] getKnots() {
        return iX;
    }

    /**
     * Returns values in knots (y values)
     */
    public double[] getValues() {
        return iY;
    }

    /**
     * Returns weights for each knot (if they are not provided explicitly by user they will be 1)
     */
    public double[] getWeights() {
        return iWeights;
    }

    /**
     * Returns knot value for given index
     * @param aIdx index of knot. Must be in range 0..getNumberOfKnots()-1
     */
    public double getKnot(int aIdx) {
        return iX[aIdx];
    }

    /**
     * Returns all coefficients from each generated polynomial.
     */
    public double[][] getCoefficients() {
        final double[][] l = new double[iSplines.length][4];
        for (int i = 0; i < iSplines.length; ++i) {
            l[i] = iSplines[i].getCoefficients();
        }

        return l;
    }

    @Override
    public String toString() {
        String result = "--------------- Cubic smoothing splines ----------------------\n";
        result += "Knots: (" + iX.length + ")\n";
        result += Arrays.toString(iX);
        result += "\n\nValues: (" + iY.length + ")\n";
        result += Arrays.toString(iY);
        result += "\n\nWeights: (" + iWeights.length + ")\n";
        result += Arrays.toString(iWeights);
        result += "\n\nPolynomials: \n";
        int count = 1;
        for (final Polynomial p : iSplines) {
            result += count++;
            result += ": ";
            result += p;
            result += "\n";
        }
        result += "--------------------------------------------------------------\n";

        return result;
    }


    // ========================================================================

    /**
     * Code below is based on Pascal code from:
     * "SMOOTHING WITH CUBIC SPLINES" by D.S.G. Pollock
     * Queen Mary and Westfield College
     * The University of London
     */
    private void Quincunx(double[] u,double[]  v,double[]  w,double[]  q)
    {
        int j;
        final int n = u.length;

        //factorisation
        u[0] = 0;

        // Changed comparing to original implementation since it was using negative
        // indices
        v[1] = v[1]/u[1];
        w[1] = w[1]/u[1];

        for (j = 2; j < n - 1; ++j) {
            u[j] = u[j]-u[j -2]*Math.pow(w[j -2],2)-u[j -1]*Math.pow(v[j -1],2);
            v[j] = (v[j]-u[j -1]*v[j -1]*w[j -1])/u[j];
            w[j] = w[j]/u[j];
        }

        //forward substitution
        q[1] = q[1] - v[0]*q[0]; // - w[-1]*q[-1];
        for (j = 2; j <  n - 1; ++j) {
            q[j] = q[j]-v[j -1]*q[j -1]-w[j -2]*q[j -2];
        }
        for (j = 1; j < n - 1; ++j) {
            q[j] = q[j]/u[j];
        }

        // back substitution
        q[n] = 0;
        for (j = n - 2; j >= 1; --j) {
            q[j] = q[j]-v[j]*q[j +1]-w[j]*q[j +2];
        }
    }

    /**
     * Code below is based on Pascal code from:
     * "SMOOTHING WITH CUBIC SPLINES" by D.S.G. Pollock
     * Queen Mary and Westfield College
     * The University of London
     *
     * with:
     * S:SplineVec  - changed to aX, aY and aSplines separate parameters
     * n - not necessary in Java
     */

    private void resolve(double[] aX, double[] aY, Polynomial[] aSplines, double aLambda, double[] aWeights)  {
        int n = aX.length;
        final double[] h = new double[n];
        final double[] r = new double[n];
        final double[] f = new double[n];
        final double[] p = new double[n];
        final double[] u = new double[n];
        final double[] v = new double[n];
        final double[] w = new double[n];
        final double[] q = new double[n + 1];

        final double[] sigma = new double[n];
        for (int i = 0; i < n; ++i) {
            sigma[i] = 1.0/aWeights[i];
        }

        // Let n point last element of arrays
        n--;

        int i, j;
        double mu;

        mu = 2 * (1 - aLambda)/(3 * aLambda);
        h[0] = aX[1] - aX[0];
        r[0] = 3 / h[0];

        for (i = 1; i <= n - 1; ++i) {
            h[i] = aX[i + 1] - aX[i];
            r[i] = 3 / h[i];
            f[i] = -(r[i - 1] + r[i]);
            p[i] = 2 * (aX[i+1] - aX[i-1]);
            q[i] = 3 * (aY[i + 1] - aY[i]) / h[i] - 3 * (aY[i] - aY[i - 1]) / h[i - 1];
        }

        for (i = 1; i <= n-1; ++i) {
            u[i] = Math.pow(r[i - 1], 2) * sigma[i - 1] + Math.pow(f[i], 2) * sigma[i] + Math.pow(r[i], 2) * sigma[i + 1];
            u[i] = mu * u[i] + p[i];
            v[i] = f[i] * r[i] * sigma[i] + r[i] * f[i+1] * sigma[i + 1];
            v[i] = mu * v[i] + h[i];
            w[i] = mu * r[i] * r[i+1] * sigma[i + 1];
        }

        Quincunx(u, v, w, q);

        // Spline Parameters
        double d = aY[0] - mu * r[0] * q[1] * sigma[0];
        // For below line there seems to be mistake in original paper since
        // there is at the end of line "* sigma[0]", after changing index to 1
        // it gives same results as in Matlab
        final double nextD = aY[1] - mu * (f[1] * q[1] + r[1] * q[2]) * sigma[1];
        double a = q[1] / (3 * h[0]);
        double b = 0;
        double c = (nextD - d) / h[0] - q[1] * h[0]/3;
        aSplines[0] = new Polynomial(a, b, c, d);

        r[0] = 0;
        double previousC = c;
        for (j = 1; j <= n - 1; ++j) {
            a = (q[j + 1] - q[j]) / (3 * h[j]);
            b = q[j];
            c = (q[j] + q[j - 1]) * h[j - 1] + previousC;
            d = r[j - 1] * q[j - 1] + f[j] * q[j] + r[j] * q[j + 1];
            d = aY[j] - mu * d * sigma[j];

            previousC = c;
            aSplines[j] = new Polynomial(a, b, c, d);
        }
    }
}
