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
    private ArrayList<Region> regionslistr;
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
    private double lastenergy; // TODO: It should be initialized to some value.
    private double bestNrj;
    protected final Parameters p;
    private final RegionStatisticsSolver RSS;
    private AnalysePatch Ap = null;

    final int levelOfMask = 0; // use mask etc of level 0
    
    double c0, c1;
    public final double energytab2[];
    
    ASplitBregmanSolver(Parameters params, double[][][] image, double[][][] mask, MasksDisplay md, int channel, AnalysePatch ap) {
        this(params, image, mask, md, channel);
        this.Ap = ap;
    }

    ASplitBregmanSolver(Parameters params, double[][][] image, double[][][] mask, MasksDisplay md, int channel) {
        this.LocalTools = new Tools(params.ni, params.nj, params.nz);
        this.channel = channel;
        bestNrj = Double.MAX_VALUE;
        this.p = params;
        this.ni = params.ni;
        this.nj = params.nj;
        this.nz = params.nz;

        // Beta MLE in and out
        this.c0 = params.cl[0];
        this.c1 = params.cl[1];
        
        this.energytab2 = new double[p.nthreads];
        
        this.md = md;

        this.image = image;

        this.w1k = new double[nz][ni][nj];
        this.w3k = new double[nz][ni][nj];
        this.w3kbest = new double[nz][ni][nj];

        this.b2xk = new double[nz][ni][nj];
        this.b2yk = new double[nz][ni][nj];

        this.b1k = new double[nz][ni][nj];
        this.b3k = new double[nz][ni][nj];

        this.w2xk = new double[nz][ni][nj];
        this.w2yk = new double[1][ni][nj];// save memory w2yk not used in 3d case

        this.Ri = new float[nz][ni][nj];
        this.Ro = new float[nz][ni][nj];

        int nzmin = (nz > 1) ?  Math.max(7, nz) : nz;

        final int nimin = Math.max(7, ni);
        final int njmin = Math.max(7, nj);

        this.temp1 = new double[nzmin][nimin][njmin];
        this.temp2 = new double[nzmin][nimin][njmin];
        this.temp3 = new double[nzmin][nimin][njmin];
        this.temp4 = new double[nzmin][nimin][njmin];
        
        this.RSS = new RegionStatisticsSolver(temp1, temp2, temp3, image, 10, p);

        // precompute eigenlaplacian
        this.eigenLaplacian = new double[ni][nj];
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                this.eigenLaplacian[i][j] = 2 + (2 - 2 * Math.cos((j) * Math.PI / (nj)) + (2 - 2 * Math.cos((i) * Math.PI / (ni))));
            }
        }

        LocalTools.fgradx2D(temp1, mask);
        LocalTools.fgrady2D(temp2, mask);

        LocalTools.copytab(w1k, mask);
        LocalTools.copytab(w3k, mask);

        if (p.RSSinit) {
            RSS.eval(w3k);

            p.cl[0] = RSS.betaMLEout;
            p.cl[1] = RSS.betaMLEin;

            IJ.log(String.format("Photometry init:%n background %7.2e %n foreground %7.2e", RSS.betaMLEout, RSS.betaMLEin));
        }

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
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    b2xk[z][i][j] = 0;
                    b2yk[z][i][j] = 0;
                    b1k[z][i][j] = 0;
                    b3k[z][i][j] = 0;
                }
            }
        }

        stepk = 0;
        final int modulo = 10;

        if (p.firstphase) {
            IJ.showStatus("Computing segmentation");
            IJ.showProgress(0.0);
        }

        boolean StopFlag = false;
        int iw3kbest = 0;
        while (stepk < p.max_nsb && !StopFlag) {
            // Bregman step
            step();

            if (energy < bestNrj) {
                LocalTools.copytab(w3kbest, w3k);
                iw3kbest = stepk;
                bestNrj = energy;
            }
            if (stepk % modulo == 0 || stepk == p.max_nsb - 1) {
                if (Math.abs((energy - lastenergy) / lastenergy) < p.tol) {
                    StopFlag = true;
                    if (p.livedisplay && p.firstphase) {
                        IJ.log("energy stop");
                    }
                }
            }
            lastenergy = energy;

            if (stepk % modulo == 0 && p.livedisplay && p.firstphase) {
                IJ.log(String.format("Energy at step %d : %7.6e", stepk, energy));
            }
            if ((stepk + 1) % p.RSSmodulo == 0 && stepk != 0) {
                RSS.eval(w3k);
                p.cl[0] = RSS.betaMLEout;
                p.cl[1] = RSS.betaMLEin;
                this.init();
                IJ.log(String.format("Photometry :%n backgroung %10.8e %n foreground %10.8e", RSS.betaMLEout, RSS.betaMLEin));
            }

            if (!p.firstphase && p.mode_intensity == 0 && (stepk == 40 || stepk == 70)) {
                Ap.find_best_thresh_and_int(w3k);
                p.cl[0] = Math.max(0, Ap.cout);
                // lower bound withg some margin
                p.cl[1] = Math.max(0.75 * (Ap.firstminval - Ap.iIntensityMin) / (Ap.iIntensityMax - Ap.iIntensityMin), Ap.cin);
                this.init();
                if (p.debug) {
                    IJ.log("region" + Ap.iInputRegion.value + " pcout" + p.cl[1]);
                    IJ.log("region" + Ap.iInputRegion.value + String.format(" Photometry :%n backgroung %10.8e %n foreground %10.8e", Ap.cout, Ap.cin));
                }
            }

            if (p.RegionsIntensitymodulo == stepk && stepk != 0) {
                IJ.log("best energy at" + iw3kbest);
                this.regions_intensity(w3kbest);
                this.init();
            }

            stepk++;
            if (stepk % modulo == 0 && p.firstphase) {
                IJ.showStatus("Computing segmentation  " + Tools.round((((double) 50 * stepk) / (p.max_nsb - 1)), 2) + "%");
            }

            if (p.firstphase) {
                IJ.showProgress(0.5 * (stepk) / (p.max_nsb - 1));
            }
        }

        if (iw3kbest < 50) { // use what iteration threshold ?
            final int iw3kbestold = iw3kbest;
            LocalTools.copytab(w3kbest, w3k);
            iw3kbest = stepk - 1;
            bestNrj = energy;
            if (p.livedisplay && p.firstphase) {
                IJ.log("Warning : increasing energy. Last computed mask is then used for first phase object segmentation." + iw3kbestold);
            }
        }

        if (p.findregionthresh) {
            this.regions_intensity_findthresh(w3kbest);
        }

        if (p.livedisplay) {
            if (p.firstphase) {
                if (p.nlevels <= 2 && p.nz == 1) {
                    md.display2regions(w3kbest[0], "Mask", channel);
                }
                if (p.nlevels <= 2 && p.nz > 1) {
                    md.display2regions3D(w3kbest, "Mask__", channel);
                }
                IJ.log("Best energy : " + Tools.round(bestNrj, 3) + ", found at step " + iw3kbest);
            }
        }
    }
    
    abstract protected void step() throws InterruptedException;
    abstract protected void init();

    private void regions_intensity(double[][][] mask) {
        final double thresh = 0.4;

        final ImageStack mask_ims = new ImageStack(p.ni, p.nj);
        for (int z = 0; z < nz; z++) {
            final byte[] mask_bytes = new byte[p.ni * p.nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (mask[z][i][j] > thresh) {
                        mask_bytes[j * p.ni + i] = 0;
                    }
                    else {
                        mask_bytes[j * p.ni + i] = (byte) 255;
                    }
                }
            }

            final ByteProcessor bp = new ByteProcessor(p.ni, p.nj);
            bp.setPixels(mask_bytes);
            mask_ims.addSlice("", bp);
        }
        
        final ImagePlus mask_im = new ImagePlus("Regions", mask_ims);
        IJ.run(mask_im, "Voronoi", "");
        IJ.run(mask_im, "Invert", "");
        IJ.run(mask_im, "3-3-2 RGB", "");
        mask_im.show("Voronoi");

        final double thr = 254;
        final FindConnectedRegions fcr = new FindConnectedRegions(mask_im);
        ArrayOps.fill(Ri, (float) thr);

        fcr.run(thr, 512 * 512, 2, (float) thr);

        this.regionslistr = fcr.results;
        final int na = regionslistr.size();

        final double total = Analysis.totalsize(regionslistr);
        IJ.log(na + " Voronoi1 cells found, total area : " + Tools.round(total, 2) + " pixels.");

        RSS.cluster_region(Ri, regionslistr);
    }

    void regions_intensity_findthresh(double[][][] mask) {
        double thresh = (channel == 0) ? p.regionthresh : p.regionthreshy;

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
        ArrayOps.fill(Ri, (float) thr);

        fcr.run(thr, p.ni * p.nj * p.nz, 0, (float) thr);// min size was 5

        if (p.dispvoronoi) {
            if (nz == 1) {
                md.display2regions(w3kbest[0], "Mask", channel);
            }
            else {
                md.display2regions3D(w3kbest, "Mask", channel);
            }

            IJ.setThreshold(mask_im, 0, 254);
            IJ.run(mask_im, "Convert to Mask", "stack");

            if (channel == 0) {
                IJ.selectWindow("Mask X");
            }
            else {
                IJ.selectWindow("Mask Y");
            }
            IJ.run("8-bit", "stack");
            final ImagePlus imp2 = IJ.getImage();

            // add images
            final ImageStack mask_ims2 = new ImageStack(p.ni, p.nj);
            for (int z = 0; z < nz; z++) {
                // imp1.setSlice(z+1);
                imp2.setSlice(z + 1);
                final byte[] mask_byte2 = new byte[p.ni * p.nj];
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        mask_byte2[j * p.ni + i] = (byte) Math.min((mask_im.getProcessor().getPixel(i, j) + imp2.getProcessor().getPixel(i, j)), 255);
                    }
                }
                final ByteProcessor bp2 = new ByteProcessor(p.ni, p.nj);
                bp2.setPixels(mask_byte2);
                mask_ims2.addSlice("", bp2);
            }
            // replace imageplus with additon of both
            if (channel == 0) {
                mask_im.setStack("Voronoi X", mask_ims2);
                mask_im.show("Voronoi X");
                IJ.selectWindow("Mask X");
            }

            else {
                mask_im.setStack("Voronoi Y", mask_ims2);
                mask_im.show("Voronoi Y");
                IJ.selectWindow("Mask Y");
            }

        }

        ArrayList<Region> regionslist = fcr.results;
        regionsvoronoi = regionslist;
        final int na = regionslist.size();

        final double total = Analysis.totalsize(regionslist);
        if (p.dispvoronoi) {
            IJ.log(na + " Voronoi cells found, total area : " + Tools.round(total, 2) + " pixels.");
        }

        // use Ri to store voronoi regions indices
        ArrayOps.fill(Ri, 255);
        RegionStatisticsSolver.cluster_region_voronoi2(Ri, regionslist);

        IJ.showStatus("Computing segmentation  " + 54 + "%");
        IJ.showProgress(0.54);

        if (p.dispvoronoi) {
            if (p.nz == 1) {
                md.display2regionsnew(Ri[0], "Regions thresholds", channel);
            }
            else {
                md.display2regions3Dnew(Ri, "Regions thresholds", channel);
            }
        }
    }
}
