package mosaic.core.imageUtils.iterators;

import mosaic.core.imageUtils.Point;

/**
 * Region iterator is responsible for iterating over some region inside input coordinates.
 * It handles all cropping stuff (if region with its offset goes beyond input coordinates, or if region is bigger than input).
 * 
 * !!! It gives Index/Point coordinates in respect to input dimensions !!!
 * 
 * Input dims
 * +-----+-------------------+
 * |                         |
 * |             Region dims |
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
public class RegionIterator extends BaseIterator
{
    SpaceIterator iInputIt;
    Point iLastPointRegionBased = null;
    
    /**
     * @param iInputDims dimensions of the input image
     * @param iRegionDims dimensions of the region
     * @param iRegionOffset offset of the region in the input image (upper left)
     */
    public RegionIterator(int[] iInputDims, int[] iRegionDims, int[] iRegionOffset) {
        super(iInputDims, iRegionDims, iRegionOffset);
        iInputIt = new SpaceIterator(iInputDims);
    }

    @Override
    public int next() {
        iLastPoint = iOffsetPoint.add(iIntersectionPointIt.next());
        return  iInputIt.pointToIndex(iLastPoint);
    }
}
