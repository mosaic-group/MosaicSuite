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
        final InitializationType type = InitializationType.getEnum(s);
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
        gd.addNumericField("Bubble_Radius", iSettings.initBubblesRadius, 0);
        gd.addNumericField("Bubble_Padding", iSettings.initBubblesDisplacement, 0);
    }

    @Override
    public void process() {
        if (gd.wasCanceled()) {
            return;
        }

        iSettings.initBubblesRadius = (int) gd.getNextNumber();
        iSettings.initBubblesDisplacement = (int) gd.getNextNumber();
    }
}

class BoxInitGUI extends InitializationGUI {

    protected BoxInitGUI(Settings aSettings) {
        super(aSettings);
        gd.setTitle("Box Initialization");
    }

    @Override
    public void createDialog() {
        gd.addNumericField("Box fill ratio", iSettings.initBoxRatio, 2);

    }

    @Override
    public void process() {
        if (gd.wasCanceled()) {
            return;
        }

        iSettings.initBoxRatio = gd.getNextNumber();
    }
}

class LocalMaxGUI extends InitializationGUI {

    protected LocalMaxGUI(Settings aSettings) {
        super(aSettings);
        gd.setTitle("Local Max Initialization");
    }

    @Override
    public void createDialog() {
        gd.addNumericField("Radius", iSettings.initLocalMaxBubblesRadius, 1);
        gd.addNumericField("Sigma", iSettings.initLocalMaxGaussBlurSigma, 1);
        gd.addNumericField("Tolerance (0-1)", iSettings.initLocalMaxTolerance, 5);
        gd.addNumericField("Region Tol", iSettings.initLocalMaxMinimumRegionSize, 1);
    }

    @Override
    public void process() {
        if (gd.wasCanceled()) {
            return;
        }

        iSettings.initLocalMaxBubblesRadius = (int) gd.getNextNumber();
        iSettings.initLocalMaxGaussBlurSigma = (int) gd.getNextNumber();
        iSettings.initLocalMaxTolerance = gd.getNextNumber();
        iSettings.initLocalMaxMinimumRegionSize = (int) gd.getNextNumber();
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
