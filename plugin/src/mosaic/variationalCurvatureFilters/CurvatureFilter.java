package mosaic.variationalCurvatureFilters;

/**
 * Common interface for all curvature filters.
 * @author Krzysztof Gonciarz
 */
public interface CurvatureFilter {
    /**
     * Implementations of this filter can be passed to runFilter method in order to
     * process only specific pixels of image. If shouldBeProcessed returns true then
     * pixel with coordinates (aX, aY) will be processed.
     */
    interface Mask {
        boolean shouldBeProcessed(int aX, int aY);
    }
    
    /**
     * Run filter on given image and perform given number of iterations.
     * @param aImg 2D image. Notice: it should be in format aImg[y][x] (first dim is Y)
     * @param aNumOfIterations Number of iterations.
     */
    void runFilter(float[][] aImg, int aNumOfIterations);
    
    /**
     * Run filter on given image and perform given number of iterations.
     * @param aImg 2D image. Notice: it should be in format aImg[y][x] (first dim is Y)
     * @param aNumOfIterations Number of iterations.
     * @param aMask Mask used for running filter only on chosen pixels.
     */
    void runFilter(float[][] aImg, int aNumOfIterations, Mask aMask);
}
