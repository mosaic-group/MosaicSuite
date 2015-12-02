package mosaic.ia;


import java.util.Arrays;
import java.util.Random;

import ij.IJ;


class HypothesisTesting {

    private final double[] CDFGrid;
    private final double[] DGrid;
    private final double[] D;
    private double[] DRand;
    private double[] T;
    private final double[] params; // same convention
    private final int type;
    private final int K;
    private final double alpha;
    private double Tob;

    public HypothesisTesting(double[] cDFGrid, double[] dGrid, double[] D, double[] params, int type, int K, double alpha) {
        super();
        CDFGrid = cDFGrid;
        DGrid = dGrid;
        this.params = params;
        this.type = type;
        this.D = D;
        this.K = K;
        this.alpha = alpha;
    }

    public boolean rankTest() {
        calculateT();
        final PotentialCalculator pcOb = new PotentialCalculator(D, params, type);
        pcOb.calculateWOEpsilon();
        Tob = -1 * pcOb.getSumPotential();

        double maxT = Double.MIN_VALUE, minT = Double.MAX_VALUE;

        for (int i = 0; i < K; i++) {
            if (minT > T[i]) {
                minT = T[i];
            }
            if (maxT < T[i]) {
                maxT = T[i];
            }

        }
        int i = 0;
        for (i = 0; i < K; i++) {
            if (Tob <= T[i]) {
                break;
            }
        }

        System.out.println("MinT: " + minT + " maxT: " + maxT);
        System.out.println("T obs: " + Tob + " found at rank: " + i);
        if (i > (int) ((1 - alpha) * K)) {
            if ((K - i) == 0) {
                System.out.println("Null hypothesis rejected, rank: " + i + " out of " + K + " p-value: " + 1.0 / K);
                IJ.showMessage("Null hypothesis: No interaction - Rejected, rank: " + i + " out of " + K + "MC runs with alpha= " + alpha + " p-value < " + 1.0 / K);
            }
            else {
                System.out.println("Null hypothesis rejected, rank: " + i + " out of " + K + " p-value: " + ((double) K - i) / K);
                IJ.showMessage("Null hypothesis: No interaction - Rejected, rank: " + i + " out of " + K + "MC runs with alpha= " + alpha + " p-value: " + ((double) K - i) / K);
            }
            return true;
        }
        else {
            IJ.showMessage("Null hypothesis accepted, rank: " + i + " out of " + K + " MC runs with alpha= " + alpha + " p-value: " + ((double) K - i) / K);
            System.out.println("Null hypothesis: No interaction - Accepted, rank: " + i + " out of " + K + " MC runs with alpha= " + alpha + " p-value: " + ((double) K - i) / K);
            return false;
        }

    }

    private void calculateT() {
        DRand = new double[D.length];
        T = new double[K];

        for (int i = 0; i < K; i++) {
            T[i] = calculateTk();
        }
        Arrays.sort(T);
    }

    private double calculateTk() {
        generateRandomD();
        final PotentialCalculator pc = new PotentialCalculator(DRand, params, type);
        pc.calculateWOEpsilon();
        return -1 * pc.getSumPotential();
    }

    private void generateRandomD() {
        // not erasing contents of DRAND
        final Random rn = new Random(System.nanoTime());
        double R = 0;

        for (int i = 0; i < D.length;) {
            R = rn.nextDouble();
            if (R >= CDFGrid[0]) // to make sure that random value will be gte the least in cdf
            {
                DRand[i] = findD(R);
                i++;
            }
        }
    }

    private double findD(double R) {
        int i;
        for (i = 0; i < CDFGrid.length - 1; i++) {
            if (R >= CDFGrid[i] && R < CDFGrid[i + 1]) {
                break;

            }
        }
        return linearInterpolation(DGrid[i], CDFGrid[i], DGrid[i + 1], CDFGrid[i + 1], R);
    }
    
    private static double linearInterpolation(double yl, double xl, double yr, double xr, double x) {
        final double m = (yl - yr) / (xl - xr);
        final double c = yl - m * xl;
        return m * x + c;
    }
}
