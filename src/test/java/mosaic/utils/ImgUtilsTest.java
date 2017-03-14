package mosaic.utils;

import org.junit.Assert;
import org.junit.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import mosaic.test.framework.CommonBase;

public class ImgUtilsTest extends CommonBase {

    @Test
    public void testImgResizeFloat() {
        final float[][] expected = new float[][] {{ 0, 3, 3},
                                                  {-1, 1, 1},
                                                  { 2, 5, 5},
                                                  { 2, 5, 5}};

        final float[][] input = new float[][] {{ 0, 3},
                                               {-1, 1},
                                               { 2, 5}};
        final float[][] output = new float[4][3];

        // Tested function
        ImgUtils.imgResize(input, output);

        compareArrays(expected, output);
    }

    @Test
    public void testImgResizeFloatSmaller() {
        final float[][] expected = new float[][] {{ 0}, {-1}};

        final float[][] input = new float[][]  {{ 0, 3},
                                                {-1, 1},
                                                { 2, 5}};
        final float[][] output = new float[2][1];

        // Tested function
        ImgUtils.imgResize(input, output);

        compareArrays(expected, output);
    }

    @Test
    public void testImgResizeDouble() {
        final double[][] expected = new double[][] {{ 0, 3, 3},
                                                    {-1, 1, 1},
                                                    { 2, 5, 5},
                                                    { 2, 5, 5}};

        final double[][] input = new double[][] {{ 0, 3},
                                                 {-1, 1},
                                                 { 2, 5}};
        final double[][] output = new double[4][3];

        // Tested function
        ImgUtils.imgResize(input, output);

        compareArrays(expected, output);
    }

    @Test
    public void testImgResizeDoubleLonger() {
        final double[][] expected = new double[][] {{ 0, 3, 3, 3},
                                                    {-1, 1, 1, 1}};

        final double[][] input = new double[][] {{ 0, 3},
                                                 {-1, 1},
                                                 { 2, 5}};
        final double[][] output = new double[2][4];

        // Tested function
        ImgUtils.imgResize(input, output);

        compareArrays(expected, output);
    }

    @Test
    public void testImgToYX2Darray() {
        final float[][] originalImgArray = {{1.0f, 2.0f},
                                            {3.0f, 4.0f},
                                            {5.0f, 6.0f},
                                            {7.0f, 8.0f},
                                            {9.0f,10.0f}};
        final FloatProcessor fp = new FloatProcessor(originalImgArray);
        Assert.assertEquals(originalImgArray.length, fp.getWidth());
        Assert.assertEquals(originalImgArray[0].length, fp.getHeight());

        final float[][] outputArray = new float[2][5];

        ImgUtils.ImgToYX2Darray(fp, outputArray, 1.0f);

        for (int x = 0; x < 5; ++x) {
            for (int y = 0; y < 2; ++y) {
                Assert.assertEquals("Values must be same (factorization value = 1.0)", originalImgArray[x][y], outputArray[y][x], 0.001f);
            }
        }
    }

    @Test
    public void testImgToYX2DarrayDouble() {
        final float[][] originalImgArray = {{1.0f, 2.0f},
                                            {3.0f, 4.0f},
                                            {5.0f, 6.0f},
                                            {7.0f, 8.0f},
                                            {9.0f,10.0f}};
        final FloatProcessor fp = new FloatProcessor(originalImgArray);
        Assert.assertEquals(originalImgArray.length, fp.getWidth());
        Assert.assertEquals(originalImgArray[0].length, fp.getHeight());

        final double[][] outputArray = new double[2][5];

        ImgUtils.ImgToYX2Darray(fp, outputArray, 1.0f);

        for (int x = 0; x < 5; ++x) {
            for (int y = 0; y < 2; ++y) {
                Assert.assertEquals("Values must be same (factorization value = 1.0)", originalImgArray[x][y], outputArray[y][x], 0.001f);
            }
        }
    }

    @Test
    public void testYX2DarrayToImg() {
        // [y][x] == [5][2] => imgDims 2x5
        final float[][] originalImgArray = {{1.0f, 2.0f},
                                            {3.0f, 4.0f},
                                            {5.0f, 6.0f},
                                            {7.0f, 8.0f},
                                            {9.0f,10.0f}};
        final FloatProcessor fp = new FloatProcessor(originalImgArray[0].length, originalImgArray.length);
        ImgUtils.YX2DarrayToImg(originalImgArray, fp, 1.0f);
        final float[] pixels = (float[])fp.getPixels();

        for (int x = 0; x < 2; ++x) {
            for (int y = 0; y < 5; ++y) {
                Assert.assertEquals("Values must be same (factorization value = 1.0)", originalImgArray[y][x], pixels[x + y * fp.getWidth()], 0.001f);
            }
        }
    }

