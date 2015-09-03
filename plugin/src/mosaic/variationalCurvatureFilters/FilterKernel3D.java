package mosaic.variationalCurvatureFilters;

/**
 * Interface for all curvature filters (running on 3x3x3 mask).
 * @author Krzysztof Gonciarz
 */
public interface FilterKernel3D {
        /**
         * This method calculates delta value which should be applied to middle pixel pointed by (aX, aY, aZ)
         * @param aImage should be in format [Z][Y][X]
         * @param aX - coordinate of processed point
         * @param aY - coordinate of processed point
         * @param aZ - coordinate of processed point
         */
        float filterKernel(float[][][] aImage, int aX, int aY, int aZ);
}
