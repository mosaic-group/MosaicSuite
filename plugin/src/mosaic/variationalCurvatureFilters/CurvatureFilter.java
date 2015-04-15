package mosaic.variationalCurvatureFilters;

public interface CurvatureFilter {
    void runFilter(float[][] aImg, int aNumOfIterations);
}