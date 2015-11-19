package mosaic.core.imageUtils.masks;

import mosaic.core.imageUtils.Connectivity;
import mosaic.core.imageUtils.Point;

public class SphereMask extends EllipseBase {


    private final Connectivity iConnectivity;
    
    /**
     * Create a sphere mask with radius and provided scaling. In case
     * of 2D this is a circle (obviously).
     * @param aRadius Radius of the circle
     * @param aSizeOfRegion Size of the region containing the circle
     * @param aScaling Coordinate spacing
     */
    public SphereMask(float aRadius, int aSizeOfRegion, float[] aScaling) {
        super(aRadius, aSizeOfRegion, aScaling);
        iConnectivity = new Connectivity(iDimensions.length, iDimensions.length - 1);
        fillMask();
    }

    /**
     * Marks in mask points which are part of circle/sphere
     */
    private void fillMask() {
        final int size = iIterator.getSize();
        for (int i = 0; i < size; ++i) {
            final Point offset = iIterator.indexToPoint(i);
            final float ellipseDist = hyperEllipse(offset);
    
            if (ellipseDist <= 1.0f) {
                // Check the neighborhood
                if (isBoundary(offset) == true) {
                    iMask[i] = FgVal;
                    ++iNumOfFgPoints;
                }
                else {
                    iMask[i] = BgVal;
                }
            }
        } 
    }

    /**
     * Checks if any of neighbor points of given aPoint is outside of circle/sphere boundary. 
     * If yes, such point is considered as a boundary of circle. 
     * @return true if boundary point.
     */
    private boolean isBoundary(Point aPoint) {
        for (Point neighborPoint : iConnectivity.iterator()) {
            neighborPoint = neighborPoint.add(aPoint);
            final float vHypEllipse = hyperEllipse(neighborPoint);
            if (vHypEllipse > 1.0f) {
                return true;
            }
        }
        return false;
    }
}
