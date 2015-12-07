package mosaic.ia;


import fr.inria.optimization.cmaes.fitness.AbstractObjectiveFunction;
import mosaic.utils.math.StatisticsUtils;
import mosaic.utils.math.StatisticsUtils.MinMaxMean;


class CMAMosaicObjectiveFunction extends AbstractObjectiveFunction {
    private final double[] iDistances; // measured NN
    private final double[] D_grid; // at which q is evalueated and p will be sampled
    private double[] P_grid;
    private final int potentialType;
    private final double[] qofD_grid, observedDGrid;

    public CMAMosaicObjectiveFunction(double[] D_grid, double[] qofD_grid, double[] d, int potentialType, double[] observedDGrid) {
        this.D_grid = D_grid;
        this.iDistances = d; // data
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

    @Override
    public boolean isFeasible(double[] x) {
        // if non param, return true.
        // if param: epsilon >=0 & epsilon <=20 & scale/threshold>min * < max
        
        if (potentialType == PotentialFunctions.NONPARAM) {
            return true;
        }
        else {
            MinMaxMean minmaxmeanDg = StatisticsUtils.getMinMaxMean(D_grid);
            MinMaxMean minmaxmeanD = StatisticsUtils.getMinMaxMean(iDistances);
            
            if (x[0] >= MACHINE_EPSILON && x[0] <= 50 && 
                x[1] >= Math.max(Math.min(minmaxmeanDg.min, minmaxmeanD.min), MACHINE_EPSILON) && x[1] <= Math.max(minmaxmeanDg.max, minmaxmeanD.max)) {
                // 50 is aribtrary. but log(Double.MAXVAL)= log((2-(2^-52))*(2^1023))= 709.7827
                return true;
            }
            else {
                return false;
            }
        }
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
    
    public double[] getPotential(double[] params) {
        PotentialCalculator pc = new PotentialCalculator(D_grid, params, potentialType);
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
    
    private double[] getGibbsPotential(double[] params) {
        PotentialCalculator pc = new PotentialCalculator(D_grid, params, potentialType);
        pc.calculate();
        return pc.getGibbsPotential();
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

    private static double MACHINE_EPSILON = 2E-16;
    private static void updateMacheps() {
        MACHINE_EPSILON = 1.0d;
        do {
            MACHINE_EPSILON /= 2.0d;
        } while (1 + MACHINE_EPSILON / 2 != 1);
        System.out.println("Machine epsilon: " + MACHINE_EPSILON);
    }
    
    
    private void findInterpInterval() {   
        // this can be optimized. search can be done better.
        int[][] interpInterval = new int[2][iDistances.length]; // will have xl and xr for each
        for (int i = 0; i < iDistances.length; i++) {
            for (int j = 0; j < D_grid.length - 1; j++) {
                if (iDistances[i] >= D_grid[j] && iDistances[i] < D_grid[j + 1]) {
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
}
