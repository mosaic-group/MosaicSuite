package mosaic.core.imageUtils.iterators;

import java.util.Iterator;

import mosaic.core.imageUtils.Point;

/**
 * Region iterator is responsible for iterating over some region inside input coordinates.
 * It handles all cropping stuff (if region with its offset goes beyond input coordinates, or if region is bigger than input).
 * 
 * Input image
 * +-----+-------------------+
 * |                         |
 * |               Region    |
 * +               O-----+   |   O = offset, P - first point of iterator, 'dot' - rest of points to be iterated.
 * |               |P....|   |
 * |               |.....|   |
 * |               +-----+   |
 * |                         |
 * +-------------------------+
 * 
 *   
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
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
     * @return index to the next index in input image coordinates
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
     * Input image
     * +-----+-------------------+
     * |I    | Index mapping     |
     * |     |                   |
     * |     |         Region    |
     * +-----+         +-----+   |
     * |               |P    |   |
     * |   ^           |     |   |
     * |    \____      |     |   |
     * |     mapped    +-----+   |
     * |                         |
     * +-------------------------+
     * 
     * Special iterator. It will iterate over all region points. After calling nextRmask we will have:
     * - getPoint will return point in input image coordinates (default behavior) - will return 'P' coordinates , but
     * - nextRmask will return index in input image coordinates as if region was mapped to beginning of 
     *   coordinates of Input image (for first point it will return index for 'I')
     */
    public int nextRmask() {
        Point p = iRegionPointIt.next();
        iDimensionChanged = p.iCoords[0] == 0 && p.numOfZerosInCoordinates() != iNumOfDimensions;
        ilastPoint = iOffsetPoint.add(p);
        return  iInputIt.pointToIndex(p);
    }
    
    boolean iDimensionChanged = false;
    public boolean getRMask() {
        return iDimensionChanged;
    }
}
