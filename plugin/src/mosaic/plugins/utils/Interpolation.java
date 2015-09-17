package mosaic.plugins.utils;

import mosaic.utils.math.Matlab;

/**
 * Interpolation functionality with some support for Matlab style 'imresize' command (does not support antialiasing)
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 *
 */
public class Interpolation {
    /**
     * Interpolation type: BILINEAR, BICUBIC, NEAREST
     */
    public static enum InterpolationType {
        BILINEAR, BICUBIC, NEAREST
    }
    
    /**
     * Interpolation mode:
     * NONE - Does not use any special adjustments if values outside source data are needed then
     *        closest border value is used.
     * SMART - Active only for BICUBIC interpolation, in other cases behaves like NONE. In case of bicubic
     *         if interpolated values are closed to border than bilinear interpolation is used to avoid
     *         artificial replication of border values
     * MATLAB - Generates output image which has same values as Matlab's imresize command. This is achieved
     *          by different mapping of  destination image onto source image.
     *          Note: antialiasing is not supported! When image is shrink it gives
     *          same results as imresize(.... ,'antialiasing'. 0)
     */
    public static enum InterpolationMode {
        NONE, SMART, MATLAB
    }
    
    /**
     * Cubic kernel taken from:
     * Robert G. Keys
     * "Cubic Convolution Interpolation for Digital Image Processing"
     * IEEE Transactions on Acoustics, Speech, and Signal Processing, 
     * Vol. ASSP-29, No. 6, December 1981
     *  
     *  See function number (15)
     */
    public static final double cubicKernel(double x) {
        double u = 0.0;
        
        double s = Math.abs(x);
        double s2 = Math.pow(x, 2);
        double s3 = s * s2;
        
        if (s <= 1) u = (1.5 * s3 - 2.5 * s2 + 1);
        else if (s <= 2) u = (-0.5 * s3 + 2.5 * s2 - 4  * s + 2);
        
        return u;
    }
    
    /**
     * Triangle kernel in range (-1, 1)
     */
    public static final double triangleKernel(double x) {
        double u = 0.0;

        if ((x > -1 ) && (x < 0)) u = x + 1;
        else if (x >= 0 && x < 1) u = 1 - x;
        
        return u;
    }
    
    /**
     * Nearest neighbour kernel in range [-0.5, 0.5)
     */
    public static final double nearestNeighbourKernel(double x) {
        double u = 0.0;
       
        if (x >= -0.5 && x < 0.5) u = 1;
        
        return u;
    }

    /**
     * Nearest neighbor interpolation of destination point aX,aY
     * @param aX - x value of point to be interpolated
     * @param aY - y value of point to be interpolated
     * @param aSrc - source data
     * @return
     */
    static private double nearestNeighbourInterpolation(double aX, double aY, double[][] aSrc) {
        int srcWidth = aSrc.length;
        int srcHeight = aSrc[0].length;
        
        int x = (int) Math.floor(aX); 
        int y = (int) Math.floor(aY);

        double value = 0;
        for (int j = 0; j <= 1; j++) {
                int v = y + j;
                double p = 0;
                for (int i = 0; i <= 1; i++) {
                        int u = x + i;
                        int us = u;
                        int vs = v;
                        if (u < 0) us = 0;
                        if (v < 0) vs = 0;
                        if (u >= srcWidth) us = srcWidth-1;
                        if (v >= srcHeight) vs = srcHeight -1;
                        p += aSrc[us][vs] * nearestNeighbourKernel(aX - u);
                }
                value += p * nearestNeighbourKernel(aY - v);
        }

        return value;
    }
    
    /**
     * Bilinear interpolation of destination point aX,aY
     * @param aX - x value of point to be interpolated
     * @param aY - y value of point to be interpolated
     * @param aSrc - source data
     * @return
     */
    static private double bilinearInterpolation(double aX, double aY, double[][] aSrc) {
        int srcWidth = aSrc.length;
        int srcHeight = aSrc[0].length;
        
        int x = (int) Math.floor(aX); 
        int y = (int) Math.floor(aY);

        double value = 0;
        for (int j = 0; j <= 1; j++) {
            int v = y + j;
            double p = 0;
            for (int i = 0; i <= 1; i++) {
                int u = x + i;
                int us = u;
                int vs = v;
                if (u < 0) us = 0;
                if (v < 0) vs = 0;
                if (u >= srcWidth) us = srcWidth-1;
                if (v >= srcHeight) vs = srcHeight -1;
                p += aSrc[us][vs] * triangleKernel(aX - u);
            }
            value += p * triangleKernel(aY - v);
        }

        return value;
    }
    
