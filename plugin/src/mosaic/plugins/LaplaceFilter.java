package mosaic.plugins;

import ij.process.FloatProcessor;
import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.PlugInFloatBase;
import mosaic.utils.math.Matlab;
import mosaic.utils.math.Matrix;

/**
 * Laplace filter implementation.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class LaplaceFilter extends PlugInFloatBase { // NO_UCD

    @Override
    protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg, int aChannelNumber) {
        // Convert input to Matrix
        final int w = aOutputImg.getWidth();
        final int h = aOutputImg.getHeight();
        final double[][] img = new double[h][w];
        ImgUtils.ImgToYX2Darray(aOrigImg, img, 1.0);
        final Matrix imgMatrix = new Matrix(img);

        // Laplacian matrix convolution with image
        final Matrix m1 = Matlab.imfilterSymmetric(imgMatrix, new Matrix(new double[][] { { 0,  1,  0 }, 
                                                                                          { 1, -4,  1 }, 
                                                                                          { 0,  1,  0 } }));
        // Update output with reslt
        final double[][] result = m1.getArrayYX();
        ImgUtils.YX2DarrayToImg(result, aOutputImg, 1.0);
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
