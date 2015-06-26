package mosaic.plugins.utils;

import ij.process.FloatProcessor;

public class ImgUtils {
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
                aNewImgArray[y][x] = pixels[xIdx + yIdx * w]/aNormalizationValue;
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
    
    /**
     * Converts ImageProcessor to 2D array with first dim X and second Y
     * If new image array is bigger than input image then additional pixels (right column(s) and
     * bottom row(s)) are padded with neighbors values.
     * All pixels are normalized by dividing them by provided normalization value (if 
     * this step is not needed 1.0 should be given).
     * 
     * @param aInputIp       Original image
     * @param aNewImgArray   Created 2D array to keep converted original image     
     * @param aNormalizationValue Maximum pixel value of original image -> converted one will be normalized [0..1]
     */
    static public void ImgToXY2Darray(final FloatProcessor aInputIp, float[][] aNewImgArray, float aNormalizationValue) {
        float[] pixels = (float[])aInputIp.getPixels();
        int w = aInputIp.getWidth();
        int h = aInputIp.getHeight();
        int arrayW = aNewImgArray.length;
        int arrayH = aNewImgArray[0].length;
        
        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= h) yIdx = h - 1;
                if (xIdx >= w) xIdx = w - 1;
                aNewImgArray[x][y] = pixels[xIdx + yIdx * w]/aNormalizationValue;
            }
        }
    }
    
    /**
     * Resizes 2D array to 2D array with different size
     * If output image array is bigger than input image then additional pixels (right column(s) and
     * bottom row(s)) are padded with neighbors values.
     * 
     * @param aInputImg      Original image array
     * @param aNewImgArray   Resized image array (must be created by user)     
     */
    static public void imgResize(float[][] aInputImg, float[][] aOutputImg) {
        int w = aInputImg[0].length;
        int h = aInputImg.length;
        int arrayW = aOutputImg[0].length;
        int arrayH = aOutputImg.length;
        
        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= h) yIdx = h - 1;
                if (xIdx >= w) xIdx = w - 1;
                aOutputImg[y][x] = aInputImg[yIdx][xIdx];
            }
        }
    }
    
    /**
     * Resizes 2D array to 2D array with different size
     * If output image array is bigger than input image then additional pixels (right column(s) and
     * bottom row(s)) are padded with neighbors values.
     * 
     * @param aInputImg      Original image array
     * @param aNewImgArray   Resized image array (must be created by user)     
     */
    static public void imgResize(double[][] aInputImg, double[][] aOutputImg) {
        int w = aInputImg[0].length;
        int h = aInputImg.length;
        int arrayW = aOutputImg[0].length;
        int arrayH = aOutputImg.length;
        
        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= h) yIdx = h - 1;
                if (xIdx >= w) xIdx = w - 1;
                aOutputImg[y][x] = aInputImg[yIdx][xIdx];
            }
        }
    }
    
    /**
     * Updates ImageProcessor image with provided 2D pixel array. All pixels are multiplied by
     * normalization value (if this step is not needed 1.0 should be provided)
     * If output image is smaller than pixel array then it is truncated.
     * 
     * @param aIp                  ImageProcessor to be updated
     * @param aImg                 2D array (first dim X, second Y)
     * @param aNormalizationValue  Normalization value.
     */
    static public void XY2DarrayToImg(final float[][] aImg, FloatProcessor aIp, float aNormalizationValue) {
        float[] pixels = (float[]) aIp.getPixels();
        int w = aIp.getWidth();
        int h = aIp.getHeight();

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                pixels[x + y * w] = aImg[x][y] * aNormalizationValue;
            }
        }
    }
    
    public static class MinMax<T> {
		T min;
		T max;

		MinMax(T aMin, T aMax) {
			min = aMin;
			max = aMax;
		}

		public T getMin() {
			return min;
		}

		public T getMax() {
			return max;
		}
    }
    
    static public MinMax<Float> findMinMax(final float[][] aImgArray) {
        final int arrayW = aImgArray[0].length;
        final int arrayH = aImgArray.length;
        
        // Find min and max value of image
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
            	final float pix = aImgArray[y][x];
            	if (pix < min) min = pix;
            	if (pix > max) max = pix;
            }
        }
        
        return new MinMax<Float>(min, max);
    }
    
    /**
     * Normalize values in array to 0..1 range
     * @param aImgArray
     */
    static public void normalize(final float[][] aImgArray) {
        final int arrayW = aImgArray[0].length;
        final int arrayH = aImgArray.length;
        
        // Find min and max value of image
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
            	final float pix = aImgArray[y][x];
            	if (pix < min) min = pix;
            	if (pix > max) max = pix;
            }
        }
        
        // Normalize with found values
		if (max != min) {
			for (int y = 0; y < arrayH; ++y) {
				for (int x = 0; x < arrayW; ++x) {
					aImgArray[y][x] = (aImgArray[y][x] - min) / (max - min);
				}
			}
		}
    }
    
    /**
     * Normalize values in array to 0..1 range
     * @param aImgArray
     */
    static public void normalize(final double[][] aImgArray) {
        final int arrayW = aImgArray[0].length;
        final int arrayH = aImgArray.length;
        
        // Find min and max value of image
        double min = Float.MAX_VALUE;
        double max = Float.MIN_VALUE;
        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                final double pix = aImgArray[y][x];
                if (pix < min) min = pix;
                if (pix > max) max = pix;
            }
        }
        
        // Normalize with found values
        if (max != min) {
            for (int y = 0; y < arrayH; ++y) {
                for (int x = 0; x < arrayW; ++x) {
                    aImgArray[y][x] = (aImgArray[y][x] - min) / (max - min);
                }
            }
        }
    }
    
    /**
     * Converts range of values in array computing for each element:  (elem * aMultiply + aShift)
     * @param aImgArray - array to be converted
     * @param aMultiply - multiplication factor
     * @param aShift - shift value
     */
    static public void convertRange(final float[][] aImgArray, float aMultiply, float aShift) {
        final int arrayW = aImgArray[0].length;
        final int arrayH = aImgArray.length;
        
        // Normalize with found values
		for (int y = 0; y < arrayH; ++y) {
			for (int x = 0; x < arrayW; ++x) {
				aImgArray[y][x] = aImgArray[y][x] * aMultiply + aShift;
			}
		}
    }
    
    
}

