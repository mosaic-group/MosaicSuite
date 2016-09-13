package mosaic.bregman.segmentation;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import ij.IJ;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.core.psf.psf;
import mosaic.utils.ArrayOps;
import mosaic.utils.Debug;
import net.imglib2.type.numeric.real.DoubleType;


class ImagePatches {
    private static final Logger logger = Logger.getLogger(ImagePatches.class);
    
    // Input params
    private final SegmentationParameters iParameters;
    private final ArrayList<Region> iRegionsList;
    private final double[][][] iImage;
    private final double[][][] w3kbest;
    private final double iGlobalMax;
    private final double iGlobalMin;
    private final double iRegularization;
    private final double iMinObjectIntensity;
    private final psf<DoubleType> iPsf;

    private final int iOversampling;
    private final int iOverInterXY;
    private final int iOverInterZ;
    private final int iSizeOverInterX;
    private final int iSizeOverInterY;
    private final int iSizeOverInterZ;

    // Output data
    private ArrayList<Region> iOutputRegionsList;
    private final short[][][] iOutputLabeledRegions;
    
    
    ImagePatches(SegmentationParameters aParameters, ArrayList<Region> aRegionsList, double[][][] aImage, double[][][] w3k, double aGlobalMin, double aGlobalMax, double aRegularization, double aMinObjectIntensity,  psf<DoubleType> aPsf) {
        logger.debug("ImagePatches: numOfInputRegions: " + aRegionsList.size() + ", inputImage: " + Debug.getArrayDims(aImage));
        
        // Save inputs
        iParameters = aParameters;
        iRegionsList = aRegionsList;
        iImage = aImage;
        w3kbest = w3k;
        iGlobalMin = aGlobalMin;
        iGlobalMax = aGlobalMax;
        iRegularization = aRegularization;
        iMinObjectIntensity = aMinObjectIntensity;
        iPsf = aPsf;
        
        // Calculate geometry
        int ni = aImage[0].length;
        int nj = aImage[0][0].length;
        int nz = aImage.length;
        iOversampling = (iParameters.interpolation > 1) ? 2 : 1;
        iOverInterXY = iOversampling * iParameters.interpolation;
        iOverInterZ = (nz == 1) ? 1 : iOversampling * iParameters.interpolation; 
        iSizeOverInterX = ni * iOverInterXY;
        iSizeOverInterY = nj * iOverInterXY;
        iSizeOverInterZ = nz * iOverInterZ;
        
        // Initialize outputs
        iOutputRegionsList = new ArrayList<Region>();
        iOutputLabeledRegions = new short[iSizeOverInterZ][iSizeOverInterX][iSizeOverInterY];

        logger.debug("              oversampling/interpolation: " + iOversampling + " / " + iParameters.interpolation);
    }

    ArrayList<Region> getRegionsList() {
        return iOutputRegionsList;
    }
    
    short[][][] getLabeledRegions() {
        return iOutputLabeledRegions;
    }

