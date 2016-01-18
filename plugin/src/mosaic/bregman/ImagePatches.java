package mosaic.bregman;


import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import ij.IJ;
import mosaic.utils.ArrayOps;


class ImagePatches {
    private static final Logger logger = Logger.getLogger(ImagePatches.class);
    
    private final Parameters iParameters;
    public ArrayList<Region> iRegionsListRefined;
    private final double[][][] iImage;
    private final int iChannel;
    final double[][][] w3kbest;
    private final double iMax;
    private final double iMin;

    private final int osxy;
    private final int sx;
    private final int sy;
    
    private final int osz; // oversampling
    private final int sz;// size of full image with oversampling
    
    private byte[] imagecolor_c1;
    
    private final ArrayList<Region> iGlobalList;
    public final short[][][] iRegionsRefined;
    
    private int iNumberOfJobs = 0;
    private int iNumOfDoneJobs = 0;


    public ImagePatches(Parameters aParameters, ArrayList<Region> aRegionsList, double[][][] aImage, int aChannel, double[][][] w3k, double aMin, double aMax) {
        logger.debug("ImagePatches ----------------------------");
        iParameters = aParameters;
        iRegionsListRefined = aRegionsList;
        iImage = aImage;
        iChannel = aChannel;
        w3kbest = w3k;
        iMin = aMin;
        iMax = aMax;
        
        if (!iParameters.subpixel) {
            iParameters.oversampling2ndstep = 1;
            iParameters.interpolation = 1;
        }
        else {
            iParameters.oversampling2ndstep = 2;
        }

        osxy = iParameters.oversampling2ndstep * iParameters.interpolation;
        sx = iParameters.ni * iParameters.oversampling2ndstep * iParameters.interpolation;
        sy = iParameters.nj * iParameters.oversampling2ndstep * iParameters.interpolation;

        sz = (iParameters.nz == 1) ?  1 : iParameters.nz * iParameters.oversampling2ndstep * iParameters.interpolation;
        osz = (iParameters.nz == 1) ? 1 : iParameters.oversampling2ndstep * iParameters.interpolation; 
        
        if (iParameters.dispint) {
            imagecolor_c1 = new byte[sz * sx * sy * 3]; // add fill background
            int b0 = (int) Math.min(255, 255 * Analysis.p.betaMLEoutdefault);
            int b1 = (int) Math.min(255, 255 * Math.sqrt(Analysis.p.betaMLEoutdefault));
            int b2 = (int) Math.min(255, 255 * Math.pow(Analysis.p.betaMLEoutdefault, 2));

            // set all to background
            for (int z = 0; z < sz; z++) {
                for (int i = 0; i < sx; i++) {
                    for (int j = 0; j < sy; j++) {
                        final int pointIdx = 3 * (z * sx * sy + i * sy + j);
                        imagecolor_c1[pointIdx + 0] = (byte) b0; // Red
                        imagecolor_c1[pointIdx + 1] = (byte) b1; // Green
                        imagecolor_c1[pointIdx + 2] = (byte) b2; // Blue
                    }
                }
            }
        }
        
        iGlobalList = new ArrayList<Region>();
        iRegionsRefined = new short[sz][sx][sy];
        ArrayOps.fill(iRegionsRefined, (short) 0);
    }

    public void run() {
        distributeRegions();
    }

    /**
     * Patch creation, distribution and assembly
     */
    private void distributeRegions() {
        // assuming rvoronoi and regionslists (objects) in same order (and same length)

        final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 1, 1, TimeUnit.DAYS, queue);

        iNumberOfJobs = iRegionsListRefined.size();
        for (final Region r : iRegionsListRefined) {
            if (iParameters.interpolation > 1) {
                iParameters.subpixel = true;
            }
            if (iParameters.subpixel) {
                iParameters.oversampling2ndstep = 2;
            }
            else {
                iParameters.oversampling2ndstep = 1;
            }
            AnalysePatch ap = new AnalysePatch(iImage, r, iParameters, iParameters.oversampling2ndstep, iChannel, iRegionsRefined, this);
            threadPool.execute(ap);
        }

        try {
            threadPool.shutdown();
            threadPool.awaitTermination(1, TimeUnit.DAYS);
        }
        catch (final InterruptedException e) {
            e.printStackTrace();
        }

        iRegionsListRefined = iGlobalList;


        // calculate regions intensities
        final ThreadPoolExecutor threadPool2 = new ThreadPoolExecutor(1, 1, 1, TimeUnit.DAYS, queue);
        for (final Region r : iRegionsListRefined) {
            ObjectProperties op = new ObjectProperties(iImage, r, sx, sy, sz, iParameters, osxy, osz, imagecolor_c1, iRegionsRefined);
            threadPool2.execute(op);
        }

        try {
            threadPool2.shutdown();
            threadPool2.awaitTermination(1, TimeUnit.DAYS);
        }
        catch (final InterruptedException e) {
            e.printStackTrace();
        }

        // here we analyse the patch
        // if we have a big region with intensity near the background kill that region
        boolean changed = false;

        final ArrayList<Region> regionslistRefinedFilter = new ArrayList<Region>();
        for (final Region r : iRegionsListRefined) {
            if (r.intensity * (iMax - iMin) + iMin > iParameters.min_region_filter_intensities) {
                regionslistRefinedFilter.add(r);
            }
            else {
                changed = true;
            }
        }
        iRegionsListRefined = regionslistRefinedFilter;
        
        // if changed, reassemble
        if (changed == true) {
            ArrayOps.fill(iRegionsRefined, (short) 0);
            assemble(iRegionsListRefined, iRegionsRefined);
        }

        IJ.log(iRegionsListRefined.size() + " objects found in " + ((iChannel == 0) ? "X" : "Y") + ".");
    }

    /**
     * Assemble the result
     * @param aRegionsListRefined List of regions to assemble
     * @param aRegionsRefined regions refined
     */
    static public void assemble(Collection<Region> aRegionsListRefined, short[][][] aRegionsRefined) {
        for (final Region r : aRegionsListRefined) {
            for (final Pix p : r.pixels) {
                aRegionsRefined[p.pz][p.px][p.py] = (short) r.value;
            }
        }
    }

    public synchronized void addRegionsToList(ArrayList<Region> localList) {
        for (final Region r : localList) {
            int index = iGlobalList.size() + 1;
            r.value = index;
            iGlobalList.add(r);
        }

        iNumOfDoneJobs++;

        IJ.showStatus("Computing segmentation  " + Tools.round(55 + (45 * ((double) iNumOfDoneJobs) / (iNumberOfJobs)), 2) + "%");
        IJ.showProgress(0.55 + 0.45 * (iNumOfDoneJobs) / (iNumberOfJobs));
    }
}
