package mosaic.variationalCurvatureFilters;

/**
 * Implementation of GC (Gaussian Curvature) 3D filter
 * @author Krzysztof Gonciarz
 */
public class FilterKernelGc3D implements FilterKernel3D {
    @Override
    public float filterKernel(float[][][] aImage, int aX, int aY, int aZ) {
        float[] d = new float[50];
        int i=aX, j=aY , k=aZ;
        
        // Calculating minimum distances (taken from Fortran code "ppm_rc_gc.f")
        d[1] = (aImage[k - 1][j][i] + aImage[k + 1][j][i]) / 2.0f - aImage[k][j][i];
        d[2] = (aImage[k][j - 1][i] + aImage[k][j + 1][i]) / 2.0f - aImage[k][j][i];
        d[3] = (aImage[k][j][i - 1] + aImage[k][j][i + 1]) / 2.0f - aImage[k][j][i];
        d[4] = (aImage[k - 1][j - 1][i - 1] + aImage[k + 1][j + 1][i + 1]) / 2.0f - aImage[k][j][i];
        d[5] = (aImage[k - 1][j - 1][ i] + aImage[k + 1][j + 1][i]) / 2.0f - aImage[k][j][i];
        d[6] = (aImage[k - 1][j - 1][i + 1] + aImage[k + 1][j + 1][i - 1]) / 2.0f - aImage[k][j][i];
        d[7] = (aImage[k - 1][j][i - 1] + aImage[k + 1][j][i + 1]) / 2.0f - aImage[k][j][i];
        d[8] = (aImage[k - 1][j][i + 1] + aImage[k + 1][j][i - 1]) / 2.0f - aImage[k][j][i];
        d[9] = (aImage[k - 1][j + 1][i - 1] + aImage[k + 1][j - 1][i + 1]) / 2.0f - aImage[k][j][i];
        d[10] = (aImage[k - 1][j + 1][i] + aImage[k + 1][j - 1][i]) / 2.0f - aImage[k][j][i];
        d[11] = (aImage[k - 1][j + 1][i + 1] + aImage[k + 1][j - 1][i - 1]) / 2.0f - aImage[k][j][i];
        d[12] = (aImage[k][j - 1][i - 1] + aImage[k][j + 1][i + 1]) / 2.0f - aImage[k][j][i];
        d[13] = (aImage[k][j - 1][i + 1] + aImage[k][j + 1][i - 1]) / 2.0f - aImage[k][j][i];

        d[14] = (aImage[k + 1][j][i] + aImage[k][j - 1][i - 1] + aImage[k + 1][j - 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[15] = (aImage[k + 1][j][i] + aImage[k][j - 1][i] + aImage[k + 1][j - 1][i]) / 3.0f - aImage[k][j][i];
        d[16] = (aImage[k + 1][j][i] + aImage[k][j - 1][i + 1] + aImage[k + 1][j - 1][i + 1]) / 3.0f - aImage[k][j][i];
        d[17] = (aImage[k + 1][j][i] + aImage[k][j][i - 1] + aImage[k + 1][j][i - 1]) / 3.0f - aImage[k][j][i];
        d[18] = (aImage[k + 1][j][i] + aImage[k][j][i + 1] + aImage[k + 1][j][i + 1]) / 3.0f - aImage[k][j][i];
        d[19] = (aImage[k + 1][j][i] + aImage[k][j + 1][i - 1] + aImage[k + 1][j + 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[20] = (aImage[k + 1][j][i] + aImage[k][j + 1][i] + aImage[k + 1][j + 1][i]) / 3.0f - aImage[k][j][i];
        d[21] = (aImage[k + 1][j][i] + aImage[k][j + 1][i + 1] + aImage[k + 1][j + 1][i + 1]) / 3.0f - aImage[k][j][i];
        d[22] = (aImage[k - 1][j][i] + aImage[k][j - 1][i - 1] + aImage[k - 1][j - 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[23] = (aImage[k - 1][j][i] + aImage[k][j - 1][i] + aImage[k - 1][j - 1][i]) / 3.0f - aImage[k][j][i];
        d[24] = (aImage[k - 1][j][i] + aImage[k][j - 1][i + 1] + aImage[k - 1][j - 1][i + 1]) / 3.0f - aImage[k][j][i];
        d[25] = (aImage[k - 1][j][i] + aImage[k][j][i - 1] + aImage[k - 1][j][i - 1]) / 3.0f - aImage[k][j][i];
        d[26] = (aImage[k - 1][j][i] + aImage[k][j][i + 1] + aImage[k - 1][j][i + 1]) / 3.0f - aImage[k][j][i];
        d[27] = (aImage[k - 1][j][i] + aImage[k][j + 1][i - 1] + aImage[k - 1][j + 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[28] = (aImage[k - 1][j][i] + aImage[k][j + 1][i] + aImage[k - 1][j + 1][i]) / 3.0f - aImage[k][j][i];
        d[29] = (aImage[k - 1][j][i] + aImage[k][j + 1][i + 1] + aImage[k - 1][j + 1][i + 1]) / 3.0f - aImage[k][j][i];
        d[30] = (aImage[k + 1][j][i - 1] + aImage[k][j + 1][i] + aImage[k + 1][j + 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[31] = (aImage[k + 1][j][i - 1] + aImage[k][j - 1][i] + aImage[k + 1][j - 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[32] = (aImage[k + 1][j][i + 1] + aImage[k][j + 1][i] + aImage[k + 1][j + 1][i + 1]) / 3.0f - aImage[k][j][i];
        d[33] = (aImage[k + 1][j][i + 1] + aImage[k][j - 1][i] + aImage[k + 1][j - 1][i + 1]) / 3.0f - aImage[k][j][i];
        d[34] = (aImage[k - 1][j][i - 1] + aImage[k][j + 1][i] + aImage[k - 1][j + 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[35] = (aImage[k - 1][j][i - 1] + aImage[k][j - 1][i] + aImage[k - 1][j - 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[36] = (aImage[k - 1][j][i + 1] + aImage[k][j + 1][i] + aImage[k - 1][j + 1][i + 1]) / 3.0f - aImage[k][j][i];
        d[37] = (aImage[k - 1][j][i + 1] + aImage[k][j - 1][i] + aImage[k - 1][j - 1][i + 1]) / 3.0f - aImage[k][j][i];
        d[38] = (aImage[k][j][i - 1] + aImage[k][j - 1][i] + aImage[k][j - 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[39] = (aImage[k][j][i - 1] + aImage[k][j + 1][i] + aImage[k][j + 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[40] = (aImage[k][j][i + 1] + aImage[k][j - 1][i] + aImage[k][j - 1][i + 1]) / 3.0f - aImage[k][j][i];
        d[41] = (aImage[k][j][i + 1] + aImage[k][j + 1][i] + aImage[k][j + 1][i + 1]) / 3.0f - aImage[k][j][i];
        d[42] = (aImage[k + 1][j - 1][i] + aImage[k][j][i - 1] + aImage[k + 1][j - 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[43] = (aImage[k + 1][j - 1][i] + aImage[k][j][i + 1] + aImage[k + 1][j - 1][i + 1]) / 3.0f - aImage[k][j][i];
        d[44] = (aImage[k + 1][j + 1][i] + aImage[k][j][i - 1] + aImage[k + 1][j + 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[45] = (aImage[k + 1][j + 1][i] + aImage[k][j][i + 1] + aImage[k + 1][j + 1][i + 1]) / 3.0f - aImage[k][j][i];
        d[46] = (aImage[k - 1][j - 1][i] + aImage[k][j][i - 1] + aImage[k - 1][j - 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[47] = (aImage[k - 1][j - 1][i] + aImage[k][j][i + 1] + aImage[k - 1][j - 1][i + 1]) / 3.0f - aImage[k][j][i];
        d[48] = (aImage[k - 1][j + 1][i] + aImage[k][j][i - 1] + aImage[k - 1][j + 1][i - 1]) / 3.0f - aImage[k][j][i];
        d[49] = (aImage[k - 1][j + 1][i] + aImage[k][j][i + 1] + aImage[k - 1][j + 1][i + 1]) / 3.0f - aImage[k][j][i];
        
        // Find minimum absolute change
        d[0] = d[1];
        for (int idx = 2; idx <= 49; ++idx) {
            float da = Math.abs(d[idx]);
            if (da < Math.abs(d[0])) {d[0] = d[idx];}
        }
        
        // Finally return minimum change 
        return d[0];
    }
}
