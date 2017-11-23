package mosaic.ia;


import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.scijava.vecmath.Point3d;

import fr.inria.optimization.cmaes.CMAEvolutionStrategy;
import ij.IJ;
import ij.ImagePlus;
import mosaic.ia.HypothesisTesting.TestResult;
import mosaic.ia.Potentials.Potential;
import mosaic.ia.Potentials.PotentialType;
import mosaic.ia.gui.DistributionsPlot;
import mosaic.ia.gui.EstimatedPotentialPlot;
import mosaic.ia.gui.PlotHistogram;
import mosaic.utils.Debug;
import mosaic.utils.math.StatisticsUtils;
import mosaic.utils.math.StatisticsUtils.MinMaxMean;

public class Analysis {
    private static final Logger logger = Logger.getLogger(Analysis.class);
    
    private Potential iPotential;
    private DistanceCalculations iDistanceCalculations;
    private double[] iContextQdDistancesGrid;
    private double[] iContextQdPdf;
    private double[] iNearestNeighborDistancesXtoY;
    private double[] iNearestNeighborDistancesXtoYPdf;
    
    private double[][] iBestPointsFound;
    private int iBestPointIndex = -1;
    
    public void calcDist(double gridSize, double kernelWeightq, double kernelWeightp, float[][][] genMask, ImagePlus iImageX, ImagePlus iImageY) {
        iDistanceCalculations = new DistanceCalculationsImage(iImageX, iImageY, genMask, gridSize, kernelWeightq, kernelWeightp);
        calcDistributions(iDistanceCalculations);
    }
    
    public void calcDist(double gridSize, double kernelWeightq, double kernelWeightp, float[][][] genMask, Point3d[] particleXSetCoordUnfiltered, Point3d[] particleYSetCoordUnfiltered, double x1,double x2,double y1,double y2,double z1,double z2) {
        iDistanceCalculations = new DistanceCalculationsCoords(particleXSetCoordUnfiltered, particleYSetCoordUnfiltered, genMask, x1, y1, z1, x2, y2, z2, gridSize, kernelWeightq, kernelWeightp);
        calcDistributions(iDistanceCalculations);
    }
    
    private void calcDistributions(DistanceCalculations dci) {
        iContextQdPdf = dci.getContextQdPdf();
        iContextQdDistancesGrid = dci.getContextQdDistancesGrid();
        iNearestNeighborDistancesXtoY = dci.getNearestNeighborsDistancesXtoY();
        iNearestNeighborDistancesXtoYPdf = dci.getNearestNeighborsDistancesXtoYPdf();
        
        StatisticsUtils.normalizePdf(iContextQdPdf, iContextQdDistancesGrid, false);
        StatisticsUtils.normalizePdf(iNearestNeighborDistancesXtoYPdf, iContextQdDistancesGrid, false);

        new DistributionsPlot(iContextQdDistancesGrid, iContextQdPdf, iNearestNeighborDistancesXtoYPdf).show();
        PlotHistogram.plot("ObservedDistances", iNearestNeighborDistancesXtoY, getOptimBins(iNearestNeighborDistancesXtoY, 8, iNearestNeighborDistancesXtoY.length / 8));
        double suggestedKernel = calcWekaWeights(iNearestNeighborDistancesXtoY);
        IJ.showMessage("Suggested Kernel wt(p): " + suggestedKernel);
        logger.debug("Suggested kernel wt(p)=" + suggestedKernel);
    }

    public static class CmaResult {
        public final double iStrength;
        public final double iThresholdScale;
        public final double iResidual;
        
        public CmaResult (double aStrength, double aThresholdScale, double aResidual) {
            iStrength = aStrength;
            iThresholdScale = aThresholdScale;
            iResidual = aResidual;
        }
        
        @Override
        public String toString() { 
            return "[strength=" + iStrength + ", threshold/scale=" + iThresholdScale + ", residual=" + iResidual + "]";
        }
    }
    
