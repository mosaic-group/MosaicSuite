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

/**
 * Creates bubbles around found maxima points.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class MaximaBubbles extends Initializer {

    private final MaximumFinderInterface iMaximumFinder;
    private int iRadius; 
    private double iSigma; 
    private double iTolerance;
    private int iMinimumRegionSize; 
    private IntensityImage iIntensityImage;
    
    /**
     * Creates bubbles around maxima points.
     * @param aIntensityImage - input intensity image
     * @param aLabelImage - label image to be labeled
     * @param aSigma - sigma for gauss pre-smoothing of intensity image
     * @param aTolerance - tolerance in range 0-1 (0 no tolerance at all, 1 full tolerance) of points around maxima points
     * @param aRadius - radius of bubble - in case when maxima point region (with threshold) is too small, then radius bubble is drawn
     * @param aMinimumRegionSize - if number of found points around maixmum is less then aMinimumRegionSize then sphere bubble with aRadius is drawn in maximum point
     */
    public MaximaBubbles(IntensityImage aIntensityImage, LabelImageRC aLabelImage, double aSigma, double aTolerance, int aRadius, int aMinimumRegionSize) {
        super(aLabelImage);
        
        iMaximumFinder = createMaximumFinder(aIntensityImage);
        iRadius = aRadius;
        iSigma = aSigma;
        iTolerance = aTolerance;
        iMinimumRegionSize = aMinimumRegionSize;
        iIntensityImage = aIntensityImage;
    }

    public void initialize() {
        smoothIntensityImage();

        List<Point> list = iMaximumFinder.getMaximaPointList(iIntensityImage.dataIntensity, iTolerance, /* excludeOnEdges */ false);
        final BinarizedIntervalIntesityImage binarizedImage = new BinarizedIntervalIntesityImage(iIntensityImage);
        
        int label = 1;
        for (final Point maximumPoint : list) {
            setLabelInPointArea(binarizedImage, label, maximumPoint);
            label++;
        }
    }

    private void setLabelInPointArea(final BinarizedIntervalIntesityImage aBinarizedImage, int aLabel, final Point aMaximumPoint) {
        // Calculate and set threshold around maximum point
        final float maximumPointValue = iIntensityImage.get(aMaximumPoint);
        final float thresholdedValue = (float) (maximumPointValue * (1.0 - iTolerance));
        aBinarizedImage.clearThresholds();
        aBinarizedImage.AddThresholdBetween(thresholdedValue, maximumPointValue);
   
        // Find all points connected to maximum
        final FloodFill ff = new FloodFill(iLabelImage.getConnFG(), aBinarizedImage, aMaximumPoint);
        if (ff.size() < iMinimumRegionSize) {
            // if region is very small, draw a bubble
            final BubbleDrawer bd = new BubbleDrawer(iLabelImage, iRadius / 2, iRadius);
            bd.drawCenter(aMaximumPoint, aLabel);
        } 
        else {
            for (final Point p : ff) {
                iLabelImage.setLabel(p, aLabel);
            }
        }
    }

    /**
     * Creates MaximumFinder basing on dimensionality of input image
     * @param aIntensityImage
     * @return
     */
    private MaximumFinderInterface createMaximumFinder(IntensityImage aIntensityImage) {
        MaximumFinderInterface maximumFinder;
        
        final int dim = aIntensityImage.getDim();
        if (dim == 2) {
            maximumFinder = new MaximumFinder2D(aIntensityImage.getDimensions());
        }
        else if (dim == 3) {
            maximumFinder = new MaximumFinder3D(aIntensityImage.getDimensions());
        }
        else {
            throw new RuntimeException("Not supported dimension for MaximumFinder");
        }
        
        return maximumFinder;
    }

    /**
     * Smoothes a copy of the {@link IntensityImage} and sets it as new
     * one. Smoothing by gaussian blur with sigma.
     */
    private void smoothIntensityImage() {
        ImagePlus imp = new Duplicator().run(iIntensityImage.imageIP);
        IJ.run(imp, "Gaussian Blur...", "sigma=" + iSigma + " stack");
        iIntensityImage = new IntensityImage(imp);
    }
}
