package mosaic.core.utils;


import ij.IJ;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;


public class MosaicImageProcessingTools {
    /**
     * Dilates all values larger than <code>threshold</code> and returns a copy of the input image.
     * A spherical structuring element of radius <code>radius</code> is used.
     * Adapted as is from Ingo Oppermann implementation
     * 
     * @param ip ImageProcessor to do the dilation with
     * @return the dilated copy of the given <code>ImageProcessor</code>
     */
    public static ImageStack dilateGeneric(ImageStack ips, int radius, int number_of_threads) {
        final FloatProcessor[] dilated_procs = new FloatProcessor[ips.getSize()];
        final AtomicInteger z = new AtomicInteger(-1);
        final Vector<Thread> threadsVector = new Vector<Thread>(number_of_threads);
        for (int thread_counter = 0; thread_counter < number_of_threads; thread_counter++) {
            threadsVector.add(new DilateGenericThread(ips, radius, dilated_procs, z));
        }
        for (final Thread t : threadsVector) {
            t.start();
        }
        for (final Thread t : threadsVector) {
            try {
                t.join();
            }
            catch (final InterruptedException ie) {
                IJ.showMessage("Calculation interrupted. An error occured in parallel dilation:\n" + ie.getMessage());
            }
        }
        final ImageStack dilated_ips = new ImageStack(ips.getWidth(), ips.getHeight());
        for (int s = 0; s < ips.getSize(); s++) {
            dilated_ips.addSlice(null, dilated_procs[s]);
        }

        return dilated_ips;
    }

    /**
     * Generates the dilation mask
     * Adapted from Ingo Oppermann implementation
     * 
     * @param mask_radius the radius of the mask (user defined)
     */
    public static int[][] generateMask(int mask_radius) {
        final int width = (2 * mask_radius) + 1;
        final int[][] mask = new int[width][width * width];
        for (int s = -mask_radius; s <= mask_radius; s++) {
            for (int i = -mask_radius; i <= mask_radius; i++) {
                for (int j = -mask_radius; j <= mask_radius; j++) {
                    final int index = (i + mask_radius) * (width) + (j + mask_radius);
                    if ((i * i) + (j * j) + (s * s) <= mask_radius * mask_radius) {
                        mask[s + mask_radius][index] = 1;
                    }
                    else {
                        mask[s + mask_radius][index] = 0;
                    }
                }
            }
        }
        return mask;
    }
}

class DilateGenericThread extends Thread {

    private final ImageStack ips;
    private final ImageProcessor[] dilated_ips;
    private final AtomicInteger atomic_z;
    private final int kernel_width;
    private final int image_width;
    private final int image_height;
    private final int image_depth;
    private final int radius;
    private final int mask[][];

    DilateGenericThread(ImageStack is, int aRadius, ImageProcessor[] dilated_is, AtomicInteger z) {
        ips = is;
        dilated_ips = dilated_is;
        atomic_z = z;
        radius = aRadius;
        
        kernel_width = radius * 2 + 1;
        image_width = ips.getWidth();
        image_height = ips.getHeight();
        image_depth = ips.getSize();
        
        mask = MosaicImageProcessingTools.generateMask(radius);
    }

    @Override
    public void run() {
        float max;
        int z;
        while ((z = atomic_z.incrementAndGet()) < image_depth) {
            final FloatProcessor out_p = new FloatProcessor(image_width, image_height);
            final float[] output = (float[]) out_p.getPixels();
            for (int y = 0; y < image_height; y++) {
                for (int x = 0; x < image_width; x++) {
                    max = Float.NEGATIVE_INFINITY;

                    // a,b,s are the kernel coordinates corresponding to x,y,z
                    for (int s = -radius; s <= radius; s++) {
                        if (z + s < 0 || z + s >= image_depth) {
                            continue;
                        }
                        final float[] current_processor_pixels = (float[]) ips.getPixels(z + s + 1);
                        for (int b = -radius; b <= radius; b++) {
                            if (y + b < 0 || y + b >= image_height) {
                                continue;
                            }
                            for (int a = -radius; a <= radius; a++) {
                                if (x + a < 0 || x + a >= image_width) {
                                    continue;
                                }
                                if (mask[s + radius][(a + radius) * kernel_width + (b + radius)] == 1) {
                                    float t;
                                    if ((t = current_processor_pixels[(y + b) * image_width + (x + a)]) > max) {
                                        max = t;
                                    }
                                }
                            }
                        }
                    }
                    output[y * image_width + x] = max;
                }
            }
            dilated_ips[z] = out_p;
        }
    }
}