    @Test
    public void testYX2DarrayToImgDouble() {
        // [y][x] == [5][2] => imgDims 2x5
        final double[][] originalImgArray = {{1.0, 2.0},
                                             {3.0, 4.0},
                                             {5.0, 6.0},
                                             {7.0, 8.0},
                                             {9.0,10.0}};
        final FloatProcessor fp = new FloatProcessor(originalImgArray[0].length, originalImgArray.length);
        ImgUtils.YX2DarrayToImg(originalImgArray, fp, 1.0f);
        final float[] pixels = (float[])fp.getPixels();

        for (int x = 0; x < 2; ++x) {
            for (int y = 0; y < 5; ++y) {
                Assert.assertEquals("Values must be same (factorization value = 1.0)", originalImgArray[y][x], pixels[x + y * fp.getWidth()], 0.001f);
            }
        }
    }

    @Test
    public void testImgToXY2Darray() {
        final float[][] originalImgArray = {{1.0f, 2.0f},
                                            {3.0f, 4.0f},
                                            {5.0f, 6.0f},
                                            {7.0f, 8.0f},
                                            {9.0f,10.0f}};
        final FloatProcessor fp = new FloatProcessor(originalImgArray);
        Assert.assertEquals(originalImgArray.length, fp.getWidth());
        Assert.assertEquals(originalImgArray[0].length, fp.getHeight());

        final float[][] outputArray = new float[5][2];

        ImgUtils.ImgToXY2Darray(fp, outputArray, 1.0f);

        for (int x = 0; x < 5; ++x) {
            for (int y = 0; y < 2; ++y) {
                Assert.assertEquals("Values must be same (factorization value = 1.0)", originalImgArray[x][y], outputArray[x][y], 0.001f);
            }
        }
    }

    @Test
    public void testXY2DarrayToImg() {
        // [x][y] == [5][2] => imgDims 5x2
        final float[][] originalImgArray = {{1.0f, 2.0f},
                                            {3.0f, 4.0f},
                                            {5.0f, 6.0f},
                                            {7.0f, 8.0f},
                                            {9.0f,10.0f}};
        final FloatProcessor fp = new FloatProcessor(originalImgArray.length, originalImgArray[0].length);
        ImgUtils.XY2DarrayToImg(originalImgArray, fp, 1.0f);
        final float[] pixels = (float[])fp.getPixels();

        for (int x = 0; x < 5; ++x) {
            for (int y = 0; y < 2; ++y) {
                Assert.assertEquals("Values must be same (factorization value = 1.0)", originalImgArray[x][y], pixels[x + y * fp.getWidth()], 0.001f);
            }
        }
    }
    
    @Test
    public void testConvertToNormalizedGloballyFloatType() {
        // [z][x][y] == [2][5][2] => 2 frames with imgDims 5x2
        final float[][][] frames = {{{1.0f, 2.0f}, // frame 1
                                     {3.0f, 4.0f},
                                     {5.0f, 6.0f},
                                     {7.0f, 8.0f},
                                     {9.0f,10.0f}},
                
                                   {{11.0f, 12.0f}, // frame 2
                                    {13.0f, 14.0f},
                                    {15.0f, 16.0f},
                                    {17.0f, 18.0f},
                                    {19.0f, 20.0f}}};
        ImageStack is = new ImageStack(frames[0].length, frames[0][0].length);
        for (int i = 0; i < frames.length; ++i) is.addSlice(new FloatProcessor(frames[i]));
        ImagePlus outImg = ImgUtils.convertToNormalizedGloballyFloatType(new ImagePlus("", is));
        ImageStack outStack = outImg.getStack();
        Assert.assertEquals(outStack.size(), 2);
        
        for (int i = 0; i < outStack.size(); ++i) {
            final float[] pixels = (float[])outStack.getProcessor(i + 1).getPixels();
            for (int x = 0; x < 5; ++x) {
                for (int y = 0; y < 2; ++y) {
                    Assert.assertEquals("Values must be same (factorization value = 1.0)", 
                                        frames[i][x][y], 
                                        pixels[x + y * 5]  * 19 + 1, // check if pixels are normalized correctly (range of values 1-19) 
                                        0.001f);
                }
            }
        }
    }
}
