package mosaic.psf2d;


/**
 * Defines a particle and holds all the relevant info for it.
 * X and Y coordinates are not in the usual graph coordinates sense but in the image sense;
 * (0, 0) is the upper left corner
 * x is horizontal left to right
 * y is vertical top to bottom
 */
public class PsfSourcePosition {
    public float iX;
    public float iY;

    /**
     * @param aX - original x coordinates
     * @param y - original y coordinates
     */
    public PsfSourcePosition(float aX, float aY) {
        iX = aX;
        iY = aY;
    }
}
