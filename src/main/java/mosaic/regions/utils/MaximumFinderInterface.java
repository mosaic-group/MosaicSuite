package mosaic.regions.utils;


import java.util.List;

import mosaic.core.imageUtils.Point;


public interface MaximumFinderInterface {
    public List<Point> getMaximaPointList(float[] pixels, double tolerance, boolean excludeOnEdges);
}
