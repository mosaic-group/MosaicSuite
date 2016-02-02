package mosaic.bregman.segmentation;


import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ZProjector;
import ij.plugin.filter.EDM;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import mosaic.bregman.segmentation.SegmentationParameters.IntensityMode;
import mosaic.bregman.segmentation.SegmentationParameters.NoiseModel;
import mosaic.core.psf.psf;
import mosaic.utils.ArrayOps;
import mosaic.utils.ImgUtils;
import net.imglib2.type.numeric.real.DoubleType;


abstract class ASplitBregmanSolver {
    protected final Tools LocalTools;

    protected ArrayList<Region> regionsvoronoi;
    protected final double[][][] image;

    protected final double[][][] w1k;
    protected final double[][][] w3k;

    protected final double[][][] w2xk;
    protected final double[][][] w2yk;

    protected final double[][][] w3kbest;
    protected final double[][][] b2xk;
    protected final double[][][] b2yk;

    protected final double[][][] b1k;
    protected final double[][][] b3k;

    protected double[][][] temp1;
    protected double[][][] temp2;
    protected double[][][] temp3;
    protected double[][][] temp4;

    protected final float[][][] Ri;

    protected final int ni, nj, nz;
    protected double energy; 
    protected final SegmentationParameters iParameters;
    private final AnalysePatch Ap;

    protected final ExecutorService executor;
    
    // These guys seems to be duplicated but it is not a case. betaMleOut/betaMleIn are 
    // being updated in 2D case but not in 3D.
    double iBetaMleOut, iBetaMleIn;
    final double[] betaMle = new double[2];
    
    final double lreg_;
    
    NoiseModel iNoiseModel;
    protected final double energytab2[];
    private final double iMinIntensity;
    psf<DoubleType> iPsf;
    
    ASplitBregmanSolver(SegmentationParameters aParameters, double[][][] image, double[][][] mask, AnalysePatch ap, double aBetaMleOut, double aBetaMleIn, double aLreg, double aMinIntensity, psf<DoubleType> aPsf) {
        iParameters = aParameters;
        ni = image[0].length; 
        nj = image[0][0].length;
        nz = image.length; 
        LocalTools = new Tools(ni, nj, nz);
        
        // Beta MLE in and out
        iBetaMleOut = aBetaMleOut;
        iBetaMleIn = aBetaMleIn;
        betaMle[0] = iBetaMleOut;
        betaMle[1] = iBetaMleIn;
        
        energytab2 = new double[iParameters.numOfThreads];
        
        this.image = image;
        
        w1k = new double[nz][ni][nj];
        w3k = new double[nz][ni][nj];
        w3kbest = new double[nz][ni][nj];
        
        b2xk = new double[nz][ni][nj];
        b2yk = new double[nz][ni][nj];
        
        b1k = new double[nz][ni][nj];
        b3k = new double[nz][ni][nj];
        
        w2xk = new double[nz][ni][nj];
        w2yk = new double[nz][ni][nj];
        
        Ri = new float[nz][ni][nj];
        
        temp1 = new double[nz][ni][nj];
        temp2 = new double[nz][ni][nj];
        temp3 = new double[nz][ni][nj];
        temp4 = new double[nz][ni][nj];
        
        LocalTools.fgradx2D(temp1, mask);
        LocalTools.fgrady2D(temp2, mask);
        
        Tools.copytab(w1k, mask);
        Tools.copytab(w3k, mask);
        
        lreg_ = aLreg;
        executor = Executors.newFixedThreadPool(iParameters.numOfThreads);
        Ap = ap;
        
        iMinIntensity = aMinIntensity;
        iNoiseModel = iParameters.noiseModel;
        iPsf = aPsf;
    }

    final double[] getBetaMLE() {
        return betaMle;
    }

    final void first_run() throws InterruptedException {
        final int firstStepNumOfIterations = 151;
        run(true, firstStepNumOfIterations);
    }
    
    final void second_run() throws InterruptedException {
        final int secondStepNumOfIterations = 101;
        run(false, secondStepNumOfIterations);
    }
    
