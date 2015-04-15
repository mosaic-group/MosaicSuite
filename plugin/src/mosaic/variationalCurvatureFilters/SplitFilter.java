package mosaic.variationalCurvatureFilters;


public class SplitFilter implements CurvatureFilter {
    int originalWidth;
    int originalHeight;
    int roundedWidth;
    int roundedHeight;
    int halfWidth; 
    int halfHeight;
    
    SplitFilterKernel iSk;
    
    public SplitFilter(SplitFilterKernel aSk) {
        iSk = aSk;
    }
    
    @Override
    public void runFilter(float[][] aImg, int aNumOfIterations) {
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
        
        mergeImage(aImg, WC, WT, BC, BT);
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
     * Merges WC/WT/BC/BT into one image. Original image was splitted by {@link splitImage}
     * @param aIp
     * @param WC
     * @param WT
     * @param BC
     * @param BT
     * @param aMaxPixelValue
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
     * @param aFilter          Filter object
     */
    private void runFilter(float[][] WC, float[][] WT, float[][] BC, float[][] BT, final int aNumOfIterations) {
        for (int i = 0; i < aNumOfIterations; ++i) {
            
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
           
            for (int y = 1; y < halfHeight - 1; ++y) {
                iSk.filterKernel(BT[y], WT[y], WC[y], BC[y], WC[y-1], BC[y-1], false);
            }
            for (int y = 1; y < halfHeight - 1; ++y) {
                iSk.filterKernel(WC[y], BC[y], BT[y+1], WT[y+1], BT[y], WT[y], false);
            }
            for (int y = 1; y < halfHeight - 1; ++y) {
                iSk.filterKernel(WT[y], BT[y], BC[y], WC[y], BC[y-1], WC[y-1], true);
            }
            for (int y = 1; y < halfHeight - 1; ++y) {
                iSk.filterKernel(BC[y], WC[y], WT[y+1], BT[y+1], WT[y], BT[y], true);
            }
        }
    }

}
