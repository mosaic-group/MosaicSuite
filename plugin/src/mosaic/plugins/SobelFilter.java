package mosaic.plugins;

import ij.process.FloatProcessor;
import mosaic.math.Matlab;
import mosaic.math.Matrix;
import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.PlugInFloatBase;

public class SobelFilter extends PlugInFloatBase {

    @Override
    protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg, int aChannelNumber) {
      int w = aOutputImg.getWidth(); 
      int h = aOutputImg.getHeight();
      double[][] ma = new double[h][w];
      ImgUtils.ImgToYX2Darray(aOrigImg, ma, 1.0);
      Matrix m = new Matrix(ma);
      
      // Sobel Filter
      Matrix m1 = Matlab.imfilterSymmetric(m, new Matrix(new double[][]{{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}}));
      Matrix m2 = Matlab.imfilterSymmetric(m, new Matrix(new double[][]{{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}}));
      m1.elementMult(m1);
      m2.elementMult(m2);
      m1.add(m2);
      m1.sqrt();
      double[][] id = m1.getArrayYX();

      ImgUtils.YX2DarrayToImg(id, aOutputImg, 1.0);
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
