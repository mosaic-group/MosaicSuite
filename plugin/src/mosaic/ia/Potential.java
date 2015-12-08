package mosaic.ia;

import java.util.Arrays;

public class Potential {

    // for non param, the support points must be close enough so that piece wise linearity makes sense, and remains smooth (non smooth => w1 w2 estim, w2 effects d2-d3)
    public static double[] dp = { 0, .05, .1, .15, .2, .25, .3, .35, .4, .45, .5, .55, .6, .65, .7, .75, .8, .85, .9, .95, 1, 1.05, 1.1, 1.15, 1.2, 1.25, 1.3, 1.35, 1.4, 1.45, 1.5, 1.55, 1.6, 1.65, 1.7, 1.75, 1.8, 1.85, 1.9, 1.95, 2 };
    public static double NONPARAM_SMOOTHNESS = .1; // fornonparam penalty; smaller the smoother (smaller => penalty is high => penalty minim gives
    public static int NONPARAM_WEIGHT_SIZE = dp.length;
    
    public static enum PotentialType {
        STEP,
        HERNQUIST,
        L1,
        L2,
        PlUMMER,
        NONPARAM,
        COULOMB
    }
    
    public static void initializeNonParamWeights(double min, double max) {
        dp = new double[NONPARAM_WEIGHT_SIZE];

        final double delta = (max - min) / (NONPARAM_WEIGHT_SIZE - 1);
        final double begin = min;
        for (int i = 0; i < NONPARAM_WEIGHT_SIZE; i++) {
            dp[i] = begin + delta * i;
        }
        
        System.out.println("Number of support points:" + NONPARAM_WEIGHT_SIZE);
        System.out.println("Min: " + min + " Max: " + max);
        System.out.println("Points: " + Arrays.toString(dp));
    }

    static double stepPotential(double di, double threshold) {
        if (di < threshold) {
            return -1d;
        }
        else {
            return 0d;
        }
    }

    static double hernquistPotential(double di, double threshold, double sigma) {
        final double z = (di - threshold) / sigma;
        if (z > 0) {
            return -1 / (1 + z);
        }
        else {
            return -1 * (1 - z);
        }
    }

    static double plummerPotential(double di, double threshold, double sigma) {
        final double z = (di - threshold) / sigma;
        if (z > 0) {
            return -1 * Math.pow(1 + z * z, -.5);
        }
        else {
            return -1;
        }
    }

    static double linearType1(double di, double threshold, double sigma) {
        final double z = (di - threshold) / sigma;
        if (z > 1) {
            return 0;
        }
        else {
            return -1 * (1 - z);
        }
    }

    static double coulomb(double di, double threshold, double sigma) {
        final double z = (di - threshold) / sigma;
        if (z != 0) {
            return 1 / (z * z);
        }
        else {
            return Math.sqrt(Double.MAX_VALUE); // this is a hack.
        }
    }

    static double linearType2(double di, double threshold, double sigma) {
        final double z = (di - threshold) / sigma;
        if (z > 1) {
            return 0;
        }
        else if (z < 0) {
            return -1;
        }
        else {
            return -1 * (1 - z);
        }
    }

    static double nonParametric(double di, double[] weights) // size(weights) <=size(dp)
    {
        final double h = Math.abs(dp[1] - dp[0]);
        double sum = 0;
        double z = 0, kappa = 0;
        for (int i = 0; i < weights.length; i++) {
            z = Math.abs(di - dp[i]);
            if (z <= h) {
                kappa = z / h;
            }
            else {
                kappa = 0;
            }
            sum = sum + weights[i] * kappa;
        }

        return sum;
    }
}
