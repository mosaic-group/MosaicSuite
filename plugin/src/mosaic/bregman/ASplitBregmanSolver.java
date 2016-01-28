package mosaic.bregman;


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
import ij.process.FloatProcessor;
import mosaic.utils.ArrayOps;


abstract class ASplitBregmanSolver {
    protected final Tools LocalTools;

    protected ArrayList<Region> regionsvoronoi;
    protected final MasksDisplay md;
    protected final double[][][] image;

    protected final double[][] eigenLaplacian;

    protected final int channel;
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
    protected final float[][][] Ro;

    protected final int ni, nj, nz;
    protected double energy; 
    protected final Parameters iParameters;
    private AnalysePatch Ap = null;

    protected ExecutorService executor;
    
    // These guys seems to be duplicated but it is not a case. betaMleOut/betaMleIn are 
    // being updated in 2D case but not in 3D.
    double iBetaMleOut, iBetaMleIn;
    final double[] betaMle = new double[2];
    
    final double[] lreg_;
    
    public final double energytab2[];
    
    ASplitBregmanSolver(Parameters aParameters, double[][][] image, double[][][] mask, MasksDisplay md, int channel, AnalysePatch ap, double aBetaMleOut, double aBetaMleIn, double[] aLreg) {
        this(aParameters, image, mask, md, channel, aBetaMleOut, aBetaMleIn, aLreg);
        Ap = ap;
    }

