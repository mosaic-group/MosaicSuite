package mosaic.plugins.utils;


import ij.IJ;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for plugIns that use float values as a algorithm base.
 * @author Krzysztof Gonciarz
 */
abstract class PlugIn8bitBase extends PlugInBase {
    // ImageJ plugIn flags defined for setup method
    private int iFlags = DOES_8G |
                         DOES_RGB |
                         DOES_STACKS | 
                         PARALLELIZE_STACKS |
                         FINAL_PROCESSING; 
  
    abstract protected void processImg(ByteProcessor aOutputImg, ByteProcessor aOrigImg);
    
    protected int getFlags() {return iFlags;}
    protected void updateFlags(int aFlag) {iFlags |= aFlag;}
    
    private class ProcessOneChannel implements Runnable {
        int i;
        ColorProcessor currentIp;
        ByteProcessor res;
        ByteProcessor orig;
        
        ProcessOneChannel(ColorProcessor currentIp, ByteProcessor res, ByteProcessor orig, int i) {
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
            currentIp.setChannel(i, res);
        }
    }
    
    @Override
    public void run(ImageProcessor aIp) {
        IJ.showStatus("In progress...");

        if (aIp instanceof ColorProcessor) {
            final int noOfChannels = aIp.getNChannels();

            // Lists to keep information about threads
            List<Thread> th = new ArrayList<Thread>(noOfChannels);
            List<ProcessOneChannel> poc = new ArrayList<ProcessOneChannel>(noOfChannels);
            for (int i = 0; i < noOfChannels; ++i) {

                final ColorProcessor currentIp = (ColorProcessor) iProcessedImg.getStack().getProcessor(aIp.getSliceNumber());
                // ColorProcessor has RGB channels starting from 1 not from 0.
                final int channelNumber = i + 1;
                final ByteProcessor res = currentIp.getChannel(channelNumber, null);
                final ByteProcessor orig = ((ColorProcessor)aIp).getChannel(channelNumber, null);
                orig.setSliceNumber(aIp.getSliceNumber());
                
                // Start separate thread on each channel
                ProcessOneChannel p = new ProcessOneChannel(currentIp, res, orig, channelNumber);
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
        }
        else if (aIp instanceof ByteProcessor) {
            int slice = aIp.getSliceNumber();
            final ByteProcessor res = (ByteProcessor) iProcessedImg.getStack().getProcessor(slice);
            final ByteProcessor orig = (ByteProcessor)aIp;
            orig.setSliceNumber(aIp.getSliceNumber());
            
            processImg(res, orig);
            
            iProcessedImg.setSlice(slice);
            iProcessedImg.getProcessor().setPixels(res.getPixels());
        }
        else {
            throw new RuntimeException("This image type cannot be processed!");
        }

        IJ.showStatus("Done.");
    }
}
