package mosaic.variationalCurvatureFilters;


public class SplitFilterKernelMc implements SplitFilterKernel {

    @Override
    public void filterKernel(float[] aMiddle, float[] aSides, float[] aDown, float[] aDownCorners, float[] aUp, float[] aUpCorners, boolean aShifted) {
        final int startIdx = 2 + (aShifted ? 1 : 0); 
        
        int rightIdx = (startIdx + 1) / 2;
        int leftIdx = rightIdx - 1;
        int middleIdx = startIdx / 2;
        
        for (int j = 1; j < aMiddle.length - 1; ++j) {
            /*
             * Naming:
             * 
             *       lu | u | ru
             *       ---+---+---
             *       l  | m |  r
             *       ---+---+---
             *       ld | d | rd
             */
            final float m8 = aMiddle[middleIdx] * 8;
            final float u = aUp[middleIdx];
            final float d = aDown[middleIdx];
            final float l = aSides[leftIdx];
            final float r = aSides[rightIdx];
            final float ld = aDownCorners[leftIdx];
            final float rd = aDownCorners[rightIdx];
            final float lu = aUpCorners[leftIdx];
            final float ru = aUpCorners[rightIdx];
            
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
            
            // Finally update middle pixel with minimum change
            aMiddle[middleIdx] += d0;
            
            // Go to next pixel
            ++rightIdx;
            ++leftIdx;
            ++middleIdx;
        }
    }
}