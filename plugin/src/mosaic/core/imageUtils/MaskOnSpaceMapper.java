package mosaic.core.imageUtils;

import java.util.Iterator;

import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.core.imageUtils.masks.Mask;

public class MaskOnSpaceMapper {
    
    // Input parameters
    private final SpaceIterator iInputIterator;
    private final Point iMaskMidPoint;
    private final Point iMaskRightDown;
    boolean fast = true;
    
    // Precalculated table of indices
    private final int[] iFgIndexes;

    // setup of iterator
    private Point iPointOffset;
    private int iIndexOffset;
    
    // current state of iterator
    private int iFgIndex;
    private int iNextIndex;
    
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
        iIndexOffset = 0;
        iPointOffset = new Point(aInputDimensions.clone()).zero();
        iFgIndex = 0;
    }
    
    public void setMidPoint(Point aMiddlePoint) {
        iPointOffset = aMiddlePoint.sub(iMaskMidPoint);
        initIndices();
    }

    public void setUpperLeft(Point aUpperLeftPoint) {
        iPointOffset = aUpperLeftPoint;
        initIndices();
    }

    private void initIndices() {
        iIndexOffset = iInputIterator.pointToIndex(iPointOffset);
        iFgIndex = 0;
        
        if (iInputIterator.isInBound(iPointOffset) && iInputIterator.isInBound((iPointOffset.add(iMaskRightDown)))) {
            fast = true;
        }
        else {
            fast = false;
        }
    }
    
    
    public boolean hasNext() {
        while (iFgIndex < iFgIndexes.length) {
            int currIdx = iFgIndexes[iFgIndex++];
            iNextIndex = iIndexOffset + currIdx;

            if (fast) { 
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
    
    public int next() {
        return iNextIndex;
    }
    
    public Point nextP() {
        return iInputIterator.indexToPoint(iNextIndex);
    }
}
