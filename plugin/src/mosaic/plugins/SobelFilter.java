package mosaic.plugins;

import ij.process.FloatProcessor;
import mosaic.math.Matlab;
import mosaic.math.Matrix;
import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.PlugInFloatBase;

/**
 * Sobel filter implementation.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class SobelFilter extends PlugInFloatBase { // NO_UCD

    @Override
    protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg, int aChannelNumber) {
      // Convert input to Matrix
      int w = aOutputImg.getWidth(); 
      int h = aOutputImg.getHeight();
      double[][] img = new double[h][w];
      ImgUtils.ImgToYX2Darray(aOrigImg, img, 1.0);
      Matrix imgMatrix = new Matrix(img);
      
      // Sobel Filter    resutl = sqrt((K1*img)^2 + (K2*img)^2)
      Matrix m1 = Matlab.imfilterSymmetric(imgMatrix, new Matrix(new double[][]{{-1,  0,  1}, 
                                                                                {-2,  0,  2}, 
                                                                                {-1,  0,  1}}));
      
      Matrix m2 = Matlab.imfilterSymmetric(imgMatrix, new Matrix(new double[][]{{-1, -2, -1}, 
                                                                                { 0,  0,  0}, 
                                                                                { 1,  2,  1}}));
      m1.elementMult(m1);
      m2.elementMult(m2);
      m1.add(m2);
      m1.sqrt();
      
      // Update output with reslt
      double[][] result = m1.getArrayYX();
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
