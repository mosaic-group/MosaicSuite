package mosaic.variationalCurvatureFilters;

public interface SplitFilterKernel {
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
    void filterKernel(float[] aMiddle, float[] aSides, float[] aDown, float[] aDownCorners, float[] aUp, float[] aUpCorners, boolean aShifted);
}