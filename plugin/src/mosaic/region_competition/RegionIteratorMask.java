package mosaic.region_competition;

/**
 * Iterates over a Region within an InputImage, 
 * but returns indices relative to the region (and not input)
 */
public class RegionIteratorMask extends RegionIterator
{
	/**
	 * Iterating over region is implemented in such a way, 
	 * that region is used as input' (for RegionIterator) and
	 * the intersection(input,region) is used as region'. 
	 * Offset' is old(Offset) to intersection
	 * So the indices are returned relative to old(region). 
	 */
	public RegionIteratorMask(int[] input, int[] region, int[] ofs) 
	{
		
		// sets the "input size" to the region size
		super(region, region, ofs);
		int[] maskSizes = region.clone();
		int[] maskOfs = new int[dimensions];
		
		//TODO: this is cropping, actually? 
		for(int i=0; i<dimensions; i++)
		{
			if(ofs[i]<0)
			{
				// if ofs < 0, then region is cropped, and the iterator' doesn't start at 0,0
				// but starts at an ofs which points to the intersection(input, region)
				maskOfs[i]= (-ofs[i]);	// start in mask and not at 0,0
				maskSizes[i]+=ofs[i];	// this may be done in crop?
			}
			else
			{
				// the startpoint of region' is within old(input), so the region'-iterator starts at 0,0
				maskOfs[i]=0;
			}
			//mask overflow
			if(ofs[i]+region[i]>input[i])
			{
				int diff=ofs[i]+region[i]-input[i];
				maskSizes[i]-=diff;
			}
		}
		
		setRegion(maskSizes);
		setOfs(maskOfs);
		crop(); // recalculates startindex and new size, cropping should be done already. 
	}
	
	
//	public RegionIteratorMask(int[] input, int[] region, int[] ofs) 
//	{
//		//alternative: 2012.03.31, untested
//		super(input, region, ofs);
//		// now region is cropped, and ofs too
//		for(int i=0; i<dimensions; i++)
//		{
//			this.ofs[i] = this.ofs[i]-ofs[i]; 
//		}
//		// if raw was negative, then this.ofs is now 0, and new ofs gets the correct new positive ofs
//		// if raw was positive, this.ofs is the same, and newofs is 0 -> start at first point of region
//		this.input = region; // new input size is the raw region size
//		this.region = this.region; // the intersection
//		crop(); // crop again, doesn't crop actually, but sets the starts and sizes
//	}
	
}