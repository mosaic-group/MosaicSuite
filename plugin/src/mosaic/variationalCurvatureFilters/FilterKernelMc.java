package mosaic.variationalCurvatureFilters;

/**
 * Implementation of MC (Mean Curvature) filter
 * @author Krzysztof Gonciarz
 */
public class FilterKernelMc implements FilterKernel {

    @Override
    public float filterKernel(float lu, float u, float ru, float l, float m, float r, float ld, float d, float rd) {
        /*
         * Naming:
         *
         *       lu | u | ru
         *       ---+---+---
         *       l  | m |  r
         *       ---+---+---
         *       ld | d | rd
         */

        final float m8 = 8 * m;

        // Calculate distances d0..d3
        final float common1 = (u+d) * 2.5f - m8;
        float d0 = common1 + r * 5.0f - ru - rd;
        final float d1 = common1 + l * 5.0f - lu - ld;

        final float common2 = (l+r) * 2.5f - m8;
        final float d2 = common2 + u * 5.0f - lu - ru;
        final float d3 = common2 + d * 5.0f - ld - rd;

        // And find minimal (absolute) change
        float d0a = Math.abs(d0);
        float da = Math.abs(d1);
        if (da < d0a) {d0 = d1; d0a = da;}
        da = Math.abs(d2);
        if (da < d0a) {d0 = d2; d0a = da;}
        da = Math.abs(d3);
        if (da < d0a) {d0 = d3;}

        d0 /= 8.0f;

        // Finally return minimum change
        return d0;
    }

}