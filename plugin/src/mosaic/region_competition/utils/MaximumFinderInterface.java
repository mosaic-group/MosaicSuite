package mosaic.region_competition.utils;

import java.util.List;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

import mosaic.core.utils.Point;

public interface MaximumFinderInterface
{
    public List<Point> getMaximaPointList(float[] pixels, 
    		double tolerance, 
    		boolean excludeOnEdges);
    
    public List<Point> getMaximaPointList(Img<FloatType> pixels, 
    		double tolerance, 
    		boolean excludeOnEdges);
}