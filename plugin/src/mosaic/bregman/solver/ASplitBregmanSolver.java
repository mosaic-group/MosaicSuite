package mosaic.bregman.solver;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import mosaic.bregman.solver.SolverParameters.NoiseModel;
import mosaic.core.psf.psf;
import net.imglib2.type.numeric.real.DoubleType;


public abstract class ASplitBregmanSolver {
    private static final Logger logger = Logger.getLogger(ASplitBregmanSolver.class);
    
    // Input parameters
    protected final SolverParameters iParameters;
    protected final double[][][] iImage;
    protected double iBetaMleOut;
    protected double iBetaMleIn;
    final double iRegularization;
    protected final psf<DoubleType> iPsf;

    // Internal data
    protected final int ni, nj, nz;
    protected final SolverTools iLocalTools;
    protected final ExecutorService executor;
    protected final double iEnergies[];
    // betaMleOut/betaMleIn are being updated in 2D case but not in 3D. In 3d only returned
    // getBetaMLE() is based on updated stuff
    public final double[] betaMle = new double[2];

    // Segmentation masks
    public final double[][][] w3k;
    public final double[][][] w3kbest;
    
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
    
    
    public static ASplitBregmanSolver create(SolverParameters aParameters, double[][][] aImage, double[][][] aMask, psf<DoubleType> aPsf) {
        return  (aImage.length > 1) 
                ? new ASplitBregmanSolver3D(aParameters, aImage, aMask, aPsf)
                : new ASplitBregmanSolver2D(aParameters, aImage, aMask, aPsf);
    }
    
    ASplitBregmanSolver(SolverParameters aParameters, double[][][] aImage, double[][][] aMask, psf<DoubleType> aPsf) {
        iParameters = aParameters;
        iImage = aImage;
        iBetaMleOut = iParameters.betaMleOut;
        iBetaMleIn = iParameters.betaMleIn;
        iRegularization = iParameters.lambdaRegularization;
        iPsf = aPsf;
        ni = aImage[0].length; 
        nj = aImage[0][0].length;
        nz = aImage.length; 

        w3k = new double[nz][ni][nj];
        SolverTools.copytab(w3k, aMask);
        w3kbest = new double[nz][ni][nj];
        
        iLocalTools = new SolverTools(ni, nj, nz);
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

    public final double getBetaMleIn() {
        return betaMle[1];
    }

    private int iIterNum = 0;
    private int iBestIterationNum = 0;
    private double iBestEnergy = Double.MAX_VALUE;
    private double iLastEnergy = 0;
    
    public final boolean performIteration(boolean aLastIteration) {
        final boolean energyEvaluation = (iIterNum % iParameters.energyEvaluationModulo == 0 || aLastIteration);
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
                SolverTools.copytab(w3kbest, w3k);
                iBestIterationNum = iIterNum;
                iBestEnergy = currentEnergy;
            }
            
            if (iIterNum != 0) {
                if (Math.abs((currentEnergy - iLastEnergy) / iLastEnergy) < iParameters.energySearchThreshold) {
                    stopFlag = true;
                }
            }
            iLastEnergy = currentEnergy;
            
            if (iParameters.debug) {
                logger.debug("Energy at step " + iIterNum + ": " + currentEnergy + " stopFlag: " + stopFlag);
            }
        }

        iIterNum++;

        return stopFlag;
    }

    public void postprocess() {
        if (iBestIterationNum < 50) { // use what iteration threshold ?
            SolverTools.copytab(w3kbest, w3k);
            iBestIterationNum = iIterNum - 1;
            iBestEnergy = iLastEnergy;
            
            logger.debug("Warning : increasing energy. Last computed mask is then used for first phase object segmentation. Iter: " + iBestIterationNum);
        }
        if (iParameters.debug) {
            logger.debug("Best energy : " + SolverTools.round(iBestEnergy, 3) + ", found at step " + iBestIterationNum);
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
    public abstract void init();
}
