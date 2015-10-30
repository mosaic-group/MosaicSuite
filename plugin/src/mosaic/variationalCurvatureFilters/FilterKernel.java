package mosaic.variationalCurvatureFilters;

/**
 * Interface for all curvature filters (running on 3x3 mask).
 * @author Krzysztof Gonciarz
 */
public interface FilterKernel {
    /**
     * This method calculates delta value which should be applied to middle 'm' pixel
     * Naming:
     *
     *       lu | u | ru
     *       ---+---+---
     *       l  | m |  r
     *       ---+---+---
     *       ld | d | rd
     *
     * @param lu
     * @param u
     * @param ru
     * @param l
     * @param m
     * @param r
     * @param ld
     * @param d
     * @param rd
     * @return Middle pixel change delta value
     */
    float filterKernel(float lu, float u, float ru, float l, float m, float r, float ld, float d, float rd);
}
