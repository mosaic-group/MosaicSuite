package mosaic.core.imageUtils.iterators;

import java.util.Iterator;

import mosaic.core.imageUtils.Point;

public abstract class BaseIterator {
    protected final int iNumOfDimensions;
    SpaceIterator iIntersectionIt;
    Iterator<Point> iIntersectionPointIt;
    
    protected Point iLastPoint = null;
    protected Point iOffsetPoint = null;

    /**
     * @param iInputDims dimensions of the input image
     * @param iRegionDims dimensions of the region
     * @param iRegionOffset offset of the region in the input image (upper left)
     */
    public BaseIterator(int[] iInputDims, int[] iRegionDims, int[] iRegionOffset) {
        // Check if all provided dimensions match input region
        iNumOfDimensions = iInputDims.length;
        if (iRegionDims.length != iNumOfDimensions || iRegionOffset.length != iNumOfDimensions) 
            throw new RuntimeException("dimensions not matching in region iterator");
        
        // Crop region to be inside input, and find offset point.
        int[] dimensionsOfIntersection = new int[iNumOfDimensions];
        int[] offsetPoint = new int[iNumOfDimensions];
        for (int d = 0; d < iNumOfDimensions; ++d) {
            int begin = (iRegionOffset[d] < 0) ? 0 : iRegionOffset[d];
            int end = ((iRegionOffset[d] + iRegionDims[d]) > iInputDims[d]) ? iInputDims[d] : iRegionOffset[d] + iRegionDims[d];
            dimensionsOfIntersection[d] = end - begin;
            offsetPoint[d] = begin;
        }
        
        iOffsetPoint = new Point(offsetPoint);
        iIntersectionIt = new SpaceIterator(dimensionsOfIntersection);
        iIntersectionPointIt = iIntersectionIt.getPointIterator();
    }

    /**
     * @return next index value
     */
    abstract public int next();
    
    /**
     * @return true if exist
     */
    public boolean hasNext() {
        return iIntersectionPointIt.hasNext();
    }

    /**
     * @return size of a cropped area: area of AND operation on 'input' and 'region'
     */
    public int getSize() {
        return iIntersectionIt.getSize();
    }
    
    /**
     * @return current point in input image coordinates
     */
    public Point getPoint() {
        return iLastPoint;
    }
}
