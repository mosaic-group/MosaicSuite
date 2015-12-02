package mosaic.ia;


import fr.inria.optimization.cmaes.fitness.AbstractObjectiveFunction;
import mosaic.ia.utils.StatisticsUtils;


class CMAMosaicObjectiveFunction extends AbstractObjectiveFunction {
    private final double[] D; // measured NN
    private final double[] D_grid; // at which q is evalueated and p will be sampled
    private double[] P_grid;
    private final int potentialType;
    private final double[] qofD_grid, observedDGrid;
    private int[][] interpInterval;

    public CMAMosaicObjectiveFunction(double[] D_grid, double[] qofD_grid, double[] d, int potentialType, double[] observedDGrid) {
        super();
        this.D_grid = D_grid;
        this.D = d; // data
        this.qofD_grid = qofD_grid;
        this.observedDGrid = observedDGrid;
        this.potentialType = potentialType;
        findInterpInterval(); // position of data d in dgrid
        updateMacheps();
    }

    public double[] getD_grid() {
        return D_grid;
    }
    
    public double[] getPGrid() {
        return P_grid;
    }

    private double[] getGibbsPotential(double[] params) {
        PotentialCalculator pc = null;
        pc = new PotentialCalculator(D_grid, params, potentialType);
        pc.calculate();
        return pc.getGibbsPotential();
    }

    public double[] getPotential(double[] params) {
        PotentialCalculator pc = null;
        pc = new PotentialCalculator(D_grid, params, potentialType);
        pc.calculate();
        return pc.getPotential();
    }

    public double l2Norm(double[] params) {
        double[] gibbspotential = getGibbsPotential(params);
        final double Z = calculateZ(gibbspotential);
        P_grid = new double[D_grid.length];
        double sumPGrid = 0;
        double l2Norm = 0;
        final double[] DiffD = new double[D_grid.length - 1];
        for (int i = 0; i < D_grid.length - 1; i++) {
            DiffD[i] = D_grid[i + 1] - D_grid[i];
        }
        for (int i = 0; i < D_grid.length; i++) {
            P_grid[i] = gibbspotential[i] * qofD_grid[i] * 1 / Z;
            sumPGrid = sumPGrid + P_grid[i];// why hundred times smq
            l2Norm = l2Norm + (P_grid[i] - observedDGrid[i]) * (P_grid[i] - observedDGrid[i]);
        }
        return l2Norm;
    }

    private double nonParamPenalty(double[] weights, double s) {
        double sum = 0;
        double diff = 0;
        for (int i = 0; i < weights.length - 1; i++) {
            diff = (weights[i] - weights[i + 1]);
            sum = sum + diff * diff;
        }
        return sum * s * s;
    }

    private static double MACHEPS = 2E-16;

    // to update machine epsilon
    private static void updateMacheps() {
        MACHEPS = 1.0d;
        do {
            MACHEPS /= 2.0d;
        } while (1 + MACHEPS / 2 != 1);
        System.out.println("Machine epsilon: " + MACHEPS);
    }
    
    @Override
    public boolean isFeasible(double[] x) {
        // if non param, return true.
        // if param: epsilon >=0 & epsilon <=20 & scale/threshold>min * < max

        if (potentialType == PotentialFunctions.NONPARAM) {
            return true;
        }
        else {
            final double[] minmaxmeanDg = StatisticsUtils.getMinMaxMeanD(D_grid);
            final double[] minmaxmeanD = StatisticsUtils.getMinMaxMeanD(D);

            if (x[0] >= MACHEPS && x[0] <= 50 && x[1] >= Math.max(Math.min(minmaxmeanDg[0], minmaxmeanD[0]), MACHEPS) && x[1] <= Math.max(minmaxmeanDg[1], minmaxmeanD[1])) {
                // 50 is aribtrary. but log(Double.MAXVAL)= log((2-(2^-52))*(2^1023))= 709.7827
                return true;
            }
            else {
                return false;
            }
        }
    }

    private void findInterpInterval() {   
        // this can be optimized. search can be done better.
        interpInterval = new int[2][D.length]; // will have xl and xr for each
        for (int i = 0; i < D.length; i++) {
            for (int j = 0; j < D_grid.length - 1; j++) {
                if (D[i] >= D_grid[j] && D[i] < D_grid[j + 1]) {
                    interpInterval[0][i] = j;
                    interpInterval[1][i] = j + 1;
                    break;
                }
            }

        }
    }

    private double calculateZ(double[] gibbspotential) {
        // using trapizoidal rule.

        final double[] DiffD = new double[D_grid.length - 1];
        for (int i = 0; i < D_grid.length - 1; i++) {
            DiffD[i] = D_grid[i + 1] - D_grid[i];
        }

        final double[] support = new double[D_grid.length];
        double sumSupport = 0;
        for (int i = 0; i < D_grid.length; i++) {
            support[i] = gibbspotential[i] * this.qofD_grid[i];
            sumSupport = sumSupport + support[i];
        }
        final double[] integrand = new double[D_grid.length - 1];
        double Z = 0;
        for (int i = 0; i < D_grid.length - 1; i++) {
            integrand[i] = (support[i] + support[i + 1]) / 2;
            Z = Z + integrand[i];
        }
        Z = Z + support[0] / 2 + support[D_grid.length - 1] / 2;

        return Z;
    }

    @Override
    public double valueOf(double[] x) {
        if (potentialType == PotentialFunctions.NONPARAM) {
            final double[] weights = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE];
            for (int i = 0; i < PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1; i++) {
                weights[i] = x[i];
            }
            weights[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1] = 0;
            return l2Norm(x) + nonParamPenalty(weights, PotentialFunctions.NONPARAM_SMOOTHNESS);
        }
        else {
            return l2Norm(x);
        }
    }
}
