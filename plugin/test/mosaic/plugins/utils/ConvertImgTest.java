package mosaic.plugins.utils;

import ij.process.FloatProcessor;

import org.junit.Assert;
import org.junit.Test;

public class ConvertImgTest {

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
        
        ConvertImg.ImgToYX2Darray(fp, outputArray, 1.0f);
        
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
        ConvertImg.YX2DarrayToImg(originalImgArray, fp, 1.0f);
        float[] pixels = (float[])fp.getPixels();
        
        for (int x = 0; x < 2; ++x) {
            for (int y = 0; y < 5; ++y) {
                Assert.assertEquals("Values must be same (factorization value = 1.0)", originalImgArray[y][x], pixels[x + y * fp.getWidth()], 0.001f);
            }
        }
    }

}