    private ASplitBregmanSolver(Parameters aParameters, double[][][] aImage, double[][][] aMask, MasksDisplay aMaskDisplay, int aChannel, double aBetaMleOut, double aBetaMleIn, double[] aLreg) {
        channel = aChannel;
        iParameters = aParameters;
        ni = aImage[0].length; 
        nj = aImage[0][0].length;
        nz = aImage.length; 
        LocalTools = new Tools(ni, nj, nz);
        
        // Beta MLE in and out
        iBetaMleOut = aBetaMleOut;
        iBetaMleIn = aBetaMleIn;
        betaMle[0] = iBetaMleOut;
        betaMle[1] = iBetaMleIn;
        
        energytab2 = new double[iParameters.nthreads];
        
        md = aMaskDisplay;

        image = aImage;

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
        Ro = new float[nz][ni][nj];

        temp1 = new double[nz][ni][nj];
        temp2 = new double[nz][ni][nj];
        temp3 = new double[nz][ni][nj];
        temp4 = new double[nz][ni][nj];
        
        // precompute eigenlaplacian
        eigenLaplacian = new double[ni][nj];
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                eigenLaplacian[i][j] = 2 + (2 - 2 * Math.cos((j) * Math.PI / (nj)) + (2 - 2 * Math.cos((i) * Math.PI / (ni))));
            }
        }

        LocalTools.fgradx2D(temp1, aMask);
        LocalTools.fgrady2D(temp2, aMask);

        LocalTools.copytab(w1k, aMask);
        LocalTools.copytab(w3k, aMask);

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    Ro[z][i][j] = (float) (iBetaMleOut);
                    Ri[z][i][j] = (float) (iBetaMleIn);
                }
            }
        }
        lreg_ = aLreg;
        executor = Executors.newFixedThreadPool(iParameters.nthreads);
    }

    double[] getBetaMLE() {
        return betaMle;
    }

    void first_run() throws InterruptedException {
        IJ.showStatus("Computing segmentation");
        IJ.showProgress(0.0);
        
        final int firstStepNumOfIterations = 151;
        run(true, firstStepNumOfIterations);
        
        regions_intensity_findthresh(w3kbest);
        
        if (iParameters.livedisplay) {
            md.display2regions(w3kbest, "Mask", channel);
        }
    }
    
    void second_run() throws InterruptedException {
        final int secondStepNumOfIterations = 101;
        
        run(false, secondStepNumOfIterations);
    }
    
    void run(boolean aFirstPhase, int aNumOfIterations) throws InterruptedException {
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
                LocalTools.copytab(w3kbest, w3k);
                bestIteration = stepk;
                bestEnergy = energy;
            }
            
            
            if (moduloStep && stepk != 0) {
                if (Math.abs((energy - lastenergy) / lastenergy) < iParameters.tol) {
                    stopFlag = true;
                }
            }
            lastenergy = energy;

            if (aFirstPhase) {
                if (moduloStep) {
                    if (iParameters.livedisplay) {
                        IJ.log(String.format("Energy at step %d: %7.6e", stepk, energy));
                        if (stopFlag) IJ.log("energy stop");
                        md.display2regions(w3k, "Mask", channel);
                    }
                    IJ.showStatus("Computing segmentation  " + Tools.round((50 * stepk)/(aNumOfIterations - 1), 2) + "%");
                }
                IJ.showProgress(0.5 * (stepk) / (aNumOfIterations - 1));
            }
            else {
                if (iParameters.mode_intensity == 0 && (stepk == 40 || stepk == 70)) {
                    Ap.find_best_thresh_and_int(w3k);
                    betaMle[0] = Math.max(0, Ap.cout);
                    // lower bound withg some margin
                    betaMle[1] = Math.max(0.75 * (Ap.firstminval - Ap.iIntensityMin) / (Ap.iIntensityMax - Ap.iIntensityMin), Ap.cin);
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
            LocalTools.copytab(w3kbest, w3k);
            bestIteration = stepk - 1;
            bestEnergy = energy;
            if (iParameters.livedisplay && aFirstPhase) {
                IJ.log("Warning : increasing energy. Last computed mask is then used for first phase object segmentation." + bestIteration);
            }
        }
        if (aFirstPhase && iParameters.livedisplay) {
            IJ.log("Best energy : " + Tools.round(bestEnergy, 3) + ", found at step " + bestIteration);
        }
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
    }

    abstract protected void step(boolean aEvaluateEnergy, boolean aLastIteration) throws InterruptedException;
    abstract protected void init();

    void regions_intensity_findthresh(double[][][] mask) {
        double thresh = (channel == 0) ? iParameters.min_intensity : iParameters.min_intensityY;

        ImagePlus mask_im = new ImagePlus();
        final ImageStack mask_ims = new ImageStack(ni, nj);

        // construct mask as an imageplus
        for (int z = 0; z < nz; z++) {
            final float[] mask_float = new float[ni * nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    mask_float[j * ni + i] = (float) mask[z][i][j];
                }
            }
            final FloatProcessor fp = new FloatProcessor(ni, nj);
            fp.setPixels(mask_float);
            mask_ims.addSlice("", fp);
        }
        mask_im.setStack("test", mask_ims);

        // project mask on single slice (maximum values)
        final ZProjector proj = new ZProjector(mask_im);
        proj.setImage(mask_im);
        proj.setStartSlice(1);
        proj.setStopSlice(nz);
        proj.setMethod(ZProjector.MAX_METHOD);
        proj.doProjection();
        mask_im = proj.getProjection();
        IJ.showStatus("Computing segmentation  52 %");
        IJ.showProgress(0.52);

        // threshold mask
        final byte[] mask_bytes = new byte[ni * nj];
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                if (((int) (255 * mask_im.getProcessor().getPixelValue(i, j))) > 255 * thresh) {
                    // weird conversion to have same thing than in find connected regions
                    mask_bytes[j * ni + i] = 0;
                }
                else {
                    mask_bytes[j * ni + i] = (byte) 255;
                }
            }
        }
        final ByteProcessor bp = new ByteProcessor(ni, nj);
        bp.setPixels(mask_bytes);
        mask_im.setProcessor("Voronoi", bp);

        // do voronoi in 2D on Z projection
        // Here we compute the Voronoi segmentation starting from the threshold mask
        final EDM filtEDM = new EDM();
        filtEDM.setup("voronoi", mask_im);
        filtEDM.run(mask_im.getProcessor());
        mask_im.getProcessor().invert();
        IJ.showStatus("Computing segmentation  " + 53 + "%");
        IJ.showProgress(0.53);

        // expand Voronoi in 3D
        final ImageStack mask_ims3 = new ImageStack(ni, nj);
        for (int z = 0; z < nz; z++) {
            final byte[] mask_bytes3 = new byte[ni * nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    mask_bytes3[j * ni + i] = (byte) mask_im.getProcessor().getPixel(i, j);//
                }
            }
            final ByteProcessor bp3 = new ByteProcessor(ni, nj);
            bp3.setPixels(mask_bytes3);
            mask_ims3.addSlice("", bp3);
        }
        mask_im.setStack("Voronoi", mask_ims3);

        // Here we are elaborating the Voronoi mask to get a nice subdivision
        final double thr = 254;
        final FindConnectedRegions fcr = new FindConnectedRegions(mask_im);
        fcr.run(ni * nj * nz, 0, (float) thr);// min size was 5

        ArrayList<Region> regionslist = fcr.getFoundRegions();
        regionsvoronoi = regionslist;

        // use Ri to store voronoi regions indices
        ArrayOps.fill(Ri, 255);
        cluster_region_voronoi2(Ri, regionslist);

        IJ.showStatus("Computing segmentation  " + 54 + "%");
        IJ.showProgress(0.54);
    }
    
    private void cluster_region_voronoi2(float[][][] Ri, ArrayList<Region> regionslist) {
        for (final Region r : regionslist) {
            for (final Pix p : r.pixels) {
                Ri[p.pz][p.px][p.py] = regionslist.indexOf(r);
            }
        }
    }
}
