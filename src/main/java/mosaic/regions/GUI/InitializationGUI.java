package mosaic.regions.GUI;


import mosaic.regions.Settings;
import mosaic.regions.RegionsUtils.InitializationType;


abstract class InitializationGUI extends SettingsBaseGUI {

    protected InitializationGUI(Settings aSettings) {
        super(aSettings);
    }

    public static InitializationGUI factory(Settings aSettings, InitializationType type) {
        InitializationGUI result = null;

        switch (type) {
            case Bubbles: {
                result = new BubblesInitGUI(aSettings);
                break;
            }
            case Rectangle: {
                result = new BoxInitGUI(aSettings);
                break;
            }
            case LocalMax: {
                result = new LocalMaxGUI(aSettings);
                break;
            }
            case File:
            case ROI_2D:
            default: {
                result = new DefaultInitGUI();
                break;
            }
        }
        return result;
    }

    public static InitializationGUI factory(Settings aSettings, String s) {
        final InitializationType type = InitializationType.valueOf(s);
        final InitializationGUI result = factory(aSettings, type);
        return result;
    }

}

class BubblesInitGUI extends InitializationGUI {

    protected BubblesInitGUI(Settings aSettings) {
        super(aSettings);
        gd.setTitle("It's bubble Time");
    }

    @Override
    public void createDialog() {
        gd.addNumericField("Bubble_Radius", iSettings.m_BubblesRadius, 0);
        gd.addNumericField("Bubble_Padding", iSettings.m_BubblesDispl, 0);
    }

    @Override
    public void process() {
        if (gd.wasCanceled()) {
            return;
        }

        iSettings.m_BubblesRadius = (int) gd.getNextNumber();
        iSettings.m_BubblesDispl = (int) gd.getNextNumber();
    }
}

class BoxInitGUI extends InitializationGUI {

    protected BoxInitGUI(Settings aSettings) {
        super(aSettings);
        gd.setTitle("Box Initialization");
    }

    @Override
    public void createDialog() {
        gd.addNumericField("Box fill ratio", iSettings.l_BoxRatio, 2);

    }

    @Override
    public void process() {
        if (gd.wasCanceled()) {
            return;
        }

        iSettings.l_BoxRatio = gd.getNextNumber();
    }
}

class LocalMaxGUI extends InitializationGUI {

    protected LocalMaxGUI(Settings aSettings) {
        super(aSettings);
        gd.setTitle("Local Max Initialization");
    }

    @Override
    public void createDialog() {
        gd.addNumericField("Radius", iSettings.l_BubblesRadius, 1);
        gd.addNumericField("Sigma", iSettings.l_Sigma, 1);
        gd.addNumericField("Tolerance (0-1)", iSettings.l_Tolerance, 5);
        gd.addNumericField("Region Tol", iSettings.l_RegionTolerance, 1);
    }

    @Override
    public void process() {
        if (gd.wasCanceled()) {
            return;
        }

        iSettings.l_BubblesRadius = (int) gd.getNextNumber();
        iSettings.l_Sigma = (int) gd.getNextNumber();
        iSettings.l_Tolerance = gd.getNextNumber();
        iSettings.l_RegionTolerance = (int) gd.getNextNumber();
    }
}

class DefaultInitGUI extends InitializationGUI {

    protected DefaultInitGUI() {
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
