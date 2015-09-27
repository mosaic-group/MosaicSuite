package mosaic.region_competition.utils;


import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import mosaic.core.utils.Point;


public class MaximumFinder2D extends MaximumFinder implements MaximumFinderInterface {

    private final int width;
    private final int height;

    public MaximumFinder2D(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public List<Point> getMaximaPointList(float[] pixels, double tolerance, boolean excludeOnEdges) {
        final ImageProcessor ip = new FloatProcessor(width, height, pixels, null);
        final Polygon poly = getMaxima(ip, tolerance, excludeOnEdges);
        final int n = poly.npoints;
        final int[] xs = poly.xpoints;
        final int[] ys = poly.ypoints;

        final ArrayList<Point> list = new ArrayList<Point>(n);
        for (int i = 0; i < n; i++) {
            list.add(new Point(new int[] { xs[i], ys[i] }));
        }

        return list;
    }
}
