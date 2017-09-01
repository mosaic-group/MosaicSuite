package mosaic.region_competition.utils;


public class LabelStatistics {
    // Label info
    public final int iLabel;
    public int iLabelCount = 0;
    
    // Label stats
    public double iMeanIntensity = 0.0;
    public double iVarIntensity = 0.0;
    public double iMedianIntensity = 0.0;
    public double iSumOfSq = 0.0;
    public double iSum = 0.0;
    
    // Label mean position
    public final double iMeanPosition[];

    /**
     * Create a statistics for label
     * @param aLabel id of the label
     * @param aNumOfDims dimensions of the problem
     */
    public LabelStatistics(int aLabel, int aNumOfDims) {
        iLabel = aLabel;
        iMeanPosition = new double[aNumOfDims];
    }

    @Override
    public String toString() {
        return "{L: " + iLabel + ", count: " + iLabelCount + ", mean: " + iMeanIntensity + "}";
    }
}
