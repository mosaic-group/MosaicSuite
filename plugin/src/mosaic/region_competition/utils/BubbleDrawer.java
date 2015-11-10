package mosaic.region_competition.utils;


import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.RegionIteratorMask;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.masks.BallMask;


/**
 * This shall be the nice version with new Sphere Iterator
 */
public class BubbleDrawer {

    private final LabelImage labelImage;
    private final RegionIteratorMask sphereIt;
    private final BallMask sphere;

    public BubbleDrawer(LabelImage labelImage, int radius, int size) {
        this.labelImage = labelImage;

        final int dim = labelImage.getNumOfDimensions();
        final int[] input = labelImage.getDimensions();
        float[] scaling = new float[dim];
        for (int i = 0; i < dim; i++) {
            scaling[i] = 1.0f;
        }
        sphere = new BallMask(radius, size, scaling);
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
