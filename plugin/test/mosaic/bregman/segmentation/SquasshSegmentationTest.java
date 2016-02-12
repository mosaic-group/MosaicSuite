package mosaic.bregman.segmentation;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
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
    public void test1ctrl2() {
//        String path = "/Users/gonciarz/Documents/MOSAIC/work/repo/MosaicSuite/plugin/Jtest_data/Squassh/ScriptR/1 Ctrl 2.tif";
//        ImagePlus ipl = loadImagePlus(path);
//        ImagePlus ip = setupChannel(ipl, 1, 2);
//        double[][][] img = ImgUtils.ImgToZXYarray(ip);
//        MinMax<Double> mm = ImgUtils.findMinMax(ip);
//        SegmentationParameters sp = new SegmentationParameters(4, 1, 0.2, 0.03, true, SegmentationParameters.IntensityMode.AUTOMATIC, SegmentationParameters.NoiseModel.POISSON, 0.63, 1, 0);
//        SquasshSegmentation ss = new SquasshSegmentation(img, sp, mm.getMin(), mm.getMax());
//        ss.run();
//        ImagePlus outImg = ImgUtils.ZXYarrayToImg(ConvertArray.toDouble(ss.iLabeledRegions), "Output");
//        IJ.saveAsTiff(outImg, "/tmp/out.tif");
//        try {
//            Thread.sleep(30000);
//        }
//        catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }
    
    @Test
    public void test2() {
//        int size = 6; int from = size/3; int to = 2*size/3;
//        int count = 0;
//        double[][] img = new double[size][size];
//        for (int x = from; x < to; x++)
//            for (int y = from; y < to; y++)
//                {img[x][y] = 1; count++;}
//        System.out.println(generateAsciiImage(img));
//        SegmentationParameters sp = new SegmentationParameters(1, 1, 0.001, 0.0, true, SegmentationParameters.IntensityMode.AUTOMATIC, SegmentationParameters.NoiseModel.POISSON, 1, 1, 0);
//        SquasshSegmentation ss = new SquasshSegmentation(new double[][][] {img}, sp, 0, 1);
//        ss.run();
//        
//        System.out.println(generateAsciiImage(ConvertArray.toDouble(ss.iLabeledRegions)[0]));
//        System.out.println(ss.iRegionsList.get(0).iPixels.size() + " vs " + count);
    }
    
    private static ImagePlus setupChannel(ImagePlus img2, final int currentFrame, final int channel) {
        final ImageStack img_s = generateImgStack(img2, currentFrame, img2.getBitDepth(), channel);
        ImagePlus imgA = new ImagePlus();
        imgA.setStack(img2.getTitle(), img_s);
        return imgA;
    }
    
    private static ImageStack generateImgStack(ImagePlus img2, final int currentFrame, final int bits, int channel) {
        int ni = img2.getWidth();
        int nj = img2.getHeight();
        int nz = img2.getNSlices();
        final ImageStack img_s = new ImageStack(ni, nj);

        for (int z = 0; z < nz; z++) {
            img2.setPosition(channel, z + 1, currentFrame);
            ImageProcessor impt = (bits == 32) ? img2.getProcessor().convertToShort(false) : img2.getProcessor();
            img_s.addSlice("", impt);
        }
        
        return img_s;
    }
    
    private String generateAsciiImage(double[][] aImage) {
        int sizeX = aImage.length;
        int sizeY = aImage[0].length;
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < sizeY; ++y) {
            for (int x = 0; x < sizeX; ++x) {
                sb.append(Tools.round(aImage[x][y], 1) + " ");
            }
            sb.append('\n');
        }
        String result = sb.toString();
        return result;
    }
}
