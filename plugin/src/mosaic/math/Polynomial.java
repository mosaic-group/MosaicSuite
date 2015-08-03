package mosaic.math;

import java.util.Arrays;

/**
 * This class implements polynomial functionality. It can calculate values at given point and
 * calculate derivative/integral of created polynomial.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class Polynomial {
    // Keeps coefficients in Matlab style (first element of array is the coefficient for highest power of polynomial,
    // ... the last is constant).
    double[] iCoeffs;
    int iDegree;
    
    /**
     * Constructs polynomial from given coefficients. They should be given in Matlab style 
     * (first element of array is the coefficient for highest power of polynomial, the last is constant).
     * @param aCoefficients
     */
    public Polynomial(double... aCoefficients) {
        setCoefficients(aCoefficients);
    }
    
    /**
     * Creates new polynomial with same coefficients
     * @return
     */
    public Polynomial copy() {
        return new Polynomial(iCoeffs);
    }
    
    /**
     * Calculates value of polynomial at given point
     * @param aX
     * @return value at point aX
     */
    public double getValue(double aX) {
        // Calculate value using this way:
        // ax^3 + bx^2 + cx + d = (((a) * x + b) * x + c) * x + d
        double result = iCoeffs[0];
        for (int n = 1; n <= iDegree; ++n) {
            result = aX * result + iCoeffs[n]; 
        }
        return result;
    }
    
    /**
     * Returns degree of polynomial
     * @return
     */
    public int getDegree() {
        return iDegree;
    }
    
    /**
     * Returns coefficient value for given degree. For example:
     * getCoefficient(0) returns value of constant
     * getCoefficient(2) returns value of coefficient at x^2
     * @param aDegree
     * @return
     */
    public double getCoefficient(int aDegree) {
        if (aDegree < 0 || aDegree > iDegree) return 0.0;
        
        return iCoeffs[iDegree - aDegree];
    }
    
    /**
     * Returns all coefficients in Matlab style 
     * (first element of array is the coefficient for highest power of polynomial, the last is constant).
     * @return
     */
    public double[] getCoefficients() {

        return iCoeffs;
    }
    
    /**
     * Creates new polynomial which is derivative of original one
     * @param aOrder - order of derivative
     * @return new polynomial
     */
    public Polynomial getDerivative(int aOrder) {
        if (aOrder < 0) throw new IllegalArgumentException("Order derivative < 0");
        if (aOrder > iDegree) return new Polynomial(0);
        
        // Plus 1 since constant
        int noOfCoefs = iDegree - aOrder + 1;
        
        double[] newCoeffs = new double[noOfCoefs];
        for (int n = 0; n < noOfCoefs; ++n) {
            double coeff = iCoeffs[n];
            for (int i = 0; i < aOrder; ++i) coeff *= (iDegree - i - n);
            newCoeffs[n] = coeff;
        }
        
        return new Polynomial(newCoeffs);
    }
    
    /**
     * Creates new polynomial which is integral of original one
     * @param aConstant - integration constant
     * @return new polynomial
     */
    public Polynomial getIntegral(double aConstant) {
        // Plus 1 since constant
        int noOfCoefs = iDegree + 1 + 1;
        
        double[] newCoeffs = new double[noOfCoefs];
        for (int n = 0; n < noOfCoefs - 1; ++n) {
            double coeff = iCoeffs[n];
            coeff /= (iDegree - n + 1);
            newCoeffs[n] = coeff;
        }
        newCoeffs[noOfCoefs - 1] = aConstant;
        
        return new Polynomial(newCoeffs);
    }
    
    /**
     * Sets coefficients and aDegree of new polynomial. Removes leading zeros from given coefficients.
     * So instead of creating: 0 * x^2 + 3 * x + 5, polynomial 3 * x + 5 will be created.
     * @param aCoefficients
     */
    private void setCoefficients(double[] aCoefficients) {
        // Compact polynomial (remove leading zeros)
        int idx = 0;
        for (double c : aCoefficients) {
            if (c != 0) break;
            ++idx;
        }
        if (idx == aCoefficients.length) idx--;
        
        // Set data
        iCoeffs = Arrays.copyOfRange(aCoefficients, idx, aCoefficients.length);
        iDegree = iCoeffs.length - 1;
    }
    
    @Override
    /**
     * Simple but enough to do debugging
     */
    public String toString() {
        String str = "f(x) = ";
        boolean firstCoefficient = true;
        for (int n = 0; n <= iDegree; ++n) {
            if (!firstCoefficient || iCoeffs[n] < 0) {
                str += " " + (iCoeffs[n] >= 0 ? "+ " : "- ");
            }
            firstCoefficient = false;
                
            // abs since sign is printed above
            str += String.format("%g", Math.abs(iCoeffs[n]));
            if ((iDegree - n) != 0) str += "*x^" + (iDegree - n);
        }
        
        return str;
    }
    
    @Override
    /**
     * Returns true if two polynomials are equal.
     * They must have same degree and values of all coefficients.
     */
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        Polynomial cmp = (Polynomial)obj;
        if (cmp.iDegree != this.iDegree) return false;
        return Arrays.equals(cmp.iCoeffs, this.iCoeffs);
    }
}
