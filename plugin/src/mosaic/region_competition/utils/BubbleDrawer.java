package mosaic.region_competition.utils;


import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIteratorMask;
import mosaic.core.utils.SphereMask;
import mosaic.region_competition.LabelImageRC;


/**
 * This shall be the nice version with new Sphere Iterator
 */
public class BubbleDrawer {

    private final LabelImageRC labelImage;
    private final RegionIteratorMask sphereIt;
    private final SphereMask sphere;

    public BubbleDrawer(LabelImageRC labelImage, int radius, int size) {
        this.labelImage = labelImage;

        final int dim = labelImage.getNumOfDimensions();
        final int[] input = labelImage.getDimensions();
        sphere = new SphereMask(radius, size, dim);
        sphereIt = new RegionIteratorMask(sphere, input);
    }

    /**
     * @param ofs upper left point
     * @param val value to draw inside the sphere
     */
    public void drawUpperLeft(Point ofs, int val) {
        sphereIt.setUpperLeft(ofs);
        while (sphereIt.hasNext()) {
            final int idx = sphereIt.next();
            labelImage.setLabel(idx, val);
        }
    }

    public void drawCenter(Point center, int val) {
        sphereIt.setMidPoint(center);
        while (sphereIt.hasNext()) {
            final int idx = sphereIt.next();
            labelImage.setLabel(idx, val);
        }
    }

}
