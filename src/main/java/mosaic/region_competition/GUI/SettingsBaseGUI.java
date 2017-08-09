package mosaic.region_competition.GUI;


import ij.gui.GenericDialog;
import mosaic.region_competition.Settings;


abstract class SettingsBaseGUI {

    protected GenericDialog gd;
    protected final Settings iSettings;

    protected SettingsBaseGUI(Settings aSettings) {
        iSettings = aSettings;
        gd = new GenericDialog("");
    }

    /**
     * Build here the {@link GenericDialog}
     */
    public abstract void createDialog();

    /**
     * Read out values from {@link GenericDialog} to {@link Settings} <br>
     * Always check first for GenericDialog.wasCancled()
     */
    public final void processDialog() {
        if (gd.wasCanceled()) {
            return;
        }
        process();
    }

    /**
     * Read values from {@link GenericDialog} (getNext() methods)
     */
    protected abstract void process();

    /**
     * Shows the dialog (waits for confirmation)
     */
    public void showDialog() {
        gd.showDialog();
    }

    /**
     * Creates an empty {@link GenericDialog}
     */
    public static GenericDialog getNoGUI() {
        GenericDialog noGUI = new GenericDialog("No additional Options");
        noGUI.addMessage("No additional options available");
        noGUI.hideCancelButton();

        return noGUI;
    }
}
