package mosaic.core.imageUtils.masks;

import mosaic.core.imageUtils.Point;

public class BallMask extends EllipseBase {

    /**
     * Create a ball mask with radius and provided scaling. In case
     * of 2D this is a disk (obviously).
     * @param aRadius Radius of the circle
     * @param aSizeOfRegion Size of the region containing the circle
     * @param aScaling Coordinate spacing
     */
    public BallMask(float aRadius, int aSizeOfRegion, float[] aScaling) {
        super(aRadius, aSizeOfRegion, aScaling);
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
                iMask[i] = FgVal;
                ++iNumOfFgPoints;
            }
            else {
                iMask[i] = BgVal;
            }
        } 
    }
}
