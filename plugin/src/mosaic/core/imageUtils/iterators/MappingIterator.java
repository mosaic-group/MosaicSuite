package mosaic.core.imageUtils.iterators;

import mosaic.core.imageUtils.Point;

/**
 * Region iterator is responsible for iterating over some region inside input coordinates.
 * It handles all cropping stuff (if region with its offset goes beyond input coordinates, or if region is bigger than input).
 * 
 * Special iterator. It will iterate over all region points. After calling nextRmask we will have:
 * - getPoint will return point in input image coordinates (default behavior) - will return 'P' coordinates , but
 * - next will return index in input image coordinates as if region was mapped to beginning of 
 *   coordinates of Input image (for first point it will return index for 'I')
 *   
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
 * 
 *   
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class MappingIterator extends BaseIterator
{
    IndexIterator iInputIt;
    Point iLastPointRegionBased = null;
    boolean iDimensionChanged = false;
    
    /**
     * @param iInputDims dimensions of the input image
     * @param iRegionDims dimensions of the region
     * @param iRegionOffset offset of the region in the input image (upper left)
     */
    public MappingIterator(int[] iInputDims, int[] iRegionDims, int[] iRegionOffset) {
        super(iInputDims, iRegionDims, iRegionOffset);
        iInputIt = new IndexIterator(iInputDims);
    }

    @Override
    public int next() {
        Point p = iIntersectionPointIt.next();
        iDimensionChanged = p.iCoords[0] == 0 && p.numOfZerosInCoordinates() != iNumOfDimensions;
        iLastPoint = iOffsetPoint.add(p);
        return  iInputIt.pointToIndex(p);
    }

    public boolean hasDimensionWrapped() {
        return iDimensionChanged;
    }
}
