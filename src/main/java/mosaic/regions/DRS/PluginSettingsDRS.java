package mosaic.regions.DRS;

import mosaic.regions.Settings;

public class PluginSettingsDRS extends Settings {
    private static final long serialVersionUID = 8823942643993425909L;
    
    // MCMC settings
    public float offBoundarySampleProbability = 0.00f;
    public boolean useBiasedProposal = false;
    public boolean usePairProposal = false;
    public float burnInFactor = 0.3f;
    
    // Input settings 
    public String initFileName = null;
    
    // Output settings
    public boolean showLabelImage = false;
    public boolean saveLabelImage = false;
    public boolean showProbabilityImage = true;
    public boolean saveProbabilityImage = false;
    
    public void copy(PluginSettingsDRS s) {
        super.copy(s);
        offBoundarySampleProbability = s.offBoundarySampleProbability;
        useBiasedProposal = s.useBiasedProposal;
        usePairProposal = s.usePairProposal;
        burnInFactor = s.burnInFactor;
    }

    public PluginSettingsDRS() {}
    public PluginSettingsDRS(PluginSettingsDRS s) { copy(s); }
}
