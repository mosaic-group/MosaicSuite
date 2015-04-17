package mosaic.variationalCurvatureFilters;

/**
 * Common interface for all curvature filters.
 * @author Krzysztof Gonciarz
 */
public interface CurvatureFilter {
    /**
     * Run filter on given image and perform given number of iterations.
     * @param aImg 2D image. Notice: it should be in format aImg[y][x] (first dim is Y)
     * @param aNumOfIterations Nubmer of iterations.
     */
    void runFilter(float[][] aImg, int aNumOfIterations);
}
