package mosaic.ia;


import java.util.List;
import java.util.Random;

import javax.vecmath.Point3d;

import fr.inria.optimization.cmaes.CMAEvolutionStrategy;
import ij.IJ;
import ij.ImagePlus;
import mosaic.ia.Potentials.Potential;
import mosaic.ia.Potentials.PotentialType;
import mosaic.ia.gui.DistributionsPlot;
import mosaic.ia.gui.EstimatedPotentialPlot;
import mosaic.ia.gui.PlotHistogram;
import mosaic.utils.math.StatisticsUtils;
import mosaic.utils.math.StatisticsUtils.MinMaxMean;
import weka.estimators.KernelEstimator;

public class Analysis {
    private static final int GridDensity = 1000;
    
    private Potential iPotential;

    private double[] iDistancesGrid;
    private double[] iDistancePdf;
    private double[] iNearestNeighborDistances;
    private double[] iNearestNeighborDistancePdf;
    
    private double[][] iBestPointsFound;
    private int iBestPointIndex = -1;
    
    private double minDistance, maxDistance, meanDistance;


    public void calcDist(double gridSize, double kernelWeightq, double kernelWeightp, float[][][] genMask, ImagePlus iImageX, ImagePlus iImageY) {
        DistanceCalculations dci = new DistanceCalculationsImage(iImageX, iImageY, genMask, gridSize, kernelWeightq, GridDensity);
        calcDistributions(dci, kernelWeightp);
    }
    
    public void calcDist(double gridSize, double kernelWeightq, double kernelWeightp, float[][][] genMask, Point3d[] particleXSetCoordUnfiltered, Point3d[] particleYSetCoordUnfiltered, double x1,double x2,double y1,double y2,double z1,double z2) {
        DistanceCalculations dci = new DistanceCalculationsCoords(particleXSetCoordUnfiltered, particleYSetCoordUnfiltered, genMask, x1, y1, z1, x2, y2, z2, gridSize, kernelWeightq, GridDensity);
        calcDistributions(dci, kernelWeightp);
    }
    
