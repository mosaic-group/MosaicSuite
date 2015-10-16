package mosaic.region_competition;


import java.util.HashMap;
import java.util.Map.Entry;

import ij.ImagePlus;
import mosaic.core.utils.LabelImage;
import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIterator;


public class LabelImageRC extends LabelImage {

    private static final int ForbiddenLabel = Integer.MAX_VALUE;

    /** Maps the label(-number) to the information of a label */
    private final HashMap<Integer, LabelInformation> iLabelMap = new HashMap<Integer, LabelInformation>();

    
    /**
     * Create a LabelImageRC from another LabelImageRC
     */
    public LabelImageRC(LabelImageRC aLabelImageRC) {
        super(aLabelImageRC);
    }

    /**
     * Creates empty LabelImageRC with given dimensions
     */
    public LabelImageRC(int[] aDimensions) {
        super(aDimensions);
    }


    /**
     * LabelImageRC loaded from file
     */
    @Override
    public void initWithImg(ImagePlus aImagePlus) {
        super.initWithImg(aImagePlus);
        initBoundary();
    }

    /**
     * Returns internal data structure with Label Map
     * @return
     */
    public HashMap<Integer, LabelInformation> getLabelMap() {
        return iLabelMap;
    }

    /**
     * Is aLabel forbidden?
     */
    public boolean isForbiddenLabel(int aLabel) {
        return (aLabel == ForbiddenLabel);
    }

    @Override
    public boolean isSpecialLabel(int aLabel) {
        if (aLabel == BGLabel || aLabel == ForbiddenLabel) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean isInnerLabel(int label) {
        if (isSpecialLabel(label) || isContourLabel(label)) {
            return false;
        }
        return true;
    }
    
    /**
     * @param label a label
     * @return the contour form of the label
     */
    @Override
    protected int labelToNeg(int label) {
        if (!isInnerLabel(label)) {
            return label;
        }
        return -label;
    }



    /**
     * sets the outermost pixels of the LabelImage to the forbidden label
     */
    public void initBoundary() {
        for (final int idx : iIterator.getIndexIterable()) {
            final Point p = iIterator.indexToPoint(idx);
            final int xs[] = p.iCoords;
            for (int d = 0; d < getNumOfDimensions(); d++) {
                final int x = xs[d];
                if (x == 0 || x == getDimension(d) - 1) {
                    setLabel(idx, ForbiddenLabel);
                    break;
                }
            }
        }
    }
    
    /**
     * Initialize the contour setting it to (-)label
     */
    public void initContour() {
        for (final int i : iIterator.getIndexIterable()) {
            final int label = getLabelAbs(i);
            if (label != BGLabel && label != ForbiddenLabel) // region pixel && label<negOfs
            {
                final Point p = iIterator.indexToPoint(i);
                for (final Point neighbor : iConnectivityFG.iterateNeighbors(p)) {
                    final int neighborLabel = getLabelAbs(neighbor);
                    if (neighborLabel != label) {
                        setLabel(p, labelToNeg(label));
                        break;
                    }
                }
            }
        }
    }

    /**
     * Calculate the center of Mass of the regions
     */
    public void calculateRegionsCenterOfMass() {
        // iterate through all the regions and reset mean_pos
        for (final Integer lbl : iLabelMap.keySet()) {
            for (int i = 0; i < iLabelMap.get(lbl).mean_pos.length; i++) {
                iLabelMap.get(lbl).mean_pos[i] = 0.0;
            }
        }

        // Iterate through all the region
        final RegionIterator ri = new RegionIterator(getDimensions());
        while (ri.hasNext()) {
            ri.next();
            final Point p = ri.getPoint();
            final int lbl = getLabelAbs(p);

            final LabelInformation lbi = iLabelMap.get(lbl);

            // Label information
            if (lbi != null) {
                for (int i = 0; i < p.iCoords.length; i++) {
                    lbi.mean_pos[i] += p.iCoords[i];
                }
            }
        }

        // Iterate through all the regions
        for (final Entry<Integer, LabelInformation> entry : iLabelMap.entrySet()) {
            for (int i = 0; i < entry.getValue().mean_pos.length; i++) {
                entry.getValue().mean_pos[i] /= entry.getValue().count;
            }
        }
    }
}