    /**
     * Patch creation, distribution and assembly
     */
    void processPatches() {
        // ---------------------------------------------------------------------
        // - Compute Patches and Regions in each patch 
        // --------------------------------------------------------------------
        int numberOfJobs = iRegionsList.size();
        int numOfDoneJobs = 0;
        int newLabel = 1;
        for (final Region inputRegion : iRegionsList) {
            AnalysePatch ap = new AnalysePatch(iImage, inputRegion, iParameters, iOversampling, w3kbest, iRegularization, iMinObjectIntensity, iPsf);
            final ArrayList<Region> regions = ap.calculateRegions();
            
            for (final Region r : regions) {
                r.iLabel = newLabel++;
                iOutputRegionsList.add(r);
            }
            numOfDoneJobs++;
            
            double progress = 55 + (45 * ((double) numOfDoneJobs) / (numberOfJobs));
            IJ.showStatus("Computing segmentation  " + SegmentationTools.round(progress, 2) + "%");
            IJ.showProgress(progress/100);
        }
        generateLabeledRegions(iOutputRegionsList, iOutputLabeledRegions);
        logger.debug("number of found regions:                      " + iOutputRegionsList.size() + ", output label regions: " + Debug.getArrayDims(iOutputLabeledRegions));

        // --------------------------------------------------------------------
        // - Postprocess computed regions 
        // --------------------------------------------------------------------
        // At this stage you can have several artifact produced by the patches for example one region can be segmented 
        // partially by two patches, this mean that at least in theory you should repatch (this produce a finer decomposition) 
        // again and rerun the second stage until each patches has one and only one region. 
        // The method eventually converge because it always produce finer decomposition, in the opposite case you stop.
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
        // Run find connected region to recompute the regions again. Recompute regions list and labeled regions.
        final LabelImage img = new LabelImage(iOutputLabeledRegions);
        img.connectedComponents();
        
        final HashMap<Integer, ArrayList<Pix>> newRegionList = new HashMap<Integer, ArrayList<Pix>>();
        final Iterator<Point> rit = new SpaceIterator(img.getDimensions()).getPointIterator();
        while (rit.hasNext()) {
            final Point p = rit.next();
            final int label = img.getLabel(p);
            if (label != 0) {
                // foreground
                ArrayList<Pix> pixels = newRegionList.get(label);
                if (pixels == null) {
                    pixels = new ArrayList<Pix>();
                    newRegionList.put(label, pixels);
                }
                pixels.add(new Pix(p.iCoords[2], p.iCoords[0], p.iCoords[1]));
            }
        }
        iOutputRegionsList.clear();
        for (Entry<Integer, ArrayList<Pix>> e : newRegionList.entrySet()) {
            // Because of connectivity may disconnect some regions (single pixels) check for regions size.
            if (e.getValue().size() >= iParameters.minRegionSize) {
                iOutputRegionsList.add(new Region(e.getKey(), e.getValue()));
            }
        }
        logger.debug("number of found regions after postprocessing: " + iOutputRegionsList.size());
        
        // Clear output label regions since regions are filtered above.
        ArrayOps.fill(iOutputLabeledRegions, (short) 0);
        generateLabeledRegions(iOutputRegionsList, iOutputLabeledRegions);
        
        // --------------------------------------------------------------------
        // - Compute regions properties (intensity...)
        // --------------------------------------------------------------------
        for (final Region r : iOutputRegionsList) {
            final ObjectProperties obj = new ObjectProperties(iImage, r, iOutputLabeledRegions, iPsf, iParameters.defaultBetaMleOut, iParameters.defaultBetaMleIn, iSizeOverInterX, iSizeOverInterY, iSizeOverInterZ, iOverInterXY, iOverInterZ);
            obj.run();
        }

        // --------------------------------------------------------------------
        // - Filter regions - remove these with small intensity
        // --------------------------------------------------------------------
        final ArrayList<Region> regionsListFiltered = new ArrayList<Region>();
        for (final Region r : iOutputRegionsList) {
            // Rescale the intensity to the original one
            r.intensity = r.intensity * (iGlobalMax - iGlobalMin) + iGlobalMin;

            if (r.intensity >= iParameters.minRegionIntensity) {
                regionsListFiltered.add(r);
            }
        }
        // Number of regions changed, re-compute labeledRegions
        if (regionsListFiltered.size() != iOutputRegionsList.size()) {
            ArrayOps.fill(iOutputLabeledRegions, (short) 0);
            generateLabeledRegions(regionsListFiltered, iOutputLabeledRegions);
            iOutputRegionsList = regionsListFiltered;
        }
        logger.debug("number of found regions after filtering:      " + iOutputRegionsList.size());
    }

    /**
     * Generate labeled regions from provided list of regions
     */
    private void generateLabeledRegions(Collection<Region> aRegionsList, short[][][] aOutputRegions) {
        for (final Region r : aRegionsList) {
            for (final Pix p : r.iPixels) {
                aOutputRegions[p.pz][p.px][p.py] = (short) r.iLabel;
            }
        }
    }
}
