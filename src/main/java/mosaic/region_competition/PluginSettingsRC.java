package mosaic.region_competition;


public class PluginSettingsRC extends Settings {
    private static final long serialVersionUID = 4308398423861694904L;

    
    public double m_OscillationThreshold = 0.02;
    
    public void copy(PluginSettingsRC s) {
        super.copy(s);
        m_OscillationThreshold = s.m_OscillationThreshold;
    }
    
    public PluginSettingsRC() {}
    public PluginSettingsRC(PluginSettingsRC s) { copy(s); }
}
