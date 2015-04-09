package mosaic.plugins;


import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;


public class VariationalModelFilter implements PlugInFilter {
    private ImagePlus iInputImagePlus;
    private final int iFlags = DOES_ALL | DOES_STACKS | CONVERT_TO_FLOAT | FINAL_PROCESSING | PARALLELIZE_STACKS;
    private final String FILE_PREFIX = "Filtered_";
    
    int originalWidth;
    int originalHeight;
    int roundedWidth;
    int roundedHeight;
    int halfWidth; 
    int halfHeight;
    
    @Override
    public int setup(final String aArgs, final ImagePlus aImp) {
        // Filter expects image to work on...
        if (aImp == null) {
            IJ.noImage();
            return DONE;
        }
        
        // This plugin utilize DOES_STACKS flag so we do not need to care about 
        // source image (it can have many channels, slices...) but unfortunately we are working
        // on original image. That is why when initial setup is called copy of original ImagePlus
        // is made and after processing ("final") original image is reverted.
        if (aArgs.equals("final")) {
            // This part is done after all processing is done
            
            // Create new image with processed data
            ImagePlus result = aImp.duplicate();
            result.setTitle(FILE_PREFIX + aImp.getTitle());
            result.show();
            
            // Revert original image
            aImp.setStack(iInputImagePlus.getStack());
        } 
        else {
            // Called at the beginning
            
            // Save original image
            iInputImagePlus = aImp.duplicate();
        }
        
        return iFlags;
    }

    @Override
    public void run(ImageProcessor aIp) {
        // We work only on float images
        boolean isFloatImg = (aIp instanceof FloatProcessor);
        if (!isFloatImg) {
            throw new IllegalArgumentException("This type of image is not supported!");
        }
        
        filterImage(aIp);
    }

    /**
     * Run filter on given image.
     * @param aInputIp Input image (will be changed during processing)
     */
    private void filterImage(ImageProcessor aInputIp) {
       
        calculateDimensionsOfProcessedImage(aInputIp);

        // Prepare all arrays to store images 
        float[][] img = new float[roundedHeight][roundedWidth]; 
        float[][] WC = new float[halfHeight][halfWidth];
        float[][] WT = new float[halfHeight][halfWidth];
        float[][] BC = new float[halfHeight][halfWidth];
        float[][] BT = new float[halfHeight][halfWidth];
        
        final float maxValueOfPixel = (float) aInputIp.getMax();
        convertImageToDivisibleBy2AndNormalize(aInputIp, img, maxValueOfPixel);

        splitImage(img, WC, WT, BC, BT);
        
        final int noOfIterations = 10;
        runFilterGc(WC, WT, BC, BT, noOfIterations);
        
        mergeImage(aInputIp, WC, WT, BC, BT, maxValueOfPixel);
    }

    /**
     * Calculates dimensions used for processing image.
     * Algorithm needs x/y dimensions to be divisible by 2.
     * 
     * @param aInputIp Original image.
     */
    private void calculateDimensionsOfProcessedImage(ImageProcessor aInputIp) {
        originalWidth = aInputIp.getWidth();
        originalHeight = aInputIp.getHeight();
        roundedWidth = (int) (Math.ceil(originalWidth/2.0) * 2);
        roundedHeight = (int) (Math.ceil(originalHeight/2.0) * 2);
        halfWidth = roundedWidth / 2;
        halfHeight = roundedHeight / 2;
    }
    
