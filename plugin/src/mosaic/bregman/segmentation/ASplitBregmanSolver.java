package mosaic.bregman.segmentation;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import ij.IJ;
import mosaic.bregman.segmentation.SegmentationParameters.IntensityMode;
import mosaic.bregman.segmentation.SegmentationParameters.NoiseModel;
import mosaic.core.psf.psf;
import net.imglib2.type.numeric.real.DoubleType;


abstract class ASplitBregmanSolver {
    private static final Logger logger = Logger.getLogger(ASplitBregmanSolver.class);
    
    // Input parameters
    protected final SegmentationParameters iParameters;
    protected final double[][][] iImage;
    private final AnalysePatch iAnalysePatch;
    protected double iBetaMleOut;
    protected double iBetaMleIn;
    final double iRegularization;
    protected final psf<DoubleType> iPsf;

    // Internal data
    protected final int ni, nj, nz;
    protected final Tools iLocalTools;
    protected final ExecutorService executor;
    protected final double iEnergies[];
    // betaMleOut/betaMleIn are being updated in 2D case but not in 3D. In 3d only returned
    // getBetaMLE() is based on updated stuff
    final double[] betaMle = new double[2];

    // Segmentation masks
    protected final double[][][] w3k;
    protected final double[][][] w3kbest;
    
    // Used by superclasses and utils
    protected final NoiseModel iNoiseModel;
    protected final double[][][] w1k;
    protected final double[][][] w2xk;
    protected final double[][][] w2yk;
    protected final double[][][] b2xk;
    protected final double[][][] b2yk;
    protected final double[][][] b1k;
    protected final double[][][] b3k;
    protected double[][][] temp1;
    protected double[][][] temp2;
    protected double[][][] temp3;
    protected double[][][] temp4;
    
    
    ASplitBregmanSolver(SegmentationParameters aParameters, double[][][] aImage, double[][][] aMask, AnalysePatch aAnalazePatch, double aBetaMleOut, double aBetaMleIn, double aRegularization, psf<DoubleType> aPsf) {
        iParameters = aParameters;
        iImage = aImage;
        iAnalysePatch = aAnalazePatch;
        iBetaMleOut = aBetaMleOut;
        iBetaMleIn = aBetaMleIn;
        iRegularization = aRegularization;
        iPsf = aPsf;
        ni = aImage[0].length; 
        nj = aImage[0][0].length;
        nz = aImage.length; 

        w3k = new double[nz][ni][nj];
        Tools.copytab(w3k, aMask);
        w3kbest = new double[nz][ni][nj];
        
        iLocalTools = new Tools(ni, nj, nz);
        executor = Executors.newFixedThreadPool(iParameters.numOfThreads);
        iEnergies = new double[iParameters.numOfThreads];
        betaMle[0] = iBetaMleOut;
        betaMle[1] = iBetaMleIn;
        
        iNoiseModel = iParameters.noiseModel;
        w1k = new double[nz][ni][nj];
        b2xk = new double[nz][ni][nj];
        b2yk = new double[nz][ni][nj];
        b1k = new double[nz][ni][nj];
        b3k = new double[nz][ni][nj];
        w2xk = new double[nz][ni][nj];
        w2yk = new double[nz][ni][nj];
        temp1 = new double[nz][ni][nj];
        temp2 = new double[nz][ni][nj];
        temp3 = new double[nz][ni][nj];
        temp4 = new double[nz][ni][nj];
    }

    final double getBetaMleIn() {
        return betaMle[1];
    }

    final void second_run() {
        final int numOfIterations = 101;
        final boolean isFirstPhase = false;
        boolean isDone = false;
        int iteration = 0;
        while (iteration < numOfIterations && !isDone) {
            isDone = performIteration(isFirstPhase, numOfIterations);
            iteration++;
        }
        postprocess(isFirstPhase);
    }

    private int iIterNum = 0;
    private int iBestIterationNum = 0;
    private double iBestEnergy = Double.MAX_VALUE;
    private double iLastEnergy = 0;
    
    final boolean performIteration(boolean aFirstPhase, int aNumOfIterations) {
        final boolean lastIteration = (iIterNum == aNumOfIterations - 1);
        final boolean energyEvaluation = (iIterNum % iParameters.energyEvaluationModulo == 0 || lastIteration);
        boolean stopFlag = false;

        try {
            step(energyEvaluation);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (energyEvaluation) {
            double currentEnergy = 0;
            for (int nt = 0; nt < iParameters.numOfThreads; nt++) {
                currentEnergy += iEnergies[nt];
            }
            
            if (currentEnergy < iBestEnergy) {
                Tools.copytab(w3kbest, w3k);
                iBestIterationNum = iIterNum;
                iBestEnergy = currentEnergy;
            }
            
            if (iIterNum != 0) {
                if (Math.abs((currentEnergy - iLastEnergy) / iLastEnergy) < iParameters.energySearchThreshold) {
                    stopFlag = true;
                }
            }
            iLastEnergy = currentEnergy;
            
            if (aFirstPhase) {
                if (iParameters.debug) {
                    logger.debug("Energy at step " + iIterNum + ": " + currentEnergy + " stopFlag: " + stopFlag);
                }
            }
        }

        if (!aFirstPhase) {
            if (iParameters.intensityMode == IntensityMode.AUTOMATIC && (iIterNum == 40 || iIterNum == 70)) {
                iAnalysePatch.estimateIntensity(w3k);
                betaMle[0] = Math.max(0, iAnalysePatch.cout);
                betaMle[1] = Math.max(0.75 * iAnalysePatch.iNormalizedMinObjectIntensity, iAnalysePatch.cin);
                init();
                if (iParameters.debug) {
                    logger.debug("Region " + iAnalysePatch.iInputRegion.iLabel + String.format(" Photometry :%n background %10.8e %n foreground %10.8e", iAnalysePatch.cout, iAnalysePatch.cin));
                }
            }
        }
        
        iIterNum++;

        return stopFlag;
    }

    void postprocess(boolean aFirstPhase) {
        if (iBestIterationNum < 50) { // use what iteration threshold ?
            Tools.copytab(w3kbest, w3k);
            iBestIterationNum = iIterNum - 1;
            iBestEnergy = iLastEnergy;
            
            logger.debug("Warning : increasing energy. Last computed mask is then used for first phase object segmentation." + iBestIterationNum);
        }
        if (aFirstPhase) { 
            if (iParameters.debug) {
                IJ.log("Best energy : " + Tools.round(iBestEnergy, 3) + ", found at step " + iBestIterationNum);
            }
        }
        try {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.DAYS);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    abstract protected void step(boolean aEvaluateEnergy) throws InterruptedException;
    abstract protected void init();
}
