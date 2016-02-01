package mosaic.bregman.segmentation;


import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import ij.IJ;
import mosaic.core.psf.psf;
import mosaic.utils.ArrayOps;
import net.imglib2.type.numeric.real.DoubleType;


class ImagePatches {
    private static final Logger logger = Logger.getLogger(ImagePatches.class);
    
    private final SegmentationParameters iParameters;
    private ArrayList<Region> iRegionsList;
    private final double[][][] iImage;
    private final double[][][] w3kbest;
    private final double iMax;
    private final double iMin;

    private final int iOverSamplingInXY;
    private final int iSizeX;
    private final int iSizeY;
    
    private final int iOverSamplingInZ;
    private final int iSizeZ;
    
    private final ArrayList<Region> iGlobalRegionsList;
    private final short[][][] iRegions;
    
    private int iNumberOfJobs = 0;
    private int iNumOfDoneJobs = 0;

    private final double iLreg;
    private final double iMinIntensity;
    private  psf<DoubleType> iPsf;
    
    ImagePatches(SegmentationParameters aParameters, ArrayList<Region> aRegionsList, double[][][] aImage, double[][][] w3k, double aMin, double aMax, double aLreg, double aMinIntensity,  psf<DoubleType> aPsf) {
        logger.debug("ImagePatches ----------------------------");
        iParameters = aParameters;
        iRegionsList = aRegionsList;
        iImage = aImage;
        w3kbest = w3k;
        iMin = aMin;
        iMax = aMax;
        iPsf = aPsf;
        if (!iParameters.subpixel) {
            iParameters.oversampling2ndstep = 1;
            iParameters.interpolation = 1;
        }
        else {
            iParameters.oversampling2ndstep = 2;
        }
        
        int ni = aImage[0].length;
        int nj = aImage[0][0].length;
        int nz = aImage.length;
        iOverSamplingInXY = iParameters.oversampling2ndstep * iParameters.interpolation;
        iSizeX = ni * iOverSamplingInXY;
        iSizeY = nj * iOverSamplingInXY;

        iOverSamplingInZ = (nz == 1) ? 1 : iParameters.oversampling2ndstep * iParameters.interpolation; 
        iSizeZ = nz * iOverSamplingInZ;
        
        iGlobalRegionsList = new ArrayList<Region>();
        iRegions = new short[iSizeZ][iSizeX][iSizeY];
        
        iLreg = aLreg;
        iMinIntensity = aMinIntensity;
    }

    ArrayList<Region> getRegionsList() {
        return iRegionsList;
    }
    
    short[][][] getRegions() {
        return iRegions;
    }
    
    void run() {
        distributeRegions();
    }

    /**
     * Patch creation, distribution and assembly
     */
    private void distributeRegions() {
        // assuming rvoronoi and regionslists (objects) in same order (and same length)

        iNumberOfJobs = iRegionsList.size();
        for (final Region r : iRegionsList) {
            if (iParameters.interpolation > 1) {
                iParameters.subpixel = true;
            }
            if (iParameters.subpixel) {
                iParameters.oversampling2ndstep = 2;
            }
            else {
                iParameters.oversampling2ndstep = 1;
            }
            AnalysePatch ap = new AnalysePatch(iImage, r, iParameters, iParameters.oversampling2ndstep, this, w3kbest, iLreg, iMinIntensity, iPsf);
            // TODO: It causes problems when run in more then 1 thread. Should be investigated why.
            ap.run();
        }

        iRegionsList = iGlobalRegionsList;
        assemble(iRegionsList, iRegions);
        
        // calculate regions intensities
        for (final Region r : iRegionsList) {
            ObjectProperties op = new ObjectProperties(iImage, r, iSizeX, iSizeY, iSizeZ, iParameters, iOverSamplingInXY, iOverSamplingInZ, iRegions, iPsf);
            op.run();
        }

        // here we analyse the patch
        // if we have a big region with intensity near the background kill that region
        boolean changed = false;
        final ArrayList<Region> regionsListFiltered = new ArrayList<Region>();
        for (final Region r : iRegionsList) {
            if (r.intensity * (iMax - iMin) + iMin > iParameters.min_region_filter_intensities) {
                regionsListFiltered.add(r);
            }
            else {
                changed = true;
            }
        }
        iRegionsList = regionsListFiltered;
        
        // if changed, reassemble
        if (changed == true) {
            ArrayOps.fill(iRegions, (short) 0);
            assemble(iRegionsList, iRegions);
        }
    }

    /**
     * Assemble the result
     * @param aRegionsListRefined List of regions to assemble
     * @param aRegionsRefined regions refined
     */
    static void assemble(Collection<Region> aRegionsListRefined, short[][][] aRegionsRefined) {
        for (final Region r : aRegionsListRefined) {
            for (final Pix p : r.pixels) {
                aRegionsRefined[p.pz][p.px][p.py] = (short) r.value;
            }
        }
    }

    synchronized void addRegionsToList(ArrayList<Region> localList) {
        for (final Region r : localList) {
            int index = iGlobalRegionsList.size() + 1;
            r.value = index;
            iGlobalRegionsList.add(r);
        }

        iNumOfDoneJobs++;

        IJ.showStatus("Computing segmentation  " + Tools.round(55 + (45 * ((double) iNumOfDoneJobs) / (iNumberOfJobs)), 2) + "%");
        IJ.showProgress(0.55 + 0.45 * (iNumOfDoneJobs) / (iNumberOfJobs));
    }
}
