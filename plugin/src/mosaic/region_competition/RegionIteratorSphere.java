package mosaic.region_competition;
import java.util.NoSuchElementException;

/**
 * Iterates over mask in coordinates of input image
 */
public class RegionIteratorSphere
{
	Mask mask;
	private int[] input;	// size of the input image
	private int[] m_Size;	// size of the mask
	int[] ofs; // "upper left" coordinates of the sphere-region within input image
	
	// iterator
	int cachedNext = -1;		// detect if there is a next next. 
	RegionIterator regionIt;
	RegionIteratorMask maskIt; 	
	
	
	/**
	 * @param inputSize	size of the input (width/height/...)
	 */
	public RegionIteratorSphere(Mask mask, int[] inputSize)
	{
		this.input = inputSize;
		this.mask = mask;
		m_Size = mask.getDimensions();
    }
	
	
	/**
	 * Set the offset of the mask region within the input 
	 * by giving the midpoint of the mask region
	 * @param midPoint
	 */
	public void setMidPoint(Point midPoint)
	{
		Point half = (new Point(m_Size)).div(2);
		ofs = midPoint.sub(half).x;
		initIterators();
	}
	
	/**
	 * Set the offset of the mask region within the input 
	 * by giving the upper left point
	 * @param upperleft Offset of the mask region
	 */
	public void setUpperLeft(Point upperleft)
	{
		this.ofs = upperleft.x;
		initIterators();
	}
	
	private void initIterators()
	{
		cachedNext = -1;
		regionIt = new RegionIterator(input, this.m_Size, ofs);
		maskIt = new RegionIteratorMask(input, this.m_Size, ofs);
	}
	
	public boolean hasNext()
	{
		if(cachedNext<0)
		{
			// no cached next, test if there is a further index
			cachedNext = calcNext();
			
			if(cachedNext<0)
				return false;
			else
				return true;
		}
		else
		{
			// there is a cached next
			return true;
		}
	}
//	
//	int precalculatedNext=-1;
//	private int calculateNext()
//	{
//		return -1;
//	}
	
	public int next()
	{
		if(cachedNext<0)
		{
			int result = calcNext();
			if(result<0)
			{
				throw new NoSuchElementException();
			}
			return result;
		}
		else
		{
			// there is a cached next. return and reset it
			int result = cachedNext;
			cachedNext = -1;
			return result;
		}
		
	}
	
	
	/**
	 * Get the next masked index (input coordinates). 
	 * @return next index in input coordinates. -1 if there is no more index
	 */
	private int calcNext()
	{
		while(regionIt.hasNext())
		{
			int idx = regionIt.next();
			int itMask = maskIt.next();
			
			if(mask.isInMask(itMask))
			{
				// found another index
				return idx;
			}
			else
			{
				// more luck next time
				continue;
			}
		}
		return -1;
	}
	
	
	
}



