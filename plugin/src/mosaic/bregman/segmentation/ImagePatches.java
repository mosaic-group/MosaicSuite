package mosaic.bregman.segmentation;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import ij.IJ;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.iterators.SpaceIterator;
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

    private final int iOverInterInXY;
    private final int iSizeX;
    private final int iSizeY;
    
    private final int iOverInterInZ;
    private final int iSizeZ;
    
    private final ArrayList<Region> iGlobalRegionsList;
    private final short[][][] iRegions;
    
    private int iNumberOfJobs = 0;
    private int iNumOfDoneJobs = 0;

    private final double iLreg;
    private final double iMinIntensity;
    private  psf<DoubleType> iPsf;
    private int oversampling2ndstep = 0;
    
    ImagePatches(SegmentationParameters aParameters, ArrayList<Region> aRegionsList, double[][][] aImage, double[][][] w3k, double aMin, double aMax, double aLreg, double aMinIntensity,  psf<DoubleType> aPsf) {
        logger.debug("ImagePatches ----------------------------");
        iParameters = aParameters;
        iRegionsList = aRegionsList;
        iImage = aImage;
        w3kbest = w3k;
        iMin = aMin;
        iMax = aMax;
        iPsf = aPsf;
        if (iParameters.interpolation > 1) {
            oversampling2ndstep = 2;
        }
        else {
            oversampling2ndstep = 1;
        }
        
        int ni = aImage[0].length;
        int nj = aImage[0][0].length;
        int nz = aImage.length;
        iOverInterInXY = oversampling2ndstep * iParameters.interpolation;
        iSizeX = ni * iOverInterInXY;
        iSizeY = nj * iOverInterInXY;

        iOverInterInZ = (nz == 1) ? 1 : oversampling2ndstep * iParameters.interpolation; 
        iSizeZ = nz * iOverInterInZ;
        
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

    /**
     * Patch creation, distribution and assembly
     */
    void distributeRegions() {
        // assuming rvoronoi and regionslists (objects) in same order (and same length)

        iNumberOfJobs = iRegionsList.size();
        for (final Region r : iRegionsList) {
            AnalysePatch ap = new AnalysePatch(iImage, r, iParameters, oversampling2ndstep, this, w3kbest, iLreg, iMinIntensity, iPsf);
            // TODO: It causes problems when run in more then 1 thread. Should be investigated why.
            ap.run();
        }

        iRegionsList = iGlobalRegionsList;
        assemble(iRegionsList, iRegions);
        
        // calculate regions intensities
        for (final Region r : iRegionsList) {
            ObjectProperties op = new ObjectProperties(iImage, r, iSizeX, iSizeY, iSizeZ, iParameters, iOverInterInXY, iOverInterInZ, iRegions, iPsf);
            op.run();
        }

        // here we analyse the patch
        // if we have a big region with intensity near the background kill that region
        boolean changed = false;
        final ArrayList<Region> regionsListFiltered = new ArrayList<Region>();
        for (final Region r : iRegionsList) {
            if (r.intensity * (iMax - iMin) + iMin > iParameters.minRegionIntensity) {
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
        
        stepThreePostprocessing();
    }

    private void stepThreePostprocessing() {
        //  ========================      Postprocessing phase
        // Well we did not finished yet at this stage you can have several artifact produced by the patches
        // for example one region can be segmented partially by two patches, this mean that at least in theory
        // you should repatch (this produce a finer decomposition) again and rerun the second stage until each
        // patches has one and only one region. The method eventually converge because it always produce finer 
        // decomposition, in the opposite case you stop.
        // The actual patching algorithm use a first phase Split-Bregman segmentation + Threasholding + Voronoi 
        // (unfortunately only 2D, for 3D a 2D Maximum projection is computed)
        // The 2D Maximal projection unfortunately complicate even more the things, and produce even more artifacts,
        // in particular for big PSF with big margins patch.
        //
        // IMPROVEMENT:
        // 1) 3D Voronoi (ImageLib2 Voronoi like segmentation)
        // 2) Good result has been achieved using Particle tracker for Patch positioning.
        // 3) Smart patches given the first phase we cut the soft membership each cut produce a segmentation
        // that include the previous one, going to zero this produce a tree graph. the patches are positioned
        // on the leaf ( other algorithms can be implemented to analyze this graph ... )
        //
        // (Temporarily we fix in this way)
        // Save the old intensity label image as an hashmap (to save memory) run find connected region to recompute 
        // the regions again. Recompute the statistics using the old intensity label image.

        // we run find connected regions
        final LabelImage img = new LabelImage(iRegions);
        img.connectedComponents();
    
        final HashMap<Integer, Region> r_list = new HashMap<Integer, Region>();
    
        // Run on all pixels of the label to add pixels to the regions
        final Iterator<Point> rit = new SpaceIterator(img.getDimensions()).getPointIterator();
        while (rit.hasNext()) {
            final Point p = rit.next();
            final int lbl = img.getLabel(p);
            if (lbl != 0) {
                // foreground
                Region r = r_list.get(lbl);
                if (r == null) {
                    r = new Region(lbl, 0);
                    r_list.put(lbl, r);
                }
                r.pixels.add(new Pix(p.iCoords[2], p.iCoords[0], p.iCoords[1]));
            }
        }
    
        // Now we run Object properties on this regions list
        ImagePatches.assemble(r_list.values(), iRegions);
    
        for (final Region r : r_list.values()) {
            final ObjectProperties obj = new ObjectProperties(iImage, r, iSizeX, iSizeY, iSizeZ, iParameters, iOverInterInXY, iOverInterInZ, iRegions, iPsf);
            obj.run();
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
