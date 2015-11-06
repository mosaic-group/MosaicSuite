package mosaic.core.imageUtils.iterators;

import java.util.Iterator;

import mosaic.core.imageUtils.Point;

public class RegionIterator
{
    final int iNumOfDimensions;
    
    IndexIterator iInputIt;
    IndexIterator iRegionIt;
    Iterator<Point> iRegionPointIt;
    Point ilastPoint = null;
    Point iOffsetPoint = null;

    /**
     * @param input dimensions of the input image
     */
    public RegionIterator(int... input) {
        this(input, input, new int[input.length]);
    }
    
    /**
     * @param iInputDims dimensions of the input image
     * @param iRegionDims dimensions of the region
     * @param iRegionOffset offset of the region in the input image (upper left)
     */
    public RegionIterator(int[] iInputDims, int[] iRegionDims, int[] iRegionOffset) {
        // Check if all provided dimensions match input region
        iNumOfDimensions = iInputDims.length;
        if (iRegionDims.length != iNumOfDimensions || iRegionOffset.length != iNumOfDimensions) 
            throw new RuntimeException("dimensions not matching in region iterator");
        
        // Crop region to be inside input, and find offset point.
        int[] dimensionsOfRegion = new int[iNumOfDimensions];
        int[] offsetPoint = new int[iNumOfDimensions];
        for (int d = 0; d < iNumOfDimensions; ++d) {
            int begin = (iRegionOffset[d] < 0) ? 0 : iRegionOffset[d];
            int end = ((iRegionOffset[d] + iRegionDims[d]) > iInputDims[d]) ? iInputDims[d] : iRegionOffset[d] + iRegionDims[d];
            dimensionsOfRegion[d] = end - begin;
            offsetPoint[d] = begin;
        }
        
        // Initialize iterators
        iInputIt = new IndexIterator(iInputDims);
        iRegionIt = new IndexIterator(dimensionsOfRegion);
        iOffsetPoint = new Point(offsetPoint);
        iRegionPointIt = iRegionIt.getPointIterator();
    }

    /**
     * @return true if exist
     */
    public boolean hasNext() {
        return iRegionPointIt.hasNext();
    }

    /**
     * @return index to the next point in input image coordinates
     */
    public int next() {
        ilastPoint = iOffsetPoint.add(iRegionPointIt.next());
        return  iInputIt.pointToIndex(ilastPoint);
    }

    /**
     * @return current point in input image coordinates
     */
    public Point getPoint() {
        return ilastPoint;
    }

    /**
     * @return size of a cropped area: area of AND operation on 'input' and 'region'
     */
    public int getSize() {
        return iRegionIt.getSize();
    }

    /**
     * next point keep track of the resetting index
     * (Example 00 01 02 03 04 05 10) from 05 to 10
     * we reset last index
     *
     * @return index of the point inside the mask extended
     *         of the size of the image (used to create for fast iterators
     *         see RegionIteratorSphere)
     */
    public int nextRmask() {
        Point p = iRegionPointIt.next();
        iDimensionChanged = p.iCoords[0] == 0 && p.numOfZerosInCoordinates() != iNumOfDimensions;
        ilastPoint = iOffsetPoint.add(p);
        return  iInputIt.pointToIndex(p);
    }
    boolean iDimensionChanged = false;
    public int getRMask() {
        return iDimensionChanged ? 1 : 0;
    }
}
