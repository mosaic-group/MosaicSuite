package mosaic.core.utils;

/**
 *
 * Mask base class
 *
 * @author Pietro Incardona
 *
 */

abstract class Mask
{
    public final byte bgVal = 0;
    public final byte fgVal = 1;

    public byte mask[]; // Java uses bytes for bools anyway; Alternative: java.util.BitSet

    /**
     *
     * Check whetever idx is a valid mask idx
     *
     * @param idx A valid mask-index
     * @return
     */
    public abstract boolean isInMask(int idx);

    /**
     *
     * Get the size of the mask
     *
     * @return int[] size of the mask
     */
    public abstract int[] getDimensions();


    /**
     *
     * Get the number of foreground points in the mask
     *
     * @return
     */
    public abstract int getFgPoints();
}