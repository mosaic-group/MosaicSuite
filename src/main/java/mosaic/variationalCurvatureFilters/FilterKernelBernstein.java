package mosaic.variationalCurvatureFilters;

/**
 * Implementation of Bernstein Filter
 * @author Krzysztof Gonciarz
 */
public class FilterKernelBernstein implements FilterKernel {

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

        // Comment from original C++ file:
        //
        // compute the movement according to half window
        // ---------------------------------------------------------
        //   f   a   b            0 1/2 0               3/7 1/7 -1/7
        //       I   e               -1 0                    -1  1/7
        //       c   d              1/2 0                        3/7
        //

        // First step
        final float m2 = 2 * m;

        float d0 = l + r - m2;
        float d1 = u + d - m2;
        float d2 = lu + rd - m2;
        float d3 = ld + ru - m2;

        // And find minimal (absolute) change
        float d0a = Math.abs(d0);
        float da = Math.abs(d1);
        if (da < d0a) {d0 = d1; d0a = da;}
        da = Math.abs(d2);
        if (da < d0a) {d0 = d2; d0a = da;}
        da = Math.abs(d3);
        if (da < d0a) {d0 = d3;}

        // Second step
        final float m7 = 7 * m;
        d0 *= 3.5f;

        final float common2 = 3 * (ru + ld) - m7;
        final float common3 = 3 * (lu + rd) - m7;

        d1 = u - lu + l + common2;
        d2 = u - ru + r + common3;
        d3 = d - ld + l + common3;
        final float d4 = d - rd + r + common2;

        // And find minimal (absolute) change
        d0a = Math.abs(d0);
        da = Math.abs(d1);
        if (da < d0a) {d0 = d1; d0a = da;}
        da = Math.abs(d2);
        if (da < d0a) {d0 = d2; d0a = da;}
        da = Math.abs(d3);
        if (da < d0a) {d0 = d3;}
        da = Math.abs(d4);
        if (da < d0a) {d0 = d4;}

        d0 /= 7.0f;

        // Finally return minimum change
        return d0;
    }

}