package mosaic.core.binarize;


import mosaic.core.image.Point;


/**
 * This interface Binarize the original image
 *
 * @author Pietro Incardona
 */

public interface BinarizedImage {

    /**
     * Evaluate the value of the pixel at one point
     *
     * @param p Point
     * @return the value of the pixel 0 or 1 (true pr false)
     */
    public abstract boolean EvaluateAtIndex(Point p);
}
