package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
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
    private void superResolution(ImageProcessor aInputIp, ImageProcessor aOriginalIp, CurvatureFilter aFilter, int aNumberOfIterations) {
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
        convertToArrayAndNormalize(aOriginalIp, img, maxValueOfPixel);

        // Run chosen filter on image
        aFilter.runFilter(img, aNumberOfIterations, new CurvatureFilter.Mask() {
            public boolean shouldBeProcessed(int x, int y) {
                // Skip pixels from original image
                return iMask.getProcessor().getf(x, y) != 0;
            }
        });

        updateOriginal(aInputIp, img, maxValueOfPixel);
    }

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
    void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
        superResolution(aOutputImg, aOrigImg, getCurvatureFilter(), getNumberOfIterations());
    }
}
