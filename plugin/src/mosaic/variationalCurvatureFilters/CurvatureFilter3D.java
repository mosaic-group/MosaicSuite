package mosaic.variationalCurvatureFilters;

/**
 * Common interface for all 3D curvature filters.
 * @author Krzysztof Gonciarz
 */
public interface CurvatureFilter3D {
    /**
     * Run filter on given image and perform given number of iterations.
     * @param aImg 3D image. Notice: it should be in format aImg[z][y][x]
     * @param aNumOfIterations Number of iterations.
     */
    void runFilter(float[][][] aImg, int aNumOfIterations);
}
