package mosaic.plugins;


import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Super resolution based on curvature filters
 * @author Krzysztof Gonciarz
 */
public abstract class PluginBase implements PlugInFilter {
    // ImageJ plugin flags defined for setup method
    private int iFlags = DOES_ALL |
                         DOES_STACKS | 
                         FINAL_PROCESSING | 
                         PARALLELIZE_STACKS;
    
    // Original input image
    private ImagePlus iInputImg;
    private ImagePlus iProcessedImg;
    private String iInputArgs;
    
    // Prefix added to filtered image
    private String iFilePrefix = "processed_";


    // Scale of output ImagePlus
    private double iScaleX = 1.0;
    private double iScaleY = 1.0;
    
    // If false new ImagePlus is generated, otherwise original (input) image
    // shall be changed
    boolean iChangeOriginal = false;
    
    abstract void setup(final String aArgs);
    abstract boolean showDialog();
    abstract void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg);
   
    @Override
    public int setup(final String aArgs, final ImagePlus aImp) {
        // Filter expects image to work on...
        if (aImp == null) {
            IJ.noImage();
            return DONE;
        }

        if (aArgs.equals("final")) {
            iProcessedImg.show();
        }
        else {
            iInputImg = aImp;
            iInputArgs = aArgs;
            
            setup(iInputArgs);
            
            if (iChangeOriginal) {
                iProcessedImg = iInputImg;
            }
            else {
                iProcessedImg = createNewEmptyImgPlus(iInputImg, iFilePrefix + iInputImg.getTitle(), iScaleX, iScaleY);
                iFlags |= NO_CHANGES;
            }

            if (!showDialog()) return DONE;
        }
        
        return iFlags;
    }
     
    class ProcessOneChannel implements Runnable {
        int i;
        ImageProcessor currentIp;
        FloatProcessor res;
        FloatProcessor orig;
        
        ProcessOneChannel(ImageProcessor currentIp, FloatProcessor res, FloatProcessor orig, int i) {
            this.i = i;
            this.currentIp = currentIp;
            this.res = res;
            this.orig = orig;
        }
        
        @Override
        public void run() {
                processImg(res, orig);
        }
        
        public void update() {
            currentIp.setPixels(i, res);
        }
    }
    
    @Override
    public void run(ImageProcessor aIp) {
        IJ.showStatus("In progress...");
        List<Thread> th = new ArrayList<Thread>(aIp.getNChannels());
        List<ProcessOneChannel> poc = new ArrayList<ProcessOneChannel>(aIp.getNChannels());
        for (int i = 0; i < aIp.getNChannels(); ++i) {
            final ImageProcessor currentIp = iProcessedImg.getStack().getProcessor(aIp.getSliceNumber());
            final FloatProcessor res = currentIp.toFloat(i, null);
            final FloatProcessor orig = aIp.toFloat(i, null);
            
            ProcessOneChannel p = new ProcessOneChannel(currentIp, res, orig, i);
            Thread t = new Thread(p);
            t.start();
            th.add(t);
            poc.add(p);
        }
        
        for (int i = 0; i < aIp.getNChannels(); ++i) {
            try {
                th.get(i).join(); 
                poc.get(i).update(); 
            } 
            catch (InterruptedException e) {}
        }
        
        IJ.showStatus("Done.");
    }

    ImagePlus createNewEmptyImgPlus(ImagePlus aOrigIp, String aTitle, double aXscale, double aYscale) {
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
}