    /**
     * Bicubic interpolation of destination point aX,aY
     * @param aX - x value of point to be interpolated
     * @param aY - y value of point to be interpolated
     * @param aSrc - source data
     * @return
     */
    static private double bicubicInterpolation(double aX, double aY, double[][] aSrc, InterpolationMode aMode) {
        int srcWidth = aSrc.length;
        int srcHeight = aSrc[0].length;
        
        int x = (int) Math.floor(aX); 
        int y = (int) Math.floor(aY);

        if (aMode == InterpolationMode.SMART && 
                (x <= 0 || x >= srcWidth - 2 || y <= 0 || y >= srcHeight - 2)) {
            // In case when there is not enough points on left/right top/bottom
            // choose linear interpolation.
            return bilinearInterpolation(aX, aY, aSrc);
        }
        
        double value = 0;
        for (int j = 0; j <= 3; j++) {
                int v = y - 1 + j;
                double p = 0;
                for (int i = 0; i <= 3; i++) {
                        int u = x - 1 + i;
                        int us = u;
                        int vs = v;
                        // If index is out of range choose value from nearest border point
                        if (u < 0) us = 0;
                        if (v < 0) vs = 0;
                        if (u >= srcWidth) us = srcWidth - 1;
                        if (v >= srcHeight) vs = srcHeight - 1;

                        p += aSrc[us][vs] * cubicKernel(aX - u);
                }
                value += p * cubicKernel(aY - v);
        }

        return value;
    }

    /**
     * Generates coordinates (or mapping) of destination points onto srouce points
     * @param aDstLen - length of destination dimension
     * @param aSrcLen - length of source dimension 
     * @param aMode - only affected for MATLAB mode, generates compatibile with Matlab points.
     * @return
     */
    private static double[] generateCoordinates(int aDstLen, int aSrcLen, InterpolationMode aMode) {
        double mapping[];
        double startIdx = 1;
        double stopIdx = aSrcLen;
        
        if (aMode == InterpolationMode.MATLAB) {
            // Matlab is using "strange" calculation of mapping dst points into src points
            double scale = ((double) aDstLen / aSrcLen);
            double constant = 0.5 * (1 - 1 / scale);
            startIdx = 1 / scale + constant;
            stopIdx = (aDstLen) / scale + constant;            
        }
        
        if (aDstLen != 1) {
            mapping = Matlab.linspaceArray(startIdx, stopIdx, aDstLen);
        }
        else {
            // If len is equal to 1 choose middle of srclen
            mapping = new double[] { ((double) aSrcLen - 1) / 2 + 1 };
        }

        return mapping;
    }

    /**
     * Resize input image with to wanted width/height
     * @param aSrcImage - input image as 2D array (first dimension should be height)
     * @param aHeight - wanted height
     * @param aWidth - wanted width
     * @param aType - type 
     * @param aMode
     * @return
     */
    static public double[][] resize(double[][] aSrcImage, int aHeight, int aWidth, InterpolationType aType, InterpolationMode aMode) {
        // Create output image array
        double[][] resultImg = new double[aHeight][aWidth];

        int srcWidth = aSrcImage.length;
        int srcHeight = aSrcImage[0].length;
        
        // Generate mapping of destination image into src image
        double[] newY = generateCoordinates(aHeight, srcWidth, aMode);
        double[] newX = generateCoordinates(aWidth, srcHeight, aMode);
        
        for (int x = 0; x <= aWidth - 1; x++) {
            double ys = newX[x] - 1;
            for (int y = 0; y <= aHeight - 1; y++) {
                double xs = newY[y] - 1;
                switch(aType) {
                    case BILINEAR:
                        resultImg[y][x] = bilinearInterpolation(xs, ys, aSrcImage);
                        break;
                    case NEAREST:
                        resultImg[y][x] = nearestNeighbourInterpolation(xs, ys, aSrcImage);
                        break;
                    case BICUBIC:
                        // Intentionally no 'break' here.
                    default:
                        resultImg[y][x] = bicubicInterpolation(xs, ys, aSrcImage, aMode);
                        break;
                }
            }
        }

        return resultImg;
    }
}
