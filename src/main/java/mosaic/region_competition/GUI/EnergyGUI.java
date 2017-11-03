package mosaic.region_competition.GUI;


import mosaic.region_competition.Settings;
import mosaic.region_competition.Region_Competition.EnergyFunctionalType;


abstract class EnergyGUI extends SettingsBaseGUI {

    protected EnergyGUI(Settings aSettings) {
        super(aSettings);
    }

    public static EnergyGUI factory(Settings aSettings, EnergyFunctionalType type) {
        EnergyGUI result = null;

        switch (type) {
            case e_PS: {
                result = new PS_GUI(aSettings);
                break;
            }
            case e_DeconvolutionPC:
            case e_PC:
            case e_PC_Gauss:
            default: {
                result = new DefaultEnergyGUI();
                break;
            }
        }
        return result;
    }

    public static EnergyGUI factory(Settings aSettings, String energy) {
        final EnergyFunctionalType type = EnergyFunctionalType.valueOf(energy);
        return factory(aSettings, type);
    }
}

class PS_GUI extends EnergyGUI {

    public PS_GUI(Settings aSettings) {
        super(aSettings);
    }

    @Override
    public void createDialog() {
        gd.setTitle("Gauss PS Options");
        gd.addNumericField("Radius", iSettings.m_GaussPSEnergyRadius, 0);
        gd.addNumericField("Beta E_Balloon", iSettings.m_BalloonForceCoeff, 4);
    }

    @Override
    public void process() {
        iSettings.m_GaussPSEnergyRadius = (int) gd.getNextNumber();
        iSettings.m_BalloonForceCoeff = (float) gd.getNextNumber();
    }
}

class DefaultEnergyGUI extends EnergyGUI {

    protected DefaultEnergyGUI() {
        super(null);
    }

    @Override
    public void createDialog() {
        gd = getNoGUI();
    }

    @Override
    public void process() {
    }

}
