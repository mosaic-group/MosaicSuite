package mosaic.regions.RC;

import mosaic.regions.Settings;

public class PluginSettingsRC extends Settings {
    private static final long serialVersionUID = 4308398423861694904L;

    //General -------------------------------------------------------------------------------------
    public double m_OscillationThreshold = 0.02;
    
    // Init Energies ------------------------------------------------------------------------------
    public float m_RegionMergingThreshold = 0.02f;

    
    public void copy(PluginSettingsRC s) {
        super.copy(s);
        m_OscillationThreshold = s.m_OscillationThreshold;
        m_OscillationThreshold = s.m_OscillationThreshold;
    }
    
    public PluginSettingsRC() {}
    public PluginSettingsRC(PluginSettingsRC s) { copy(s); }
}