    /**
     * Converts input image to divisible by 2 (in case if original image is not).
     * Additional pixels are filled with neighbors values.
     * 
     * @param aInputIp       Original image
     * @param aNewImgArray   Created matrix (with correct dimensions) to keep converted original image     
     * @param aMaxPixelValue Maximum pixel value of original image -> converted one will be normalized [0..1]
     */
    private void convertImageToDivisibleBy2AndNormalize(ImageProcessor aInputIp, float[][] aNewImgArray, float aMaxPixelValue) {
        float[] pixels = (float[])aInputIp.getPixels();
    
        if (aMaxPixelValue < 1.0f) aMaxPixelValue = 1.0f;
        for (int x = 0; x < roundedWidth; ++x) {
            for (int y = 0; y < roundedHeight; ++y) {
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= originalHeight) yIdx = originalHeight - 1;
                if (xIdx >= originalWidth) xIdx = originalWidth - 1;
                aNewImgArray[y][x] = (float)pixels[xIdx + yIdx * originalWidth]/aMaxPixelValue;
    
            }
        }
    }

    /**
     * Splits input image into four sets containing every second pixel. 
     * BT WT
     * WC BC
     * Each one of 1/2 height x 1/2 width of converted image.
     * 
     * @param aConvertedImg Converted and normalized image.
     * @param WC            
     * @param WT
     * @param BC
     * @param BT
     */
    private void splitImage(float[][] aConvertedImg, float[][] WC, float[][] WT, float[][] BC, float[][] BT) {
        for (int x = 0; x < roundedWidth; ++x) {
            for (int y = 0; y < roundedHeight; ++y) {
                     if (x % 2 == 0 && y % 2 == 0) BT[y/2][x/2] = aConvertedImg[y][x];
                else if (x % 2 == 1 && y % 2 == 0) WT[y/2][x/2] = aConvertedImg[y][x];
                else if (x % 2 == 0 && y % 2 == 1) WC[y/2][x/2] = aConvertedImg[y][x];
                else if (x % 2 == 1 && y % 2 == 1) BC[y/2][x/2] = aConvertedImg[y][x];
            }
        }
    }

    /**
     * Merges WC/WT/BC/BT into one image
     * @param aIp
     * @param WC
     * @param WT
     * @param BC
     * @param BT
     * @param aMaxPixelValue
     */
    private void mergeImage(ImageProcessor aIp, float[][] WC, float[][] WT, float[][] BC, float[][] BT, float aMaxPixelValue) {
        float[] pixels = (float[])aIp.getPixels();
        
        for (int x = 0; x < originalWidth; ++x) {
            for (int y = 0; y < originalHeight; ++y) {
                     if (x % 2 == 0 && y % 2 == 0) pixels[x + y * originalWidth] = BT[y/2][x/2] * aMaxPixelValue;
                else if (x % 2 == 1 && y % 2 == 0) pixels[x + y * originalWidth] = WT[y/2][x/2] * aMaxPixelValue;
                else if (x % 2 == 0 && y % 2 == 1) pixels[x + y * originalWidth] = WC[y/2][x/2] * aMaxPixelValue;
                else if (x % 2 == 1 && y % 2 == 1) pixels[x + y * originalWidth] = BC[y/2][x/2] * aMaxPixelValue;
            }
        }
    }

    /**
     *  /      0    1    2    3     4    5     - Original image indices
     *    /       0         1         2    .   - Indices of split parts (BT, WT ...)
     *        -----------------------------.
     *  0 0  | BT   WT | BT   WT | BT   WT .
     *  1    | WC   BC | WC   BC | WC   BC .
     *       |---------+---------+-------- .
     *  2 1  | BT   WT | BT   WT | BT   WT .
     *  3    | WC   BC | WC   BC | WC   BC .
     *       |---------+---------+-------- .
     *  4 2  | BT   WT | BT   WT | BT   WT .
     *  5    | WC   BC | WC   BC | WC   BC .
     *       ...............................      
     * 
     * @param WC             2D array containing part of image
     * @param WT             2D array containing part of image
     * @param BC             2D array containing part of image
     * @param BT             2D array containing part of image
     * @param noOfIterations Number of iteration to run filter
     */
    private void runFilterGc(float[][] WC, float[][] WT, float[][] BC, float[][] BT, final int noOfIterations) {
        for (int i = 0; i < noOfIterations; ++i) {
            
            /*          0         1         2    .
             *      -----------------------------.
             *  0  | BT   WT | BT   WT | BT   WT .
             *     | WC   BC | WC   BC | WC   BC .
             *     |---------+---------+-------- .
             *     ...............................
             * Shifted:              ^- true
             *                 ^------- false      
             *      
             * aMiddle aSides aDown aDownCorners aUp aUpCorners aShifted
             */
            
//            for (int y = 1; y < halfHeight - 1; ++y) {
//                runGcFilterOnSplittedPart(BT[y], WT[y], WC[y], BC[y], WC[y-1], BC[y-1], false);
//            }
//            for (int y = 1; y < halfHeight - 1; ++y) {
//                runGcFilterOnSplittedPart(WC[y], BC[y], BT[y+1], WT[y+1], BT[y], WT[y], false);
//            }
//            for (int y = 1; y < halfHeight - 1; ++y) {
//                runGcFilterOnSplittedPart(WT[y], BT[y], BC[y], WC[y], BC[y-1], WC[y-1], true);
//            }
//            for (int y = 1; y < halfHeight - 1; ++y) {
//                runGcFilterOnSplittedPart(BC[y], WC[y], WT[y+1], BT[y+1], WT[y], BT[y], true);
//            }
            
            
            
      for (int y = 1; y < halfHeight - 1; ++y) {
          runMcFilterOnSplittedPart(BT[y], WT[y], WC[y], BC[y], WC[y-1], BC[y-1], false);
      }
      for (int y = 1; y < halfHeight - 1; ++y) {
          runMcFilterOnSplittedPart(WC[y], BC[y], BT[y+1], WT[y+1], BT[y], WT[y], false);
      }
      for (int y = 1; y < halfHeight - 1; ++y) {
          runMcFilterOnSplittedPart(WT[y], BT[y], BC[y], WC[y], BC[y-1], WC[y-1], true);
      }
      for (int y = 1; y < halfHeight - 1; ++y) {
          runMcFilterOnSplittedPart(BC[y], WC[y], WT[y+1], BT[y+1], WT[y], BT[y], true);
      }
            
            
        }
    }

    /**
     * Runs filter on extracted part of original image (after splitting). 
     * All parameters are given as a lines of original BT/WC/WT/BC parts.
     *           
     * aShifted == false
     * ------------------------------------
     *     n-1      |         n
     * ------------------------------------
     * aUpCorners   | aUp     aUpCorners
     * aSides       | aMiddle aSides
     * aDownCorners | aDown   aDownCorners
     * 
     * aShifted == true
     * ------------------------------------
     *         n            |    n+1
     * ------------------------------------
     * aUpCorners   aUp     | aUpCorners
     * aSides       aMiddle | aSides
     * aDownCorners aDown   | aDownCorners
     * 
     * @param aMiddle 
     * @param aSides
     * @param aDown
     * @param aDownCorners
     * @param aUp
     * @param aUpCorners
     * @param aShifted
     */
    private void runGcFilterOnSplittedPart(float[] aMiddle, float[] aSides, float[] aDown, float[] aDownCorners, float[] aUp, float[] aUpCorners, boolean aShifted) {
        final int startIdx = 2 + (aShifted ? 1 : 0); 
        
        int rightIdx = (startIdx + 1) / 2;
        int leftIdx = rightIdx - 1;
        int middleIdx = startIdx / 2;
        
        for (int j = 1; j < halfWidth - 1; ++j) {
            /*
             * Naming:
             * 
             *       lu | u | ru
             *       ---+---+---
             *       l  | m |  r
             *       ---+---+---
             *       ld | d | rd
             */
            final float m2 = 2 * aMiddle[middleIdx];
            final float m3 = 3 * aMiddle[middleIdx];
            final float u = aUp[middleIdx];
            final float d = aDown[middleIdx];
            final float l = aSides[leftIdx];
            final float r = aSides[rightIdx];
            final float ld = aDownCorners[leftIdx];
            final float rd = aDownCorners[rightIdx];
            final float lu = aUpCorners[leftIdx];
            final float ru = aUpCorners[rightIdx];
            
            // Calculate d0..d7
            float d0 = (u+d)-m2;
            float d1 = (l+r)-m2;
            float d2 = (lu+rd)-m2;
            float d3 = (ru+ld)-m2;
       
            float d4 = (lu+u+l)-m3;
            float d5 = (ru+u+r)-m3;
            float d6 = (l+ld+d)-m3;
            float d7 = (r+d+rd)-m3;

            // And find minimum (absolute) change
            float da; 
        
            float d0a = Math.abs(d0);
            da = Math.abs(d1);
            if (da < d0a) {d0 = d1; d0a = da;}
            da = Math.abs(d2);
            if (da < d0a) {d0 = d2; d0a = da;}
            da = Math.abs(d3);
            if (da < d0a) {d0 = d3;}
           
            float d4a = Math.abs(d4);
            da = Math.abs(d5);
            if (da < d4a) {d4 = d5; d4a = da;}
            da = Math.abs(d6);
            if (da < d4a) {d4 = d6; d4a = da;}
            da = Math.abs(d7);
            if (da < d4a) {d4 = d7;}
           
            d0 /= 2;
            d4 /= 3;
           
            if (Math.abs(d4) < Math.abs(d0)) d0 = d4;
   
            // Finally update middle pixel with minimum change
            aMiddle[middleIdx] += d0;
            
            // Go to next pixel
            ++rightIdx;
            ++leftIdx;
            ++middleIdx;
        }
    }
    
    private void runMcFilterOnSplittedPart(float[] aMiddle, float[] aSides, float[] aDown, float[] aDownCorners, float[] aUp, float[] aUpCorners, boolean aShifted) {
        final int startIdx = 2 + (aShifted ? 1 : 0); 
        
        int rightIdx = (startIdx + 1) / 2;
        int leftIdx = rightIdx - 1;
        int middleIdx = startIdx / 2;
        
        for (int j = 1; j < halfWidth - 1; ++j) {
            /*
             * Naming:
             * 
             *       lu | u | ru
             *       ---+---+---
             *       l  | m |  r
             *       ---+---+---
             *       ld | d | rd
             */
            final float m8 = aMiddle[middleIdx] * 8;
            final float u = aUp[middleIdx];
            final float d = aDown[middleIdx];
            final float l = aSides[leftIdx];
            final float r = aSides[rightIdx];
            final float ld = aDownCorners[leftIdx];
            final float rd = aDownCorners[rightIdx];
            final float lu = aUpCorners[leftIdx];
            final float ru = aUpCorners[rightIdx];
            
            // Calculate d0..d3

            float common1 = (u+d) * 2.5f - m8;
            float d0 = common1 + r * 5.0f - ru - rd;
            float d1 = common1 + l * 5.0f - lu - ld;
            
            float common2 = (l+r) * 2.5f - m8;
            float d2 = common2 + u * 5.0f - lu - ru;
            float d3 = common2 + d * 5.0f - ld - rd;
       
            // And find minimal (absolute) change
            float d0a = Math.abs(d0);
            float da = Math.abs(d1);
            if (da < d0a) {d0 = d1; d0a = da;}
            da = Math.abs(d2);
            if (da < d0a) {d0 = d2; d0a = da;}
            da = Math.abs(d3);
            if (da < d0a) {d0 = d3;}
            
            d0 /= 8.0f;
            
            // Finally update middle pixel with minimum change
            aMiddle[middleIdx] += d0;
            
            // Go to next pixel
            ++rightIdx;
            ++leftIdx;
            ++middleIdx;
        }
    }
    
}
