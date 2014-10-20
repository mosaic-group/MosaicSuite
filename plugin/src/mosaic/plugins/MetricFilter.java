package mosaic.plugins;

import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.RegionIterator;
import mosaic.core.utils.RegionIteratorMask;
import mosaic.core.utils.SphereMask;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import io.scif.img.ImgOpener;

class MetricFilter implements PlugInFilter
{

	@Override
	public void run(ImageProcessor arg0) 
	{

		
	}

	<T extends RealType<T>> MetricFilter(Img<T> img, Class<T> cls, int radius)
	{
		// Create an intensity image from the img
		
		IntensityImage ImgI = new IntensityImage(img,cls);
		
		// Out image
		IntensityImage ImgO = new IntensityImage(ImgI.getDimensions());
		
		// Iterate through all the points
		
		RegionIterator rg = new RegionIterator(ImgI.getDimensions());
		
		// the radius
		
		SphereMask mask = new SphereMask(radius, 2*radius+1, ImgI.getDimensions().length);
		RegionIteratorMask rgm = new RegionIteratorMask(mask, ImgI.getDimensions());
		
		while(rg.hasNext())
		{
			rg.next();
			
			// center the point here
			
			
		}
	}
	
	@Override
	public int setup(String arg0, ImagePlus aImp) 
	{		
		return 0;
	}
	
}