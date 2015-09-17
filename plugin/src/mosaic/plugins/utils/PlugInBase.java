package mosaic.plugins.utils;


import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * Base for plugIns that use float values as a algorithm base.
 * @author Krzysztof Gonciarz
 */
abstract class PlugInBase implements ExtendedPlugInFilter {

    // Original input image
    protected ImagePlus iInputImg;
    protected ImagePlus iProcessedImg;
    protected String iInputArgs;

    // Prefix added to filtered image
    protected String iFilePrefix = "processed_";

    // Scale of output ImagePlus
    protected double iScaleX = 1.0;
    protected double iScaleY = 1.0;

    // Decides where result of processing shall be placed.
    /**
     * UPDATE_ORIGINAL - updates source image with processed data
     * GENERATE_NEW - crates new image with same parameters (type, number of slices...) as original one
     * NONE - do nothing, plugin itself should generate and update output image
     */
    protected enum ResultOutput {UPDATE_ORIGINAL, GENERATE_NEW, NONE};
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

    abstract protected boolean showDialog();
    abstract protected int getFlags();
    abstract protected void updateFlags(int aFlag);
    abstract protected boolean setup(final String aArgs);
    protected void postprocessBeforeShow() {};
    protected void postprocessFinal() {};

    @Override
    public int setup(final String aArgs, final ImagePlus aImp) {
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
                iProcessedImg = createNewEmptyImgPlus(iInputImg, iFilePrefix + iInputImg.getTitle(), iScaleX, iScaleY);
                updateFlags(NO_CHANGES);
            } else {
                updateFlags(NO_CHANGES);
            }
        }

        return getFlags();
    }

    @Override
    public void setNPasses(int arg0) {
        // Nothing to do here
    }

    @Override
    public int showDialog(ImagePlus arg0, String arg1, PlugInFilterRunner arg2) {
        if (!showDialog()) {
            return DONE;
        }

        return getFlags();
    }

    /**
     * Creates new empty ImagePlus basing on information from original aOrigIp image.
     * Newly generated img will have same structure (like slices/frames/channels, composite,
     * calibration settings etc.). It can be rescaled if needed by providing aXscale and aYscale
     * other than 1.0
     * @param aOrigIp
     * @param aTitle
     * @param aXscale
     * @param aYscale
     * @return newly created ImagePlus
     */
    private ImagePlus createNewEmptyImgPlus(ImagePlus aOrigIp, String aTitle, double aXscale, double aYscale) {
        return createNewEmptyImgPlus(aOrigIp, aTitle, aXscale, aYscale, false);
    }

    protected ImagePlus createNewEmptyImgPlus(ImagePlus aOrigIp, String aTitle, double aXscale, double aYscale, boolean convertToRgb) {
        int nSlices = aOrigIp.getStackSize();
        int w=aOrigIp.getWidth();
        int h=aOrigIp.getHeight();

        ImagePlus copyIp = aOrigIp.createImagePlus();

        int newWidth = (int)aXscale*w;
        int newHeight = (int)aYscale*h;

        ImageStack origStack = aOrigIp.getStack();
        ImageStack copyStack = new ImageStack(newWidth, newHeight);
        ImageProcessor ip1, ip2;
        for (int i = 1; i <= nSlices; i++) {
            ip1 = origStack.getProcessor(i);
            String label = origStack.getSliceLabel(i);
            if (!convertToRgb) {
                ip2 = ip1.createProcessor(newWidth, newHeight);
            }
            else {
                ip2 = new ColorProcessor(newWidth, newHeight);
            }
            if (ip2 != null) {
                copyStack.addSlice(label, ip2);
            }
        }

        copyIp.setStack(aTitle, copyStack);

        Calibration cal = copyIp.getCalibration();
        if (cal.scaled()) {
            cal.pixelWidth *= 1.0 / aXscale;
            cal.pixelHeight *= 1.0 / aYscale;
        }

        int[] dim = aOrigIp.getDimensions();
        copyIp.setDimensions(dim[2], dim[3], dim[4]);

        if (aOrigIp.isComposite()) {
            copyIp = new CompositeImage(copyIp, ((CompositeImage)aOrigIp).getMode());
            ((CompositeImage)copyIp).copyLuts(aOrigIp);
        }


        if (aOrigIp.isHyperStack()) {
            copyIp.setOpenAsHyperStack(true);
        }
        copyIp.changes = true;

        return copyIp;
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

    protected ImagePlus getInputImg() {
        return iInputImg;
    }

    protected void setProcessedImg(ImagePlus aProcessedImg) {
        iProcessedImg = aProcessedImg;
    }
}
