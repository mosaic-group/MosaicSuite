package mosaic.plugins.utils;

import ij.process.FloatProcessor;

public class ConvertImg {
    /**
     * Converts ImageProcessor to 2D array with first dim Y and second X
     * If new image array is bigger than input image then additional pixels (right column(s) and
     * bottom row(s)) are padded with neighbors values.
     * All pixels are normalized by dividing them by provided normalization value (if 
     * this step is not needed 1.0 should be given).
     * 
     * @param aInputIp       Original image
     * @param aNewImgArray   Created 2D array to keep converted original image     
     * @param aNormalizationValue Maximum pixel value of original image -> converted one will be normalized [0..1]
     */
    static public void ImgToYX2Darray(final FloatProcessor aInputIp, float[][] aNewImgArray, float aNormalizationValue) {
        float[] pixels = (float[])aInputIp.getPixels();
        int w = aInputIp.getWidth();
        int h = aInputIp.getHeight();
        int arrayW = aNewImgArray[0].length;
        int arrayH = aNewImgArray.length;
        
        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= h) yIdx = h - 1;
                if (xIdx >= w) xIdx = w - 1;
                aNewImgArray[y][x] = (float)pixels[xIdx + yIdx * w]/aNormalizationValue;
            }
        }
    }
    
    /**
     * Updates ImageProcessor image with provided 2D pixel array. All pixels are multiplied by
     * normalization value (if this step is not needed 1.0 should be provided)
     * If output image is smaller than pixel array then it is truncated.
     * 
     * @param aIp                  ImageProcessor to be updated
     * @param aImg                 2D array (first dim Y, second X)
     * @param aNormalizationValue  Normalization value.
     */
    static public void YX2DarrayToImg(final float[][] aImg, FloatProcessor aIp, float aNormalizationValue) {
        float[] pixels = (float[]) aIp.getPixels();
        int w = aIp.getWidth();
        int h = aIp.getHeight();

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                pixels[x + y * w] = aImg[y][x] * aNormalizationValue;
            }
        }
    }
}