    public void cmaOptimization(List<CmaResult> aResultsOutput, int cmaReRunTimes, boolean aRepetitiveResults) {
        final FitFunction fitfun = new FitFunction(iContextQdPdf, iContextQdDistancesGrid, iNearestNeighborDistancesXtoYPdf, iNearestNeighborDistancesXtoY, iPotential);
        iBestPointsFound = new double[cmaReRunTimes][iPotential.numOfDimensions()];
        double[] bestFunctionValue = new double[cmaReRunTimes];
        double bestFitness = Double.MAX_VALUE;
        boolean diffFitness = false;
        
        for (int cmaRunNumber = 0; cmaRunNumber < cmaReRunTimes; cmaRunNumber++) {
            final CMAEvolutionStrategy cma = createNewConfiguredCma(aRepetitiveResults);
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
            logCmaResultInfo(cma);

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

        logger.debug("Best Parameters Found:" + Debug.getString(iBestPointsFound[iBestPointIndex]) + " fit function value=" + bestFunctionValue[iBestPointIndex]);
        
        if (diffFitness) {
            IJ.showMessage("Warning: Optimization returned different results for reruns. The results may not be accurate. Displaying the parameters and the plots corr. to best fitness.");
        }
        fitfun.l2Norm(iBestPointsFound[iBestPointIndex]); // to calc pgrid for best params
        new EstimatedPotentialPlot(iContextQdDistancesGrid, iPotential, iBestPointsFound[iBestPointIndex], bestFunctionValue[iBestPointIndex]).show();
        double[] observedModelFitPdPdf = fitfun.getObservedModelFitPdPdf() ;
        StatisticsUtils.normalizePdf(observedModelFitPdPdf, iContextQdDistancesGrid, false);
        new DistributionsPlot(iContextQdDistancesGrid, observedModelFitPdPdf, iContextQdPdf, iNearestNeighborDistancesXtoYPdf, iPotential, iBestPointsFound[iBestPointIndex], bestFunctionValue[iBestPointIndex]).show();
    }

    private void addNewOutputResult(List<CmaResult> aResultsOutput, double aBestFunctionValue, double[] aBestPointFound) {
        double strength = 0;
        double thresholdOrScale = 0;
        if (iPotential.getType() != PotentialType.NONPARAM) {
            strength = aBestPointFound[0];
            thresholdOrScale = aBestPointFound[1];
        }
        aResultsOutput.add(new CmaResult(strength, thresholdOrScale, aBestFunctionValue));
    }

    private void printCurrentIterationInfo(final CMAEvolutionStrategy cma) {
        // Print every infoFilter'th message from CMA
        final int infoFilter = 150;
        // Print every 15 info annotation (header with column names)
        final int annotationFilter = 15 * infoFilter;

        if (cma.getCountIter() % annotationFilter == 1) {
            logger.debug(cma.getPrintAnnotation());
        }
        if (cma.getCountIter() % infoFilter == 1) {
            logger.debug(cma.getPrintLine());
        }
    }

    private CMAEvolutionStrategy createNewConfiguredCma(boolean aRepetitiveResults) {
        final CMAEvolutionStrategy cma = new CMAEvolutionStrategy();
        if (aRepetitiveResults) {
            cma.setSeed(1);
            cma.setRand(new Random(1));
        }
        cma.options.writeDisplayToFile = 0;
        cma.options.stopFitness = 1e-12; // optional setting
        cma.options.stopTolFun = 1e-15;
        cma.options.verbosity = -1; // disable hello msg from CMA-ES
        cma.setDimension(iPotential.numOfDimensions());
        
        final double[] initialX = new double[iPotential.numOfDimensions()];
        final double[] initialSigma = new double[iPotential.numOfDimensions()];
        final Random rn = aRepetitiveResults ? new Random(123456) : new Random();
        if (iPotential.getType() == PotentialType.NONPARAM) {
            for (int i = 0; i < iPotential.numOfDimensions(); i++) {
                initialX[i] = iDistanceCalculations.getMeanXtoYdistance() * rn.nextDouble();
                initialSigma[i] = initialX[i] / 3;
            }
        }
        else {
            // For other than NOPARAM potentials dimensions are always = 2
            initialX[0] = rn.nextDouble() * 5; // epsilon. average strength of 5
            if (iDistanceCalculations.getMeanXtoYdistance() != 0) {
                initialX[1] = rn.nextDouble() * iDistanceCalculations.getMeanXtoYdistance();
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

    private void logCmaResultInfo(final CMAEvolutionStrategy cma) {
        logger.debug(cma.getPrintLine());
        logger.debug("Terminated due to:");
        for (final String s : cma.stopConditions.getMessages()) {
            logger.debug("    " + s);
        }
        logger.debug("Best function value " + cma.getBestFunctionValue() + " at evaluation " + cma.getBestEvaluationNumber());
    }
 
    
    public TestResult hypothesisTesting(int monteCarloRunsForTest, double alpha) {
        if (iBestPointsFound == null) {
            IJ.showMessage("Error: Run estimation first");
            return null;
        }
        else if (iPotential.getType() == PotentialType.NONPARAM) {
            IJ.showMessage("Hypothesis test is not applicable for Non Parametric potential \n since it does not have 'strength' parameter");
            return null;
        }
        else {
            logger.debug("Running hypothesis testing with #runs=" + monteCarloRunsForTest + " alpha=" + alpha + " potential parameters=" + Debug.getString(iBestPointsFound[iBestPointIndex]));
            
            final HypothesisTesting ht = new HypothesisTesting(StatisticsUtils.calculateCdfFromPdf(iContextQdPdf, iContextQdDistancesGrid), 
                                                               iContextQdDistancesGrid, 
                                                               iNearestNeighborDistancesXtoY, 
                                                               iBestPointsFound[iBestPointIndex], 
                                                               iPotential, 
                                                               monteCarloRunsForTest, alpha);
            TestResult rankTest = ht.rankTest();
            logger.debug("Hypothesis testing result: [" + rankTest + "]");
            return rankTest;
        }
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
        final double silBandwidth = 0.9 * Math.min(StatisticsUtils.calcStandardDev(distances), (q3 - q1) / 1.34) * Math.pow(distances.length, -0.2);
        logger.debug("Silverman's bandwidth: " + silBandwidth);

        return silBandwidth;
    }
    
    public static double calcWekaWeights(double[] distances) {
        MinMaxMean mmm = StatisticsUtils.getMinMaxMean(distances);
        final double range = mmm.max - mmm.min;
        final double bw = calcSilvermanBandwidth(distances);
        return ((1.0d / distances.length) * (range / bw) * (range / bw));
    }
    
    public double getMinDistance() {
        return iDistanceCalculations.getMinXtoYdistance();
    }

    public double getMaxDistance() {
        return iDistanceCalculations.getMaxXtoYdistance();
    }

    public double[] getDistances() {
        return iNearestNeighborDistancesXtoY;
    }
    
    public void setPotentialType(Potential potentialType) {
        this.iPotential = potentialType;
    }
}
