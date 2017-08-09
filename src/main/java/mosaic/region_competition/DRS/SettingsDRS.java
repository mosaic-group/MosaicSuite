package mosaic.region_competition.DRS;

public class SettingsDRS {
    
    // Shape control
    public boolean allowFusion = true;
    public boolean allowFission = true;
    public boolean allowHandles = true;

    // TODO: Should be removed when not needed (after moving iteratino to main plugin)
    public int maxNumOfIterations = 300;

    // TODO: Should be removed when not needed
    public boolean usingDeconvolutionPcEnergy = false;

    public SettingsDRS(boolean aAllowFusion,
                       boolean aAllowFission,
                       boolean aAllowHandles,
                       int aMaxNumOfIterations,
                       boolean aUsingDeconvolutionPcEnergy)
    {
        allowFusion = aAllowFusion;
        allowFission = aAllowFission;
        allowHandles = aAllowHandles;
        maxNumOfIterations = aMaxNumOfIterations;
        usingDeconvolutionPcEnergy = aUsingDeconvolutionPcEnergy;
    }
}