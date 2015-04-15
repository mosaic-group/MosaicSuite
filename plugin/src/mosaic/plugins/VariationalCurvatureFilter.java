package mosaic.plugins;


import mosaic.variationalCurvatureFilters.CurvatureFilter;
import mosaic.variationalCurvatureFilters.SplitFilter;
import mosaic.variationalCurvatureFilters.SplitFilterKernelGc;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;


public class VariationalCurvatureFilter implements PlugInFilter {
    private ImagePlus iInputImagePlus;
    private final int iFlags = DOES_ALL | DOES_STACKS | CONVERT_TO_FLOAT | FINAL_PROCESSING | PARALLELIZE_STACKS;
    private final String FILE_PREFIX = "filtered_";
    
    int originalWidth;
    int originalHeight;
    
    @Override
    public int setup(final String aArgs, final ImagePlus aImp) {
        // Filter expects image to work on...
        if (aImp == null) {
            IJ.noImage();
            return DONE;
        }
        
        // This plugin utilize DOES_STACKS flag so we do not need to care about 
        // source image (it can have many channels, slices...) but unfortunately we are working
        // on original image. That is why when initial setup is called copy of original ImagePlus
        // is made and after processing ("final") original image is reverted.
        if (aArgs.equals("final")) {
            // This part is done after all processing is done
            
            // Create new image with processed data
            ImagePlus result = aImp.duplicate();
            result.setTitle(FILE_PREFIX + aImp.getTitle());
            result.show();
            
            // Revert original image
            aImp.setStack(iInputImagePlus.getStack());
        } 
        else {
            // Called at the beginning
            
            // Save original image
            iInputImagePlus = aImp.duplicate();
        }
        
        return iFlags;
    }

    @Override
    public void run(ImageProcessor aIp) {
        // We work only on float images
        boolean isFloatImg = (aIp instanceof FloatProcessor);
        if (!isFloatImg) {
            throw new IllegalArgumentException("This type of image is not supported!");
        }
        
        filterImage(aIp);
    }

    /**
     * Run filter on given image.
     * @param aInputIp Input image (will be changed during processing)
     */
    private void filterImage(ImageProcessor aInputIp) {
        originalWidth = aInputIp.getWidth();
        originalHeight = aInputIp.getHeight();

        final float maxValueOfPixel = (float) aInputIp.getMax();
        float[][] img = new float[originalHeight][originalWidth]; 
        convertToArrayAndNormalize(aInputIp, img, maxValueOfPixel);

        final int noOfIterations = 10;

        CurvatureFilter cf = new SplitFilter(new SplitFilterKernelGc());
        cf.runFilter(img, noOfIterations);
       
        updateOriginal(aInputIp, img, maxValueOfPixel);
    }

    /**
     * Converts input image to divisible by 2 (in case if original image is not).
     * Additional pixels are filled with neighbors values.
     * 
     * @param aInputIp       Original image
     * @param aNewImgArray   Created matrix (with correct dimensions) to keep converted original image     
     * @param aMaxPixelValue Maximum pixel value of original image -> converted one will be normalized [0..1]
     */
    private void convertToArrayAndNormalize(ImageProcessor aInputIp, float[][] aNewImgArray, float aMaxPixelValue) {
        float[] pixels = (float[])aInputIp.getPixels();
    
        if (aMaxPixelValue < 1.0f) aMaxPixelValue = 1.0f;
        for (int x = 0; x < originalWidth; ++x) {
            for (int y = 0; y < originalHeight; ++y) {
                aNewImgArray[y][x] = (float)pixels[x + y * originalWidth]/aMaxPixelValue;
    
            }
        }
    }

    private void updateOriginal(ImageProcessor aIp, float[][] aImg, float aMaxPixelValue) {
        float[] pixels = (float[])aIp.getPixels();
        
        for (int x = 0; x < originalWidth; ++x) {
            for (int y = 0; y < originalHeight; ++y) {
                     pixels[x + y * originalWidth] = aImg[y][x] * aMaxPixelValue;
            }
        }
    }
}