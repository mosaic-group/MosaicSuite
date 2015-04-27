package mosaic.plugins;


import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mosaic.variationalCurvatureFilters.CurvatureFilter;

/**
 * Plugin providing variational curvature filters (GC/TV/MC) functionality for ImageJ/Fiji
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
    private void filterImage(ImageProcessor aOutputIp, ImageProcessor aOriginalIp, CurvatureFilter aFilter, int aNumberOfIterations) {
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
        convertToArrayAndNormalize(aOriginalIp, img, maxValueOfPixel);
        
        // Run chosen filter on image      
        aFilter.runFilter(img, aNumberOfIterations);

        // Update input image with a result
        updateOriginal(aOutputIp, img, maxValueOfPixel);
    }

    /**
     * Converts ImageProcessor to 2D array with first dim Y and second X
     * If new image array is bigger than input image then additional pixels (right column(s) and
     * bottom row(s)) are padded with neighbors values.
     * All pixels are normalized by dividing them by provided normalization value (if 
     * this step is not needed 1.0 should be given).
     * 
     * @param aInputIp       Original image
     * @param aNewImgArray   Created 2D array to keep converted original image     
     * @param aNormalizationValue Maximum pixel value of original image -> converted one will be normalized [0..1]
     */
    private void convertToArrayAndNormalize(ImageProcessor aInputIp, float[][] aNewImgArray, float aNormalizationValue) {
        float[] pixels = (float[])aInputIp.getPixels();
        int w = aInputIp.getWidth();
        int h = aInputIp.getHeight();
        int arrayW = aNewImgArray[0].length;
        int arrayH = aNewImgArray.length;
        
        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= h) yIdx = h - 1;
                if (xIdx >= w) xIdx = w - 1;
                aNewImgArray[y][x] = (float)pixels[xIdx + yIdx * w]/aNormalizationValue;
            }
        }
    }

    /**
     * Updates ImageProcessor image with provided 2D pixel array. All pixels are multiplied by
     * normalization value (if this step is not needed 1.0 should be provided)
     * If output image is smaller than pixel array then it is truncated.
     * 
     * @param aIp                  ImageProcessor to be updated
     * @param aImg                 2D array (first dim Y, second X)
     * @param aNormalizationValue  Normalization value.
     */
    private void updateOriginal(ImageProcessor aIp, float[][] aImg, float aNormalizationValue) {
        float[] pixels = (float[]) aIp.getPixels();
        int w = aIp.getWidth();
        int h = aIp.getHeight();

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                pixels[x + y * w] = aImg[y][x] * aNormalizationValue;
            }
        }
    }

    @Override
    boolean setup(String aArgs) {
        if (aArgs.equals("updateOriginal")) setChangeOriginal(true);
        setFilePrefix("filtered_");
        setSplitMethodMenu(true);
        return true;
    }

    @Override
    void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
        filterImage(aOutputImg, aOrigImg, getCurvatureFilter(), getNumberOfIterations());
    }
}
