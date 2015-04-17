package mosaic.plugins;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
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
 * Plugin providing variational curvature filters (GC/TV/MC) functionality for ImageJ/Fiji
 * @author Krzysztof Gonciarz
 */
public class VariationalCurvatureFilter implements PlugInFilter {
    // ImageJ plugin flags defined for setup method
    private final int iFlags =  DOES_ALL | 
                                DOES_STACKS | 
                                CONVERT_TO_FLOAT | 
                                FINAL_PROCESSING | 
                                PARALLELIZE_STACKS;
    
    // Prefix added to filtered image
    private final String FILE_PREFIX = "filtered_";
    
    // Image dimensions
    int originalWidth;
    int originalHeight;
    int roundedWidth;
    int roundedHeight;
    
    // Original input image copy
    private ImagePlus iInputImagePlus;
    
    // Chosen filter
    CurvatureFilter iCf;
    
    // Number of iterations to run filter
    private int iNumberOfIterations;
    
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
            return DONE;
        } 
        else {
            // Called at the beginning
            
            // Save original image
            iInputImagePlus = aImp.duplicate();
        }
        
        if (!showDialog()) return DONE;
        
        return iFlags;
    }

    @Override
    public void run(ImageProcessor aIp) {
        // We work only on float images
        final boolean isFloatImg = (aIp instanceof FloatProcessor);
        if (!isFloatImg) {
            throw new IllegalArgumentException("This type of image is not supported!");
        }
        
        IJ.showStatus("Filtering...");
        filterImage(aIp, iCf, iNumberOfIterations);
        IJ.showStatus("Done.");
    }

    boolean showDialog() {
        final String[] filters = {"GC", "MC", "TV"};
        final String[] types = {"Split", "No split"};
        
        GenericDialog gd = new GenericDialog("Curvature Filter Settings");
    
        gd.addRadioButtonGroup("Filter type: ", filters, 1, 3, filters[0]);
        gd.addRadioButtonGroup("Method: ", types, 1, 2, types[0]);
        gd.addNumericField("Number of iterations: ", 10, 0);
        
        gd.showDialog();
        
        if (!gd.wasCanceled()) {
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
            
            if (iCf != null && iNumberOfIterations > 0) return true;
        }
        
        return false;
    }

    /**
     * Run filter on given image.
     * @param aInputIp Input image (will be changed during processing)
     */
    private void filterImage(ImageProcessor aInputIp, CurvatureFilter aFilter, int aNumberOfIterations) {
        // Get dimensions of input image
        originalWidth = aInputIp.getWidth();
        originalHeight = aInputIp.getHeight();
        
        // Generate 2D array for image (it will be rounded up to be divisible
        // by 2). Possible additional points will be filled with last column/row
        // values in convertToArrayAndNormalize
        roundedWidth = (int) (Math.ceil(originalWidth/2.0) * 2);
        roundedHeight = (int) (Math.ceil(originalHeight/2.0) * 2);
        float[][] img = new float[roundedHeight][roundedWidth]; 

        // create (normalized) 2D array with input image
        float maxValueOfPixel = (float) aInputIp.getMax();
        if (maxValueOfPixel < 1.0f) maxValueOfPixel = 1.0f;
        convertToArrayAndNormalize(aInputIp, img, maxValueOfPixel);
        
        // Run chosen filter on image      
        aFilter.runFilter(img, aNumberOfIterations);

        // Update input image with a result
        updateOriginal(aInputIp, img, maxValueOfPixel);
    }

    /**
     * Converts ImageProcessor to 2D array with first dim Y and second X
     * All pixels are normalized by dividing them by provided normalization value (if 
     * this step is not needed 1.0 should be given).
     * 
     * @param aInputIp       Original image
     * @param aNewImgArray   Created 2D array to keep converted original image     
     * @param aNormalizationValue Maximum pixel value of original image -> converted one will be normalized [0..1]
     */
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

    /**
     * Updates ImageProcessor image with provided 2D pixel array. All pixels are multiplied by
     * normalization value (if this step is not needed 1.0 should be provided)
     * 
     * @param aIp                  ImageProcessor to be updated
     * @param aImg                 2D array (first dim Y, second X)
     * @param aNormalizationValue  Normalization value.
     */
    private void updateOriginal(ImageProcessor aIp, float[][] aImg, float aNormalizationValue) {
        float[] pixels = (float[])aIp.getPixels();
        
        for (int x = 0; x < originalWidth; ++x) {
            for (int y = 0; y < originalHeight; ++y) {
                     pixels[x + y * originalWidth] = aImg[y][x] * aNormalizationValue;
            }
        }
    }
}
