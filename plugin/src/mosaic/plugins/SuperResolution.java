package mosaic.plugins;


import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mosaic.plugins.utils.CurvatureFilterBase;
import mosaic.plugins.utils.ImgUtils;
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
    private void superResolution(FloatProcessor aInputIp, FloatProcessor aOriginalIp, CurvatureFilter aFilter, int aNumberOfIterations) {
        // Get dimensions of input image
        final int originalWidth = aOriginalIp.getWidth();
        final int originalHeight = aOriginalIp.getHeight();

        // Create array able to keep twice bigger image (super resolutions orignalDim x 2)
        final int superHeight = originalHeight * 2;
        final int superWidth = originalWidth * 2;
        final float[][] img = new float[superHeight][superWidth];

        // create (normalized) 2D array with input image
        float maxValueOfPixel = (float) aInputIp.getMax();
        if (maxValueOfPixel < 1.0f) {
            maxValueOfPixel = 1.0f;
        }
        convertToArrayAndNormalize(aOriginalIp, img, maxValueOfPixel);

        // Run chosen filter on image
        aFilter.runFilter(img, aNumberOfIterations, new CurvatureFilter.Mask() {
            @Override
            public boolean shouldBeProcessed(int x, int y) {
                // Skip pixels from original image
                return !(x % 2 == 1 && y % 2 == 1);
            }
        });

        ImgUtils.YX2DarrayToImg(img, aInputIp, maxValueOfPixel);
    }

    /**
     * Copies aInputIp pixels to aNewImgArray with step 2 and shifted by 1:
     * For example from image 4x2 (XxY) it creates 8x4 witch padding pixels 'o':
     *   +----+
     *   |1234|
     *   |5678|
     *   +----+
     *
     *   +--------+
     *   |oooooooo|
     *   |o1o2o3o4|
     *   |oooooooo|
     *   |o5o6o7o8|
     *   +--------+
     *
     * @param aInputIp Original image
     * @param aNewImgArray Created 2D array to keep converted original image
     * @param aNormalizationValue Maximum pixel value of original image -> converted one will be normalized [0..1]
     */
    private void convertToArrayAndNormalize(ImageProcessor aInputIp, float[][] aNewImgArray, float aNormalizationValue) {
        final float[] pixels = (float[])aInputIp.getPixels();
        final int w = aInputIp.getWidth();
        final int h = aInputIp.getHeight();

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                aNewImgArray[2*y+1][2*x+1] = pixels[x + y * w] / aNormalizationValue;

            }
        }
    }

    @Override
    protected boolean setup(String aArgs) {
        setFilePrefix("resized_");

        // Super resolution will generate 2x bigger image than original one.
        setScaleX(2.0);
        setScaleY(2.0);

        return true;
    }

    @Override
    protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg, int aChannelNumber) {
        superResolution(aOutputImg, aOrigImg, getCurvatureFilter(), getNumberOfIterations());
    }
}
