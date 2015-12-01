package mosaic.ia;


import java.awt.Color;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

import javax.vecmath.Point3d;

import fr.inria.optimization.cmaes.CMAEvolutionStrategy;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import mosaic.ia.gui.PlotHistogram;
import mosaic.ia.gui.PlotQP;
import mosaic.ia.gui.PlotQPNN;
import mosaic.ia.nn.DistanceCalculations;
import mosaic.ia.nn.DistanceCalculationsCoords;
import mosaic.ia.nn.DistanceCalculationsImage;
import mosaic.ia.utils.IAPUtils;
import mosaic.ia.utils.ImageProcessUtils;
import weka.estimators.KernelEstimator;


public class Analysis {
    private final ImagePlus iImageX, iImageY;
    private ImagePlus mask, genMask;
    private final Point3d[] particleXSetCoordUnfiltered;
    private final Point3d[] particleYSetCoordUnfiltered;

    private final int dgrid_size = 1000;
    private double[] q_D_grid, NN_D_grid;
    private double x1, y1, x2, y2, z1, z2;
    private int cmaReRunTimes;
    private boolean showAllRerunResults = false;
    private int bestFitnessindex = 0;

    private double[] iDistances; // Nearest neighbour;
    private int potentialType; // 1 is step, 2 is hernquist, 5 is nonparam
    private double[][] best;
    private double[] allFitness;
    private double[] q, nnObserved, dgrid;

    private double minD, maxD, meanD;
    private final boolean isImage; // to distinguish b/wimage and coords

    public Analysis(ImagePlus X, ImagePlus Y) {
        this.iImageX = X;
        this.iImageY = Y;
        isImage = true;
        this.particleXSetCoordUnfiltered = null;
        this.particleYSetCoordUnfiltered = null;
    }

    public Analysis(Point3d[] Xcoords, Point3d[] Ycoords, 
            double x1,double x2,double y1,double y2,double z1,double z2) {
        this.particleXSetCoordUnfiltered = Xcoords;
        this.particleYSetCoordUnfiltered = Ycoords;
        this.iImageX = null;
        this.iImageY = null;
        isImage = false;
        
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.z1 = z1;
        this.z2 = z2;
    }

