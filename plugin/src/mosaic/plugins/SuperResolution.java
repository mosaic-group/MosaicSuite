package mosaic.plugins;


import ij.gui.GenericDialog;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mosaic.variationalCurvatureFilters.CurvatureFilter;
import mosaic.variationalCurvatureFilters.FilterKernel;
import mosaic.variationalCurvatureFilters.FilterKernelGc;
import mosaic.variationalCurvatureFilters.FilterKernelMc;
import mosaic.variationalCurvatureFilters.FilterKernelTv;
import mosaic.variationalCurvatureFilters.NoSplitFilter;
import mosaic.variationalCurvatureFilters.SplitFilter;

/**
 * Super resolution based on curvature filters
 * @author Krzysztof Gonciarz
 */
public class SuperResolution extends PluginBase {
    // Chosen filter
    CurvatureFilter iCf;
    
    // Number of iterations to run filter
    private int iNumberOfIterations;

    /**
     * Takes information from user about wanted filterType, filtering method and
     * number of iterations.
     * @return true in case if configuration was successful - false otherwise.
     */
    @Override
    boolean showDialog() {
        final String[] filters = {"GC", "MC", "TV"};
        final String[] types = {"Split", "No split"};
        
        GenericDialog gd = new GenericDialog("Curvature Filter Settings");
    
        gd.addRadioButtonGroup("Filter type: ", filters, 1, 3, filters[0]);
        gd.addRadioButtonGroup("Method: ", types, 1, 2, types[1]);
        gd.addNumericField("Number of iterations: ", 10, 0);
        
        gd.showDialog();
        
        if (!gd.wasCanceled()) {
            // Create user's chosen filter
            String filter = gd.getNextRadioButton();
            String type = gd.getNextRadioButton();
            iNumberOfIterations = (int)gd.getNextNumber();
            FilterKernel fk = null;
            if (filter.equals(filters[0])) {
                fk = new FilterKernelGc(); 
            } 
            else if (filter.equals(filters[1])) {
                fk = new FilterKernelMc(); 
            }
            else if (filter.equals(filters[2])) {
                fk = new FilterKernelTv(); 
            }
            if (fk == null) return false;
            
            if (type.equals(types[0])) {
                iCf = new SplitFilter(fk); 
            }
            else {
                iCf = new NoSplitFilter(fk); 
            }
            
            if (iCf != null && iNumberOfIterations >= 0) return true;
        }
        
        return false;
    }
    
    /**
     * Run filter on given image.
     * @param aInputIp Input image (will be changed during processing)
     * @param aFilter Filter to be used
     * @param aNumberOfIterations Number of iterations for filter
     */
    private void superResolution(ImageProcessor aInputIp, ImageProcessor aOriginalIp, CurvatureFilter aFilter, int aNumberOfIterations) {
        // Image dimensions
        int originalWidth;
        int originalHeight;
        
        // Get dimensions of input image
        originalWidth = aOriginalIp.getWidth();
        originalHeight = aOriginalIp.getHeight();
        
        int superHeight = originalHeight * 2;
        int superWidth = originalWidth * 2;
        float[][] img = new float[superHeight][superWidth]; 

        // create (normalized) 2D array with input image
        float maxValueOfPixel = (float) aInputIp.getMax();
        if (maxValueOfPixel < 1.0f) 
            maxValueOfPixel = 1.0f;
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
        
        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                     pixels[x + y * w] = aImg[y][x] * aNormalizationValue;
            }
        }
    }

    @Override
    void setup(String aArgs) {
        setFilePrefix("resized_");
        setScaleX(2.0);
        setScaleY(2.0);
    }

    @Override
    void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
        superResolution(aOutputImg, aOrigImg, iCf, iNumberOfIterations);
    }
}
