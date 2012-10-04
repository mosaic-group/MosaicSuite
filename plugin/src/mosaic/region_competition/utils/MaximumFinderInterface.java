package mosaic.region_competition.utils;

import java.util.List;

import mosaic.region_competition.Point;

public interface MaximumFinderInterface
{
    public List<Point> getMaximaPointList(float[] pixels, 
    		double tolerance, 
    		boolean excludeOnEdges);
}