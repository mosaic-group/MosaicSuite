package mosaic.plugins.utils;


import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * Base for plugIns that use float values as a algorithm base.
 * @author Krzysztof Gonciarz
 */
abstract class PlugInBase implements PlugInFilter {
    
    // Original input image
    protected ImagePlus iInputImg;
    protected ImagePlus iProcessedImg;
    protected String iInputArgs;
    
    // Prefix added to filtered image
    protected String iFilePrefix = "processed_";

    // Scale of output ImagePlus
    protected double iScaleX = 1.0;
    protected double iScaleY = 1.0;
    
    // If false new ImagePlus is generated, otherwise original (input) image
    // shall be changed
    boolean iChangeOriginal = false;
   
    abstract protected boolean showDialog();
    abstract protected int getFlags();
    abstract protected void updateFlags(int aFlag);
    abstract protected boolean setup(final String aArgs);
    
    @Override
    public int setup(final String aArgs, final ImagePlus aImp) {
        // Filter expects image to work on...
        if (aImp == null) {
            IJ.noImage();
            return DONE;
        }

        if (aArgs.equals("final")) {
        	// Changed during updating
        	iProcessedImg.setSlice(1);
            iProcessedImg.show();
        }
        else {
            iInputImg = aImp;
            iInputArgs = aArgs;
            
            // Allow plugin to make it own configuration
            if (!setup(iInputArgs)) return DONE;
            
            // Set the original image as being processed or generate new empty copy of
            // input img otherwise
            if (iChangeOriginal) {
                iProcessedImg = iInputImg;
            }
            else {
                iProcessedImg = createNewEmptyImgPlus(iInputImg, iFilePrefix + iInputImg.getTitle(), iScaleX, iScaleY);
                updateFlags(NO_CHANGES);
            }

            if (!showDialog()) return DONE;
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
    protected ImagePlus createNewEmptyImgPlus(ImagePlus aOrigIp, String aTitle, double aXscale, double aYscale) {
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
            ip2 = ip1.createProcessor(newWidth, newHeight);
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

    public void setScaleX(double aScaleX) {
        iScaleX = aScaleX;
    }

    public void setScaleY(double aScaleY) {
        iScaleY = aScaleY;
    }

    public void setChangeOriginal(boolean aChangeOriginal) {
        iChangeOriginal = aChangeOriginal;
    }   
    
    public void setFilePrefix(String aFilePrefix) {
        iFilePrefix = aFilePrefix;
    }
    
    public ImagePlus getInputImg() {
        return iInputImg;
    }
}
