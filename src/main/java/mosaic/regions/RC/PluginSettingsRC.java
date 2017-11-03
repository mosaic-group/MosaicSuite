package mosaic.regions.RC;

import mosaic.regions.Settings;

public class PluginSettingsRC extends Settings {
    private static final long serialVersionUID = 4308398423861694904L;

    //General -------------------------------------------------------------------------------------
    public double oscillationThreshold = 0.02;
    
    // Init Energies ------------------------------------------------------------------------------
    public float energyRegionMergingThreshold = 0.02f;

    
    public void copy(PluginSettingsRC s) {
        super.copy(s);
        oscillationThreshold = s.oscillationThreshold;
        oscillationThreshold = s.oscillationThreshold;
    }
    
    public PluginSettingsRC() {}
    public PluginSettingsRC(PluginSettingsRC s) { copy(s); }
}
