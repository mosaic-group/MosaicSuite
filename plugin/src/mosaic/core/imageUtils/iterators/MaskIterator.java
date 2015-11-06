package mosaic.core.imageUtils.iterators;

/**
 * Region iterator is responsible for iterating over some region inside input coordinates.
 * It handles all cropping stuff (if region with its offset goes beyond input coordinates, or if region is bigger than input).
 * 
 * !!! It gives Index/Point coordinates in respect to region dimensions !!!
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
public class MaskIterator extends BaseIterator {

    IndexIterator iRegionIt;
    
    /**
     * @param iInputDims dimensions of the input image
     * @param iRegionDims dimensions of the region
     * @param iRegionOffset offset of the region in the input image (upper left)
     */
    public MaskIterator(int[] iInputDims, int[] iRegionDims, int[] iRegionOffset) {
        super(iInputDims, iRegionDims, iRegionOffset);
        iRegionIt = new IndexIterator(iRegionDims);
    }
    
    @Override
    public int next() {
        iLastPoint = iIntersectionPointIt.next();
        return  iRegionIt.pointToIndex(iLastPoint);
    }
}
