package mosaic.region_competition.GUI;


import mosaic.plugins.Region_Competition.InitializationType;
import mosaic.region_competition.Settings;


abstract class InitializationGUI extends GUImeMore {

    protected InitializationGUI(Settings settings) {
        super(settings);
    }

    public static InitializationGUI factory(Settings settings, InitializationType type) {
        InitializationGUI result = null;

        switch (type) {
            case Bubbles: {
                result = new BubblesInitGUI(settings);
                break;
            }
            case Rectangle: {
                result = new BoxInitGUI(settings);
                break;
            }
            case LocalMax: {
                result = new LocalMaxGUI(settings);
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

    public static InitializationGUI factory(Settings settings, String s) {
        final InitializationType type = InitializationType.valueOf(s);
        final InitializationGUI result = factory(settings, type);
        return result;
    }

}

class BubblesInitGUI extends InitializationGUI {

    protected BubblesInitGUI(Settings settings) {
        super(settings);
        gd.setTitle("It's bubble Time");
    }

    @Override
    public void createDialog() {
        gd.addNumericField("Bubble_Radius", settings.m_BubblesRadius, 0);
        gd.addNumericField("Bubble_Padding", settings.m_BubblesDispl, 0);
    }

    @Override
    public void process() {
        if (gd.wasCanceled()) {
            return;
        }

        settings.m_BubblesRadius = (int) gd.getNextNumber();
        settings.m_BubblesDispl = (int) gd.getNextNumber();
    }
}

class BoxInitGUI extends InitializationGUI {

    protected BoxInitGUI(Settings settings) {
        super(settings);
        gd.setTitle("Box Initialization");
    }

    @Override
    public void createDialog() {
        gd.addNumericField("Box fill ratio", settings.l_BoxRatio, 2);

    }

    @Override
    public void process() {
        if (gd.wasCanceled()) {
            return;
        }

        settings.l_BoxRatio = gd.getNextNumber();
    }
}

class LocalMaxGUI extends InitializationGUI {

    protected LocalMaxGUI(Settings settings) {
        super(settings);
        gd.setTitle("Local Max Initialization");
    }

    @Override
    public void createDialog() {
        gd.addNumericField("Radius", settings.l_BubblesRadius, 1);
        gd.addNumericField("Sigma", settings.l_Sigma, 1);
        gd.addNumericField("Tolerance (0-1)", settings.l_Tolerance, 5);
        gd.addNumericField("Region Tol", settings.l_RegionTolerance, 1);
    }

    @Override
    public void process() {
        if (gd.wasCanceled()) {
            return;
        }

        settings.l_BubblesRadius = (int) gd.getNextNumber();
        settings.l_Sigma = (int) gd.getNextNumber();
        settings.l_Tolerance = gd.getNextNumber();
        settings.l_RegionTolerance = (int) gd.getNextNumber();
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