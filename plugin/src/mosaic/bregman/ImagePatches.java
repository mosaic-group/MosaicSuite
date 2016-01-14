package mosaic.bregman;


import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ij.IJ;
import ij.WindowManager;
import mosaic.bregman.GUI.GenericGUI;
import mosaic.utils.ArrayOps;


class ImagePatches {

    private int jobs_done;
    private int nb_jobs;
    private final int osxy;
    private final int osz; // oversampling
    private final int sx;
    private final int sy;
    private final int sz;// size of full image with oversampling
    private final Parameters p;
    public final short[][][] regions_refined;
    private final double[][][] image;
    final double[][][] w3kbest;
    private byte[] imagecolor_c1;

    private final double max;
    private final double min;
    public ArrayList<Region> regionslist_refined;
    private final ArrayList<Region> globalList;
    private final int channel;

    public ImagePatches(Parameters pa, ArrayList<Region> regionslist, double[][][] imagei, int channeli, double[][][] w3k, double min, double max) {
        if (!pa.subpixel) {
            pa.oversampling2ndstep = 1;
            pa.interpolation = 1;
        }
        else {
            pa.oversampling2ndstep = 2;
        }

        this.w3kbest = w3k;

        this.channel = channeli;
        this.p = pa;
        this.osxy = p.oversampling2ndstep * pa.interpolation;
        this.globalList = new ArrayList<Region>();
        this.regionslist_refined = regionslist;
        this.image = imagei;
        this.sx = p.ni * pa.oversampling2ndstep * pa.interpolation;
        this.sy = p.nj * pa.oversampling2ndstep * pa.interpolation;
        this.jobs_done = 0;
        this.max = max;
        this.min = min;

        if (p.nz == 1) {
            this.sz = 1;
            this.osz = 1;
        }
        else {
            this.sz = p.nz * p.oversampling2ndstep * pa.interpolation;
            this.osz = p.oversampling2ndstep * pa.interpolation;
        }

        regions_refined = new short[sz][sx][sy];

        if (p.dispint) {
            imagecolor_c1 = new byte[sz * sx * sy * 3]; // add fill background
            int b0, b1, b2;
            b0 = (int) Math.min(255, 255 * Analysis.p.betaMLEoutdefault);
            b1 = (int) Math.min(255, 255 * Math.sqrt(Analysis.p.betaMLEoutdefault));
            b2 = (int) Math.min(255, 255 * Math.pow(Analysis.p.betaMLEoutdefault, 2));

            // set all to background
            for (int z = 0; z < sz; z++) {
                for (int i = 0; i < sx; i++) {
                    final int t = z * sx * sy * 3 + i * sy * 3;
                    for (int j = 0; j < sy; j++) {
                        imagecolor_c1[t + j * 3 + 0] = (byte) b0; // Red
                        imagecolor_c1[t + j * 3 + 1] = (byte) b1; // Green
                        imagecolor_c1[t + j * 3 + 2] = (byte) b2; // Blue
                    }
                }
            }
        }
        ArrayOps.fill(regions_refined, (short) 0);
    }

    public void run() {
        distribute_regions();
    }

    /**
     * Patch creation, distribution and assembly
     */
    private void distribute_regions() {
        // assuming rvoronoi and regionslists (objects) in same order (and same length)

        final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
        ThreadPoolExecutor threadPool;
        if (p.debug == true) {
            threadPool = new ThreadPoolExecutor(1, 1, 1, TimeUnit.DAYS, queue);
        }
        else {
            threadPool = new ThreadPoolExecutor(/* p.nthreads */1, /* p.nthreads */1, 1, TimeUnit.DAYS, queue);
        }

        nb_jobs = regionslist_refined.size();
        AnalysePatch ap;
        for (final Region r : regionslist_refined) {
            if (p.interpolation > 1) {
                p.subpixel = true;
            }
            if (p.subpixel) {
                p.oversampling2ndstep = 2;
            }
            else {
                p.oversampling2ndstep = 1;
            }
            ap = new AnalysePatch(image, r, p, p.oversampling2ndstep, channel, regions_refined, this);
            threadPool.execute(ap);
        }

        threadPool.shutdown();

        try {
            threadPool.awaitTermination(1, TimeUnit.DAYS);
        }
        catch (final InterruptedException ex) {
        }

        regionslist_refined = globalList;

        // final LinkedBlockingQueue<Runnable> queue2 = new
        // LinkedBlockingQueue<Runnable>();
        final ThreadPoolExecutor threadPool2 = new ThreadPoolExecutor(1, 1, 1, TimeUnit.DAYS, queue);

        // calculate regions intensities
        for (final Region r : regionslist_refined) {
            ObjectProperties Op = new ObjectProperties(image, r, sx, sy, sz, p, osxy, osz, imagecolor_c1, regions_refined);
            threadPool2.execute(Op);
        }

        threadPool2.shutdown();
        try {
            threadPool2.awaitTermination(1, TimeUnit.DAYS);
        }
        catch (final InterruptedException ex) {}

        // here we analyse the patch
        // if we have a big region with intensity near the background
        // kill that region
        boolean changed = false;

        final ArrayList<Region> regionslist_refined_filter = new ArrayList<Region>();

        for (final Region r : regionslist_refined) {
            if (r.intensity * (max - min) + min > p.min_region_filter_intensities) {
                regionslist_refined_filter.add(r);
            }
            else {
                changed = true;
            }
        }

        regionslist_refined = regionslist_refined_filter;

        // if changed, reassemble
        if (changed == true) {
            ArrayOps.fill(regions_refined, (short) 0);
            assemble(regionslist_refined, regions_refined);
        }

        regionslist_refined = regionslist_refined_filter;

        final int no = regionslist_refined.size();
        if (channel == 0) {
            IJ.log(no + " objects found in X.");
            final Frame frame = WindowManager.getFrame("Log");
            if (frame != null) {
                GenericGUI.setwindowlocation(100, 680, frame);
            }
        }
        else {
            IJ.log(no + " objects found in Y.");
        }
    }

    /**
     * Assemble the result
     * @param regionslist_refined List of regions to assemble
     * @param regions_refined regions refined
     */
    static public void assemble(Collection<Region> regionslist_refined, short[][][] regions_refined) {
        for (final Region r : regionslist_refined) {
            for (final Pix v : r.pixels) {
                regions_refined[v.pz][v.px][v.py] = (short) r.value;
            }
        }
    }

    public synchronized void addRegionstoList(ArrayList<Region> localList) {
        int index;// build index of region (will be r.value)
        for (final Region r : localList) {
            index = globalList.size() + 1;
            r.value = index;
            globalList.add(r);
        }

        jobs_done++;

        IJ.showStatus("Computing segmentation  " + Tools.round(55 + (45 * ((double) jobs_done) / (nb_jobs)), 2) + "%");
        IJ.showProgress(0.55 + 0.45 * (jobs_done) / (nb_jobs));
    }
}
