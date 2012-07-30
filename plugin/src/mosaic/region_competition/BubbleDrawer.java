package mosaic.region_competition;


public class BubbleDrawer extends SphereBitmapImageSource
{
//TODO make nicer class hierarchiy
	public BubbleDrawer(LabelImage labelImage, int radius, int size)
	{
		super(labelImage, radius, size);
	}
	
	/**
	 * @param start upper left point
	 * @param val 	value to draw inside the sphere
	 */
	void doSphereIteration(Point start, int val)
	{
		
		//TODO change region iterator so one can reuse the iterator and just set the new ofs
		RegionIterator it = new RegionIterator(labelImage.dimensions, this.m_Size, start.x);
		RegionIteratorMask maskIt = new RegionIteratorMask(labelImage.dimensions, this.m_Size, start.x);
		
		while(it.hasNext())	// iterate over sphere region
		{
			int idx = it.next();
			
			int maskIdx = maskIt.next();
			int maskvalue = mask[maskIdx];
			
			if(maskvalue==m_ForegroundValue)
			{
				labelImage.setLabel(idx, val);
			}
//			else
//			{
//				labelImage.setLabel(idx, labelImage.bgLabel);
//			}

		}
		
	}
}
