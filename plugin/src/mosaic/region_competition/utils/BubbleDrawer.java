package mosaic.region_competition.utils;

import mosaic.region_competition.LabelImage;
import mosaic.region_competition.Point;
import mosaic.region_competition.RegionIteratorSphere;
import mosaic.region_competition.SphereMask;

/**
 * This shall be the nice version with new Sphere Iterator
 */
public class BubbleDrawer
{
	LabelImage labelImage;
	RegionIteratorSphere sphereIt;
	SphereMask sphere;
	
	public BubbleDrawer(LabelImage labelImage, int radius, int size)
	{
		this.labelImage = labelImage;
		
		int dim = labelImage.getDim();
		int[] input = labelImage.getDimensions();
		sphere = new SphereMask(radius, size, dim);
		sphereIt = new RegionIteratorSphere(sphere, input);
	}
	
	/**
	 * @param ofs 	upper left point
	 * @param val 	value to draw inside the sphere
	 */
	public void drawUpperLeft(Point ofs, int val)
	{
		sphereIt.setUpperLeft(ofs);
		while(sphereIt.hasNext())
		{
			int idx = sphereIt.next();
			labelImage.setLabel(idx, val);
		}
	}
	
	public void drawCenter(Point center, int val)
	{
		sphereIt.setMidPoint(center);
		while(sphereIt.hasNext())
		{
			int idx = sphereIt.next();
			labelImage.setLabel(idx, val);
		}
	}
	
	
}
