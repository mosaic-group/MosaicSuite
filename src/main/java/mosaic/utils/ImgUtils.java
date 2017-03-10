package mosaic.utils;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.filter.EDM;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import mosaic.utils.ArrayOps.MinMax;
import mosaic.utils.math.Matrix;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

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
     * Creates 3D [z][x][y] binary (boolean) array from provided ImagePlus
     * @param aImage input image
     * @return generated array with false for values == 0 and true otherwise
     */
    public static boolean[][][] imgToZXYbinaryArray(ImagePlus aInputImage) {
        int ni = aInputImage.getWidth();
        int nj = aInputImage.getHeight();
        int nz = aInputImage.getNSlices();
        
        final boolean[][][] cellmask = new boolean[nz][ni][nj];
        for (int z = 0; z < nz; z++) {
            aInputImage.setSlice(z + 1);
            ImageProcessor ip = aInputImage.getProcessor();
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    cellmask[z][i][j] = ip.getPixelValue(i, j) != 0;
                }
            }
        }
        
        return cellmask;
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
     * Creates ImagePlus (FloatProcessor) from provided array
     * @param aImgArray 3D array with image data [z][x][y]
     * @param aTitle tile of generated image
     * @return the generated ImagePlus
     */
    public static ImagePlus ZXYarrayToImg(double[][][] aImgArray, String aTitle) {
        int iWidth = aImgArray[0].length;
        int iHeigth = aImgArray[0][0].length;
        int iDepth = aImgArray.length;
        final ImageStack stack = new ImageStack(iWidth, iHeigth);
        
        for (int z = 0; z < iDepth; z++) {
            stack.addSlice("", new FloatProcessor(ConvertArray.toFloat(aImgArray[z])));
        }
        
        final ImagePlus img = new ImagePlus();
        img.setStack(stack);
        img.changes = false;
        img.setTitle(aTitle);
        
        return img;
    }
    
    /**
     * Creates ImagePlus (FloatProcessor) from provided array
     * @param aImgArray 3D array with image data [z][x][y]
     * @param aTitle tile of generated image
     * @return the generated ImagePlus
     */
    public static ImagePlus ZXYarrayToImg(short[][][] aImgArray, String aTitle) {
        ImagePlus img = ZXYarrayToImg(ConvertArray.toDouble(aImgArray), aTitle);
        img.setTitle(aTitle);
        
        return img;
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

    public static enum OutputType {ORIGINAL, RGB, FLOAT}
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
    static public ImagePlus createNewEmptyImgPlus(final ImagePlus aOrigIp, String aTitle, double aXscale, double aYscale, OutputType aType) {
        final int nSlices = aOrigIp.getStackSize();
        final int w=aOrigIp.getWidth();
        final int h=aOrigIp.getHeight();

        ImagePlus copyIp = aOrigIp.createImagePlus();

        final int newWidth = (int)aXscale*w;
        final int newHeight = (int)aYscale*h;

        final ImageStack origStack = aOrigIp.getStack();
        final ImageStack copyStack = new ImageStack(newWidth, newHeight);
        for (int i = 1; i <= nSlices; i++) {
            ImageProcessor ip1 = origStack.getProcessor(i);
            final String label = origStack.getSliceLabel(i);
            ImageProcessor ip2;
            switch (aType) {
                case RGB: 
                    ip2 = new ColorProcessor(newWidth, newHeight);
                    break;
                case FLOAT:
                    ip2 = new FloatProcessor(newWidth, newHeight);
                    break;
                case ORIGINAL:
                    // Intentionally no 'break' here
                default:
                    ip2 = ip1.createProcessor(newWidth, newHeight);
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

        if (aOrigIp.isComposite() && !(aType == OutputType.RGB && copyIp.getStackSize() > 1)) {
            copyIp = new CompositeImage(copyIp, ((CompositeImage)aOrigIp).getMode());
            ((CompositeImage)copyIp).copyLuts(aOrigIp);
        }

        if (aOrigIp.isHyperStack()) {
            copyIp.setOpenAsHyperStack(true);
        }
        copyIp.changes = true;

        return copyIp;
    }

    /**
     * @param aImg - input image
     * @return new and empty image with same size/type as input image
     */
    public static <T extends RealType< T >> Img<T> createNewEmpty(Img<T> aImg) {
        int numDimensions = aImg.numDimensions();
        long[] dims = new long[numDimensions];
        aImg.dimensions(dims);
        return aImg.factory().create(dims, aImg.firstElement().copy());
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
                    float value = imp.getPixelValue(i, j);
                    if (value > max) {
                        max = value;
                    }
                    if (value < min) {
                        min = value;
                    }
                }
            }
        }
        
        return new MinMax<Double>(min, max);
    }
    
    /**
     * @return created ImagePlus from provided image containing only specified frame and channel
     */
    public static ImagePlus extractFrameAsImage(ImagePlus aImage, final int aFrame, final int aChannel, boolean aMakeCopy) {
        int ni = aImage.getWidth();
        int nj = aImage.getHeight();
        int nz = aImage.getNSlices();
        
        final ImageStack is = new ImageStack(ni, nj);
        for (int z = 1; z <= nz; z++) {
            aImage.setPosition(aChannel, z, aFrame);
            ImageProcessor ip = (aMakeCopy) ? aImage.getProcessor().duplicate() : aImage.getProcessor();
            is.addSlice("", ip);
        }
        
        return new ImagePlus(aImage.getTitle(), is);
    }

    /**
     * @param aImage input image
     * @return the folder where the image is stored or null if impossible to detect
     */
    public static String getImageDirectory(ImagePlus aImage) {
        if (aImage == null) {
            return null;
        }
    
        if (aImage.getOriginalFileInfo() == null || aImage.getOriginalFileInfo().directory == "") {
            return null;
        }
        return aImage.getOriginalFileInfo().directory;
    }
    
    /**
     * @param aImage input image
     * @return Absolute path to image or null if impossible to detect.
     */
    public static String getImageAbsolutePath(ImagePlus aImage) {
        if (aImage != null) {
            FileInfo fi = aImage.getOriginalFileInfo();
            if (fi != null) {
                if (fi.url != null && !fi.url.equals(""))
                    return fi.url;
                else if (fi.directory != null && fi.fileName != null)
                    return fi.directory + fi.fileName;
            }
        }
        return null;
    }
    
    enum Type { GRAY8, GRAY16, GRAY32, COLOR_256, COLOR_RGB }
    /**
     * @return String with information of provided image (dimensions/channels/frames)
     */
    public static String getImageInfo(ImagePlus aImage) {
        
        return "Image title: [" + aImage.getTitle() + "]" +
               " Path: [" + getImageAbsolutePath(aImage) + "] " +
               " Type: " + Type.values()[aImage.getType()] +
               " BitDepth: " + aImage.getBitDepth() + 
               " Dims(x/y/z): "+ aImage.getWidth() + "/" + aImage.getHeight() + "/" + aImage.getNSlices() + 
               " NumOfFrames: " + aImage.getNFrames() + 
               " NumOfChannels: " + aImage.getNChannels();
    }
    
    /**
     * @return String with information of provided image (dimensions/type/bitdepth)
     */
    public static <T extends RealType< T >> String getImageInfo(Img<T> img1) {
        String str = "Type: " + img1.firstElement().getClass().getName();
        str += " BitDepth: " + img1.firstElement().getBitsPerPixel();
        str += " Dims:";
        int numDimensions = img1.numDimensions();
        for (int i = 0; i < numDimensions; ++i)
            str += img1.dimension(i) + ((i == numDimensions - 1) ? "" : "/");
        return str;
    }
    
    /**
     * Runs distance transform on provided image (this image will be changed)
     * @param aImage input image
     * @return input image transformed
     */
    public static ImagePlus runDistanceTransform(ImagePlus aImage) {
        boolean tempBlackBackground = ij.Prefs.blackBackground;
        ij.Prefs.blackBackground = true;
        final EDM filtEDM = new EDM();
        filtEDM.setup("Exact Euclidean Distance Transform (3D)", aImage);
        filtEDM.run(aImage.getProcessor());
        ij.Prefs.blackBackground = tempBlackBackground;
        
        return aImage;
    }
    
    /**
     * Remove holes from binarized 8-bit image (background = 0, object = 255).
     * @param aImage input image
     * @return input image transformed
     */
    public static ImagePlus removeHoles(ImagePlus aImage) {
        IJ.run(aImage, "Invert", "stack");
        
        // "Fill Holes" is using Prefs.blackBackground global setting. We need false here.
        boolean tempBlackbackground = ij.Prefs.blackBackground;
        ij.Prefs.blackBackground = false;
        IJ.run(aImage, "Fill Holes", "stack");
        ij.Prefs.blackBackground = tempBlackbackground;

        IJ.run(aImage, "Invert", "stack");
        return aImage;
    }
    
    /**
     * Converts image to binary (it is converted to 8-bit image with background = 0 and object = 255 values)
     * @param aImage input image
     * @return input image transformed
     */
    public static ImagePlus binarizeImage(ImagePlus aImage) {
        new ImageConverter(aImage).convertToGray8();
        byte[] pixels = (byte[]) aImage.getProcessor().getPixels();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (pixels[i] == 0) ? (byte) 0 : (byte) 255;
        }
        return aImage;
    }
    
    /**
     * @param aImageMatrix matrix with image
     * @param aTitle title of output image
     * @return ImagePlus of Float type
     */
    public static ImagePlus matrixToImage(Matrix aImageMatrix, String aTitle) {
        final double[][] result = aImageMatrix.getArrayYX();
        FloatProcessor fp = new FloatProcessor(result[0].length, result.length);
        ImgUtils.YX2DarrayToImg(result, fp, 1.0);
        return new ImagePlus(aTitle, fp);
    }
    
    /**
     * Converts *current* ImageProcessor to Matrix (converting it to FloatProcessor first).
     * @param aImage image to be converted to matrix
     * @return output Matrix
     */
    public static Matrix imageToMatrix(ImagePlus aIamge) {
        FloatProcessor fp = aIamge.getProcessor().convertToFloatProcessor();
        final double[][] img = new double[aIamge.getHeight()][aIamge.getWidth()];
        ImgUtils.ImgToYX2Darray(fp, img, 1.0);
        return new Matrix(img);
    }
    
    
    /**
     * Pads (extends) input image processor in each direction by aPadSize pixels. Values of those pixels 
     * are the closes pixels in original image. 
     * @param aIp
     * @param aPadSize
     * @return pad ImageProcessor of same type as input.
     */
    public static ImageProcessor padImageProcessor(ImageProcessor aIp, int aPadSize) {
        final int width = aIp.getWidth();
        final int height = aIp.getHeight();
        final int newWidth = width + 2 * aPadSize;
        final int newHeight = height + 2 * aPadSize;
        
        final ImageProcessor paddedIp = aIp.createProcessor(newWidth, newHeight);
        
        for (int x = 0; x < newWidth; ++x) {
            for (int y = 0; y < newHeight; ++y) {
                int xc = x - aPadSize;
                int yc = y - aPadSize;
                if (xc < 0) xc = 0; if (xc >= width) xc = width - 1;
                if (yc < 0) yc = 0; if (yc >= height) yc = height - 1;
                
                paddedIp.set(x, y, aIp.get(xc, yc));
            }
        }

        return paddedIp;
    }

    /**
     * Crops (shrinks) input image processor by aCropSize pixels from each side of image. 
     * @param aIp
     * @param aCropSize
     * @return pad ImageProcessor of same type as input.
     */
    public static ImageProcessor cropImageProcessor(ImageProcessor aIp, int aCropSize) {
        final int width = aIp.getWidth();
        final int height = aIp.getHeight();
        final int newWidth = width - 2 * aCropSize;
        final int newHeight = height - 2 * aCropSize;
        
        final ImageProcessor cropIp = aIp.createProcessor(newWidth, newHeight);
        
        for (int x = 0; x < newWidth; ++x) {
            for (int y = 0; y < newHeight; ++y) {
                cropIp.set(x, y, aIp.get(x + aCropSize, y + aCropSize));
            }
        }

        return cropIp;
    }
}

