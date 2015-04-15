package mosaic.variationalCurvatureFilters;

public interface NoSplitFilterKernel {
    void filterKernel(int aPos, float[] aCurrentRow, float[] aPreviousRow, float[] aNextRow);
}
