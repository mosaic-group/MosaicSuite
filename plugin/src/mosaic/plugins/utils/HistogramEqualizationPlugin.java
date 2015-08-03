package mosaic.plugins.utils;

import ij.process.ByteProcessor;

/**
 * This class serves as a example of how PlugIn8BitBase should be used.
 * @author Krzysztof Gonciarz
 */
public class HistogramEqualizationPlugin extends PlugIn8bitBase {

    @Override
    protected void processImg(ByteProcessor aOutputImg, ByteProcessor aOrigImg, int aChannelNumber) {
    	// get input/original image pixels
    	byte[] pixels = (byte[]) aOrigImg.getPixelsCopy();
    	
    	// perform equalization
    	do8bitHistogramEqualization(pixels);

    	// set processed pixels to output image
        aOutputImg.setPixels(pixels);
    }

    /**
     * Perform histogram equalization on input image pixels. This method works on 8-bit gray scale
     * images only.
     * @param aImgPixels Input image
     */
	private void do8bitHistogramEqualization(byte[] aImgPixels) {
		final int GRAY_LEVELS = 256;
		
		// Calculate histogram of a image
		int[] hist = new int[GRAY_LEVELS];
    	for (int i = 0; i < aImgPixels.length; ++i) {
    		int pv = aImgPixels[i] & 0xff;
    		hist[pv] += 1; 
    	}
    	
    	// Calculate cumulative image histogram
    	for (int i = 1; i < GRAY_LEVELS; ++i) {
    		hist[i] = hist[i] + hist[i-1];
    	}
    	
    	// Calculate  transformation 'T' of image brightness
    	double[] T = new double[GRAY_LEVELS];
    	final double G = GRAY_LEVELS;
    	final double N_M = aImgPixels.length; // N * M -> width * height
    	for (int i = 0; i < GRAY_LEVELS; ++i) {
    		T[i] = (hist[i]) * (G-1)/(N_M);
    	}

    	// Apply new scale of brightness to image
    	for (int i = 0; i < aImgPixels.length; ++i) {
    		int pv = aImgPixels[i] & 0xff;
    		double d = T[pv];
    		aImgPixels[i] = (byte)d; 
    	}
	}

    @Override
    protected boolean showDialog() {
    	// No dialog - just return
        return true;
    }

    @Override
    protected boolean setup(String aArgs) {
        setFilePrefix("equalized_");

        return true;
    }
}
