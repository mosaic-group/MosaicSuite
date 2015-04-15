package mosaic.variationalCurvatureFilters;


public class NoSplitFilterKernelMc implements NoSplitFilterKernel {

    @Override
    public void filterKernel(int aPos, float[] aCurrentRow, float[] aPreviousRow, float[] aNextRow) {
        /*
         * Naming:
         * 
         *       lu | u | ru
         *       ---+---+---
         *       l  | m |  r
         *       ---+---+---
         *       ld | d | rd
         */
        final float m8 = 8 * aCurrentRow[aPos];
        final float u = aPreviousRow[aPos];
        final float d = aNextRow[aPos];
        final float l = aCurrentRow[aPos - 1];
        final float r = aCurrentRow[aPos + 1];
        final float ld = aNextRow[aPos - 1];
        final float rd = aNextRow[aPos + 1];
        final float lu = aPreviousRow[aPos - 1];
        final float ru = aPreviousRow[aPos + 1];
        
        
        // Calculate distances d0..d3
        float common1 = (u+d) * 2.5f - m8;
        float d0 = common1 + r * 5.0f - ru - rd;
        float d1 = common1 + l * 5.0f - lu - ld;
        
        float common2 = (l+r) * 2.5f - m8;
        float d2 = common2 + u * 5.0f - lu - ru;
        float d3 = common2 + d * 5.0f - ld - rd;
        

        // And find minimal (absolute) change
        float d0a = Math.abs(d0);
        float da = Math.abs(d1);
        if (da < d0a) {d0 = d1; d0a = da;}
        da = Math.abs(d2);
        if (da < d0a) {d0 = d2; d0a = da;}
        da = Math.abs(d3);
        if (da < d0a) {d0 = d3;}
        
        d0 /= 8.0f;

        // Finally update middle pixel with found minimum change
        aCurrentRow[aPos] += d0;
    }
}