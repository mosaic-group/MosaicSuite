package mosaic.core.utils;

public class RectangleMask  extends Mask
{
	int dim;
	int rad;
	
	int m_Size[];
	int m_Radius[];
	float spacing[];
	
	IndexIterator iterator;
	int fgPoints = 0;
	
	/**
	 * Create a rectangle mask
	 * 
	 * @param radius 	Radius of the circle
	 * @param size 		Size of the region containing the circle
	 * @param dim		dimensionality
	 */
	public RectangleMask(int l[]) 
	{	
		m_Size = new int[dim];
		
		for (int i = 0; i < dim; i++) 
		{
			m_Size[i] = l[i];
		}
		
		iterator = new IndexIterator(m_Size);
		
		mask = new byte[iterator.getSize()];
		fillMask();		
	}
	
	private boolean isBoundary(Point ofs)
	{
		for (int i = 0 ; i < ofs.x.length ; i++)
		{
			if (ofs.x[i] == 0)
				return true;
			if (ofs.x[i] == m_Size[i]-1)
				return true;
		}
		
		return false;
	}
	
	private void fillMask()
	{
		fgPoints = 0;
		int size = iterator.getSize();
		for(int i=0; i<size; i++)
		{
			Point ofs = iterator.indexToPoint(i);
			
			// Check the neighboorhood
				
			if (isBoundary(ofs) == true)
			{
				mask[i] = fgVal;
				fgPoints++;
			}
			else
			{
				mask[i]=bgVal;
			}	
		} //for
	}
	
	@Override
	public boolean isInMask(int idx)
	{
		return mask[idx] == fgVal;
	}

	@Override
	public int[] getDimensions()
	{
		return this.m_Size;
	}
	

	/*
	 * Get the number or Foreground points in the mask
	*/
	@Override
	public int getFgPoints() {return fgPoints;};
	
	
}

