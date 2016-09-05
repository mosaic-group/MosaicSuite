package mosaic.core.imageUtils.masks;


/**
 * Mask interface
 */
public interface Mask {

    public static final boolean FgVal = true;
    public static final boolean BgVal = !FgVal;

    /**
     * Check weather aIndex is a mask index
     * @param aIndex true if a mask index
     * @return
     */
    boolean isInMask(int aIndex);

    /**
     * Get the dimensions of the mask
     * @return dimensions of a mask
     */
     int[] getDimensions();

    /**
     * Get the number of foreground points in the mask
     * @return
     */
    int getNumOfFgPoints();
}
