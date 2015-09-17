package mosaic.variationalCurvatureFilters;

/**
 * Implementation of GC (Gaussian Curvature) filter
 * @author Krzysztof Gonciarz
 */
public class FilterKernelGc implements FilterKernel {
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

        final float m2 = 2 * m;
        final float m3 = 3 * m;

        // Calculate distances d0..d7
        float d0 = (u+d)-m2;
        float d1 = (l+r)-m2;
        float d2 = (lu+rd)-m2;
        float d3 = (ru+ld)-m2;

        float d4 = (lu+u+l)-m3;
        float d5 = (ru+u+r)-m3;
        float d6 = (l+ld+d)-m3;
        float d7 = (r+d+rd)-m3;

        // And find minimum (absolute) change
        float da;

        float d0a = Math.abs(d0);
        da = Math.abs(d1);
        if (da < d0a) {d0 = d1; d0a = da;}
        da = Math.abs(d2);
        if (da < d0a) {d0 = d2; d0a = da;}
        da = Math.abs(d3);
        if (da < d0a) {d0 = d3;}

        float d4a = Math.abs(d4);
        da = Math.abs(d5);
        if (da < d4a) {d4 = d5; d4a = da;}
        da = Math.abs(d6);
        if (da < d4a) {d4 = d6; d4a = da;}
        da = Math.abs(d7);
        if (da < d4a) {d4 = d7;}

        d0 /= 2;
        d4 /= 3;

        if (Math.abs(d4) < Math.abs(d0)) {
            d0 = d4;
        }

        // Finally return minimum change
        return d0;
    }

}