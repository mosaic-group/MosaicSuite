package mosaic.bregman;


import java.util.ArrayList;

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

    protected int stepk;
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
    protected final Parameters p;
    private AnalysePatch Ap = null;

    double c0, c1;
    public final double energytab2[];
    
    ASplitBregmanSolver(Parameters params, double[][][] image, double[][][] mask, MasksDisplay md, int channel, AnalysePatch ap) {
        this(params, image, mask, md, channel);
        Ap = ap;
    }

    private ASplitBregmanSolver(Parameters aParams, double[][][] aImage, double[][][] aMask, MasksDisplay aMaskDisplay, int aChannel) {
        LocalTools = new Tools(aParams.ni, aParams.nj, aParams.nz);
        channel = aChannel;
        p = aParams;
        ni = aParams.ni;
        nj = aParams.nj;
        nz = aParams.nz;

        // Beta MLE in and out
        c0 = aParams.cl[0];
        c1 = aParams.cl[1];
        
        energytab2 = new double[p.nthreads];
        
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
                    Ro[z][i][j] = (float) (p.cl[0]);
                    Ri[z][i][j] = (float) (p.cl[1]);
                }
            }
        }
    }

    void first_run() throws InterruptedException {
        if (p.firstphase) {
            IJ.showStatus("Computing segmentation");
            IJ.showProgress(0.0);
        }

        stepk = 0;
        final int modulo = 10;
        int bestIteration = 0;
        boolean stopFlag = false;
        double bestEnergy = Double.MAX_VALUE;
        double lastenergy = 0;
        
        while (stepk < p.max_nsb && !stopFlag) {
            step();

            if (energy < bestEnergy) {
                LocalTools.copytab(w3kbest, w3k);
                bestIteration = stepk;
                bestEnergy = energy;
            }
            
            final boolean moduloStep = stepk % modulo == 0 || stepk == p.max_nsb - 1;
            
            if (moduloStep && stepk != 0) {
                if (Math.abs((energy - lastenergy) / lastenergy) < p.tol) {
                    stopFlag = true;
                }
            }
            lastenergy = energy;

            if (p.firstphase) {
                if (moduloStep) {
                    if (p.livedisplay) {
                        IJ.log(String.format("Energy at step %d: %7.6e", stepk, energy));
                        if (stopFlag) IJ.log("energy stop");
                    }
                    IJ.showStatus("Computing segmentation  " + Tools.round((50 * stepk)/(p.max_nsb - 1), 2) + "%");
                }
                IJ.showProgress(0.5 * (stepk) / (p.max_nsb - 1));
            }
            else {
                if (p.mode_intensity == 0 && (stepk == 40 || stepk == 70)) {
                    Ap.find_best_thresh_and_int(w3k);
                    p.cl[0] = Math.max(0, Ap.cout);
                    // lower bound withg some margin
                    p.cl[1] = Math.max(0.75 * (Ap.firstminval - Ap.iIntensityMin) / (Ap.iIntensityMax - Ap.iIntensityMin), Ap.cin);
                    init();
                    if (p.debug) {
                        IJ.log("region" + Ap.iInputRegion.value + " pcout" + p.cl[1]);
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
            if (p.livedisplay && p.firstphase) {
                IJ.log("Warning : increasing energy. Last computed mask is then used for first phase object segmentation." + bestIteration);
            }
        }
        if (p.firstphase) {
            regions_intensity_findthresh(w3kbest);
            
            if (p.livedisplay) {
                md.display2regions(w3kbest, "Mask", channel);
                IJ.log("Best energy : " + Tools.round(bestEnergy, 3) + ", found at step " + bestIteration);
            }
        }
    }
    
    abstract protected void step() throws InterruptedException;
    abstract protected void init();

    void regions_intensity_findthresh(double[][][] mask) {
        double thresh = (channel == 0) ? p.min_intensity : p.min_intensityY;

        ImagePlus mask_im = new ImagePlus();
        final ImageStack mask_ims = new ImageStack(p.ni, p.nj);

        // construct mask as an imageplus
        for (int z = 0; z < nz; z++) {
            final float[] mask_float = new float[p.ni * p.nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    mask_float[j * p.ni + i] = (float) mask[z][i][j];
                }
            }
            final FloatProcessor fp = new FloatProcessor(p.ni, p.nj);
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
        final byte[] mask_bytes = new byte[p.ni * p.nj];
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                if (((int) (255 * mask_im.getProcessor().getPixelValue(i, j))) > 255 * thresh) {
                    // weird conversion to have same thing than in find connected regions
                    mask_bytes[j * p.ni + i] = 0;
                }
                else {
                    mask_bytes[j * p.ni + i] = (byte) 255;
                }
            }
        }
        final ByteProcessor bp = new ByteProcessor(p.ni, p.nj);
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
        final ImageStack mask_ims3 = new ImageStack(p.ni, p.nj);
        for (int z = 0; z < nz; z++) {
            final byte[] mask_bytes3 = new byte[p.ni * p.nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    mask_bytes3[j * p.ni + i] = (byte) mask_im.getProcessor().getPixel(i, j);//
                }
            }
            final ByteProcessor bp3 = new ByteProcessor(p.ni, p.nj);
            bp3.setPixels(mask_bytes3);
            mask_ims3.addSlice("", bp3);
        }
        mask_im.setStack("Voronoi", mask_ims3);

        // Here we are elaborating the Voronoi mask to get a nice subdivision
        final double thr = 254;
        final FindConnectedRegions fcr = new FindConnectedRegions(mask_im);
        fcr.run(p.ni * p.nj * p.nz, 0, (float) thr);// min size was 5

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
