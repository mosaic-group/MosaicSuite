package mosaic.variationalCurvatureFilters;


public class SplitFilterKernelGc implements SplitFilterKernel {
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
            final float m2 = 2 * aMiddle[middleIdx];
            final float m3 = 3 * aMiddle[middleIdx];
            final float u = aUp[middleIdx];
            final float d = aDown[middleIdx];
            final float l = aSides[leftIdx];
            final float r = aSides[rightIdx];
            final float ld = aDownCorners[leftIdx];
            final float rd = aDownCorners[rightIdx];
            final float lu = aUpCorners[leftIdx];
            final float ru = aUpCorners[rightIdx];
            
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
           
            if (Math.abs(d4) < Math.abs(d0)) d0 = d4;

            // Finally update middle pixel with minimum change
            aMiddle[middleIdx] += d0;
            
            // Go to next pixel
            ++rightIdx;
            ++leftIdx;
            ++middleIdx;
        }
    }
    
}