package mosaic.variationalCurvatureFilters;


public class NoSplitFilterKernelGc implements NoSplitFilterKernel {

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
        final float m2 = 2 * aCurrentRow[aPos];
        final float m3 = 3 * aCurrentRow[aPos];
        final float u = aPreviousRow[aPos];
        final float d = aNextRow[aPos];
        final float l = aCurrentRow[aPos - 1];
        final float r = aCurrentRow[aPos + 1];
        final float ld = aNextRow[aPos - 1];
        final float rd = aNextRow[aPos + 1];
        final float lu = aPreviousRow[aPos - 1];
        final float ru = aPreviousRow[aPos + 1];
        
        // Calculate distances d0..d7
        float d0 = (u+d)-m2;
        float d1 = (l+r)-m2;
        float d2 = (lu+rd)-m2;
        float d3 = (ru+ld)-m2;

        float d4 = (lu+u+l)-m3;
        float d5 = (ru+u+r)-m3;
        float d6 = (l+ld+d)-m3;
        float d7 = (r+d+rd)-m3;

        // And find minimum (absolute) change of d0..d7
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

        // Finally update middle pixel with found minimum change
        aCurrentRow[aPos] += d0;
    }
}