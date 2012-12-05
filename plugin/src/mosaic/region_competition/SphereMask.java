package mosaic.region_competition;


public class SphereMask extends Mask
{
	int dim;
	int rad;
	
	int m_Size[];
	int m_Radius[];
	
	IndexIterator iterator;
	int fgPoints = 0;
	
	/*
	 * Get the number or Foreground points in the mask
	*/
	@Override
	public int getFgPoints() {return fgPoints;};
	
	/**
	 * @param radius 	Radius of the sphere
	 * @param size 		Size of the region containing the sphere
	 * @param dim		dimensionality
	 */
	public SphereMask(int radius, int size, int dim)
	{
		this.dim = dim;
		rad = radius;
		
		m_Size = new int[dim];
		m_Radius = new int[dim];
		
		for (int i = 0; i < dim; i++) 
		{
			m_Radius[i] = radius;
			m_Size[i] = size;
		}
		
		iterator = new IndexIterator(m_Size);
		
		mask = new byte[iterator.getSize()];
		fillMask();			
    }
	
	
	private void fillMask()
	{
		fgPoints = 0;
		int size = iterator.getSize();
		for(int i=0; i<size; i++)	// over region
		{
			Point ofs = iterator.indexToPoint(i);
			
			int[] vIndex = (ofs).x;

			float vHypEllipse = 0;
			for(int vD = 0; vD < dim; vD++)
			{
				vHypEllipse += 
					(vIndex[vD] - (m_Size[vD]-1) / 2.0)
					*(vIndex[vD] - (m_Size[vD]-1) / 2.0)
					/(m_Radius[vD] * m_Radius[vD]);
			}
			
			if(vHypEllipse <= 1.0f)
			{
				fgPoints++;
				
				// is in region
				mask[i]=fgVal;
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

	public int getRadius()
	{
		return this.rad;
	}

	@Override
	public int[] getDimensions()
	{
		return this.m_Size;
	}
	
}