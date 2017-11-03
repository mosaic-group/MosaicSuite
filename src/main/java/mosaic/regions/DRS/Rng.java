package mosaic.regions.DRS;

import org.apache.commons.math3.random.MersenneTwister;

/**
 * MersenneTwister RNG with some helper methods based on ITK implementation to be on the same page with
 * C++ implementation of DRS.
 */
public class Rng extends MersenneTwister {
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates random number generator with 5489 seed common to C++ boost and 
     * standard library implementation.
     */
    public Rng() {
        super(5489);
    }
    
    /**
     * Creates random number generator with provided seed
     * @param aSeed
     */
    public Rng(int aSeed) {
        super(aSeed);
    }
    
    /**
     * Generate a integer in scope [0, n] for n < 2^32-2
     * @param n
     * @return random number
     */
    public int GetIntegerVariate(int n) {
        final long mask = 0xffffffffL;
        // Find which bits are used in n
        long used = n;
        used |= used >> 1;
        used |= used >> 2;
        used |= used >> 4;
        used |= used >> 8;
        used |= used >> 16;
        used |= used >> 32;

        // Draw numbers until one is found in [0,n]
        long i;
        do {
            i = (nextInt() & mask) & used; // toss unused bits to shorten search
        }
        while ( i > n );

        return (int)i;
    }
    
    /**
     * Generate double in range [0, 1]
     * @return random number
     */
    public double GetVariate() {
        return nextDouble();
    }
    
    /**
     * Generate double in range [a, b)
     * @param a
     * @param b
     * @return random number
     */
    public double GetUniformVariate(double a, double b) {
        double u = GetVariateWithOpenUpperRange();
        return ( (1.0 - u) * a + u * b );
    }
    
    /**
     * Generate double in range [0, 1)
     * @return random number
     */
    private double GetVariateWithOpenUpperRange() {
        return (nextInt() & 0xffffffffL) * ( 1.0 / 4294967296.0 );
    }
}
