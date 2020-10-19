package mosaic.core.utils;


import ij.ImagePlus;
import ij.ImageStack;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import org.apache.log4j.Logger;

public class DilateImageClij {
    private static final Logger logger = Logger.getLogger(DilateImageClij.class);

    /**
     * Dilates all values and returns a copy of the input image.
     * A spherical structuring element of radius <code>radius</code> is used.
     * Adapted as is from Ingo Oppermann implementation
     * 
     * @param ips ImageProcessor to do the dilation with
     * @return the dilated copy of the given <code>ImageProcessor</code>
     */
    public static ImageStack dilate(ImageStack ips, int radius) {
        // get CLIJ2 instance with default device
        CLIJ2 clij2 = CLIJ2.getInstance();
        // push image and create empty one for result (with same parameters as input image)
        ClearCLBuffer imgBuffer = clij2.push(new ImagePlus("", ips));
        ClearCLBuffer outBuffer = clij2.create(imgBuffer);

        // Run kernel
        if (ips.getSize() == 1) {
            logger.debug("maximum2dSphere before");
            clij2.maximum2DSphere(imgBuffer, outBuffer, radius, radius);
            logger.debug("maximum2dSphere after");
        }
        else {
            logger.debug("maximum3dSphere before");
            clij2.maximum3DSphere(imgBuffer, outBuffer, radius, radius, radius);
            logger.debug("maximum3dSphere after");
        }
        // Get output and clear CLIJ2 buffers
        ImagePlus imgRes = clij2.pull(outBuffer);
        clij2.clear();

        return imgRes.getImageStack();
    }
}

