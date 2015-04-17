package mosaic.variationalCurvatureFilters;


public class FilterKernelTv implements FilterKernel {
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

        final float m5 = 5 * m;
        float common = 0.0f;
        
        // Calculate distances d0..d7
        common = u + d - m5;
        float d0 = common + lu + ld + l;
        float d1 = common + ru + rd + r;
        
        common = l + r -m5;
        float d2 = common + lu + ru + u;
        float d3 = common + ld + rd + d;
   
        common = lu + ru + u - m5;
        float d4 = common + l + ld;
        float d5 = common + r + rd;
        
        common = ld + rd + d - m5;
        float d6 = common + l + lu;
        float d7 = common + r + ru;

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
       
        if (Math.abs(d4) < Math.abs(d0)) d0 = d4;

        // Finally return minimum change 
        return d0;
    }
    
}