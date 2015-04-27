package mosaic.plugins;


import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mosaic.variationalCurvatureFilters.CurvatureFilter;

/**
 * Super resolution based on curvature filters
 * @author Krzysztof Gonciarz
 */
public class SuperResolution extends CurvatureFilterBase {
    
    /**
     * Run filter on given image.
     * @param aInputIp Input image (will be changed during processing)
     * @param aFilter Filter to be used
     * @param aNumberOfIterations Number of iterations for filter
     */
    private void superResolution(ImageProcessor aInputIp, ImageProcessor aOriginalIp, CurvatureFilter aFilter, int aNumberOfIterations) {
        // Get dimensions of input image
        int originalWidth = aOriginalIp.getWidth();
        int originalHeight = aOriginalIp.getHeight();
        
        int superHeight = originalHeight * 2;
        int superWidth = originalWidth * 2;
        float[][] img = new float[superHeight][superWidth]; 

        // create (normalized) 2D array with input image
        float maxValueOfPixel = (float) aInputIp.getMax();
        if (maxValueOfPixel < 1.0f) maxValueOfPixel = 1.0f;
        convertToArrayAndNormalize(aOriginalIp, img, maxValueOfPixel); //maxValueOfPixel);

        // Run chosen filter on image      
        aFilter.runFilter(img, aNumberOfIterations, new CurvatureFilter.Mask() {
            public boolean shouldBeProcessed(int x, int y) {
                // Skip pixels from original image
                return !(x % 2 == 1 && y % 2 == 1);
            }
        });

        updateOriginal(aInputIp, img, maxValueOfPixel);
    }
    
    private void convertToArrayAndNormalize(ImageProcessor aInputIp, float[][] aNewImgArray, float aNormalizationValue) {
        float[] pixels = (float[])aInputIp.getPixels();
        int w = aInputIp.getWidth();
        int h = aInputIp.getHeight();
        
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                aNewImgArray[2*y+1][2*x+1] = (float)pixels[x + y * w] / aNormalizationValue;
    
            }
        }
    }
    
    private void updateOriginal(ImageProcessor aIp, float[][] aImg, float aNormalizationValue) {
        float[] pixels = (float[])aIp.getPixels();
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
        setFilePrefix("resized_");
        
        // Super resolution will generate 2x bigger image than original one.
        setScaleX(2.0);
        setScaleY(2.0);
        
        return true;
    }

    @Override
    void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
        superResolution(aOutputImg, aOrigImg, getCurvatureFilter(), getNumberOfIterations());
    }
}
