package mosaic.bregman;


import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
//import java.util.Date;
import java.util.Iterator;
//import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


class ImagePatches {

    ImagePlus regsresultx;
    ImagePlus regsresulty;
    int interp;
    int interpz;
    int jobs_done;
    int nb_jobs;
    // reg
    int osxy;
    int osz; // oversampling
    int sx;
    int sy;
    int sz;// size of full image with oversampling
    Parameters p;
    public short[][][] regions_refined;
    double[][][] image;
    double[][][] w3kbest;
    byte[] imagecolor_c1;// for display ints = new int [dz][di][dj][3];

    double max;
    double min;
    public ArrayList<Region> regionslist_refined;
    private final ArrayList<Region> globalList;
    int channel;
    boolean fcallx, fcally;

    public ImagePatches(Parameters pa, ArrayList<Region> regionslist, double[][][] imagei, int channeli, double[][][] w3k, double min, double max) {
        if (!pa.subpixel) {
            pa.oversampling2ndstep = 1;
            pa.interpolation = 1;
        }
        else {
            pa.oversampling2ndstep = pa.overs;
        }
        this.regsresulty = new ImagePlus();
        this.regsresultx = new ImagePlus();
        this.w3kbest = w3k;

        this.channel = channeli;
        this.p = pa;
        this.interp = pa.interpolation;
        this.osxy = p.oversampling2ndstep * pa.interpolation;
        this.globalList = new ArrayList<Region>();
        this.regionslist_refined = regionslist;
        this.image = imagei;
        this.sx = p.ni * pa.oversampling2ndstep * pa.interpolation;
        this.sy = p.nj * pa.oversampling2ndstep * pa.interpolation;
        this.jobs_done = 0;
        this.max = max;
        this.min = min;
        // IJ.log("sx " + sx);
        if (p.nz == 1) {
            this.sz = 1;
            this.osz = 1;
            this.interpz = 1;
        }
        else {
            this.sz = p.nz * p.oversampling2ndstep * pa.interpolation;
            this.osz = p.oversampling2ndstep * pa.interpolation;
        }
        // IJ.log("regions refined");
        // Tools.showmem();
        // Runtime.getRuntime().gc();
        // Tools.showmem();

        regions_refined = new short[sz][sx][sy];
        // Tools.showmem();

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
        fill_refined();
    }

    public void run() {
        distribute_regions();
    }

    /**
     * Patch creation, distribution and assembly
     */

    private void distribute_regions() {
        // assuming rvoronoi and regionslists (objects) in same order (and same
        // length)

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
        for (final Iterator<Region> it = regionslist_refined.iterator(); it.hasNext();) {
            final Region r = it.next();
            // if (r.value==5){
            // IJ.log("call os " + p.oversampling2ndstep);
            // IJ.log("interp " + p.interpolation);
            if (p.interpolation > 1) {
                p.subpixel = true;
            }
            if (p.subpixel) {
                p.oversampling2ndstep = p.overs;
            }
            else {
                p.oversampling2ndstep = 1;
            }
            ap = new AnalysePatch(image, r, p, p.oversampling2ndstep, channel, regions_refined, this);
            if (p.mode_voronoi2) {
                threadPool.execute(ap);
                // IJ.log("size q:"+ queue.size());
            }
            else {
                // ap.run();//execute
            }

            // ///////////////

            // add refined result into regions refined :
            if (!p.mode_voronoi2) {

                if (interp == 1) {
                    assemble_result(ap, r);
                }
                else {
                    assemble_result_interpolated(ap, r);
                }
            }

            // else{
            // assemble_result_voronoi2(ap);
            // }
            // }
        }
        // IJ.log("loop done");

        //

        threadPool.shutdown();

        try {
            // IJ.log("await termination");
            threadPool.awaitTermination(1, TimeUnit.DAYS);
        }
        catch (final InterruptedException ex) {
        }

        // long lStartTime = new Date().getTime(); //start time

        if (p.mode_voronoi2) {
            // find_regions();
            regionslist_refined = globalList;

            // final LinkedBlockingQueue<Runnable> queue2 = new
            // LinkedBlockingQueue<Runnable>();
            final ThreadPoolExecutor threadPool2 = new ThreadPoolExecutor(1, 1, 1, TimeUnit.DAYS, queue);

            ObjectProperties Op;
            // calculate regions intensities
            for (final Iterator<Region> it = regionslist_refined.iterator(); it.hasNext();) {
                final Region r = it.next();
                // IJ.log("int region :" +r.value);
                Op = new ObjectProperties(image, r, sx, sy, sz, p, osxy, osz, imagecolor_c1, regions_refined);
                threadPool2.execute(Op);
                // Op.run();
            }

            threadPool2.shutdown();
            try {
                // IJ.log("await termination");
                // Tools.showmem();
                threadPool2.awaitTermination(1, TimeUnit.DAYS);
            }
            catch (final InterruptedException ex) {
            }

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
                for (int i = 0; i < regions_refined.length; i++) {
                    for (int j = 0; j < regions_refined[i].length; j++) {
                        for (int k = 0; k < regions_refined[i][j].length; k++) {
                            regions_refined[i][j][k] = 0;
                        }
                    }
                }
                assemble(regionslist_refined);
            }

            //

            regionslist_refined = regionslist_refined_filter;
        }

