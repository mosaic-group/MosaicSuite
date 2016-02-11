package mosaic.utils;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mosaic.utils.ArrayOps.MinMax;

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
        final float[] pixels = (float[])aInputIp.getPixels();
        final int w = aInputIp.getWidth();
        final int h = aInputIp.getHeight();
        final int arrayW = aNewImgArray[0].length;
        final int arrayH = aNewImgArray.length;

        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= h) {
                    yIdx = h - 1;
                }
                if (xIdx >= w) {
                    xIdx = w - 1;
                }
                aNewImgArray[y][x] = pixels[xIdx + yIdx * w]/aNormalizationValue;
            }
        }
    }

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
    static public void ImgToYX2Darray(final FloatProcessor aInputIp, double[][] aNewImgArray, double aNormalizationValue) {
        final float[] pixels = (float[])aInputIp.getPixels();
        final int w = aInputIp.getWidth();
        final int h = aInputIp.getHeight();
        final int arrayW = aNewImgArray[0].length;
        final int arrayH = aNewImgArray.length;

        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= h) {
                    yIdx = h - 1;
                }
                if (xIdx >= w) {
                    xIdx = w - 1;
                }
                aNewImgArray[y][x] = pixels[xIdx + yIdx * w]/aNormalizationValue;
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
    static void ImgToXY2Darray(final FloatProcessor aInputIp, float[][] aNewImgArray, float aNormalizationValue) {
        final float[] pixels = (float[])aInputIp.getPixels();
        final int w = aInputIp.getWidth();
        final int h = aInputIp.getHeight();
        final int arrayW = aNewImgArray.length;
        final int arrayH = aNewImgArray[0].length;
    
        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= h) {
                    yIdx = h - 1;
                }
                if (xIdx >= w) {
                    xIdx = w - 1;
                }
                aNewImgArray[x][y] = pixels[xIdx + yIdx * w]/aNormalizationValue;
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
        final float[] pixels = (float[]) aIp.getPixels();
        final int w = aIp.getWidth();
        final int h = aIp.getHeight();

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                pixels[x + y * w] = aImg[y][x] * aNormalizationValue;
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
    static public void YX2DarrayToImg(final double[][] aImg, FloatProcessor aIp, double aNormalizationValue) {
        final float[] pixels = (float[]) aIp.getPixels();
        final int w = aIp.getWidth();
        final int h = aIp.getHeight();

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                pixels[x + y * w] = (float)(aImg[y][x] * aNormalizationValue);
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
    static void XY2DarrayToImg(final float[][] aImg, FloatProcessor aIp, float aNormalizationValue) {
        final float[] pixels = (float[]) aIp.getPixels();
        final int w = aIp.getWidth();
        final int h = aIp.getHeight();

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                pixels[x + y * w] = aImg[x][y] * aNormalizationValue;
            }
        }
    }
    
    /**
     * Creates ImagePlus (FloatProcessor) from provided array
     * @param aImgArray 3D array with image data [z][x][y]
     * @param aTitle tile of generated image
     * @return the generated ImagePlus
     */
    public static ImagePlus ZXYarrayToImg(double[][][] aImgArray, String aTitle) {
        ImagePlus img = ZXYarrayToImg(aImgArray);
        img.setTitle(aTitle);
        
        return img;
    }
    
    /**
     * Creates ImagePlus (FloatProcessor) from provided array
     * @param aImgArray 3D array with image data [z][x][y]
     * @return the generated ImagePlus
     */
    public static ImagePlus ZXYarrayToImg(double[][][] aImgArray) {
        int iWidth = aImgArray[0].length;
        int iHeigth = aImgArray[0][0].length;
        int iDepth = aImgArray.length;
        final ImageStack stack = new ImageStack(iWidth, iHeigth);
    
        for (int z = 0; z < iDepth; z++) {
            final float[][] pixels = new float[iWidth][iHeigth];
            for (int i = 0; i < iWidth; i++) {
                for (int j = 0; j < iHeigth; j++) {
                    pixels[i][j] = (float) aImgArray[z][i][j];
                }
            }
            stack.addSlice("", new FloatProcessor(pixels));
        }
    
        final ImagePlus img = new ImagePlus();
        img.setStack(stack);
        img.changes = false;
        
        return img;
    }

    /**
     * Creates 3D [z][x][y] array from provided ImagePlus
     * @param aImage input image
     * @return generated array
     */
    public static double[][][] ImgToZXYarray(ImagePlus aImage) {
        int ni = aImage.getWidth();
        int nj = aImage.getHeight();
        int nz = aImage.getNSlices();
        double[][][] imgArray = new double[nz][ni][nj];
        for (int z = 0; z < nz; z++) {
            aImage.setSlice(z + 1);
            ImageProcessor ip = aImage.getProcessor();
            
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    imgArray[z][i][j] = ip.getPixelValue(i, j);
                }
            }
        }
        
        return imgArray;
    }
    
    /**
     * Resizes 2D array to 2D array with different size
     * If output image array is bigger than input image then additional pixels (right column(s) and
     * bottom row(s)) are padded with neighbors values.
     *
     * @param aInputImg      Original image array
     * @param aNewImgArray   Resized image array (must be created by user)
     */
    static void imgResize(float[][] aInputImg, float[][] aOutputImg) {
        final int w = aInputImg[0].length;
        final int h = aInputImg.length;
        final int arrayW = aOutputImg[0].length;
        final int arrayH = aOutputImg.length;

        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= h) {
                    yIdx = h - 1;
                }
                if (xIdx >= w) {
                    xIdx = w - 1;
                }
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
        final int w = aInputImg[0].length;
        final int h = aInputImg.length;
        final int arrayW = aOutputImg[0].length;
        final int arrayH = aOutputImg.length;

        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= h) {
                    yIdx = h - 1;
                }
                if (xIdx >= w) {
                    xIdx = w - 1;
                }
                aOutputImg[y][x] = aInputImg[yIdx][xIdx];
            }
        }
    }

    /**
     * Generates new empty (in terms of image data) ImagePlus basing on original provided one. Generated one, has all properties
     * of input one - like same structure (noOfSlices/dimensions etc.), same calibration, composite, hyperstack settings.
     * @param aOrigIp - original input image
     * @param aTitle - title for newly generated image
     * @param aXscale - scale for x dim
     * @param aYscale - scale for y dim
     * @param convertToRgb - should output ImagePlus be RGB regardless of input?
     * @return
     */
    static public ImagePlus createNewEmptyImgPlus(final ImagePlus aOrigIp, String aTitle, double aXscale, double aYscale, boolean convertToRgb) {
        final int nSlices = aOrigIp.getStackSize();
        final int w=aOrigIp.getWidth();
        final int h=aOrigIp.getHeight();

        ImagePlus copyIp = aOrigIp.createImagePlus();

        final int newWidth = (int)aXscale*w;
        final int newHeight = (int)aYscale*h;

        final ImageStack origStack = aOrigIp.getStack();
        final ImageStack copyStack = new ImageStack(newWidth, newHeight);
        ImageProcessor ip1, ip2;
        for (int i = 1; i <= nSlices; i++) {
            ip1 = origStack.getProcessor(i);
            final String label = origStack.getSliceLabel(i);
            if (!convertToRgb) {
                ip2 = ip1.createProcessor(newWidth, newHeight);
            }
            else {
                ip2 = new ColorProcessor(newWidth, newHeight);
            }
            if (ip2 != null) {
                copyStack.addSlice(label, ip2);
            }
        }

        copyIp.setStack(aTitle, copyStack);

        final Calibration cal = copyIp.getCalibration();
        if (cal.scaled()) {
            cal.pixelWidth *= 1.0 / aXscale;
            cal.pixelHeight *= 1.0 / aYscale;
        }

        final int[] dim = aOrigIp.getDimensions();
        copyIp.setDimensions(dim[2], dim[3], dim[4]);

        if (aOrigIp.isComposite()) {
            copyIp = new CompositeImage(copyIp, ((CompositeImage)aOrigIp).getMode());
            ((CompositeImage)copyIp).copyLuts(aOrigIp);
        }


        if (aOrigIp.isHyperStack()) {
            copyIp.setOpenAsHyperStack(true);
        }
        copyIp.changes = true;

        return copyIp;
    }

    public static MinMax<Double> findMinMax(ImagePlus img) {
        int ni = img.getWidth();
        int nj = img.getHeight();
        int nz = img.getNSlices();
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        
        for (int z = 0; z < nz; z++) {
            img.setSlice(z + 1);
            ImageProcessor imp = img.getProcessor();
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (imp.getPixel(i, j) > max) {
                        max = imp.getPixel(i, j);
                    }
                    if (imp.getPixel(i, j) < min) {
                        min = imp.getPixel(i, j);
                    }
                }
            }
        }
        
        return new MinMax<Double>(min, max);
    }
}

