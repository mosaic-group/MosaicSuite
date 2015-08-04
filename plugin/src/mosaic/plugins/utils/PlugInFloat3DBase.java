package mosaic.plugins.utils;


import ij.IJ;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base for plugIns operate on 3D images that use float values as a algorithm base.
 * @author Krzysztof Gonciarz
 */
public abstract class PlugInFloat3DBase extends PlugInBase {
    // ImageJ plugIn flags defined for setup method
    private int iFlags = DOES_ALL |
                         FINAL_PROCESSING;
    

    abstract protected void processImg(FloatProcessor[] aOutputImg, FloatProcessor[] aOrigImg, int aChannelNumber);
    
    protected int getFlags() {return iFlags;}
    protected void updateFlags(int aFlag) {iFlags |= aFlag;}

    // Class to keep all information needed for one thread: input/output processor arrays
    // and information how to update ImageProcessor (color channel)
    private class ProcessOneCube implements Runnable {
        int iColorChannel;
        ImageProcessor[] iCurrentIp;
        FloatProcessor[] iResultImg;
        FloatProcessor[] iOriginalImg;
        
        ProcessOneCube(ImageProcessor[] aCurrentIp, FloatProcessor[] aResultImg, FloatProcessor[] aOriginalImg, int aColorChannel) {
            this.iColorChannel = aColorChannel;
            this.iCurrentIp = aCurrentIp;
            this.iResultImg = aResultImg;
            this.iOriginalImg = aOriginalImg;
        }
        
        @Override
        public void run() {
                processImg(iResultImg, iOriginalImg, iColorChannel);
        }
        
        public void update() {
            if (iResultOutput != ResultOutput.NONE) {
                for (int slice = 0; slice < iCurrentIp.length; ++slice) {
                    iCurrentIp[slice].setPixels(iColorChannel, iResultImg[slice]);
                }
            }
        }
    }
    
    @Override
    public void run(ImageProcessor aIp) {
        IJ.showStatus("In progress...");
        
        final int noOfChannels = iInputImg.getNChannels();
        final int noOfSlices = iInputImg.getNSlices();
        final int noOfFrames = iInputImg.getNFrames();
        final int noOfColorChannelsInProcessor = iInputImg.getProcessor().getNChannels();

        // Lists to keep information about threads
        List<Thread> th = new ArrayList<Thread>();
        List<ProcessOneCube> poc = new ArrayList<ProcessOneCube>();
        
        // Generate thread for all frames, channels and colorChannels. Only slices (z-axis) are put together to
        // for cube for further processing.
        for (int frame = 0; frame < noOfFrames; ++frame) {
            for (int channel = 0; channel < noOfChannels; ++channel) {
                for (int colorChannel = 0; colorChannel < noOfColorChannelsInProcessor; ++colorChannel) {
                    ImageProcessor[] currentIps = new ImageProcessor[noOfSlices];
                    FloatProcessor[] results = new FloatProcessor[noOfSlices];
                    FloatProcessor[] originals = new FloatProcessor[noOfSlices];
                    for (int slice = 0; slice < noOfSlices; ++slice) {
                        FloatProcessor result = null;
                        ImageProcessor currentIp = null;
                        int stackNo = iInputImg.getStackIndex(channel + 1, slice + 1, frame + 1);
                        
                        if (iResultOutput != ResultOutput.NONE) {
                            currentIp = iProcessedImg.getStack().getProcessor(stackNo);
                            result = currentIp.toFloat(colorChannel, null);
                        }
                        ImageProcessor processor = iInputImg.getStack().getProcessor(stackNo);
                        
                        final FloatProcessor orig = processor.toFloat(colorChannel, null);
                        orig.setSliceNumber(stackNo);
                        currentIps[slice] = currentIp;
                        results[slice] = result;
                        originals[slice] = orig;
                    }
                    
                    // Generate separate thread on each color channel with full z-stack (for 3D processing)
                    ProcessOneCube p = new ProcessOneCube(currentIps, results, originals, colorChannel);
                    Thread t = new Thread(p);
                    th.add(t);
                    poc.add(p);
                }
            }
        }
        
        // Run all previously generated threads using maximum number of threads that can be run by system.
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            for (Runnable r : th) executor.execute(r);
            executor.shutdown(); 
            // Wait long enough to be sure that all tasks are finished. 
            // (NOTE: tasks running longer than about 292 billion years will not be finished properly ;-) ).
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Update output image - this is not thread safe operation so it is done in sequence. Should be fast
        // anyway - all processing tasks were done in threads.
        for (int i = 0; i < th.size(); ++i) {
                poc.get(i).update(); 
        }
        
        IJ.showStatus("Done.");
    }
}
