package mosaic.region_competition.initializers;


import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;

import java.util.List;

import mosaic.core.binarize.BinarizedIntervalIntesityImage;
import mosaic.core.utils.FloodFill;
import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.Point;
import mosaic.region_competition.LabelImageRC;
import mosaic.region_competition.utils.BubbleDrawer;
import mosaic.region_competition.utils.MaximumFinder2D;
import mosaic.region_competition.utils.MaximumFinder3D;
import mosaic.region_competition.utils.MaximumFinderInterface;


public class MaximaBubbles extends Initializer {

    private final MaximumFinderInterface maximumFinder;
    private int rad = 5; // bubble size
    private double sigma = 2.0; // smoothing gauss sigma
    private double tolerance = 0.005; // localmax tolerance
    private boolean excludeOnEdges = false;

    private int regionThreshold = 4; // regions smaller than this values will be bubbled

    IntensityImage intensityImage;
    
    public MaximaBubbles(IntensityImage intensityImage, LabelImageRC labelImage, int rad_t, double sigma_t, double tol_t, int r_t) {
        super(labelImage);
        this.intensityImage = intensityImage;
        final int dim = labelImage.getDim();
        if (dim == 2) {
            final int[] dims = intensityImage.getDimensions();
            maximumFinder = new MaximumFinder2D(dims[0], dims[1]);
        }
        else if (dim == 3) {
            maximumFinder = new MaximumFinder3D(labelImage.getDimensions());
        }
        else {
            throw new RuntimeException("Not supported dimension for MaximumFinder");
        }

        rad = rad_t;
        sigma = sigma_t;
        tolerance = tol_t;
        regionThreshold = r_t;
    }

    /**
     * Smoothes a copy of the {@link IntensityImage} and sets it as new
     * one. Smoothing by gaussian blur with sigma.
     */
    private void smoothIntensityImage() {
        ImagePlus imp = intensityImage.imageIP;

        imp = new Duplicator().run(imp);
        IJ.run(imp, "Gaussian Blur...", "sigma=" + sigma + " stack");
        IJ.wait(40);
        this.intensityImage = new IntensityImage(imp);
    }

    public void initFloodFilled() {
        smoothIntensityImage();

        List<Point> list;
        list = maximumFinder.getMaximaPointList(intensityImage.dataIntensity, tolerance, excludeOnEdges);

        final BinarizedIntervalIntesityImage foo = new BinarizedIntervalIntesityImage(intensityImage);
        int color = 1;
        for (final Point p : list) {
            final float val2 = intensityImage.get(p);
            final float val1 = (float) (val2 * (1.0 - 2 * tolerance));
            foo.AddThresholdBetween(val1, val2);

            final FloodFill ff = new FloodFill(labelImage.getConnFG(), foo, p);

            int n = 0;
            for (final Point pp : ff) {
                labelImage.setLabel(pp, color);
                n++;
            }

            // System.out.print("Region " + n);

            // if region was very small, draw a bubble
            if (n < regionThreshold) {
                final BubbleDrawer bd = new BubbleDrawer(labelImage, rad / 2, rad);
                bd.drawCenter(p, color);
            }

            color++;

            foo.clearThresholds();
        }
    }

    public void setGaussSigma(int sigma) {
        this.sigma = sigma;
    }

    public void setBubbleRadius(int radius) {
        this.rad = radius;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    /**
     *
     */
    public void setExcludeOnEdges(boolean b) {
        this.excludeOnEdges = b;
    }

    /**
     * If floodfilled maximum is smaller than this value,
     * it draws a bubble with radius rad.
     */
    public void setMinimalRegionThreshold(int regionThreshold) {
        this.regionThreshold = regionThreshold;

    }

}
