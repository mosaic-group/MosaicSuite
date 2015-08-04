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
public abstract class PlugIn8bitBase extends PlugInBase {
    // ImageJ plugIn flags defined for setup method
    private int iFlags = DOES_8G |
                         DOES_RGB |
                         DOES_STACKS | 
                         PARALLELIZE_STACKS |
                         FINAL_PROCESSING; 
  
    protected static final int CHANNEL_R = 0;
    protected static final int CHANNEL_G = 1;
    protected static final int CHANNEL_B = 2;
    protected static final int CHANNEL_8G = 3;
    
    abstract protected void processImg(ByteProcessor aOutputImg, ByteProcessor aOrigImg, int aChannelNumber);
    
    protected int getFlags() {return iFlags;}
    protected void updateFlags(int aFlag) {iFlags |= aFlag;}
    
    private class ProcessOneChannel implements Runnable {
        int channelNumber;
        ColorProcessor processedProcessor;
        ByteProcessor result;
        ByteProcessor original;
        
        ProcessOneChannel(ColorProcessor aProcessedProcessor, ByteProcessor aResult, ByteProcessor aOriginal, int aChannelNumber) {
            this.channelNumber = aChannelNumber;
            this.processedProcessor = aProcessedProcessor;
            this.result = aResult;
            this.original = aOriginal;
        }
        
        @Override
        public void run() {
                // Make channels to be in range 0-2 for RGB
                processImg(result, original, channelNumber - 1);
        }
        
        public void update() {
            if (iResultOutput != ResultOutput.NONE) {
                processedProcessor.setChannel(channelNumber, result);
            }
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
                ColorProcessor processedProcessor = null;
                ByteProcessor result = null;
                // ColorProcessor has RGB channels starting from 1 not from 0.
                final int channelNumber = i + 1;
                if (iResultOutput != ResultOutput.NONE) {
                    processedProcessor = (ColorProcessor) iProcessedImg.getStack().getProcessor(aIp.getSliceNumber());
                    result = processedProcessor.getChannel(channelNumber, null);
                }
                final ByteProcessor original = ((ColorProcessor)aIp).getChannel(channelNumber, null);
                original.setSliceNumber(aIp.getSliceNumber());
                
                // Start separate thread on each channel
                ProcessOneChannel p = new ProcessOneChannel(processedProcessor, result, original, channelNumber);
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
            // Must be handled separately. There is no common parent for ByteProcessor and ColorProcessor 
            // to be used in ProcessOneChannel (like ImageProcessor could be but it missing setChannel method(!) ).
            int slice = aIp.getSliceNumber();
            final ByteProcessor res = (ByteProcessor) iProcessedImg.getStack().getProcessor(slice);
            final ByteProcessor orig = (ByteProcessor)aIp;
            orig.setSliceNumber(aIp.getSliceNumber());
            
            processImg(res, orig, CHANNEL_8G);
            
            iProcessedImg.setSlice(slice);
            iProcessedImg.getProcessor().setPixels(res.getPixels());
        }
        else {
            throw new RuntimeException("This image type cannot be processed!");
        }

        IJ.showStatus("Done.");
    }
}
