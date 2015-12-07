package mosaic.ia;


import java.util.List;
import java.util.Random;

import javax.vecmath.Point3d;

import fr.inria.optimization.cmaes.CMAEvolutionStrategy;
import ij.IJ;
import ij.ImagePlus;
import mosaic.ia.gui.DistributionsPlot;
import mosaic.ia.gui.EstimatedPotentialPlot;
import mosaic.ia.gui.PlotHistogram;
import mosaic.utils.math.StatisticsUtils;
import mosaic.utils.math.StatisticsUtils.MinMaxMean;
import weka.estimators.KernelEstimator;


public class Analysis {
    private static final int GridDensity = 1000;
    
    private int potentialType;

    private double[] iDistancesDistribution;
    private double[] iProbabilityOfDistanceDistribution;
    private double[] iNearestNeighborDistances;
    private double[] iNearestNeighborDistanceDistribution;
    
    private double[][] bestPointFound;
    private int bestIndex = 0;
    
    private double minDistance, maxDistance, meanDistance;


    public boolean calcDist(double gridSize, double kernelWeightq, double kernelWeightp, float[][][] genMask, ImagePlus iImageX, ImagePlus iImageY) {
        DistanceCalculations dci;
        dci = new DistanceCalculationsImage(iImageX, iImageY, genMask, gridSize, kernelWeightq, GridDensity);
        return calcDistributions(dci, kernelWeightp);
    }
    
    public boolean calcDist(double gridSize, double kernelWeightq, double kernelWeightp, float[][][] genMask, Point3d[] particleXSetCoordUnfiltered, Point3d[] particleYSetCoordUnfiltered, double x1,double x2,double y1,double y2,double z1,double z2) {
        DistanceCalculations dci;
        dci = new DistanceCalculationsCoords(particleXSetCoordUnfiltered, particleYSetCoordUnfiltered, genMask, x1, y1, z1, x2, y2, z2, gridSize, kernelWeightq, GridDensity);
        return calcDistributions(dci, kernelWeightp);
    }
    
    private boolean calcDistributions(DistanceCalculations dci, double kernelWeightp) {
        iNearestNeighborDistances = dci.getNearestNeighborsDistances();
        iDistancesDistribution = dci.getDistancesDistribution();
        // TODO: It is not sure if it should be normalized here or later in when calculating CMA. 
        //       In original code there is misleading info, it says that it should be *NOT* normalized but
        //       at the same time output from old DistanceCalculations is already normalized...)
        iProbabilityOfDistanceDistribution = StatisticsUtils.normalizePdf(dci.getProbabilityOfDistancesDistribution(), false);
        KernelEstimator kernelEstimatorNN = createkernelDensityEstimator(iNearestNeighborDistances, kernelWeightp);
        iNearestNeighborDistanceDistribution = new double[iDistancesDistribution.length];
        for (int i = 0; i < iDistancesDistribution.length; i++) {
            iNearestNeighborDistanceDistribution[i] = kernelEstimatorNN.getProbability(iDistancesDistribution[i]);
        }
        StatisticsUtils.normalizePdf(iNearestNeighborDistanceDistribution, false);
        
        MinMaxMean mmm = StatisticsUtils.getMinMaxMean(iNearestNeighborDistances);
        minDistance = mmm.min;
        maxDistance = mmm.max;
        meanDistance = mmm.mean;
        System.out.println("Min/Max/Mean distance of X to Y: " + mmm.min + " / " + mmm.max + " / " + mmm.mean);
        
        new DistributionsPlot(iDistancesDistribution, iProbabilityOfDistanceDistribution, iNearestNeighborDistanceDistribution).show();
        PlotHistogram.plot("ObservedDistances", iNearestNeighborDistances, getOptimBins(iNearestNeighborDistances, 8, iNearestNeighborDistances.length / 8));
        IJ.showMessage("Suggested Kernel wt(p): " + calcWekaWeights(iNearestNeighborDistances));
    
        return true;
    }

    public static class Result {
        public final double iStrength;
        public final double iThresholdScale;
        public final double iResidual;
        
        public Result (double aStrength, double aThresholdScale, double aResidual) {
            iStrength = aStrength;
            iThresholdScale = aThresholdScale;
            iResidual = aResidual;
        }
        
        @Override
        public String toString() { 
            return "[strength="+ iStrength + ", threshold/scale="+iThresholdScale+", residual="+iResidual+"]";
        }
    }
    
