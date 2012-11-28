package mosaic.region_competition;

public abstract class Mask
{
	public final byte bgVal = 0;
	public final byte fgVal = 1;
	
	public byte mask[]; // Java uses bytes for bools anyway; Alternative: java.util.BitSet
	/**
	 * @param idx A valid mask-index
	 * @return
	 */
	public abstract boolean isInMask(int idx);
	public abstract int[] getDimensions();
	public abstract int getFgPoints();
}