    public boolean calcDist(double gridSize, double kernelWeightq, double kernelWeightp) {
        DistanceCalculations dci;
        if (isImage == true) {
            dci = new DistanceCalculationsImage(iImageX, iImageY, mask, gridSize, kernelWeightq, dgrid_size);
            dci.calcDistances();
        }
        else {
            dci = new DistanceCalculationsCoords(particleXSetCoordUnfiltered, particleYSetCoordUnfiltered, genMask, x1, y1, z1, x2, y2, z2, gridSize, kernelWeightq, dgrid_size);
            dci.calcDistances();
        }
        iDistances = dci.getDistances();
    
        dgrid = dci.getqOfD()[0];
        q = dci.getqOfD()[1];
        q_D_grid = q; // q_D_grid is supposed to be not normalized - will be normalized again in CMAObj... but its OK.
        System.out.println("p set to:" + kernelWeightp);
        // System.out.println("Weka weight:"+IAPUtils.calcWekaWeights(D));
        KernelEstimator kernelEstimatorNN = IAPUtils.createkernelDensityEstimator(iDistances, kernelWeightp);
    
        // generateKernelDensityforD
        NN_D_grid = new double[dgrid.length];
        NN_D_grid[0] = kernelEstimatorNN.getProbability(dgrid[0]);
        for (int i1 = 1; i1 < dgrid.length; i1++) {
            NN_D_grid[i1] = kernelEstimatorNN.getProbability(dgrid[i1]);
        }
        nnObserved = IAPUtils.normalize(NN_D_grid);
        String xlabel = "Distance";
        if (isImage) {
            xlabel = xlabel + " (" + iImageX.getCalibration().getUnit() + ")";
        }
        PlotQP.plot(xlabel, dgrid, q, nnObserved);
        PlotHistogram.plot("ObservedDistances", iDistances, IAPUtils.getOptimBins(iDistances, 8, iDistances.length / 8));
        final double[] minMaxMean = IAPUtils.getMinMaxMeanD(iDistances);
        minD = minMaxMean[0];
        maxD = minMaxMean[1];
        meanD = minMaxMean[2];
        System.out.println("min d" + minD);
    
        IJ.showMessage("Suggested Kernel wt(p): " + IAPUtils.calcWekaWeights(iDistances));
    
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
    public boolean cmaOptimization(List<Result> aResultsOutput) {
        final CMAMosaicObjectiveFunction fitfun = new CMAMosaicObjectiveFunction(dgrid, q_D_grid, iDistances, potentialType, IAPUtils.normalize(NN_D_grid));
        if (potentialType == PotentialFunctions.NONPARAM) {
            best = new double[cmaReRunTimes][PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
        }
        else {
            best = new double[cmaReRunTimes][2];
        }
        allFitness = new double[cmaReRunTimes];
        double bestFitness = Double.MAX_VALUE;
        boolean diffFitness = false;
        for (int k = 0; k < cmaReRunTimes; k++) {
            final CMAEvolutionStrategy cma = new CMAEvolutionStrategy();
            cma.options.writeDisplayToFile = 0;
    
            cma.readProperties(); // read options, see file
            // CMAEvolutionStrategy.properties
            cma.options.stopFitness = 1e-12; // optional setting
            cma.options.stopTolFun = 1e-15;
            final Random rn = new Random(System.nanoTime());
            if (potentialType == PotentialFunctions.NONPARAM) {
                cma.setDimension(PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1);
                final double[] initialX = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
                final double[] initialsigma = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
                for (int i = 0; i < PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1; i++) {
                    initialX[i] = meanD * rn.nextDouble();
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
    
                if (meanD != 0) {
                    initialX[1] = rn.nextDouble() * meanD;
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
    
                if (meanD != 0) {
                    initialX[1] = rn.nextDouble() * meanD;
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
    
            // initialize cma and get fitness array to fill in later
            final double[] fitness = cma.init();
    
            // iteration loop
            while (cma.stopConditions.getNumber() == 0) {
    
                // --- core iteration step ---
                final double[][] pop = cma.samplePopulation(); // get a new population
                // of solutions
                for (int i = 0; i < pop.length; ++i) { 
                    // for each candidate solution i
                    // a simple way to handle constraints that define a convex feasible domain
                    // (like box constraints, i.e. variable boundaries) via "blind re-sampling"
                    // assumes that the feasible domain is convex, the optimum is
                    while (!fitfun.isFeasible(pop[i])) {
                        // not located on (or very close to) the domain boundary,
                        pop[i] = cma.resampleSingle(i); // initialX is feasible
                    }
                    // and initialStandardDeviations are sufficiently small to
                    // prevent quasi-infinite looping here compute fitness/objective value
                    fitness[i] = fitfun.valueOf(pop[i]); // fitfun.valueOf() is to be minimized
                }
                cma.updateDistribution(fitness); // pass fitness array to update
                // search distribution
                // --- end core iteration step ---
    
                final int outmod = 150;
                if (cma.getCountIter() % (15 * outmod) == 1) {
                    cma.printlnAnnotation(); // might write file as well
                }
                if (cma.getCountIter() % outmod == 1) {
                    cma.println();
                }
            }
            // evaluate mean value as it is the best estimator for the optimum
            cma.setFitnessOfMeanX(fitfun.valueOf(cma.getMeanX())); // updates the best ever solution
    
            cma.println();
            cma.println("Terminated due to");
            for (final String s : cma.stopConditions.getMessages()) {
                cma.println("  " + s);
            }
            cma.println("best function value " + cma.getBestFunctionValue() + " at evaluation " + cma.getBestEvaluationNumber());
    
            allFitness[k] = cma.getBestFunctionValue();
            if (allFitness[k] < bestFitness) {
                if (k > 0 && bestFitness - allFitness[k] > allFitness[k] * .00001) {
                    diffFitness = true;
                }
                bestFitness = allFitness[k];
                bestFitnessindex = k;
    
            }
            best[k] = cma.getBestX();
            double strength = 0;
            double thresholdOrScale = 0;
            if (potentialType != PotentialFunctions.NONPARAM) {
                strength = best[k][0];
                thresholdOrScale = best[k][1];
            }
            double residual = allFitness[k];
            aResultsOutput.add(new Result(strength, thresholdOrScale, residual));
        }
    
        if (diffFitness) {
            IJ.showMessage("Warning: Optimization returned different results for reruns. The results may not be accurate. Displaying the parameters and the plots corr. to best fitness.");
        }
    
        if (!showAllRerunResults) { // show only best
            final double[] fitPotential = fitfun.getPotential(best[bestFitnessindex]);
            fitfun.l2Norm(best[bestFitnessindex]); // to calc pgrid for best params
            final Plot plot = new Plot("Estimated potential", "distance", "Potential value", fitfun.getD_grid(), fitPotential);
            double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
            for (int i = 0; i < fitPotential.length; i++) {
                if (fitPotential[i] < min) {
                    min = fitPotential[i];
                }
                if (fitPotential[i] > max) {
                    max = fitPotential[i];
                }
            }
    
            plot.setLimits(fitfun.getD_grid()[0] - 1, fitfun.getD_grid()[fitfun.getD_grid().length - 1], min, max);
            plot.setColor(Color.BLUE);
            plot.setLineWidth(2);
            final DecimalFormat format = new DecimalFormat("#.####E0");
    
            if (potentialType == PotentialFunctions.NONPARAM) {
                String estim = "";
                final double[] dp = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
                double minW = Double.MAX_VALUE, maxW = Double.MIN_VALUE;
                for (int i = 0; i < PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1; i++) {
                    dp[i] = PotentialFunctions.dp[i];
                    estim = estim + "w[" + i + "]=" + best[bestFitnessindex][i] + " ";
                    if (best[bestFitnessindex][i] < minW) {
                        minW = best[bestFitnessindex][i];
                    }
                    if (best[bestFitnessindex][i] > maxW) {
                        maxW = best[bestFitnessindex][i];
                    }
                }
                System.out.println(estim);
                final Plot plotWeight = new Plot("Estimated Nonparam weights for best fitness:", "Support", "Weight", new double[1], new double[1]);
                plot.addLabel(.65, .3, "Residual: " + format.format(allFitness[bestFitnessindex]));
    
                plotWeight.setLimits(dp[0], dp[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1 - 1], minW, maxW);
                plotWeight.addPoints(dp, best[bestFitnessindex], Plot.CROSS);
                plotWeight.setColor(Color.RED);
    
                plotWeight.setLineWidth(2);
                plotWeight.show();
            }
            else if (potentialType == PotentialFunctions.STEP) {
                best[bestFitnessindex][0] = Math.abs(best[bestFitnessindex][0]);// epsil
                best[bestFitnessindex][1] = Math.abs(best[bestFitnessindex][1]);
                System.out.println("Best parameters: Epsilon, Threshold:" + best[0] + " " + best[bestFitnessindex][1]);
    
                plot.addLabel(.65, .3, "Strength: " + format.format(best[bestFitnessindex][0]));
                plot.addLabel(.65, .4, "Threshold: " + format.format(best[bestFitnessindex][1]));
                plot.addLabel(.65, .5, "Residual: " + format.format(allFitness[bestFitnessindex]));
            }
            else {
                best[bestFitnessindex][0] = Math.abs(best[bestFitnessindex][0]);
                best[bestFitnessindex][1] = Math.abs(best[bestFitnessindex][1]);
                System.out.println("Best parameters:  Epsilon, Sigma:" + best[bestFitnessindex][0] + " " + best[bestFitnessindex][1]);
                plot.addLabel(.65, .3, "Strength: " + format.format(best[bestFitnessindex][0]));
                plot.addLabel(.65, .4, "Scale: " + format.format(best[bestFitnessindex][1]));
                plot.addLabel(.65, .5, "Residual: " + format.format(allFitness[bestFitnessindex]));
            }
            System.out.println("N= " + iDistances.length);
            plot.show();
            
            double[] P_grid = fitfun.getPGrid();
            P_grid = IAPUtils.normalize(P_grid);
            String xlabel = "Distance";
            if (isImage) {
                xlabel = xlabel + " (" + iImageX.getCalibration().getUnit() + ")";
            }
            PlotQPNN.plot(xlabel, dgrid, P_grid, q, nnObserved, potentialType, best, allFitness, bestFitnessindex);
        }
    
        return true;
    }

    public boolean hypTest(int monteCarloRunsForTest, double alpha) {
        if (best == null) {
            IJ.showMessage("Error: Run estimation first");
        }
        else if (potentialType == PotentialFunctions.NONPARAM) {
            IJ.showMessage("Hypothesis test is not applicable for Non Parametric potential \n since it does not have 'strength' parameter");
            return false;
        }
    
        System.out.println("Running test with " + monteCarloRunsForTest + " and " + alpha);
        final HypothesisTesting ht = new HypothesisTesting(IAPUtils.calculateCDF(q_D_grid), dgrid, iDistances, best[bestFitnessindex], potentialType, monteCarloRunsForTest, alpha);
        ht.rankTest();
        return true;
    }

    public boolean generateMask() {
        genMask = new ImagePlus();
        if (iImageY != null) {
            genMask = ImageProcessUtils.generateMask(iImageY);
            System.out.println("Generated mask: " + genMask.getType());
            return true;
        }
        return false;
    }

    public boolean loadMask() {
        ImagePlus tempMask = ImageProcessUtils.openImage();
        if (tempMask == null) {
            IJ.showMessage("Filetype not recognized");
            return false;
        }
        tempMask.show("Mask loaded" + tempMask.getTitle());
    
        if (tempMask.getType() != ImagePlus.GRAY8) {
            IJ.showMessage("ERROR: Loaded mask not 8 bit gray");
            return false;
        }
    
        if (isImage) {
            if (tempMask.getHeight() != iImageY.getHeight() || tempMask.getWidth() != iImageY.getWidth() || tempMask.getNSlices() != iImageY.getNSlices()) {
                IJ.showMessage("ERROR: Loaded mask size does not match with image size");
                return false;
            }
        }
    
        genMask = tempMask;
        return true;
    }

    public boolean applyMask() {
        if (genMask == null) {
            return false;
        }
        genMask.updateImage();
        mask = genMask;
        return true;
    }

    public boolean resetMask() {
        mask = null;
        return true;
    }
    
    public String getMaskTitle() {
        return mask.getTitle();
    }

    public double getMinD() {
        return minD;
    }

    public double getMaxD() {
        return maxD;
    }

    public double[] getDistances() {
        return iDistances;
    }
    
    public void setPotentialType(int potentialType) {
        this.potentialType = potentialType;
    }

    public void setCmaReRunTimes(int cmaReRunTimes) {
        this.cmaReRunTimes = cmaReRunTimes;
    }
}
