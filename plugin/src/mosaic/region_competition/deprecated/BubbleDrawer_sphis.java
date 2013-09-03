package mosaic.region_competition.deprecated;

import mosaic.region_competition.LabelImage;
import mosaic.region_competition.Point;
import mosaic.region_competition.RegionIterator;
import mosaic.region_competition.RegionIteratorMask;


public class BubbleDrawer_sphis extends SphereBitmapImageSource_sphis
{
//TODO make nicer class hierarchy
	public BubbleDrawer_sphis(LabelImage labelImage, int radius, int size)
	{
		super(labelImage, radius, size);
	}
	
	/**
	 * @param start upper left point
	 * @param val 	value to draw inside the sphere
	 */
	public void doSphereIteration(Point start, int val)
	{
		
		//TODO change region iterator so one can reuse the iterator and just set the new ofs
		RegionIterator it = new RegionIterator(labelImage.getDimensions(), this.m_Size, start.x);
		RegionIteratorMask maskIt = new RegionIteratorMask(labelImage.getDimensions(), this.m_Size, start.x);
		
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