    private void calcDistributions(DistanceCalculations dci, double kernelWeightp) {
        iNearestNeighborDistances = dci.getNearestNeighborsDistances();
        iDistancesGrid = dci.getDistancesGrid();
        // TODO: It is not sure if it should be normalized here or later in when calculating CMA. 
        //       In original code there is misleading info, it says that it should be *NOT* normalized but
        //       at the same time output from old DistanceCalculations is already normalized...)
        iDistancePdf = StatisticsUtils.normalizePdf(dci.getProbabilityOfDistancesDistribution(), false);
        KernelEstimator kernelEstimatorNN = createkernelDensityEstimator(iNearestNeighborDistances, kernelWeightp);
        iNearestNeighborDistancePdf = new double[iDistancesGrid.length];
        for (int i = 0; i < iDistancesGrid.length; i++) {
            iNearestNeighborDistancePdf[i] = kernelEstimatorNN.getProbability(iDistancesGrid[i]);
        }
        StatisticsUtils.normalizePdf(iNearestNeighborDistancePdf, false);
        
        MinMaxMean mmm = StatisticsUtils.getMinMaxMean(iNearestNeighborDistances);
        minDistance = mmm.min;
        maxDistance = mmm.max;
        meanDistance = mmm.mean;
        System.out.println("Min/Max/Mean distance of X to Y: " + mmm.min + " / " + mmm.max + " / " + mmm.mean);
        
        new DistributionsPlot(iDistancesGrid, iDistancePdf, iNearestNeighborDistancePdf).show();
        PlotHistogram.plot("ObservedDistances", iNearestNeighborDistances, getOptimBins(iNearestNeighborDistances, 8, iNearestNeighborDistances.length / 8));
        IJ.showMessage("Suggested Kernel wt(p): " + calcWekaWeights(iNearestNeighborDistances));
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
    
    public void cmaOptimization(List<Result> aResultsOutput, int cmaReRunTimes) {
        final FitFunction fitfun = new FitFunction(iDistancesGrid, iDistancePdf, iNearestNeighborDistances, iPotential, iNearestNeighborDistancePdf);
        if (iPotential.getType() == PotentialType.NONPARAM) {
            iBestPointsFound = new double[cmaReRunTimes][iPotential.numOfDimensions()];
        }
        else {
            iBestPointsFound = new double[cmaReRunTimes][iPotential.numOfDimensions()];
        }
        double[] bestFunctionValue = new double[cmaReRunTimes];
        double bestFitness = Double.MAX_VALUE;
        boolean diffFitness = false;
        
        for (int cmaRunNumber = 0; cmaRunNumber < cmaReRunTimes; cmaRunNumber++) {
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

            bestFunctionValue[cmaRunNumber] = cma.getBestFunctionValue();
            if (bestFunctionValue[cmaRunNumber] < bestFitness) {
                if (cmaRunNumber > 0 && bestFitness - bestFunctionValue[cmaRunNumber] > bestFunctionValue[cmaRunNumber] * 0.00001) {
                    diffFitness = true;
                }
                bestFitness = bestFunctionValue[cmaRunNumber];
                iBestPointIndex = cmaRunNumber;
            }
            iBestPointsFound[cmaRunNumber] = cma.getBestX();
            
            addNewOutputResult(aResultsOutput, bestFunctionValue[cmaRunNumber], iBestPointsFound[cmaRunNumber]);
        }

        if (diffFitness) {
            IJ.showMessage("Warning: Optimization returned different results for reruns. The results may not be accurate. Displaying the parameters and the plots corr. to best fitness.");
        }

        fitfun.l2Norm(iBestPointsFound[iBestPointIndex]); // to calc pgrid for best params
        new EstimatedPotentialPlot(iDistancesGrid, iPotential, iBestPointsFound[iBestPointIndex], bestFunctionValue[iBestPointIndex]).show();
        double[] P_grid = StatisticsUtils.normalizePdf(fitfun.getObservedNearestNeighborDistancesPdf(), false);
        new DistributionsPlot(iDistancesGrid, P_grid, iDistancePdf, iNearestNeighborDistancePdf, iPotential, iBestPointsFound[iBestPointIndex], bestFunctionValue[iBestPointIndex]).show();
    }

    private void addNewOutputResult(List<Result> aResultsOutput, double aBestFunctionValue, double[] aBestPointFound) {
        double strength = 0;
        double thresholdOrScale = 0;
        if (iPotential.getType() != PotentialType.NONPARAM) {
            strength = aBestPointFound[0];
            thresholdOrScale = aBestPointFound[1];
        }
        double residual = aBestFunctionValue;
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
        cma.setDimension(iPotential.numOfDimensions());
        
        final double[] initialX = new double[iPotential.numOfDimensions()];
        final double[] initialSigma = new double[iPotential.numOfDimensions()];
        final Random rn = new Random(System.nanoTime());
        if (iPotential.getType() == PotentialType.NONPARAM) {
            for (int i = 0; i < iPotential.numOfDimensions(); i++) {
                initialX[i] = meanDistance * rn.nextDouble();
                initialSigma[i] = initialX[i] / 3;
            }
        }
        else {
            // For other than NOPARAM potentials dimensions are always = 2
            initialX[0] = rn.nextDouble() * 5; // epsilon. average strength of 5
            if (meanDistance != 0) {
                initialX[1] = rn.nextDouble() * meanDistance;
                initialSigma[1] = initialX[1] / 3;
            }
            else {
                initialX[1] = 0;
                initialSigma[1] = 1E-3;
            }
            initialSigma[0] = initialX[0] / 3;
        }
        
        cma.setTypicalX(initialX);
        cma.setInitialStandardDeviations(initialSigma);
        
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
 
    public void hypothesisTesting(int monteCarloRunsForTest, double alpha) {
        if (iBestPointsFound == null) {
            IJ.showMessage("Error: Run estimation first");
        }
        else if (iPotential.getType() == PotentialType.NONPARAM) {
            IJ.showMessage("Hypothesis test is not applicable for Non Parametric potential \n since it does not have 'strength' parameter");
        }
        else {
            System.out.println("Running test with " + monteCarloRunsForTest + " and " + alpha);
            final HypothesisTesting ht = new HypothesisTesting(StatisticsUtils.calculateCdf(iDistancePdf, true), 
                                                               iDistancesGrid, 
                                                               iNearestNeighborDistances, 
                                                               iBestPointsFound[iBestPointIndex], 
                                                               iPotential, 
                                                               monteCarloRunsForTest, alpha);
            ht.rankTest();
        }
    }

    public static KernelEstimator createkernelDensityEstimator(double[] distances, double weight) {
        final KernelEstimator kernel = new KernelEstimator(0.01);
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
    
    public double getMinDistance() {
        return minDistance;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public double[] getDistances() {
        return iNearestNeighborDistances;
    }
    
    public void setPotentialType(Potential potentialType) {
        this.iPotential = potentialType;
    }
}
