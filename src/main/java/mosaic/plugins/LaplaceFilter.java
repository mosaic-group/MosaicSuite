package mosaic.plugins;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import mosaic.plugins.utils.PlugInFloatBase;
import mosaic.utils.ImgUtils;
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
    protected void postprocessBeforeShow() {
        iProcessedImg.resetDisplayRange();
    }
    
    @Override
    protected boolean showDialog() {
        return true;
    }

    @Override
    protected boolean setup(String aArgs) {
        // Regardless on input output will be always float image, generate one with same geometry.
        setResultDestination(ResultOutput.NEW_BY_PLUGIN);
        iProcessedImg = ImgUtils.createNewEmptyImgPlus(iInputImg, "laplace_" + iInputImg.getTitle(), 1, 1, ImgUtils.OutputType.FLOAT);
        updateFlags(DOES_ALL);
        return true;
    }
    
    public static void main(String[] args) {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = LaplaceFilter.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ImageJ();

        // open the Clown sample
        ImagePlus image = IJ.openImage("https://upload.wikimedia.org/wikipedia/commons/3/3f/Bikesgray.jpg");
        image.show();

        // run the plugin
        IJ.runPlugIn(clazz.getName(), "");
    }
}
