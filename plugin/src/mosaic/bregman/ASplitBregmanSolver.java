package mosaic.bregman;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ZProjector;
import ij.plugin.filter.EDM;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_2D;


class ASplitBregmanSolver {

    protected Tools LocalTools;
    protected DoubleDCT_2D dct2d;
    protected double totaltime = 0;
    private boolean StopFlag;

    protected ArrayList<Region> regionsvoronoi;
    private ArrayList<Region> regionslistr;
    protected MasksDisplay md;
    protected double[][][] image;
    protected double weight;
    private double norm;
    protected double[][][][] speedData; // used only

    protected double[][] eigenLaplacian;

    protected int stepk;

    protected int channel;
    protected double[][][][] w1k;
    protected double[][][][] w3k;

    protected double[][][][] w2xk;
    protected double[][][][] w2yk;

    protected double[][][][] w3kbest;
    private int iw3kbest;
    protected double[][][][] b2xk;
    protected double[][][][] b2yk;

    protected double[][][][] b1k;
    protected double[][][][] b3k;

    protected int[][][] maxmask;

    protected double[][][][] temp1;
    protected double[][][][] temp2;
    protected double[][][][] temp3;
    protected double[][][][] temp4;

    protected float[][][][] Ri;
    protected float[][][][] Ro;

    protected double[] energytab;
    protected double[] normtab;
    protected int ni, nj, nz;
    protected int nl;
    protected double energy, lastenergy;
    protected double bestNrj;
    protected Parameters p;
    private RegionStatisticsSolver RSS;
    private AnalysePatch Ap;

    ASplitBregmanSolver(Parameters params, double[][][] image, double[][][][] speedData, double[][][][] mask, MasksDisplay md, int channel, AnalysePatch ap) {
        this(params, image, speedData, mask, md, channel);
        this.Ap = ap;
    }

