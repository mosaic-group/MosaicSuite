package mosaic.ia;


// 5 is nonparam etc; make a parameter for this so that the numbers can be uniform and global

import mosaic.ia.utils.IAPUtils;
import fr.inria.optimization.cmaes.fitness.AbstractObjectiveFunction;


class CMAMosaicObjectiveFunction extends AbstractObjectiveFunction {

    /**
     * @param threshold
     * @param sigma
     * @param epsilon
     * @param type
     * @return
     */
    private double[] D; // measured NN

    private double[] D_grid; // at which q is evalueated and p will be sampled

    private double[] P_grid;

    public double[] getD_grid() {
        return D_grid;
    }

    public double[] getD() {
        return D;
    }

    public double[] getPGrid() {
        return P_grid;
    }

    private int potentialType;

    private double[] qofD_grid, observedDGrid;

    private int[][] interpInterval;

    public CMAMosaicObjectiveFunction(double[] D_grid, double[] qofD_grid, double[] d, int potentialType, double[] observedDGrid) {
        super();
        this.D_grid = D_grid;
        this.D = d; // data
        this.qofD_grid = qofD_grid;
        this.observedDGrid = observedDGrid;
        this.potentialType = potentialType;
        findInterpInterval(); // position of data d in dgrid
        // System.out.println(D[100]+" "+D[101]+" "+D_grid[100]+" "+D_grid[101]);
        // PlotUtils.plotDoubleArray("Distance internval", D,interpInterval[0]);
    }

    private double[] getGibbsPotential(double[] params) {

        PotentialCalculator pc = null;
        pc = new PotentialCalculator(D_grid, params, potentialType);
        pc.calculate();
        // double [] gibbspotential=new double[D_grid.length];
        return pc.getGibbsPotential();

    }

    public double[] getPotential(double[] params) {

        PotentialCalculator pc = null;
        pc = new PotentialCalculator(D_grid, params, potentialType);
        pc.calculate();
        // double [] gibbspotential=new double[D_grid.length];
        return pc.getPotential();

    }

    public double l2Norm(double[] params) {

        double[] gibbspotential = new double[D_grid.length];

        gibbspotential = getGibbsPotential(params);
        // PlotUtils.plotDoubleArray("Gibbs", D_grid, gibbspotential);
        // double sumPotential=pc.getSumPotential();
        double Z = calculateZ(gibbspotential);
        // Z=Z*100000;
        P_grid = new double[D_grid.length];
        double sumPGrid = 0;
        double l2Norm = 0;
        double[] DiffD = new double[D_grid.length - 1];
        for (int i = 0; i < D_grid.length - 1; i++) {
            DiffD[i] = D_grid[i + 1] - D_grid[i];
        }
        for (int i = 0; i < D_grid.length; i++) {
            P_grid[i] = gibbspotential[i] * qofD_grid[i] * 1 / Z;
            sumPGrid = sumPGrid + P_grid[i];// why hundred times smq
            l2Norm = l2Norm + (P_grid[i] - observedDGrid[i]) * (P_grid[i] - observedDGrid[i]);
        }
        // System.out.println("sum p grid"+sumPGrid);

        // System.out.println("L2Norm:"+l2Norm);
        return l2Norm;
    }

    private double nonParamPenalty(double[] weights, double s) {
        double sum = 0;
        double diff = 0;
        for (int i = 0; i < weights.length - 1; i++) {
            diff = (weights[i] - weights[i + 1]);
            sum = sum + diff * diff;
        }
        // System.out.println(sum/(s*s));
        return sum * s * s;

    }

    @Override
    public boolean isFeasible(double[] x) {
        // if non param, return true.
        // if param: epsilon >=0 & epsilon <=20
        // & scale/threshold>min * < max

        if (potentialType == PotentialFunctions.NONPARAM) {
            return true;
        }
        else {
            double[] minmaxmeanDg = IAPUtils.getMinMaxMeanD(D_grid);
            double[] minmaxmeanD = IAPUtils.getMinMaxMeanD(D);
            // System.out.println(x[0]+" "+x[1]+" "+"min:"+minmaxmean[0]+"max:"+minmaxmean[1]);

            if (x[0] >= IAPUtils.MACHEPS && x[0] <= 50 && x[1] >= Math.max(Math.min(minmaxmeanDg[0], minmaxmeanD[0]), IAPUtils.MACHEPS) && x[1] <= Math.max(minmaxmeanDg[1], minmaxmeanD[1])) {
                // is
                // aribtrary.
                // but
                // log(Double.MAXVAL)=
                // log((2-(2^-52))*(2^1023))=
                // 709.7827
                return true;
            }
            else {
                return false;
            }
        }
    }

    private void findInterpInterval() // this can be optimized. search can be
    // done better.
    {
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

        double[] DiffD = new double[D_grid.length - 1];
        for (int i = 0; i < D_grid.length - 1; i++) {
            DiffD[i] = D_grid[i + 1] - D_grid[i];
        }

        double[] support = new double[D_grid.length];
        double sumSupport = 0;
        for (int i = 0; i < D_grid.length; i++) {
            support[i] = gibbspotential[i] * this.qofD_grid[i];
            sumSupport = sumSupport + support[i];
        }
        /*
         * System.out.println("Support 0:"+support[0]);
         * System.out.println("sum Support:"+sumSupport);
         */
        double[] integrand = new double[D_grid.length - 1];
        double Z = 0;
        for (int i = 0; i < D_grid.length - 1; i++) {
            integrand[i] = (support[i] + support[i + 1]) / 2;
            // Z=Z+integrand[i]*DiffD[i];
            Z = Z + integrand[i];
        }
        // Z=Z+integrand[0]*DiffD[0]/2+integrand[D_grid.length-2]*DiffD[D_grid.length-2]/2;
        // Z=Z+support[0]*DiffD[0]/2+support[D_grid.length-1]*DiffD[D_grid.length-2]/2;
        Z = Z + support[0] / 2 + support[D_grid.length - 1] / 2;

        /*
         * System.out.println("Z: "+Z);
         * System.out.println("DiffD[0]"+DiffD[0]);
         * System.out.println("Q(0)"+qofD_grid[0]);
         */
        return Z;
    }

    @Override
    public double valueOf(double[] x) {

        // take mod;

        /*
         * System.out.print("Current evaluation params:");
         * for (int i=0;i<x.length;i++)
         * {
         * System.out.print(x[i]+",");
         * }
         */
        if (potentialType == PotentialFunctions.NONPARAM) {
            double[] weights = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE];
            for (int i = 0; i < PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1; i++) {
                weights[i] = x[i];
            }
            weights[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1] = 0;
            // return loglikelhihoodSummed(weights)+nonParamPenalty(weights,
            // PotentialFunctions.NONPARAM_SMOOTHNESS);
            return l2Norm(x) + nonParamPenalty(weights, PotentialFunctions.NONPARAM_SMOOTHNESS);
        }
        else {
            // return loglikelhihoodSummed(x);
            // return l1Norm(x);
            return l2Norm(x);
        }
    }

}
