package mosaic.core.imageUtils;

import java.util.Iterator;

import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.core.imageUtils.masks.Mask;

/**
 * Class for mapping given mask on input area at given point. It iterates only
 * on 'set' or 'true' points of mask and returned indices (point) lies only in input
 * area (points outside are cropped).
 * 
 * Input dims
 * +-----+-------------------+
 * |                         |
 * |             Mask        |
 * +               O------------+    
 * |               |TT..TT..T..t|   T,t - true (set) points of mask
 * |               |.....TTTTttt|   T - only capital 'T' are mapped on input region at given upper-left O(ffset)
 * |               +------------+       returned indices/points are given in respect of input dimensions!
 * |                         |
 * +-------------------------+
 * 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class MaskOnSpaceMapper {
    
    // Input parameters
    private final SpaceIterator iInputIterator;
    private final Point iMaskMidPoint;
    private final Point iMaskRightDown;
    boolean iIsSthToCrop = false;
    
    // Precalculated table of indices
    private final int[] iFgIndexes;

    // setup of iterator
    private Point iPointOffset;
    private int iIndexOffset;
    
    // current state of iterator
    private int iFgIndex;
    private int iNextIndex;
    
    /**
     * @param aMask - input mask
     * @param aInputDimensions - dimensions of area on which mask will be mapped
     */
    public MaskOnSpaceMapper(Mask aMask, int[] aInputDimensions) {
        iInputIterator = new SpaceIterator(aInputDimensions);
        iMaskMidPoint = new Point(aMask.getDimensions()).div(2);
        iMaskRightDown = new Point(aMask.getDimensions()).sub(new Point(aMask.getDimensions().clone()).one());

        iFgIndexes = new int[aMask.getNumOfFgPoints()];
        SpaceIterator maskIterator = new SpaceIterator(aMask.getDimensions());
        Iterator<Integer> iter = maskIterator.getIndexIterator();
        int idxFG = 0;
        while(iter.hasNext()) {
            int idx = iter.next();
            if (aMask.isInMask(idx)) {
                iFgIndexes[idxFG++] = iInputIterator.pointToIndex(maskIterator.indexToPoint(idx));
            }
        }
        
        // By default offset points to left upper point
        iPointOffset = new Point(aInputDimensions.clone()).zero();
        initIndices();
    }
    
    /**
     * Sets middle point of mask in aMiddlePoint of input area
     * @param aMiddlePoint
     */
    public void setMiddlePoint(Point aMiddlePoint) {
        iPointOffset = aMiddlePoint.sub(iMaskMidPoint);
        initIndices();
    }

    /**
     * Sets left-upper corner of mask in aUpperLeftPoint of input area
     * @param aUpperLeftPoint
     */
    public void setUpperLeft(Point aUpperLeftPoint) {
        iPointOffset = aUpperLeftPoint;
        initIndices();
    }

    /**
     * @return true if there is still something to iterate on.
     */
    public boolean hasNext() {
        while (iFgIndex < iFgIndexes.length) {
            int currIdx = iFgIndexes[iFgIndex++];
            iNextIndex = iIndexOffset + currIdx;

            if (!iIsSthToCrop) { 
                return true;
            }
            else {
                Point imgPoint = iPointOffset.add(iInputIterator.indexToPoint(currIdx));
                if (iInputIterator.isInBound(imgPoint)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * @return index of input area where mask is 'true'
     */
    public int next() {
        return iNextIndex;
    }
    
    /**
     * @return point of input area where mask is 'true'
     */
    public Point nextPoint() {
        return iInputIterator.indexToPoint(iNextIndex);
    }

    /**
     * Initialize indices and decides if this is a simple mapping (if whole mask is in input 
     * area and do not need to be cropped)
     */
    private void initIndices() {
        iIndexOffset = iInputIterator.pointToIndex(iPointOffset);
        iFgIndex = 0;
        
        if (iInputIterator.isInBound(iPointOffset) && iInputIterator.isInBound((iPointOffset.add(iMaskRightDown)))) {
            // Whole mask is in input area.
            iIsSthToCrop = false;
        }
        else {
            iIsSthToCrop = true;
        }
    }
}
