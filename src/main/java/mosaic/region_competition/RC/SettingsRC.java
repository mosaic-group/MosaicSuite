package mosaic.region_competition.RC;


public class SettingsRC {
    
    // Shape control
    public boolean allowFusion = true;
    public boolean allowFission = true;
    public boolean allowHandles = true;

    // Oscillation detection
    public int maxNumOfIterations = 300;
    public double oscillationThreshold = 0.02;
    
    // TODO: This should be gone soon
    boolean usingDeconvolutionPcEnergy = false;
    
    
    public SettingsRC(boolean aAllowFusion,
                      boolean aAllowFission,
                      boolean aAllowHandles,
                      int aMaxNumOfIterations,
                      double aOscillationThreshold,
                      boolean aUsingDeconvolutionPcEnergy)
    {
        allowFusion = aAllowFusion;
        allowFission = aAllowFission;
        allowHandles = aAllowHandles;
        
        maxNumOfIterations = aMaxNumOfIterations;
        oscillationThreshold = aOscillationThreshold;
        
        usingDeconvolutionPcEnergy = aUsingDeconvolutionPcEnergy;
    }
}
