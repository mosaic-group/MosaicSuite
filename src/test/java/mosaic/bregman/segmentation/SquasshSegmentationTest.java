package mosaic.bregman.segmentation;

import org.junit.Test;

import mosaic.test.framework.CommonBase;


public class SquasshSegmentationTest extends CommonBase {

    @Test
    public void testGenerateSeriesOfSquassh() {
//        String path = "/Users/gonciarz/small.tif";
//        ImagePlus ip = loadImagePlus(path);
//        
//        double[][][] img = ImgUtils.ImgToZXYarray(ip);
//        short[][] blank = new short[img[0].length][img[0][0].length];
//        
//        int numOfSteps = 20;
//        float fromVal = 0;
//        float toVal = 0.2f;
//        float fromValInt = 0.2f;
//        float toValInt = 0.8f;
//        short[][][] out = new short[numOfSteps *numOfSteps][][];
//        for (int j = 0; j < numOfSteps; j++) {
//        for (int i = 0; i < numOfSteps; i++) {
//            System.out.println(i * (1.0/(numOfSteps)));
//            SegmentationParameters sp = new SegmentationParameters(
//                    4, // num threads
//                    1, // interpolation (multiplication factor)
//                    i * (toVal - fromVal) / numOfSteps,  // regularization
//                    j * (toValInt - fromValInt) / numOfSteps, // min obj intensity
//                    true, // exclude z edges
//                    SegmentationParameters.IntensityMode.AUTOMATIC, // intensity mode 
//                    SegmentationParameters.NoiseModel.GAUSS, // noise model
//                    1, // sigma xy
//                    1, // sigma z
//                    0, // min regions intensity
//                    2); // min region size
//            SquasshSegmentation ss = new SquasshSegmentation(img, sp, 0, 255);
//            ss.run();
//            out[i +j * numOfSteps] = (ss.iRegionsList.size() > 0) ? ss.iLabeledRegions[0] : blank;
//        }}
//        ImagePlus outImg = ImgUtils.ZXYarrayToImg(ConvertArray.toDouble(out), "Output");
//        IJ.saveAsTiff(outImg, "/tmp/out.tif");
//        System.out.println("Range: " +fromVal +"-"+toVal + "   step: " + (toVal - fromVal) / numOfSteps);
    }

    @Test
    public void test1ctrl2() {
//        String path = "/Users/gonciarz/Documents/MOSAIC/work/repo/MosaicSuite/plugin/Jtest_data/Squassh/ScriptR/1 Ctrl 2.tif";
//        ImagePlus ipl = loadImagePlus(path);
//        System.out.println(ImgUtils.getImageInfo(ipl));
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
    
//    @Test
//    public void test2() {
//        int size = 6 ; int from = size/3; int to = 2*size/3;
//        int count = 0;
//        double[][] img = new double[size][size];
//        for (int x = from; x < to; x++)
//            for (int y = from; y < to; y++)
//                {img[x][y] = 0.1; count++;}
//        System.out.println(generateAsciiImage(img));
//        SegmentationParameters sp = new SegmentationParameters(1, 1, 0.3, 0.01, true, SegmentationParameters.IntensityMode.HIGH, SegmentationParameters.NoiseModel.POISSON, 0.1, 0.1, 0);
//        SquasshSegmentation ss = new SquasshSegmentation(new double[][][] {img}, sp, 0, 0.1);
//        ss.run();
//        
//        System.out.println(generateAsciiImage(ConvertArray.toDouble(ss.iLabeledRegions)[0]));
//        System.out.println(ss.iRegionsList.get(0).iPixels.size() + " vs " + count);
//    }
//    
//    private String generateAsciiImage(double[][] aImage) {
//        System.out.println(Debug.getArrayDims(aImage));
//        int sizeX = aImage.length;
//        int sizeY = aImage[0].length;
//        StringBuilder sb = new StringBuilder();
//        for (int y = 0; y < sizeY; ++y) {
//            for (int x = 0; x < sizeX; ++x) {
//                sb.append(Tools.round(aImage[x][y], 1) + " ");
//            }
//            sb.append('\n');
//        }
//        String result = sb.toString();
//        return result;
//    }
    
    @Test
    public void testGray() {
//        String path1 = "/tmp/test1.tif";
//        ImagePlus ip1 = loadImagePlus(path1);
//        String path2 = "/tmp/test2.tif";
//        ImagePlus ip2 = loadImagePlus(path2);
//        int channels = 2;
//        ImagePlus[] ips = new ImagePlus[] {ip1, ip2};
//        ImageStack is = new ImageStack(ip1.getWidth(), ip1.getHeight());
//        for (int f = 0; f < ip1.getNFrames();f++)
//        for (int z = 0; z < ip1.getNSlices();z++)
//        for (int c = 0; c < channels; c++) {
//            int sidx = ip1.getStackIndex(1, z + 1, f + 1);
//            mosaic.utils.Debug.print(sidx, c, z, f);
//            is.addSlice(ips[c].getStack().getProcessor(sidx));
//        }
//        
//        ip1.setStack(is);
//        ip1.setDimensions(2, 1, 3);
//        ip1.setOpenAsHyperStack(true);
//        ip1.show();
//      try {
//      Thread.sleep(30000);
//      }
//      catch (InterruptedException e) {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//      }
    }
}
