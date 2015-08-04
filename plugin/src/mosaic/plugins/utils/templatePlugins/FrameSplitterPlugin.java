package mosaic.plugins.utils.templatePlugins;

import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.PlugInFloat3DBase;
import ij.process.FloatProcessor;

/**
 * This class serves as a example of how PlugInFloat3DBase should be used.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class FrameSplitterPlugin extends PlugInFloat3DBase {

    @Override
    protected void processImg(FloatProcessor[] aOutputImg, FloatProcessor[] aOrigImg, int aChannelNumber) {
        // Get dimensions of input image and create container for it
        int originalWidth = aOrigImg[0].getWidth();
        int originalHeight = aOrigImg[0].getHeight();
        int noOfSlices = aOrigImg.length;
        float[][] img = new float[originalHeight][originalWidth]; 

        // Process all images in cube going through all z-axis slices.
        for (int slice = 0; slice < noOfSlices; ++slice) {
            ImgUtils.ImgToYX2Darray(aOrigImg[slice], img, 1.0f);
            
            // Processing of img...
            
            ImgUtils.YX2DarrayToImg(img, aOutputImg[slice], 1.0f);
        }
    }

    @Override
    protected boolean showDialog() {
        return true;
    }

    @Override
    protected boolean setup(String aArgs) {
        return true;
    }
}