        // Tools.showmem();
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

    private void assemble(ArrayList<Region> regionslist_refined) {
        for (final Iterator<Region> it = regionslist_refined.iterator(); it.hasNext();) {
            final Region r = it.next();

            for (final Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
                final Pix v = it2.next();
                // count number of free edges
                regions_refined[v.pz][v.px][v.py] = (short) r.value;

            }
        }
    }

    /**
     * Assemble the result
     *
     * @param regionslist_refined List of regions to assemble
     * @param regions_refined regions refined
     */

    static public void assemble(Collection<Region> regionslist_refined, short[][][] regions_refined) {
        for (final Iterator<Region> it = regionslist_refined.iterator(); it.hasNext();) {
            final Region r = it.next();

            for (final Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
                final Pix v = it2.next();
                regions_refined[v.pz][v.px][v.py] = (short) r.value;

            }
        }
    }

    public synchronized void addRegionstoList(ArrayList<Region> localList) {
        // boolean disp=false;
        // if (localList.size()>1)
        // disp=true;
        int index;// build index of region (will be r.value)
        for (final Iterator<Region> it = localList.iterator(); it.hasNext();) {
            final Region r = it.next();
            index = globalList.size() + 1;
            // if (disp){
            // IJ.log("index" + index + "size" + r.pixels.size());
            // }
            r.value = index;
            globalList.add(r);
        }

        jobs_done++;

        IJ.showStatus("Computing segmentation  " + round(55 + (45 * ((double) jobs_done) / (nb_jobs)), 2) + "%");
        IJ.showProgress(0.55 + 0.45 * (jobs_done) / (nb_jobs));

    }

    private double round(double y, int z) {
        // Special tip to round numbers to 10^-2
        y *= Math.pow(10, z);
        y = (int) y;
        y /= Math.pow(10, z);
        return y;
    }

    private void assemble_result(AnalysePatch ap, Region r) {
        final ArrayList<Pix> rpixels = new ArrayList<Pix>();
        int pixcount = 0;
        for (int z = 0; z < ap.sz; z++) {
            for (int i = 0; i < ap.sx; i++) {
                for (int j = 0; j < ap.sy; j++) {
                    if (ap.object[z][i][j] == 1) {
                        regions_refined[z + ap.offsetz * osz][i + ap.offsetx * osxy][j + ap.offsety * osxy] = (short) r.value;
                        rpixels.add(new Pix(z + ap.offsetz * osz, i + ap.offsetx * osxy, j + ap.offsety * osxy));
                        // rpixels.add(new Pix(z,i,j));
                        pixcount++;
                    }

                    // else
                    // regions_refined[z+ap.offsetz*osz][i+ap.offsetx*osxy][j+ap.offsety*osxy]
                    // =0;
                }
            }
        }
        // assign new pixel list to region and refined size
        r.pixels = rpixels;
        r.rsize = pixcount;
        r.intensity = ap.cin * (ap.intmax - ap.intmin) + ap.intmin;
    }

    private void assemble_result_interpolated(AnalysePatch ap, Region r) {

        final ArrayList<Pix> rpixels = new ArrayList<Pix>();
        int pixcount = 0;
        for (int z = 0; z < ap.isz; z++) {
            for (int i = 0; i < ap.isx; i++) {
                for (int j = 0; j < ap.isy; j++) {
                    if (ap.interpolated_object[z][i][j] == 1) {
                        regions_refined[z + ap.offsetz * osz][i + ap.offsetx * osxy][j + ap.offsety * osxy] = (short) r.value;
                        rpixels.add(new Pix(z + ap.offsetz * osz, i + ap.offsetx * osxy, j + ap.offsety * osxy));
                        // rpixels.add(new
                        // Pix(z+ap.offsetz*osz,i+ap.offsetx*osxy,i+ap.offsetx*osxy));
                        pixcount++;
                    }
                }
            }
        }
        // assign new pixel list to region and refined size
        r.pixels = rpixels;
        r.rsize = pixcount;
        r.intensity = ap.cin * (ap.intmax - ap.intmin) + ap.intmin;
    }

    private void fill_refined() {
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    regions_refined[z][i][j] = 0;
                }
            }
        }
    }
}
