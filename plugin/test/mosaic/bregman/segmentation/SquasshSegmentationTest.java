package mosaic.bregman.segmentation;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import ij.IJ;
import ij.ImagePlus;
import mosaic.test.framework.CommonBase;
import mosaic.utils.ArrayOps.MinMax;
import mosaic.utils.ArrayOps;
import mosaic.utils.ConvertArray;
import mosaic.utils.ImgUtils;


public class SquasshSegmentationTest extends CommonBase {

    @Test
    public void testGenerateSeriesOfSquassh() {
//        String path = "/Users/gonciarz/Documents/MOSAIC/work/testInputs/Crop_12-12.tif";
//        ImagePlus ip = loadImagePlus(path);
//        
//        double[][][] img = ImgUtils.ImgToZXYarray(ip);
//        short[][] blank = new short[img[0].length][img[0][0].length];
//        
//        int numOfSteps = 256;
//        short[][][] out = new short[numOfSteps][][];
//        for (int i = 0; i < numOfSteps; i++) {
//            System.out.println(i * (1.0/(numOfSteps)));
//            SegmentationParameters sp = new SegmentationParameters(4, 1, 0.1, i * (1.0/(numOfSteps-1)), true, SegmentationParameters.IntensityMode.AUTOMATIC, SegmentationParameters.NoiseModel.GAUSS, 1, 1, 0);
//            SquasshSegmentation ss = new SquasshSegmentation(img, sp, 0, 255);
//            ss.run();
//            out[i] = (ss.iRegionsList.size() > 0) ? ss.iLabeledRegions[0] : blank;
//        }
//        ImagePlus outImg = ImgUtils.ZXYarrayToImg(ConvertArray.toDouble(out), "Output");
//        IJ.saveAsTiff(outImg, "/tmp/out.tif");
    }

    @Test
    public void test2() {
//        int size = 9;
//        int count = 0;
//        double[][] img = new double[size][size];
//        for (int x = size/3; x <=size/3*2; x++)
//            for (int y = size/3; y <=size/3*2; y++)
//                {img[x][y] = 1; count++;}
//        
//        SegmentationParameters sp = new SegmentationParameters(1, 1, 0.9, 0.0, true, SegmentationParameters.IntensityMode.AUTOMATIC, SegmentationParameters.NoiseModel.POISSON, 1, 1, 0);
//        SquasshSegmentation ss = new SquasshSegmentation(new double[][][] {img}, sp, 0, 1);
//        ss.run();
//        
//        System.out.println(Arrays.deepToString(ss.iLabeledRegions));
//        System.out.println(ss.iRegionsList.get(0).iPixels.size() + " vs " + count);
    }
}
