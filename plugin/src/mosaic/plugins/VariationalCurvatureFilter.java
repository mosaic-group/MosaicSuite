package mosaic.plugins;


import ij.process.FloatProcessor;
import mosaic.plugins.utils.ConvertImg;
import mosaic.plugins.utils.CurvatureFilterBase;
import mosaic.variationalCurvatureFilters.CurvatureFilter;


/**
 * PlugIn providing variational curvature filters (GC/TV/MC) functionality for ImageJ/Fiji
 * @author Krzysztof Gonciarz
 */
public class VariationalCurvatureFilter extends CurvatureFilterBase {
    
    /**
     * Run filter on given image.
     * @param aOutputIp Image with result
     * @param aOriginalIp Input image
     * @param aFilter Filter to be used
     * @param aNumberOfIterations Number of iterations for filter
     */
    private void filterImage(FloatProcessor aOutputIp, FloatProcessor aOriginalIp, CurvatureFilter aFilter, int aNumberOfIterations) {
        // Get dimensions of input image
        int originalWidth = aOriginalIp.getWidth();
        int originalHeight = aOriginalIp.getHeight();
        
        // Generate 2D array for image (it will be rounded up to be divisible
        // by 2). Possible additional points will be filled with last column/row
        // values in convertToArrayAndNormalize
        int roundedWidth = (int) (Math.ceil(originalWidth/2.0) * 2);
        int roundedHeight = (int) (Math.ceil(originalHeight/2.0) * 2);
        float[][] img = new float[roundedHeight][roundedWidth]; 

        // create (normalized) 2D array with input image
        float maxValueOfPixel = (float) aOriginalIp.getMax(); 
        if (maxValueOfPixel < 1.0f) maxValueOfPixel = 1.0f;
        ConvertImg.ImgToYX2Darray(aOriginalIp, img, maxValueOfPixel);
        
        // Run chosen filter on image      
        aFilter.runFilter(img, aNumberOfIterations);

        // Update input image with a result
        ConvertImg.YX2DarrayToImg(img, aOutputIp, maxValueOfPixel);
    }

    @Override
    protected boolean setup(String aArgs) {
        if (aArgs.equals("updateOriginal")) setChangeOriginal(true);
        setFilePrefix("filtered_");
        setSplitMethodMenu(true);
        return true;
    }

    @Override
    protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
        filterImage(aOutputImg, aOrigImg, getCurvatureFilter(), getNumberOfIterations());
    }
}
