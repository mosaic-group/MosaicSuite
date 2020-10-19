package mosaic.core.utils;

import static org.junit.Assert.*;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.StackWindow;
import mosaic.test.framework.CommonBase;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import org.apache.log4j.Logger;
import org.junit.Test;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij.CLIJ;

import java.util.ArrayList;


public class DilateImageTest extends CommonBase  {
    private static final Logger logger = Logger.getLogger(DilateImageTest.class);

    @Test
    public void testDilate() {

        int radius = 5;

        ImagePlus img = loadImagePlus("/Users/gonciarz/Desktop/pt/singleFrame.tif");

        logger.debug("============> CPU start");
//        ImageStack dimg = DilateImage.dilate(img.getImageStack().convertToFloat(), radius, 4);
        logger.debug("<============ CPU stop");
//        new StackWindow(new ImagePlus("dilated ", dimg));

        logger.debug("============> CLIJ2 start");
        CLIJ.debug = true;
//        CLIJ2 clij2 = CLIJ2.getInstance("GeForce GT 750M");

        ArrayList<String> clDevs = CLIJ.getAvailableDeviceNames();
        System.out.println(clDevs);
        CLIJ2 clij2 = CLIJ2.getInstance(clDevs.get(1));

        ClearCLBuffer imgBuffer = clij2.push(img);
        ClearCLBuffer outBuffer = clij2.create(imgBuffer);

        logger.debug("maximum2dSphere before");
        clij2.maximum2DSphere(imgBuffer, outBuffer, radius, radius);
//        clij2.maximum3DSphere(imgBuffer, outBuffer, radius, radius, radius);
        logger.debug("maximum2dSphere after");

        ImagePlus imgRes = clij2.pull(outBuffer);
        imgBuffer.close();
        outBuffer.close();
        logger.debug("<============ CLIJ2 stop");

//        imgRes.show();

        //        Maximum2DSphere.maximum2DSphere(clij2, imgBuffer, outBuffer, radius, radius);

//        sleep(8000);
    }
}
