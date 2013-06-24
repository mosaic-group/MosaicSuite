package mosaic.region_competition.initializers;

import mosaic.region_competition.LabelImage;
import mosaic.region_competition.RegionIterator;

public class BoxInitializer extends Initializer
{

	public BoxInitializer(LabelImage labelImage)
	{
		super(labelImage);
	}
	
	double ratio = 0.95;
	

	/**
	 * creates an initial guess (of the size r*labelImageSize)
	 * @param r fraction of sizes of the guess
	 */
	public void initRatio(double r)
	{
		int[] region = dimensions.clone();
		int[] ofs = dimensions.clone();
		for(int i=0; i<dim; i++)
		{
			region[i]=(int)(region[i]*r);
			ofs[i] = (dimensions[i]-region[i])/2;
		}
		
		
		
		int label = 1;
		RegionIterator it = new RegionIterator(dimensions, region, ofs);
		while(it.hasNext())
		{
			int idx = it.next();
			labelImage.setLabel(idx, label);
		}
	}

	@Override
	public void initDefault()
	{
		initRatio(ratio);
	}

}
