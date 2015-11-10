package mosaic.core.imageUtils.masks;

import mosaic.core.imageUtils.Connectivity;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.iterators.SpaceIterator;

public class CircleMask implements Mask {

    private final float iMiddlePoint[];
    private final float iRadius[];
    private final float iScaling[];
    private final int[] iDimensions;
    
    private final boolean iMask[];
    
    private final SpaceIterator iIterator;
    private final Connectivity iConnectivity;
    
    private int iNumOfFgPoints = 0;

    /**
     * Create a circle mask with radius and provided scaling
     * @param aRadius Radius of the circle
     * @param aSizeOfRegion Size of the region containing the circle
     * @param aScaling Coordinate spacing
     */
    public CircleMask(float aRadius, int aSizeOfRegion, float[] aScaling) {
        int numOfDims = aScaling.length;
        
        iMiddlePoint = new float[numOfDims];
        iRadius = new float[numOfDims];
        iDimensions = new int[numOfDims];
        for (int i = 0; i < numOfDims; ++i) {
            iRadius[i] = aRadius;
            iMiddlePoint[i] = aSizeOfRegion / 2.0f;
            iDimensions[i] = aSizeOfRegion;
        }
        iScaling = aScaling;

        iIterator = new SpaceIterator(iDimensions);
        iConnectivity = new Connectivity(numOfDims, numOfDims - 1);
        
        iMask = new boolean[iIterator.getSize()];
        
        fillMask();
    }

    @Override
    public boolean isInMask(int aIndex) {
        return iMask[aIndex] == FgVal;
    }

    @Override
    public int[] getDimensions() {
        return iDimensions;
    }

    @Override
    public int getNumOfFgPoints() {
        return iNumOfFgPoints;
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
     * Calculates equation of a circle divided by squared radius. It takes care about scaling.
     * @param aPoint - input point in region containing circle
     * @return value of ((x - x0)^2 + (y - y0)^2 + ...) / r^2   
     *         if value == 1 we are on boundary of circle
     */
    private float hyperEllipse(Point aPoint) {
        int[] iCoords = aPoint.iCoords;
        float result = 0.0f;
        for (int vD = 0; vD < iRadius.length; vD++) {
            // add 0.5 to each coordinate to place it in the middle of pixel
            float dist = (iCoords[vD] + 0.5f - iMiddlePoint[vD]) * iScaling[vD];
            result += dist * dist / (iRadius[vD] * iRadius[vD]);
        }
        return result;
    }

    /**
     * Checks if any of neighbor points of given aPoint is outside of circle/sphere boundary. If yes, such point
     * is considered as a boundary of circle. 
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
