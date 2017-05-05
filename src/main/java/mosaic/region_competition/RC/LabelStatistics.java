package mosaic.region_competition.RC;


public class LabelStatistics {
    // Label info
    public int iLabel = 0;
    public int iLabelCount;
    
    // Label stats
    public double iMeanIntensity = 0;
    public double iVarIntensity = 0;
    public double iMedianIntensity = 0;
    
    // Label mean position
    public double iMeanPosition[] = null;

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
        return "L: " + iLabel + " count: " + iLabelCount + " mean: " + iMeanIntensity;
    }
}
