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
    private final double[][][] w3kbest;
    private final double iMax;
    private final double iMin;

    private final int iOverSamplingInXY;
    private final int iSizeX;
    private final int iSizeY;
    
    private final int iOverSamplingInZ;
    private final int iSizeZ;
    
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

        iOverSamplingInXY = iParameters.oversampling2ndstep * iParameters.interpolation;
        iSizeX = iParameters.ni * iOverSamplingInXY;
        iSizeY = iParameters.nj * iOverSamplingInXY;

        iOverSamplingInZ = (iParameters.nz == 1) ? 1 : iParameters.oversampling2ndstep * iParameters.interpolation; 
        iSizeZ = iParameters.nz * iOverSamplingInZ;
        
        iGlobalList = new ArrayList<Region>();
        iRegionsRefined = new short[iSizeZ][iSizeX][iSizeY];
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
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 4, 1, TimeUnit.DAYS, queue);

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
            AnalysePatch ap = new AnalysePatch(iImage, r, iParameters, iParameters.oversampling2ndstep, iChannel, this, w3kbest);
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
        assemble(iRegionsListRefined, iRegionsRefined);
        
        // calculate regions intensities
        final ThreadPoolExecutor threadPool2 = new ThreadPoolExecutor(1, 4, 1, TimeUnit.DAYS, queue);
        for (final Region r : iRegionsListRefined) {
            ObjectProperties op = new ObjectProperties(iImage, r, iSizeX, iSizeY, iSizeZ, iParameters, iOverSamplingInXY, iOverSamplingInZ, iRegionsRefined);
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
