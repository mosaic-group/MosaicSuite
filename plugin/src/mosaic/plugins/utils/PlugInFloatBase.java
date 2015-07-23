package mosaic.plugins.utils;


import ij.IJ;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for plugIns that use float values as a algorithm base.
 * @author Krzysztof Gonciarz
 */
public abstract class PlugInFloatBase extends PlugInBase {
    // ImageJ plugIn flags defined for setup method
    private int iFlags = DOES_ALL |
                         DOES_STACKS | 
                         FINAL_PROCESSING | 
                         PARALLELIZE_STACKS;
    

    abstract protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg, int aChannelNumber);
    
    protected int getFlags() {return iFlags;}
    protected void updateFlags(int aFlag) {iFlags |= aFlag;}

    private class ProcessOneChannel implements Runnable {
        int i;
        ImageProcessor currentIp;
        FloatProcessor res;
        FloatProcessor orig;
        
        ProcessOneChannel(ImageProcessor currentIp, FloatProcessor res, FloatProcessor orig, int aChannel) {
            this.i = aChannel;
            this.currentIp = currentIp;
            this.res = res;
            this.orig = orig;
        }
        
        @Override
        public void run() {
                processImg(res, orig, i);
        }
        
        public void update() {
            currentIp.setPixels(i, res);
        }
    }
    
    @Override
    public void run(ImageProcessor aIp) {
        IJ.showStatus("In progress...");
        
        final int noOfChannels = aIp.getNChannels();
        
        // Lists to keep information about threads
        List<Thread> th = new ArrayList<Thread>(noOfChannels);
        List<ProcessOneChannel> poc = new ArrayList<ProcessOneChannel>(noOfChannels);
        
        for (int i = 0; i < noOfChannels; ++i) {
            final ImageProcessor currentIp = iProcessedImg.getStack().getProcessor(aIp.getSliceNumber());
            final FloatProcessor res = currentIp.toFloat(i, null);
            final FloatProcessor orig = aIp.toFloat(i, null);
            orig.setSliceNumber(aIp.getSliceNumber());

            // Start separate thread on each channel
            ProcessOneChannel p = new ProcessOneChannel(currentIp, res, orig, i);
            Thread t = new Thread(p);
            t.start();
            th.add(t);
            poc.add(p);
        }
        
        for (int i = 0; i < noOfChannels; ++i) {
            try {
                // wait for each channel to complete and then run update method - it will 
                // be run in sequence which is needed since ImageProcessor is not thread safe
                th.get(i).join(); 
                poc.get(i).update(); 
            } 
            catch (InterruptedException e) {}
        }
        
        IJ.showStatus("Done.");
    }
}
