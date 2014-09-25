package mosaic.region_competition.utils;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

import mosaic.core.utils.Point;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;



public class MaximumFinder2D extends MaximumFinder implements MaximumFinderInterface
{
	int width;
	int height;
	
	public MaximumFinder2D(int width, int height)
	{
		this.width = width;
		this.height = height;
	}
	
	@Override
    public List<Point> getMaximaPointList(float[] pixels, double tolerance, boolean excludeOnEdges)
    {
    	ImageProcessor ip = new FloatProcessor(width, height, pixels,null);
    	Polygon poly = getMaxima(ip, tolerance, excludeOnEdges);
    	int n = poly.npoints;
    	int[] xs = poly.xpoints;
    	int[] ys = poly.ypoints;
    	
    	ArrayList<Point> list = new ArrayList<Point>(n);
    	for(int i=0; i<n; i++)
    	{
    		list.add(new Point(new int[]{xs[i], ys[i]}));
    	}
    	
    	return list;
    }
	
	@Override
    public List<Point> getMaximaPointList(Img< FloatType > pixels, double tolerance, boolean excludeOnEdges)
    {
		float pixels_prc[] = new float [(int) pixels.size()];
		Cursor < FloatType > vCrs = pixels.cursor();
		
		int loc[] = new int [2];
		
		while (vCrs.hasNext())
		{
			vCrs.fwd();
			vCrs.localize(loc);
			pixels_prc[loc[1]*width+loc[0]] = vCrs.get().get();
		}
		
		List<Point> list = getMaximaPointList(pixels_prc,tolerance,excludeOnEdges);
    	
    	return list;
    }

}
