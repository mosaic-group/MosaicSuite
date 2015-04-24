package mosaic.plugins;


import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.io.Opener;
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
public class Inpainting extends PluginBase {
    // Chosen filter
    CurvatureFilter iCf;
    
    // Number of iterations to run filter
    private int iNumberOfIterations;

    // Mask
    ImagePlus iMask;
    
    // Image dimensions
    int originalWidth;
    int originalHeight;
    int roundedWidth;
    int roundedHeight;
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
        // Get dimensions of input image
        originalWidth = aOriginalIp.getWidth();
        originalHeight = aOriginalIp.getHeight();
        
        // Generate 2D array for image (it will be rounded up to be divisible
        // by 2). Possible additional points will be filled with last column/row
        // values in convertToArrayAndNormalize
        roundedWidth = (int) (Math.ceil(originalWidth/2.0) * 2);
        roundedHeight = (int) (Math.ceil(originalHeight/2.0) * 2);
        float[][] img = new float[roundedHeight][roundedWidth]; 

        // create (normalized) 2D array with input image
        float maxValueOfPixel = (float) aOriginalIp.getMax();
        if (maxValueOfPixel < 1.0f) maxValueOfPixel = 1.0f;
        convertToArrayAndNormalize(aOriginalIp, img, maxValueOfPixel);

        // Run chosen filter on image      
        aFilter.runFilter(img, aNumberOfIterations, new CurvatureFilter.Mask() {
            public boolean shouldBeProcessed(int x, int y) {
                // Skip pixels from original image
                return iMask.getProcessor().getf(x,y) != 0;
            }
        });

        updateOriginal(aInputIp, img, maxValueOfPixel);
    }
    
    private void convertToArrayAndNormalize(ImageProcessor aInputIp, float[][] aNewImgArray, float aNormalizationValue) {
        float[] pixels = (float[])aInputIp.getPixels();
    
        for (int y = 0; y < roundedHeight; ++y) {
            for (int x = 0; x < roundedWidth; ++x) {
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= originalHeight) yIdx = originalHeight - 1;
                if (xIdx >= originalWidth) xIdx = originalWidth - 1;
                aNewImgArray[y][x] = (float)pixels[xIdx + yIdx * originalWidth]/aNormalizationValue;
    
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
        setFilePrefix("inpainting_");

        OpenDialog od = new OpenDialog("(Inpainting) Open mask file", "");
        String directory = od.getDirectory();
        String name = od.getFileName();

        if (name != null) {
            iMask = new Opener().openImage(directory + name);
        }
                 //TODO: Check if mask is valid (resolution, type of image)
    }

    @Override
    void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
        superResolution(aOutputImg, aOrigImg, iCf, iNumberOfIterations);
    }
}
