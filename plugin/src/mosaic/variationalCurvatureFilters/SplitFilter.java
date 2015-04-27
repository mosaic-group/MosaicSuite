package mosaic.variationalCurvatureFilters;

/**
 * This class implements filter running in "split" mode. 
 * Image is divided into 4 subsets called WC, WT, BC, BT
 * It requires proper filter kernel (GC, MC, TV...)
 * @author Krzysztof Gonciarz
 */
public class SplitFilter implements CurvatureFilter {
    // Original image dimensions
    int originalWidth;
    int originalHeight;
    
    // Rounded up to divisible by 2 dimensions
    int roundedWidth;
    int roundedHeight;
    
    // rounded(Width/Height)/2
    int halfWidth; 
    int halfHeight;
    
    // Keeps provided split filter kernel
    FilterKernel iFk;
    
    public SplitFilter(FilterKernel aFk) {
        iFk = aFk;
    }
    
    /**
     * Runs a configured filter kernel on given image with given number
     * of iterations.
     */
    @Override
    public void runFilter(float[][] aImg, int aNumOfIterations) {
        // Calculate dimensions of processed image
        originalWidth = aImg[0].length;
        originalHeight = aImg.length;
        roundedWidth = (int) (Math.ceil(originalWidth/2.0) * 2);
        roundedHeight = (int) (Math.ceil(originalHeight/2.0) * 2);
        halfWidth = roundedWidth / 2;
        halfHeight = roundedHeight / 2;
        
        // Filter image
        float[][] WC = new float[halfHeight][halfWidth];
        float[][] WT = new float[halfHeight][halfWidth];
        float[][] BC = new float[halfHeight][halfWidth];
        float[][] BT = new float[halfHeight][halfWidth];
        splitImage(aImg, WC, WT, BC, BT);
        
        runFilter(WC, WT, BC, BT, aNumOfIterations);
        
        // Merge it back to original dimensions
        mergeImage(aImg, WC, WT, BC, BT);
    }
    
    @Override
    public void runFilter(float[][] aImg, int aNumOfIterations, Mask aMask) {
        throw new RuntimeException("Split filter cannot be used with mask.");
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
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= originalHeight) yIdx = originalHeight - 1;
                if (xIdx >= originalWidth) xIdx = originalWidth - 1;
                     if (x % 2 == 0 && y % 2 == 0) BT[y/2][x/2] = aConvertedImg[yIdx][xIdx];
                else if (x % 2 == 1 && y % 2 == 0) WT[y/2][x/2] = aConvertedImg[yIdx][xIdx];
                else if (x % 2 == 0 && y % 2 == 1) WC[y/2][x/2] = aConvertedImg[yIdx][xIdx];
                else if (x % 2 == 1 && y % 2 == 1) BC[y/2][x/2] = aConvertedImg[yIdx][xIdx];
            }
        }
    }

    /**
     * Merges WC/WT/BC/BT into one image. Original image was split by {@link splitImage}
     * @param aIp
     * @param WC
     * @param WT
     * @param BC
     * @param BT
     */
    private void mergeImage(float[][] aOutputImg, float[][] WC, float[][] WT, float[][] BC, float[][] BT) {
        for (int x = 0; x < originalWidth; ++x) {
            for (int y = 0; y < originalHeight; ++y) {
                     if (x % 2 == 0 && y % 2 == 0) aOutputImg[y][x] = BT[y/2][x/2];
                else if (x % 2 == 1 && y % 2 == 0) aOutputImg[y][x] = WT[y/2][x/2];
                else if (x % 2 == 0 && y % 2 == 1) aOutputImg[y][x] = WC[y/2][x/2];
                else if (x % 2 == 1 && y % 2 == 1) aOutputImg[y][x] = BC[y/2][x/2];
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
     * @param WC               2D array containing part of image
     * @param WT               2D array containing part of image
     * @param BC               2D array containing part of image
     * @param BT               2D array containing part of image
     * @param aNumOfIterations Number of iteration to run filter
     */
    private void runFilter(float[][] WC, float[][] WT, float[][] BC, float[][] BT, final int aNumOfIterations) {
        for (int i = 0; i < aNumOfIterations; ++i) {
            
            /* 
             * aMiddle aSides aDown aDownCorners aUp aUpCorners aShifted
             * 
             * 
             * For BC and WC index range is always 0..n-1 (n - length in Y dim)
             * For WT and BT index range is 1..n-1 if original img is divisible by 2
             * and 1..n-2 if original image is not divisible by 2. 
             */
            for (int y = 0; y < halfHeight - 1; ++y) {
                processOneImageLine(BC[y], WC[y], WT[y+1], BT[y+1], WT[y], BT[y], true);
            }
            for (int y = 1; y < originalHeight/2; ++y) {
                processOneImageLine(WT[y], BT[y], BC[y], WC[y], BC[y-1], WC[y-1], true);
            }
            for (int y = 0; y < halfHeight - 1; ++y) {
                processOneImageLine(WC[y], BC[y], BT[y+1], WT[y+1], BT[y], WT[y], false);
            }
            for (int y = 1; y < originalHeight/2; ++y) {
                processOneImageLine(BT[y], WT[y], WC[y], BC[y], WC[y-1], BC[y-1], false);
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
     */
    public void processOneImageLine(float[] aMiddle, float[] aSides, float[] aDown, float[] aDownCorners, float[] aUp, float[] aUpCorners, boolean aShifted) {
        /*
         * endIdx point to the last column in subsets (BC, BT..) that should be used. 
         * In case of aShifted==true it is always n-2 (n - length of array)
         * In case of aShifted==false we have two cases:
         * - originalWidth is even: then endIdx should be n-1
         * - originalWidth is odd:  then endIdx should be n-2
         */
        int endIdx = (originalWidth - (aShifted ? 1 : 0))/2 - 1;
        
        /*
         * startIdx and left/middle/right indices are calculated depending on aShifted flag.
         * This is done to correctly calculate indices of neighbors:
         *          0         1         2    .
         *      -----------------------------.
         *  0  | BT   WT | BT   WT | BT   WT .
         *     | WC   BC | WC   BC | WC   BC .
         *     |---------+---------+-------- .
         *     ...............................
         * Shifted:              ^- true
         *                 ^------- false  
         */
        final int startIdx = 2 + (aShifted ? -1 : 0);
        int rightIdx = (startIdx + 1) / 2;
        int leftIdx = rightIdx - 1;
        int middleIdx = startIdx / 2;
        
        for (int j = startIdx/2; j <= endIdx; ++j) {
            /*
             * Naming:
             * 
             *       lu | u | ru
             *       ---+---+---
             *       l  | m |  r
             *       ---+---+---
             *       ld | d | rd
             */
            final float m = aMiddle[middleIdx];
            final float u = aUp[middleIdx];
            final float d = aDown[middleIdx];
            final float l = aSides[leftIdx];
            final float r = aSides[rightIdx];
            final float ld = aDownCorners[leftIdx];
            final float rd = aDownCorners[rightIdx];
            final float lu = aUpCorners[leftIdx];
            final float ru = aUpCorners[rightIdx];
            
            aMiddle[middleIdx] += iFk.filterKernel(lu, u, ru, l, m, r, ld, d, rd);
            
            // Go to next pixel
            ++rightIdx;
            ++leftIdx;
            ++middleIdx;
        }
    }
}
