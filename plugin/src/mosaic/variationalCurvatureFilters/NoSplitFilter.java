package mosaic.variationalCurvatureFilters;

/**
 * This class implements filter running in "no split" mode. (Image is not divided into 4 subsets)
 * It requires proper filter kernel (GC, MC, TV...)
 * @author Krzysztof Gonciarz
 */
public class NoSplitFilter implements CurvatureFilter {
    FilterKernel iFk;
    
    public NoSplitFilter(FilterKernel aFilterKernel) {
        iFk = aFilterKernel;
    }
    
    @Override
    public void runFilter(float[][] aImg, int aNumOfIterations) {
        int M = aImg.length - 1;
        int N = aImg[0].length - 1;
        float[] pCurrentRow, pNextRow, pPreviousRow;

        for (int it = 0; it < aNumOfIterations; ++it) {
            for (int seq = 0; seq <= 3; ++seq) {
                // Sequence:
                // col | row | set corresponding to split filter
                // ---------------------------------------------
                // 1     1     BC
                // 1     2     WT
                // 2     1     WC
                // 2     2     BT
                int col = seq/2 + 1;
                int row = seq%2 + 1;

                for (int i = row; i < M; i += 2) {
                    pPreviousRow = aImg[i - 1];
                    pCurrentRow = aImg[i];
                    pNextRow = aImg[i + 1];

                    for (int j = col; j < N; j += 2) {
                        final float m = pCurrentRow[j];
                        final float u = pPreviousRow[j];
                        final float d = pNextRow[j];
                        final float l = pCurrentRow[j - 1];
                        final float r = pCurrentRow[j + 1];
                        final float ld = pNextRow[j - 1];
                        final float rd = pNextRow[j + 1];
                        final float lu = pPreviousRow[j - 1];
                        final float ru = pPreviousRow[j + 1];
                        
                        pCurrentRow[j] += iFk.filterKernel(lu, u, ru, l, m, r, ld, d, rd);
                    }
                }
            }
        }
    }

    @Override
    public void runFilter(float[][] aImg, int aNumOfIterations, Mask aMask) {
        int M = aImg.length - 1;
        int N = aImg[0].length - 1;
        float[] pCurrentRow, pNextRow, pPreviousRow;

        for (int it = 0; it < aNumOfIterations; ++it) {
            for (int seq = 0; seq <= 3; ++seq) {
                // Sequence:
                // col | row | set corresponding to split filter
                // ---------------------------------------------
                // 1     1     BC
                // 1     2     WT
                // 2     1     WC
                // 2     2     BT
                int col = seq/2 + 1;
                int row = seq%2 + 1;
                
                for (int i = row; i < M; i += 2) {
                    pPreviousRow = aImg[i - 1];
                    pCurrentRow = aImg[i];
                    pNextRow = aImg[i + 1];

                    for (int j = col; j < N; j += 2) {
                        if (aMask.shouldBeProcessed(j,i)) {
                            final float m = pCurrentRow[j];
                            final float u = pPreviousRow[j];
                            final float d = pNextRow[j];
                            final float l = pCurrentRow[j - 1];
                            final float r = pCurrentRow[j + 1];
                            final float ld = pNextRow[j - 1];
                            final float rd = pNextRow[j + 1];
                            final float lu = pPreviousRow[j - 1];
                            final float ru = pPreviousRow[j + 1];
                            
                            pCurrentRow[j] += iFk.filterKernel(lu, u, ru, l, m, r, ld, d, rd);
                        }
                    }
                }
            }
        }
    }
}