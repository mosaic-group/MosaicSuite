package mosaic.region_competition;


public class PluginSettingsDRS extends Settings {
    private static final long serialVersionUID = 8823942643993425909L;
    
    
    public float offBoundarySampleProbability = 0.00f;
    public boolean useBiasedProposal = false;
    public boolean usePairProposal = false;
    public float burnInFactor = 0.3f;
    
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
