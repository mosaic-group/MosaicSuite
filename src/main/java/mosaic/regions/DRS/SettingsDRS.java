package mosaic.regions.DRS;

public class SettingsDRS {
    
    // Shape control
    public boolean allowFusion = true;
    public boolean allowFission = true;
    public boolean allowHandles = true;

    public int maxNumOfIterations = 300;
    
    // MCMC parameters
    float offBoundarySampleProbability = 0.00f;
    boolean useBiasedProposal = false;
    boolean usePairProposal = false;
    float burnInFactor = 0.3f;

    public SettingsDRS(boolean aAllowFusion,
                       boolean aAllowFission,
                       boolean aAllowHandles,
                       int aMaxNumOfIterations,
                       float aOffBoundarySamplingProbability,
                       boolean aUseBiasedProposal,
                       boolean aUsePairProporsal,
                       float aBurnInFactor)
    {
        allowFusion = aAllowFusion;
        allowFission = aAllowFission;
        allowHandles = aAllowHandles;
        maxNumOfIterations = aMaxNumOfIterations;
        offBoundarySampleProbability = aOffBoundarySamplingProbability;
        useBiasedProposal = aUseBiasedProposal;
        usePairProposal = aUsePairProporsal;
        burnInFactor = aBurnInFactor;
    }
}