    private final void run(boolean aFirstPhase, int aNumOfIterations) throws InterruptedException {
        int stepk = 0;
        final int modulo = 10;
        int bestIteration = 0;
        boolean stopFlag = false;
        double bestEnergy = Double.MAX_VALUE;
        double lastenergy = 0;
        
        while (stepk < aNumOfIterations && !stopFlag) {
            final boolean lastIteration = (stepk == aNumOfIterations - 1);
            final boolean energyEvaluation = (stepk % iParameters.energyEvaluationModulo == 0);
            final boolean moduloStep = (stepk % modulo == 0 || lastIteration);

            step(energyEvaluation, lastIteration);

            if (energy < bestEnergy) {
                Tools.copytab(w3kbest, w3k);
                bestIteration = stepk;
                bestEnergy = energy;
            }
            
            
            if (moduloStep && stepk != 0) {
                if (Math.abs((energy - lastenergy) / lastenergy) < iParameters.energySearchThreshold) {
                    stopFlag = true;
                }
            }
            lastenergy = energy;

            if (aFirstPhase) {
                if (moduloStep) {
                    if (iParameters.debug) {
                        IJ.log(String.format("Energy at step %d: %7.6e", stepk, energy));
                        if (stopFlag) IJ.log("energy stop");
                    }
                    IJ.showStatus("Computing segmentation  " + Tools.round((50 * stepk)/(aNumOfIterations - 1), 2) + "%");
                }
                IJ.showProgress(0.5 * (stepk) / (aNumOfIterations - 1));
            }
            else {
                if (iParameters.intensityMode == IntensityMode.AUTOMATIC && (stepk == 40 || stepk == 70)) {
                    Ap.find_best_thresh_and_int(w3k);
                    betaMle[0] = Math.max(0, Ap.cout);
                    // lower bound withg some margin
                    betaMle[1] = Math.max(0.75 * (iMinIntensity - Ap.iIntensityMin) / (Ap.iIntensityMax - Ap.iIntensityMin), Ap.cin);
                    init();
                    if (iParameters.debug) {
                        IJ.log("region" + Ap.iInputRegion.value + " pcout" + betaMle[1]);
                        IJ.log("region" + Ap.iInputRegion.value + String.format(" Photometry :%n backgroung %10.8e %n foreground %10.8e", Ap.cout, Ap.cin));
                    }
                }
            }

            stepk++;
        }

        if (bestIteration < 50) { // use what iteration threshold ?
            Tools.copytab(w3kbest, w3k);
            bestIteration = stepk - 1;
            bestEnergy = energy;
            
            if (aFirstPhase) {
                if (iParameters.debug) {
                    IJ.log("Warning : increasing energy. Last computed mask is then used for first phase object segmentation." + bestIteration);
                }
            }
        }
        if (aFirstPhase) { 
            if (iParameters.debug) {
                IJ.log("Best energy : " + Tools.round(bestEnergy, 3) + ", found at step " + bestIteration);
            }
        }
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
    }

    abstract protected void step(boolean aEvaluateEnergy, boolean aLastIteration) throws InterruptedException;
    abstract protected void init();

    void regions_intensity_findthresh(double[][][] mask) {
        ImagePlus maskImg = ImgUtils.ZXYarrayToImg(mask);
        
        // project mask on single slice (maximum values)
        final ZProjector proj = new ZProjector(maskImg);
        proj.setImage(maskImg);
        proj.setStartSlice(1);
        proj.setStopSlice(nz);
        proj.setMethod(ZProjector.MAX_METHOD);
        proj.doProjection();
        maskImg = proj.getProjection();
        IJ.showStatus("Computing segmentation 52%");
        IJ.showProgress(0.52);

        // threshold mask
        ImageProcessor imp = maskImg.getProcessor();
        final byte[] mask_bytes = new byte[ni * nj];
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                // weird conversion to have same thing than in find connected regions
                if ( (int)(255 * imp.getPixelValue(i, j)) <= (int)(255 * iMinIntensity) ) {
                    mask_bytes[j * ni + i] = (byte) 255;
                }
            }
        }
        final ByteProcessor bp = new ByteProcessor(ni, nj, mask_bytes);
        maskImg.setProcessor("Mask Thresholded", bp);

        // do voronoi in 2D on Z projection
        // Here we compute the Voronoi segmentation starting from the threshold mask
        final EDM filtEDM = new EDM();
        filtEDM.setup("voronoi", maskImg);
        filtEDM.run(maskImg.getProcessor());
        maskImg.getProcessor().invert();
        IJ.showStatus("Computing segmentation 53%");
        IJ.showProgress(0.53);

        // expand Voronoi in 3D
        ImageProcessor impVoronoi = maskImg.getProcessor();
        final byte[] maskBytesVoronoi = new byte[ni * nj];
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                maskBytesVoronoi[j * ni + i] = (byte) impVoronoi.getPixelValue(i, j);
            }
        }
        final ImageStack imgStackVoronoi = new ImageStack(ni, nj);
        for (int z = 0; z < nz; z++) {
            imgStackVoronoi.addSlice("", new ByteProcessor(ni, nj, maskBytesVoronoi.clone()));
        }
        maskImg.setStack("Voronoi", imgStackVoronoi);

        // Here we are elaborating the Voronoi mask to get a nice subdivision
        final FindConnectedRegions fcr = new FindConnectedRegions(maskImg);
        fcr.run(ni * nj * nz, 0, /* threshold > 254 = 255 */ 254f, iParameters.excludeEdgesZ, 1, 1);
        regionsvoronoi = fcr.getFoundRegions();
        
        // use Ri to store voronoi regions indices
        ArrayOps.fill(Ri, 255);
        for (final Region r : regionsvoronoi) {
            for (final Pix p : r.pixels) {
                Ri[p.pz][p.px][p.py] = regionsvoronoi.indexOf(r);
            }
        }
        IJ.showStatus("Computing segmentation 54%");
        IJ.showProgress(0.54);
    }
}