    public boolean cmaOptimization(List<Result> aResultsOutput, int cmaReRunTimes) {
        final CMAMosaicObjectiveFunction fitfun = new CMAMosaicObjectiveFunction(iDistancesDistribution, iProbabilityOfDistanceDistribution, iNearestNeighborDistances, potentialType, iNearestNeighborDistanceDistribution);
        if (potentialType == PotentialFunctions.NONPARAM) {
            PotentialFunctions.initializeNonParamWeights(minDistance, maxDistance);
            bestPointFound = new double[cmaReRunTimes][PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
        }
        else {
            bestPointFound = new double[cmaReRunTimes][2];
        }
        double[] bestFunctionValue = new double[cmaReRunTimes];
        double bestFitness = Double.MAX_VALUE;
        boolean diffFitness = false;
        
        for (int k = 0; k < cmaReRunTimes; k++) {
            final CMAEvolutionStrategy cma = createNewConfiguredCma();
            final double[] fitness = cma.init();

            while (cma.stopConditions.getNumber() == 0) {
                final double[][] populations = cma.samplePopulation(); // get a new population of solutions
                for (int i = 0; i < populations.length; ++i) { 
                    // for each candidate solution 'i' a simple way to handle constraints that define a convex feasible domain
                    // (like box constraints, i.e. variable boundaries) via "blind re-sampling" assumes that the feasible domain 
                    // is convex, the optimum is not located on (or very close to) the domain boundary,
                    while (!fitfun.isFeasible(populations[i])) {
                        populations[i] = cma.resampleSingle(i); // initialX is feasible
                    }
                    // and initialStandardDeviations are sufficiently small to
                    // prevent quasi-infinite looping here compute fitness/objective value
                    fitness[i] = fitfun.valueOf(populations[i]); // fitfun.valueOf() is to be minimized
                }
                cma.updateDistribution(fitness);
                printCurrentIterationInfo(cma);
            }
            
            // evaluate mean value as it is the best estimate for the optimum
            cma.setFitnessOfMeanX(fitfun.valueOf(cma.getMeanX()));
            printCmaResultInfo(cma);

            bestFunctionValue[k] = cma.getBestFunctionValue();
            if (bestFunctionValue[k] < bestFitness) {
                if (k > 0 && bestFitness - bestFunctionValue[k] > bestFunctionValue[k] * 0.00001) {
                    diffFitness = true;
                }
                bestFitness = bestFunctionValue[k];
                bestIndex = k;
            }
            bestPointFound[k] = cma.getBestX();
            
            addNewOutputResult(aResultsOutput, bestFunctionValue, k);
        }

        if (diffFitness) {
            IJ.showMessage("Warning: Optimization returned different results for reruns. The results may not be accurate. Displaying the parameters and the plots corr. to best fitness.");
        }

        fitfun.l2Norm(bestPointFound[bestIndex]); // to calc pgrid for best params
        new EstimatedPotentialPlot(iDistancesDistribution, fitfun.getPotential(bestPointFound[bestIndex]), potentialType, bestPointFound[bestIndex], bestFunctionValue[bestIndex]).show();
        double[] P_grid = StatisticsUtils.normalizePdf(fitfun.getPGrid(), false);
        new DistributionsPlot(iDistancesDistribution, P_grid, iProbabilityOfDistanceDistribution, iNearestNeighborDistanceDistribution, potentialType, bestPointFound[bestIndex], bestFunctionValue[bestIndex]).show();

        return true;
    }

    private void addNewOutputResult(List<Result> aResultsOutput, double[] allFitness, int k) {
        double strength = 0;
        double thresholdOrScale = 0;
        if (potentialType != PotentialFunctions.NONPARAM) {
            strength = bestPointFound[k][0];
            thresholdOrScale = bestPointFound[k][1];
        }
        double residual = allFitness[k];
        aResultsOutput.add(new Result(strength, thresholdOrScale, residual));
    }

    private void printCurrentIterationInfo(final CMAEvolutionStrategy cma) {
        // Print every infoFilter'th message from CMA
        final int infoFilter = 150;
        // Print every 15 info annotation (header with column names)
        final int annotationFilter = 15 * infoFilter;

        if (cma.getCountIter() % annotationFilter == 1) {
            cma.printlnAnnotation();
        }
        if (cma.getCountIter() % infoFilter == 1) {
            cma.println();
        }
    }

    private CMAEvolutionStrategy createNewConfiguredCma() {
        final CMAEvolutionStrategy cma = new CMAEvolutionStrategy();
        cma.options.writeDisplayToFile = 0;
        cma.readProperties(); // read options, see file CMAEvolutionStrategy.properties
        cma.options.stopFitness = 1e-12; // optional setting
        cma.options.stopTolFun = 1e-15;
        final Random rn = new Random(System.nanoTime());
        if (potentialType == PotentialFunctions.NONPARAM) {
            cma.setDimension(PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1);
            final double[] initialX = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
            final double[] initialsigma = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
            for (int i = 0; i < PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1; i++) {
                initialX[i] = meanDistance * rn.nextDouble();
                initialsigma[i] = initialX[i] / 3;
            }
            cma.setInitialX(initialX);
            cma.setInitialStandardDeviations(initialsigma);
        }
        else if (potentialType == PotentialFunctions.STEP) {
            cma.setDimension(2);
            final double[] initialX = new double[2];
            final double[] initialsigma = new double[2];
            initialX[0] = rn.nextDouble() * 5; // epsilon. average strength of 5

            if (meanDistance != 0) {
                initialX[1] = rn.nextDouble() * meanDistance;
                initialsigma[1] = initialX[1] / 3;
            }
            else {
                initialX[1] = 0;
                initialsigma[1] = 1E-3;
            }
            initialsigma[0] = initialX[0] / 3;
            cma.setTypicalX(initialX);
            cma.setInitialStandardDeviations(initialsigma);
        }
        else {
            cma.setDimension(2);
            final double[] initialX = new double[2];
            final double[] initialsigma = new double[2];
            initialX[0] = rn.nextDouble() * 5; // epsilon. average strength of 5

            if (meanDistance != 0) {
                initialX[1] = rn.nextDouble() * meanDistance;
                initialsigma[1] = initialX[1] / 3;
            }
            else {
                initialX[1] = 0;
                initialsigma[1] = 1E-3;
            }
            initialsigma[0] = initialX[0] / 3;
            cma.setTypicalX(initialX);
            cma.setInitialStandardDeviations(initialsigma);
        }
        return cma;
    }

    private void printCmaResultInfo(final CMAEvolutionStrategy cma) {
        cma.println();
        cma.println("Terminated due to");
        for (final String s : cma.stopConditions.getMessages()) {
            cma.println("  " + s);
        }
        cma.println("best function value " + cma.getBestFunctionValue() + " at evaluation " + cma.getBestEvaluationNumber());
    }
 
    public boolean hypothesisTesting(int monteCarloRunsForTest, double alpha) {
        if (bestPointFound == null) {
            IJ.showMessage("Error: Run estimation first");
            return false;
        }
        else if (potentialType == PotentialFunctions.NONPARAM) {
            IJ.showMessage("Hypothesis test is not applicable for Non Parametric potential \n since it does not have 'strength' parameter");
            return false;
        }
    
        System.out.println("Running test with " + monteCarloRunsForTest + " and " + alpha);
        final HypothesisTesting ht = new HypothesisTesting(StatisticsUtils.calculateCdf(iProbabilityOfDistanceDistribution, true), 
                                                           iDistancesDistribution, 
                                                           iNearestNeighborDistances, 
                                                           bestPointFound[bestIndex], 
                                                           potentialType, 
                                                           monteCarloRunsForTest, alpha);
        ht.rankTest();
        return true;
    }

    public static KernelEstimator createkernelDensityEstimator(double[] distances, double weight) {
        final KernelEstimator kernel = new KernelEstimator(0.01);
        // weight is important, since bandwidth is calculated with it:
        // http://stackoverflow.com/questions/3511012/how-ist-the-bandwith-calculated-in-weka-kernelestimator-class
        System.out.println("Weight:" + weight);
        for (double value : distances) {
            kernel.addValue(value, weight); 
        }

        return kernel;
    }
    
    /**
     * Return the optimal bin number for a histogram of the data given in array, sing the
     * Freedman and Diaconis rule (bin_space = 2*IQR/n^(1/3)).
     * Inspired from Fiji TMUtils.java
     */
    public static int getOptimBins(double[] values, int minBinNumber, int maxBinNumber) {
        final int size = values.length;
        final double q1 = StatisticsUtils.getPercentile(values, 0.25);
        final double q3 = StatisticsUtils.getPercentile(values, 0.75);
        final double interQRange = q3 - q1;
        final double binWidth = 2 * interQRange * Math.pow(size, -0.33);
        MinMaxMean range = StatisticsUtils.getMinMaxMean(values);

        int noBins = (int) ((range.max - range.min) / binWidth + 1);
        if (noBins > maxBinNumber) {
            noBins = maxBinNumber;
        }
        else if (noBins < minBinNumber) {
            noBins = minBinNumber;
        }
        return noBins;
    }
    
    private static double calcSilvermanBandwidth(double[] distances) {
        final double q1 = StatisticsUtils.getPercentile(distances, 0.25);
        final double q3 = StatisticsUtils.getPercentile(distances, 0.75);
        final double silBandwidth = 0.9 * Math.min(StatisticsUtils.calcStandardDev(distances), (q3 - q1) / 1.34) * Math.pow(distances.length, -.2);
        System.out.println("Silverman's bandwidth: " + silBandwidth);

        return silBandwidth;
    }
    
    public static double calcWekaWeights(double[] distances) {
        MinMaxMean mmm = StatisticsUtils.getMinMaxMean(distances);
        final double range = mmm.max - mmm.min;
        final double bw = calcSilvermanBandwidth(distances);
        return ((1.0d / distances.length) * (range / bw) * (range / bw));
    }
    
    public double getMinD() {
        return minDistance;
    }

    public double getMaxD() {
        return maxDistance;
    }

    public double[] getDistances() {
        return iNearestNeighborDistances;
    }
    
    public void setPotentialType(int potentialType) {
        this.potentialType = potentialType;
    }
}