    ASplitBregmanSolver(Parameters params, double[][][] image, double[][][][] speedData, double[][][][] mask, MasksDisplay md, int channel) {
        // initialization

        this.LocalTools = new Tools(params.ni, params.nj, params.nz);
        this.channel = channel;
        bestNrj = Double.MAX_VALUE;
        this.p = params;
        this.ni = params.ni;
        this.nj = params.nj;
        this.nz = params.nz;

        this.nl = p.nlevels;

        this.energytab = new double[nl];
        this.normtab = new double[nl];

        this.StopFlag = false;
        this.md = md;
        // IJ.log("nlevels asplit" + p.nlevels);
        dct2d = new DoubleDCT_2D(ni, nj);
        // IJ.log("nlevels asplit" + p.nlevels);
        // speedData used as temp tab

        // if (nz>1)dct3d= new DoubleDCT_3D(nz,ni,nj);
        this.image = image;
        this.speedData = speedData;// used only for NRegions and two regions
                                   // without PSF
        // this.mask=mask;

        this.eigenLaplacian = new double[ni][nj];

        // allocate
        this.w1k = new double[nl][nz][ni][nj];
        this.w3k = new double[nl][nz][ni][nj];
        this.w3kbest = new double[nl][nz][ni][nj];
        // this.w3kp= new double [nl] [nz] [ni] [nj];

        this.b2xk = new double[nl][nz][ni][nj];
        this.b2yk = new double[nl][nz][ni][nj];

        this.b1k = new double[nl][nz][ni][nj];
        this.b3k = new double[nl][nz][ni][nj];

        this.w2xk = new double[nl][nz][ni][nj];
        this.w2yk = new double[nl][1][ni][nj];// save memory w2yk not used in 3d
                                              // case

        this.Ri = new float[nl][nz][ni][nj];
        this.Ro = new float[nl][nz][ni][nj];

        this.maxmask = new int[nz][ni][nj];

        int nzmin;
        if (nz > 1) {
            nzmin = Math.max(7, nz);
        }
        else
            nzmin = nz;

        int nimin = Math.max(7, ni);
        int njmin = Math.max(7, nj);

        this.temp1 = new double[nl][nzmin][nimin][njmin];
        // IJ.log(" ni " + ni +" nj" +nj+ " nl " +nl+ " nzmin "+nzmin);
        this.temp2 = new double[nl][nzmin][nimin][njmin];
        this.temp3 = new double[nl][nzmin][nimin][njmin];
        this.temp4 = new double[nl][nzmin][nimin][njmin];// hack to make it work
                                                         // : used for eigenPSF
                                                         // 7*7*7

        // temp4, temp5, speedData
        this.RSS = new RegionStatisticsSolver(temp1[0], temp2[0], temp3[0], image, 10, p);

        // precompute eigenlaplacian
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                this.eigenLaplacian[i][j] = 2 + (2 - 2 * Math.cos((j) * Math.PI / (nj)) + (2 - 2 * Math.cos((i) * Math.PI / (ni))));
            }
        }

        for (int i = 0; i < nl; i++) {
            // temp1=w2xk temp2=w2yk
            LocalTools.fgradx2D(temp1[i], mask[i]);
            LocalTools.fgrady2D(temp2[i], mask[i]);

            LocalTools.copytab(w1k[i], mask[i]);
            LocalTools.copytab(w3k[i], mask[i]);
        }

        if (p.RSSinit) {
            RSS.eval(w3k[0]);

            p.cl[0] = RSS.betaMLEout;
            p.cl[1] = RSS.betaMLEin;

            IJ.log(String.format("Photometry init:%n background %7.2e %n foreground %7.2e", RSS.betaMLEout, RSS.betaMLEin));
        }

        if (p.remask) {
            LocalTools.createmask(mask, image, p.cl);
            md.display2regionsnewd(mask[0][0], "remask init", 0);
        }

        for (int l = 0; l < nl; l++) {
            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        Ro[l][z][i][j] = (float) (p.cl[0]);
                        Ri[l][z][i][j] = (float) (p.cl[1]);
                    }
                }
            }
        }
    }

    // first run
    void first_run() throws InterruptedException {

        // initialize variables
        for (int l = 0; l < nl; l++) {
            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        b2xk[l][z][i][j] = 0;
                        b2yk[l][z][i][j] = 0;
                        b1k[l][z][i][j] = 0;
                        b3k[l][z][i][j] = 0;
                    }
                }
            }
        }

        stepk = 0;
        totaltime = 0;
        int modulo = p.dispEmodulo;

        if (p.firstphase) {
            IJ.showStatus("Computing segmentation");
            IJ.showProgress(0.0);
        }

        double lastenergy_mod = Double.MAX_VALUE;

        while (stepk < p.max_nsb && !StopFlag) {
            // Bregman step
            step();

            if (energy < bestNrj) {
                LocalTools.copytab(w3kbest[0], w3k[0]);
                iw3kbest = stepk;
                bestNrj = energy;
            }
            if (stepk % modulo == 0 || stepk == p.max_nsb - 1) {
                // IJ.log(String.format("Ediff %d : %7.6e", stepk,
                // Math.abs((energy-lastenergy)/lastenergy)));
                if (Math.abs((energy - lastenergy) / lastenergy) < p.tol) {
                    StopFlag = true;
                    if (p.livedisplay && p.firstphase) {
                        IJ.log("energy stop");
                    }
                }

                // experiment to speedup we stop if the energy increase after
                // evaluation modulo step
                if (p.firstphase == true) {
                    if (stepk % modulo == 0 && p.fastsquassh == true) {
                        if ((energy - lastenergy_mod) > 0)
                            StopFlag = true;
                        else
                            lastenergy_mod = energy;
                    }
                }

            }
            lastenergy = energy;
            // energy output

            if (stepk % modulo == 0 && p.livedisplay && p.firstphase) IJ.log(String.format("Energy at step %d : %7.6e", stepk, energy));
            if ((stepk + 1) % p.RSSmodulo == 0 && stepk != 0) {
                RSS.eval(w3k[0]);
                p.cl[0] = RSS.betaMLEout;
                p.cl[1] = RSS.betaMLEin;
                this.init();
                IJ.log(String.format("Photometry :%n backgroung %10.8e %n foreground %10.8e", RSS.betaMLEout, RSS.betaMLEin));
            }

            if (!p.firstphase && p.mode_intensity == 0 && (stepk == 40 || stepk == 70)) { 
                // && new mode automatic intensity
                // && p.mode_intensity==0
                // 
                // do it all the time

                Ap.find_best_thresh_and_int(w3k[0]);
                p.cl[0] = Math.max(0, Ap.cout);
                p.cl[1] = Math.max(0.75 * (Ap.firstminval - Ap.intmin) / (Ap.intmax - Ap.intmin), Ap.cin);// lower
                                                                                                          // bound
                                                                                                          // withg
                                                                                                          // some
                                                                                                          // margin
                // p.cl[1]=Ap.cin;
                this.init();
                if (p.debug) {
                    IJ.log("region" + Ap.r.value + " pcout" + p.cl[1]);
                    IJ.log("region" + Ap.r.value + String.format(" Photometry :%n backgroung %10.8e %n foreground %10.8e", Ap.cout, Ap.cin));
                }
            }

            if (p.RegionsIntensitymodulo == stepk && stepk != 0) {
                IJ.log("best energy at" + iw3kbest);
                this.regions_intensity(w3kbest[0]);
                this.init();

            }

            stepk++;
            if (stepk % modulo == 0 && p.firstphase) IJ.showStatus("Computing segmentation  " + Tools.round((((double) 50 * stepk) / (p.max_nsb - 1)), 2) + "%");

            if (p.firstphase) IJ.showProgress(0.5 * (stepk) / (p.max_nsb - 1));
        }

        if (iw3kbest < 50) { // use what iteration threshold ?
            int iw3kbestold = iw3kbest;
            LocalTools.copytab(w3kbest[0], w3k[0]);
            iw3kbest = stepk - 1;
            bestNrj = energy;
            if (p.livedisplay && p.firstphase) IJ.log("Warning : increasing energy. Last computed mask is then used for first phase object segmentation." + iw3kbestold);
        }

        if (p.findregionthresh) {
            this.regions_intensity_findthresh(w3kbest[0]);
        }

        if (!p.mode_voronoi2) IJ.showStatus("Segmentation Done");
        if (p.livedisplay && p.firstphase) {
            if (p.nlevels <= 2 && p.nz == 1) md.display2regions(w3kbest[0][0], "Mask", channel);
        }
        if (p.livedisplay && p.nz > 1 && p.nlevels <= 2 && p.firstphase) md.display2regions3D(w3kbest[0], "Mask", channel);

        if (p.livedisplay) {
            if (p.nlevels > 2) md.display(maxmask, "Masks");
        }
        if (p.livedisplay && p.firstphase) {
            IJ.log("Best energy : " + Tools.round(bestNrj, 3) + ", found at step " + iw3kbest);
            IJ.log("Total phase one time: " + totaltime / 1000 + "s");
        }
    }

    protected void step() throws InterruptedException {
        long lStartTime = new Date().getTime(); // start time
        CountDownLatch RegionsTasksDoneSignal = new CountDownLatch(nl);// subprob
                                                                       // 1 and
                                                                       // 3
        CountDownLatch UkDoneSignal = new CountDownLatch(nl);
        CountDownLatch W3kDoneSignal = new CountDownLatch(1);

        for (int l = 0; l < nl; l++) {
            new Thread(new SingleRegionTask(RegionsTasksDoneSignal, UkDoneSignal, W3kDoneSignal, l, channel, this, LocalTools)).start();
        }

        // %-- w3k subproblem
        UkDoneSignal.await();

        ProjectSimplexSpeed.project(w3k, temp4, ni, nj, nl);

        W3kDoneSignal.countDown();
        RegionsTasksDoneSignal.await();

        LocalTools.max_mask(maxmask, w3k);

        // number of != pixels in max mask (stop criterion ?)
        // int diff;
        // diff=Tools.computediff(maxmask, maxmask0);
        // IJ.log("diff in pixels : " + diff);
        // Tools.copytab(maxmask0, maxmask);

        // doneSignal2.await();
        norm = 0;
        energy = 0;
        for (int l = 0; l < nl; l++) {
            energy += energytab[l];
            norm = Math.max(norm, normtab[l]);
        }

        if (p.livedisplay) md.display(maxmask, "Masks");

        long lEndTime = new Date().getTime(); // end time

        long difference = lEndTime - lStartTime; // check different
        totaltime += difference;
        // IJ.log("Elapsed milliseconds: " + difference);

    }

    protected void init() {
        if (p.debug) IJ.log("init super");
    }

    protected void compute_eigenPSF() {
    }

    private void regions_intensity(double[][][] mask) {
        // short [] [] [] regions;
        // ArrayList<Region> regionslistr;
        double thresh = 0.4;

        ImagePlus mask_im = new ImagePlus();
        ImageStack mask_ims = new ImageStack(p.ni, p.nj);
        for (int z = 0; z < nz; z++) {
            byte[] mask_bytes = new byte[p.ni * p.nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (mask[z][i][j] > thresh)
                        mask_bytes[j * p.ni + i] = 0;
                    else
                        mask_bytes[j * p.ni + i] = (byte) 255;
                }
            }

            ByteProcessor bp = new ByteProcessor(p.ni, p.nj);
            bp.setPixels(mask_bytes);
            mask_ims.addSlice("", bp);

        }
        mask_im.setStack("Regions", mask_ims);

        IJ.run(mask_im, "Voronoi", "");
        IJ.run(mask_im, "Invert", "");
        IJ.run(mask_im, "3-3-2 RGB", "");
        mask_im.show("Voronoi");

        double thr = 254;
        FindConnectedRegions fcr = new FindConnectedRegions(mask_im);

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    Ri[0][z][i][j] = (float) thr;
                }
            }
        }

        fcr.run(thr, 0, 512 * 512, 2, 0, Ri[0], false, false);

        // regions=fcr.tempres;
        this.regionslistr = fcr.results;
        int na = regionslistr.size();

        double total = Analysis.totalsize(regionslistr);
        IJ.log(na + " Voronoi1 cells found, total area : " + Tools.round(total, 2) + " pixels.");

        RSS.cluster_region(Ri[0], Ro[0], regionslistr);
    }

    void regions_intensity_findthresh(double[][][] mask) {
        // short [] [] [] regions;
        ArrayList<Region> regionslist;
        double thresh;
        if (channel == 0)
            thresh = p.regionthresh;// 0.19
        else
            thresh = p.regionthreshy;
        // IJ.log("thresh" + thresh);
        ImagePlus mask_im = new ImagePlus();
        ImageStack mask_ims = new ImageStack(p.ni, p.nj);

        // construct mask as an imageplus
        for (int z = 0; z < nz; z++) {
            float[] mask_float = new float[p.ni * p.nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    mask_float[j * p.ni + i] = (float) mask[z][i][j];
                }
            }
            FloatProcessor fp = new FloatProcessor(p.ni, p.nj);
            fp.setPixels(mask_float);
            mask_ims.addSlice("", fp);
        }
        mask_im.setStack("test", mask_ims);
        // mask_im.show("test");

        // project mask on single slice (maximum values)
        ZProjector proj = new ZProjector(mask_im);
        proj.setImage(mask_im);
        proj.setStartSlice(1);
        proj.setStopSlice(nz);
        proj.setMethod(ZProjector.MAX_METHOD);
        proj.doProjection();
        mask_im = proj.getProjection();
        IJ.showStatus("Computing segmentation  " + 52 + "%");
        IJ.showProgress(0.52);

        // threshold mask
        byte[] mask_bytes = new byte[p.ni * p.nj];
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                if (((int) (255 * mask_im.getProcessor().getPixelValue(i, j))) > 255 * thresh)
                    // weird conversion to have same thing than in find
                    // connected regions
                    mask_bytes[j * p.ni + i] = 0;
                else
                    mask_bytes[j * p.ni + i] = (byte) 255;
            }
        }
        ByteProcessor bp = new ByteProcessor(p.ni, p.nj);
        bp.setPixels(mask_bytes);
        mask_im.setProcessor("Voronoi", bp);

        // do voronoi in 2D on Z projection

        // perform voronoi

        /*
         * Here we compute the Voronoi segmentation starting from the threshold
         * mask
         */

        EDM filtEDM = new EDM();
        filtEDM.setup("voronoi", mask_im);
        filtEDM.run(mask_im.getProcessor());
        mask_im.getProcessor().invert();
        IJ.showStatus("Computing segmentation  " + 53 + "%");
        IJ.showProgress(0.53);

        // expand Voronoi in 3D
        ImageStack mask_ims3 = new ImageStack(p.ni, p.nj);
        for (int z = 0; z < nz; z++) {
            byte[] mask_bytes3 = new byte[p.ni * p.nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    mask_bytes3[j * p.ni + i] = (byte) mask_im.getProcessor().getPixel(i, j);//
                    // mask_bytes3[j * p.ni + i]= (byte) (bp_water.getPixel(i,j)
                    // & 0xFF);
                }
            }
            ByteProcessor bp3 = new ByteProcessor(p.ni, p.nj);
            bp3.setPixels(mask_bytes3);
            mask_ims3.addSlice("", bp3);
        }
        mask_im.setStack("Voronoi", mask_ims3);

        // Here we are elaborating the Voronoi mask to get a nice subdivision

        // mask_im.duplicate().show();
        double thr = 254;
        FindConnectedRegions fcr = new FindConnectedRegions(mask_im);

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    Ri[0][z][i][j] = (float) thr;
                }
            }
        }

        if (p.mode_voronoi2)
            fcr.run(thr, 1, p.ni * p.nj * p.nz, 0, 0, Ri[0], false, false);// min size was 5
        else
            fcr.run(thr, 1, p.ni * p.nj * p.nz, 5, 0, Ri[0], false, false);// min size was 5

        if (p.dispvoronoi) {
            if (nz == 1)
                md.display2regions(w3kbest[0][0], "Mask", channel);
            else
                md.display2regions3D(w3kbest[0], "Mask", channel);

            IJ.setThreshold(mask_im, 0, 254);
            IJ.run(mask_im, "Convert to Mask", "stack");

            if (channel == 0)
                IJ.selectWindow("Mask X");
            else
                IJ.selectWindow("Mask Y");
            IJ.run("8-bit", "stack");
            ImagePlus imp2 = IJ.getImage();

            // add images
            ImageStack mask_ims2 = new ImageStack(p.ni, p.nj);
            for (int z = 0; z < nz; z++) {
                // imp1.setSlice(z+1);
                imp2.setSlice(z + 1);
                byte[] mask_byte2 = new byte[p.ni * p.nj];
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        mask_byte2[j * p.ni + i] = (byte) Math.min((mask_im.getProcessor().getPixel(i, j) + imp2.getProcessor().getPixel(i, j)), 255);
                    }
                }
                ByteProcessor bp2 = new ByteProcessor(p.ni, p.nj);
                bp2.setPixels(mask_byte2);
                mask_ims2.addSlice("", bp2);
            }
            // replace imageplus with additon of both

            if (channel == 0) {
                mask_im.setStack("Voronoi X", mask_ims2);

                // IJ.run(imp3, "Invert LUT","stack");
                mask_im.show("Voronoi X");
                IJ.selectWindow("Mask X");
            }

            else {
                mask_im.setStack("Voronoi Y", mask_ims2);

                // IJ.run(imp3, "Invert LUT","stack");
                mask_im.show("Voronoi Y");
                IJ.selectWindow("Mask Y");
            }

        }

        // regions=fcr.tempres;
        regionslist = fcr.results;
        regionsvoronoi = regionslist;
        int na = regionslist.size();

        double total = Analysis.totalsize(regionslist);
        if (p.dispvoronoi) IJ.log(na + " Voronoi cells found, total area : " + Tools.round(total, 2) + " pixels.");

        // use Ri to store voronoi regions indices
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    Ri[0][z][i][j] = 255;
                }
            }
        }

        RegionStatisticsSolver RSS2;
        if (p.mode_voronoi2) {
            RSS2 = new RegionStatisticsSolver(temp1[0], temp2[0], temp3[0], image, 10, p);
        }
        else
            RSS2 = new RegionStatisticsSolver(temp1[0], temp2[0], temp3[0], w3kbest[0], 10, p);

        long lStartTime = new Date().getTime(); // start time

        if (p.mode_voronoi2)
            RSS2.cluster_region_voronoi2(Ri[0], Ro[0], regionslist);
        else
            RSS2.cluster_region(Ri[0], Ro[0], regionslist);

        long lEndTime = new Date().getTime(); // end time

        IJ.showStatus("Computing segmentation  " + 54 + "%");
        IJ.showProgress(0.54);

        long difference = lEndTime - lStartTime; // check different
        totaltime += difference;
        if (p.dispvoronoi) {
            if (p.nz == 1) {
                md.display2regionsnew(Ri[0][0], "Regions thresholds", channel, true);
            }
            else {
                md.display2regions3Dnew(Ri[0], "Regions thresholds", channel);
            }
        }
    }
}
