package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.process.FloatProcessor;
import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.CurvatureFilterBase;
import mosaic.variationalCurvatureFilters.CurvatureFilter;

/**
 * Super resolution based on curvature filters
 * @author Krzysztof Gonciarz
 */
public class Inpainting extends CurvatureFilterBase {
    
    // Mask with inpainting pixels
    ImagePlus iMask;

    /**
     * Run filter on given image.
     * 
     * @param aInputIp Input image (will be changed during processing)
     * @param aFilter Filter to be used
     * @param aNumberOfIterations Number of iterations for filter
     */
    private void superResolution(FloatProcessor aInputIp, FloatProcessor aOriginalIp, CurvatureFilter aFilter, int aNumberOfIterations) {
        // Get dimensions of input image
        int originalWidth = aOriginalIp.getWidth();
        int originalHeight = aOriginalIp.getHeight();

        // Generate 2D array for image (it will be rounded up to be divisible
        // by 2). Possible additional points will be filled with last column/row
        // values in convertToArrayAndNormalize
        int roundedWidth = (int) (Math.ceil(originalWidth / 2.0) * 2);
        int roundedHeight = (int) (Math.ceil(originalHeight / 2.0) * 2);
        float[][] img = new float[roundedHeight][roundedWidth];

        // create (normalized) 2D array with input image
        float maxValueOfPixel = (float) aOriginalIp.getMax();
        if (maxValueOfPixel < 1.0f) maxValueOfPixel = 1.0f;
        ImgUtils.ImgToYX2Darray(aOriginalIp, img, maxValueOfPixel);

        // Run chosen filter on image
        aFilter.runFilter(img, aNumberOfIterations, new CurvatureFilter.Mask() {
            public boolean shouldBeProcessed(int x, int y) {
                // Skip pixels from original image
                return iMask.getProcessor().getf(x, y) != 0;
            }
        });

        ImgUtils.YX2DarrayToImg(img, aInputIp, maxValueOfPixel);
    }

    @Override
    protected boolean setup(String aArgs) {
        setFilePrefix("inpainting_");

        OpenDialog od = new OpenDialog("(Inpainting) Open mask file", "");
        String directory = od.getDirectory();
        String name = od.getFileName();

        if (name != null) {
            iMask = new Opener().openImage(directory + name);

            int iw = getInputImg().getWidth();
            int ih = getInputImg().getHeight();
            
            if (iMask.getWidth() != iw || iMask.getHeight() != ih) {
                IJ.error("Mask should have same dimensions as input image!" +
                         " Input image: [" + iw + "x" + ih +"]" +
                         " Mask: [" + iMask.getWidth() + "x" + iMask.getHeight() + "]");
                return false;
            }
            
            return true;
        }
        
        return false;
    }

    @Override
    protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
        superResolution(aOutputImg, aOrigImg, getCurvatureFilter(), getNumberOfIterations());
    }
}
