package mosaic.region_competition.initializers;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;

import java.util.List;

import mosaic.region_competition.FloodFill;
import mosaic.region_competition.IntensityImage;
import mosaic.region_competition.LabelImage;
import mosaic.region_competition.MultipleThresholdImageFunction;
import mosaic.region_competition.MultipleThresholdIntenityImageFunction;
import mosaic.region_competition.Point;
import mosaic.region_competition.utils.BubbleDrawer;
import mosaic.region_competition.utils.MaximumFinder2D;
import mosaic.region_competition.utils.MaximumFinder3D;
import mosaic.region_competition.utils.MaximumFinderInterface;

public class MaximaBubbles extends DataDrivenInitializer
{
	MaximumFinderInterface maximumFinder;
	int rad = 5;	// bubble size
	int sigma = 2; 	// smoothing gauss sigma
	double tolerance = 0.005;		// localmax tolerance
	boolean excludeOnEdges = false;
	
	int regionThreshold = 4; // regions smaller than this values will be bubbled
	
	public MaximaBubbles(IntensityImage intensityImage, LabelImage labelImage)
	{
		super(intensityImage, labelImage);
		int dim = labelImage.getDim();
		if(dim==2)
		{
			int[] dims = intensityImage.getDimensions();
			maximumFinder = new MaximumFinder2D(dims[0], dims[1]);
		} else if(dim==3)
		{
			maximumFinder = new MaximumFinder3D(labelImage.getDimensions());
			
		} else
		{
			throw new RuntimeException("Not supported dimension for MaximumFinder");
		}
	}

	
	/**
	 * Smoothes a copy of the {@link IntensityImage} and sets it as new 
	 * one. Smoothing by gaussian blur with sigma. 
	 */
	private void smoothIntensityImage()
	{
		ImagePlus imp = intensityImage.imageIP;
		imp = new Duplicator().run(imp);
		IJ.run(imp, "Gaussian Blur...", "sigma="+sigma+" stack");
		this.intensityImage = new IntensityImage(imp);
	}
	
	public void initFloodFilled()
	{
		smoothIntensityImage();
		
		List<Point> list;
		list = maximumFinder.getMaximaPointList(intensityImage.dataIntensity, tolerance, excludeOnEdges);
		
		
		MultipleThresholdImageFunction foo = new MultipleThresholdIntenityImageFunction(intensityImage);
		int color = 1;
		for(Point p : list)
		{
			float val2 = intensityImage.get(p);
			float val1 = (float)(val2*(1.0-2*tolerance));
			foo.AddThresholdBetween(val1, val2);
			
			FloodFill ff = new FloodFill(labelImage.getConnFG(), foo, p);
			
			int n=0;
			for(Point pp : ff)
			{
				labelImage.setLabel(pp, color);
				n++;
			}
			
			// if region was very small, draw a bubble
			if(n<regionThreshold)
			{
				BubbleDrawer bd = new BubbleDrawer(labelImage, regionThreshold/2, regionThreshold);
				bd.drawCenter(p, color);
			}
			
			color++;
			
			foo.clearThresholds();
		}
	}

	public void initBrightBubbles()
	{
		smoothIntensityImage();
		
		List<Point> list;
		list = maximumFinder.getMaximaPointList(intensityImage.dataIntensity, tolerance, false);
		
		BubbleDrawer bubbler = new BubbleDrawer(labelImage, rad, 2*rad+1);
		
		int color = 1;
		for(Point p : list)
		{
			bubbler.drawCenter(p, color++);
		}
	}
	
	
	
	@Override
	public void initDefault()
	{
		initBrightBubbles();
	}
	
	
	
	

	public void setGaussSigma(int sigma)
	{
		this.sigma = sigma;
	}
	
	public void setBubbleRadius(int radius)
	{
		this.rad = radius;
	}
	
	public void setTolerance(double tolerance)
	{
		this.tolerance = tolerance;
	}
	
	/**
	 * 
	 */
	public void setExcludeOnEdges(boolean b)
	{
		this.excludeOnEdges = b;
	}
	
	/**
	 * If floodfilled maximum is smaller than this value, 
	 * it draws a bubble with radius rad.
	 */
	public void setMinimalRegionThreshold(int regionThreshold)
	{
		this.regionThreshold = regionThreshold;
		
	}
	
	
	
	
	
	
	
	
	

}
