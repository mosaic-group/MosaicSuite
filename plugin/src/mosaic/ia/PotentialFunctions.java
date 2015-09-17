package mosaic.ia;


public class PotentialFunctions {

    // for non param, the support points must be close enough so that piece wise linearity makes sense, and remains smooth (non smooth => w1 w2 estim, w2 effects d2-d3)
    // static double [] dp={0,5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95}; // for non parametric
    // static double [] dp={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40};
    // static int [] dp={0,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,36,38,40};
    // static double [] dp={0,.5,1,1.5,2,2.5,3,3.5,4,4.5,5,5.5,6,6.5,7,7.5,8,8.5,9,9.5,1,10,10.5,11,11.5,12,12.5,13,13.5,14,14.5,15,15.5,16,16.5,17,17.5,18,18.5,19,19.5,20};
    public static double[] dp = { 0, .05, .1, .15, .2, .25, .3, .35, .4, .45, .5, .55, .6, .65, .7, .75, .8, .85, .9, .95, 1, 1.05, 1.1, 1.15, 1.2, 1.25, 1.3, 1.35, 1.4, 1.45, 1.5, 1.55, 1.6, 1.65,
            1.7, 1.75, 1.8, 1.85, 1.9, 1.95, 2 };
    public static double NONPARAM_SMOOTHNESS = .1; // fornonparam penalty; smaller the smoother (smaller => penalty is high => penalty minim gives

    public static int NONPARAM_WEIGHT_SIZE = 41; // 21st is 0
    public static final int PARAM_NON_STEP = 3;
    public static final int PARAM_STEP = 2;
    public static final int STEP = 1;
    public static final int HERNQUIST = 2;
    public static final int L1 = 3;
    public static final int L2 = 4;
    public static final int PlUMMER = 5;
    public static final int COULOMB = 7;
    public static final int NONPARAM = 6;

    public static void initializeNonParamWeights(double min, double max) {
        // PotentialFunctions.NONPARAM_WEIGHT_SIZE=weight_size;
        System.out.println("# of support points changed to:" + PotentialFunctions.NONPARAM_WEIGHT_SIZE);
        PotentialFunctions.dp = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE];

        System.out.println("Min:" + min + " Max:" + max);
        double inc = (max - min) / PotentialFunctions.NONPARAM_WEIGHT_SIZE;
        double begin = -inc;
        for (int i = 0; i < PotentialFunctions.NONPARAM_WEIGHT_SIZE; i++) {
            PotentialFunctions.dp[i] = begin + inc * i;
            System.out.print(PotentialFunctions.dp[i] + ",");
        }
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

        double z = (di - threshold) / sigma;
        if (z > 0) {
            return -1 / (1 + z);
        }
        else {
            return -1 * (1 - z);
        }

    }

    static double plummerPotential(double di, double threshold, double sigma) {
        double z = (di - threshold) / sigma;
        if (z > 0) {
            return -1 * Math.pow(1 + z * z, -.5);
        }
        else {
            return -1;
        }

    }

    static double linearType1(double di, double threshold, double sigma) {
        double z = (di - threshold) / sigma;
        if (z > 1) {
            return 0;
        }
        else {
            return -1 * (1 - z);
        }

    }

    static double coulomb(double di, double threshold, double sigma) {
        double z = (di - threshold) / sigma;
        if (z != 0) {
            return 1 / (z * z);
        }
        else {
            return Math.sqrt(Double.MAX_VALUE); // this is a hack.
        }

    }

    static double linearType2(double di, double threshold, double sigma) {
        double z = (di - threshold) / sigma;
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

        double h = Math.abs(dp[1] - dp[0]);
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
