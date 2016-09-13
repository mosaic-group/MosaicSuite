package mosaic.plugins.utils;


import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import mosaic.utils.ImgUtils;

/**
 * Base for plugIns that use float values as a algorithm base.
 * @author Krzysztof Gonciarz
 */
abstract class PlugInBase implements ExtendedPlugInFilter {

    // Original input image
    protected ImagePlus iInputImg;
    protected ImagePlus iProcessedImg;
    private String iInputArgs;

    // Prefix added to filtered image
    private String iFilePrefix = "processed_";

    // Scale of output ImagePlus
    private double iScaleX = 1.0;
    private double iScaleY = 1.0;

    // Decides where result of processing shall be placed.
    /**
     * UPDATE_ORIGINAL - updates source image with processed data
     * GENERATE_NEW - crates new image with same parameters (type, number of slices...) as original one
     * NONE - do nothing, plugin itself should generate and update output image
     */
    protected enum ResultOutput {UPDATE_ORIGINAL, GENERATE_NEW, NONE}
    ResultOutput iResultOutput = ResultOutput.GENERATE_NEW;

    /*
     * Timeline of plugin's life:
     * 1. initialize variables with input stuff
     * 2. setup(final String aArgs) is called
     * 3. showDialog() is called
     * 4. set/getFlags()
     * 5. postprocess()
     * 6. shows output...
     */

    /**
     * Called during setup phase. It allows plugin to make it own configuration 
     * @param aArgs arguments passed to plugin
     * @return true if setup phase is correct.
     */
    abstract protected boolean setup(final String aArgs);
    
    /**
     * Called after setup phase. Should return true on success or false otherwise. If false is returned
     * execution is cancelled.
     * @return true if success
     */
    abstract protected boolean showDialog();
    
    /**
     * Should return flags ( DOES_8G ...) required by plugin
     * @return
     */
    abstract protected int getFlags();
    
    /**
     * Should allow to modify flags set by plugin (implementation like {iFlags |= aFlag;} should be 
     * sufficient in many cases)
     * @param aFlag
     */
    abstract protected void updateFlags(int aFlag);
    
    /**
     * Called after run() phase is finished but before showing any result. 
     */
    protected void postprocessBeforeShow() {}
    
    /**
     * Called after results are shown to user (i.e. generated output images).
     */
    protected void postprocessFinal() {}

    @Override
    final public int setup(final String aArgs, final ImagePlus aImp) {
        // Filter expects image to work on...
        if (aImp == null) {
            IJ.noImage();
            return DONE;
        }

        if (aArgs.equals("final")) {
            postprocessBeforeShow();

            // Changed during updating
            iProcessedImg.setSlice(1);
            iProcessedImg.show();

            postprocessFinal();
        }
        else {
            iInputImg = aImp;
            iInputArgs = aArgs;

            // Allow plugin to make it own configuration
            if (!setup(iInputArgs)) {
                return DONE;
            }

            // Set the original image as being processed or generate new empty copy of
            // input img otherwise
            if (iResultOutput == ResultOutput.UPDATE_ORIGINAL) {
                iProcessedImg = iInputImg;
            }
            else if (iResultOutput == ResultOutput.GENERATE_NEW){
                iProcessedImg = ImgUtils.createNewEmptyImgPlus(iInputImg, iFilePrefix + iInputImg.getTitle(), iScaleX, iScaleY, false);
                updateFlags(NO_CHANGES);
            } else {
                updateFlags(NO_CHANGES);
            }
        }

        return getFlags();
    }

    @Override
    final public void setNPasses(int arg0) {
        // Nothing to do here
    }

    @Override
    final public int showDialog(ImagePlus arg0, String arg1, PlugInFilterRunner arg2) {
        if (!showDialog()) {
            return DONE;
        }

        return getFlags();
    }

    protected void setScaleX(double aScaleX) {
        iScaleX = aScaleX;
    }

    protected void setScaleY(double aScaleY) {
        iScaleY = aScaleY;
    }

    protected void setResultDestination(ResultOutput aResultOutput) {
        iResultOutput = aResultOutput;
    }

    protected void setFilePrefix(String aFilePrefix) {
        iFilePrefix = aFilePrefix;
    }
}
