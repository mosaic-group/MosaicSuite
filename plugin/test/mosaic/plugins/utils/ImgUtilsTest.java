package mosaic.plugins.utils;

import static org.junit.Assert.assertEquals;
import ij.process.FloatProcessor;
import mosaic.plugins.utils.ImgUtils.MinMax;
import mosaic.test.framework.CommonBase;

import org.junit.Assert;
import org.junit.Test;

public class ImgUtilsTest extends CommonBase {

    @Test
    public void testNormalizeFloat() {
        float[][] expected = new float[][] {{0.25f, 1}, {0, 0.5f}, {0.75f, 0.25f}};

        float[][] input = new float[][] {{0, 3}, {-1, 1}, {2, 0}};

        // Tested function
        ImgUtils.normalize(input);

        compareArrays(expected, input);
    }

    @Test
    public void testNormalizeDouble() {
        double[][] expected = new double[][] {{0.25f, 1}, {0, 0.5f}, {0.75f, 0.25f}};

        double[][] input = new double[][] {{0, 3}, {-1, 1}, {2, 0}};

        // Tested function
        ImgUtils.normalize(input);

        compareArrays(expected, input);
    }

    @Test
    public void testImgResizeFloat() {
        float[][] expected = new float[][] {{ 0, 3, 3},
                {-1, 1, 1},
                { 2, 5, 5},
                { 2, 5, 5}};

        float[][] input = new float[][] {{ 0, 3},
                {-1, 1},
                { 2, 5}};
        float[][] output = new float[4][3];

        // Tested function
        ImgUtils.imgResize(input, output);

        compareArrays(expected, output);
    }

    @Test
    public void testImgResizeFloatSmaller() {
        float[][] expected = new float[][] {{ 0},
                {-1}};

        float[][] input = new float[][] {{ 0, 3},
                {-1, 1},
                { 2, 5}};
        float[][] output = new float[2][1];

        // Tested function
        ImgUtils.imgResize(input, output);

        compareArrays(expected, output);
    }

    @Test
    public void testImgResizeDouble() {
        double[][] expected = new double[][] {{ 0, 3, 3},
                {-1, 1, 1},
                { 2, 5, 5},
                { 2, 5, 5}};

        double[][] input = new double[][] {{ 0, 3},
                {-1, 1},
                { 2, 5}};
        double[][] output = new double[4][3];

        // Tested function
        ImgUtils.imgResize(input, output);

        compareArrays(expected, output);
    }

    @Test
    public void testImgResizeDoubleLonger() {
        double[][] expected = new double[][] {{ 0, 3, 3, 3},
                {-1, 1, 1, 1}};

        double[][] input = new double[][] {{ 0, 3},
                {-1, 1},
                { 2, 5}};
        double[][] output = new double[2][4];

        // Tested function
        ImgUtils.imgResize(input, output);

        compareArrays(expected, output);
    }

    @Test
    public void testFindMinMax() {
        MinMax<Float> expected = new MinMax<Float>(-1f, 5f);

        float[][] input = new float[][] {{ 0, 3},
                {-1, 1},
                { 2, 5}};

        // Tested function
        MinMax<Float> result = ImgUtils.findMinMax(input);

        assertEquals("Min", expected.getMin(), result.getMin(), 0.001f);
        assertEquals("Max", expected.getMax(), result.getMax(), 0.001f);
    }

    @Test
    public void testConvertRange() {
        float[][] expected = new float[][] {{0, 3}, {-1, 1}, {2, 0}};

        float[][] input = new float[][] {{0.25f, 1}, {0, 0.5f}, {0.75f, 0.25f}};

        // Tested function
        ImgUtils.convertRange(input, 3 - (-1), -1);

        compareArrays(expected, input);
    }

    @Test
    public void testImgToYX2Darray() {
        final float[][] originalImgArray = {{1.0f, 2.0f},
                {3.0f, 4.0f},
                {5.0f, 6.0f},
                {7.0f, 8.0f},
                {9.0f,10.0f}};
        FloatProcessor fp = new FloatProcessor(originalImgArray);
        Assert.assertEquals(originalImgArray.length, fp.getWidth());
        Assert.assertEquals(originalImgArray[0].length, fp.getHeight());

        float[][] outputArray = new float[2][5];

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
        FloatProcessor fp = new FloatProcessor(originalImgArray);
        Assert.assertEquals(originalImgArray.length, fp.getWidth());
        Assert.assertEquals(originalImgArray[0].length, fp.getHeight());

        double[][] outputArray = new double[2][5];

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
        FloatProcessor fp = new FloatProcessor(originalImgArray[0].length, originalImgArray.length);
        ImgUtils.YX2DarrayToImg(originalImgArray, fp, 1.0f);
        float[] pixels = (float[])fp.getPixels();

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
        FloatProcessor fp = new FloatProcessor(originalImgArray[0].length, originalImgArray.length);
        ImgUtils.YX2DarrayToImg(originalImgArray, fp, 1.0f);
        float[] pixels = (float[])fp.getPixels();

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
        FloatProcessor fp = new FloatProcessor(originalImgArray);
        Assert.assertEquals(originalImgArray.length, fp.getWidth());
        Assert.assertEquals(originalImgArray[0].length, fp.getHeight());

        float[][] outputArray = new float[5][2];

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
        FloatProcessor fp = new FloatProcessor(originalImgArray.length, originalImgArray[0].length);
        ImgUtils.XY2DarrayToImg(originalImgArray, fp, 1.0f);
        float[] pixels = (float[])fp.getPixels();

        for (int x = 0; x < 5; ++x) {
            for (int y = 0; y < 2; ++y) {
                Assert.assertEquals("Values must be same (factorization value = 1.0)", originalImgArray[x][y], pixels[x + y * fp.getWidth()], 0.001f);
            }
        }
    }

    static private void compareArrays(float[][] expected, float[][] result) {
        for (int i = 0; i < expected.length; ++i) {
            for (int j = 0; j < expected[0].length; ++j) {
                assertEquals("Elements at [" + i + "][" + j + "] should be same", expected[i][j], result[i][j], 0.0f);
            }
        }
    }

    static private void compareArrays(double[][] expected, double[][] result) {
        for (int i = 0; i < expected.length; ++i) {
            for (int j = 0; j < expected[0].length; ++j) {
                assertEquals("Elements at [" + i + "][" + j + "] should be same", expected[i][j], result[i][j], 0.0f);
            }
        }
    }
}

