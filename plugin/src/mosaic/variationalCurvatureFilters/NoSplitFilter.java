package mosaic.variationalCurvatureFilters;


public abstract class NoSplitFilter implements CurvatureFilter {
    NoSplitFilterKernel iNsk;
    
    NoSplitFilter(NoSplitFilterKernel aNsk) {
        iNsk = aNsk;
    }
    
    @Override
    public void runFilter(float[][] aImg, int aNumOfIterations) {
        int M = aImg.length - 1;
        int N = aImg[0].length - 1;
        float[] pCurrentRow, pNextRow, pPreviousRow;

        for (int it = 0; it < aNumOfIterations; ++it) {
            for (int sq = 3; sq >= 0; --sq) {
                // Sequence:
                // col row set
                // --------------
                // 2   2   BT
                // 2   1   WC
                // 1   2   WT
                // 1   1   BC
                int col = sq / 2 + 1;
                int row = sq % 2 + 1;

                for (int i = row; i < M; i += 2) {
                    pPreviousRow = aImg[i - 1];
                    pCurrentRow = aImg[i];
                    pNextRow = aImg[i + 1];

                    for (int j = col; j < N; j += 2) {
                        iNsk.filterKernel(j, pCurrentRow, pPreviousRow, pNextRow);
                    }
                }
            }
        }
    }
}
