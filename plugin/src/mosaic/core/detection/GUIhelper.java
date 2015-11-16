package mosaic.core.detection;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Vector;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.StackWindow;

/**
 * Temporary class with GUI stuff moved from FeaturePointDetector
 * TODO: Obviously it must be refactored by moving into much more proper places.
 */
public class GUIhelper {
    /**
     * gd has to be shown with showDialog and handles the fields added to the dialog
     * with addUserDefinedParamtersDialog(gd).
     * 
     * @param gd <code>GenericDialog</code> at which the UserDefinedParameter fields where added.
     * @return true if user changed the parameters and false if the user didn't changed them.
     */
    public static Boolean getUserDefinedParameters(GenericDialog gd, FeaturePointDetector fpd) {
        final int rad = (int) gd.getNextNumber();
        final double cut = gd.getNextNumber();
        final float per = ((float) gd.getNextNumber()) / 100;
        final float intThreshold = per * 100;
        final boolean absolute = gd.getNextBoolean();

        return fpd.setDetectionParameters(cut, per, rad, intThreshold, absolute);
    }

    /**
     * Gets user defined params that are necessary to display the preview of particle detection.
     */
    public static Boolean getUserDefinedPreviewParams(GenericDialog gd, FeaturePointDetector fpd) {

        @SuppressWarnings("unchecked")
        // the warning is due to old imagej code.
        final Vector<TextField> vec = gd.getNumericFields();
        final int rad = Integer.parseInt((vec.elementAt(0)).getText());
        final double cut = Double.parseDouble((vec.elementAt(1)).getText());
        final float per = (Float.parseFloat((vec.elementAt(2)).getText())) / 100;
        final float intThreshold = per * 100;
        @SuppressWarnings("unchecked")
        // the warning is due to old imagej code
        final Vector<Checkbox> vecb = gd.getCheckboxes();
        final boolean absolute = vecb.elementAt(0).getState();

        // even if the frames were already processed (particles detected) but
        // the user changed the detection params then the frames needs to be processed again
        return fpd.setDetectionParameters(cut, per, rad, intThreshold, absolute);// , sigma_fac);
    }
    
    public static void addUserDefinedParametersDialog(GenericDialog gd, FeaturePointDetector fpd) {
        gd.addMessage("Particle Detection:");
        // These 3 params are only relevant for non text_files_mode
        gd.addNumericField("Radius", fpd.getRadius(), 0);
        gd.addNumericField("Cutoff", fpd.getCutoff(), 1);
        gd.addNumericField("Per/Abs", fpd.getPercentile() * 100, 5, 6, " ");

        gd.addCheckbox("Absolute", false);
    }
    
    
    // ==========================

    
    public static PreviewCanvas generatePreviewCanvas(ImagePlus imp) {
        // save the current magnification factor of the current image window
        final double magnification = imp.getWindow() != null ? imp.getWindow().getCanvas().getMagnification() : 1;

        // generate the previewCanvas - while generating the drawing will be done
        final PreviewCanvas preview_canvas = new PreviewCanvas(imp, magnification);

        // display the image and canvas in a stackWindow
        final StackWindow sw = new StackWindow(imp, preview_canvas);

        // magnify the canvas to match the original image magnification
        while (sw.getCanvas().getMagnification() < magnification) {
            preview_canvas.zoomIn(0, 0);
        }
        return preview_canvas;
    }
    
    // =================================
    
    /**
     * Creates the preview panel that gives the options to preview and save the detected particles,
     * and also a scroll bar to navigate through the slices of the movie <br>
     * Buttons and scrollbar created here use this PreviewInterface previewHandler as <code>ActionListener</code> and <code>AdjustmentListener</code>
     * 
     * @return the preview panel
     */
    public static Panel makePreviewPanel(final PreviewInterface previewHandler, final ImagePlus img) {

        final Panel preview_panel = new Panel();
        final GridBagLayout gridbag = new GridBagLayout();
        final GridBagConstraints c = new GridBagConstraints();
        preview_panel.setLayout(gridbag);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;

        /* scroll bar to navigate through the slices of the movie */
        final Scrollbar preview_scrollbar = new Scrollbar(Scrollbar.HORIZONTAL, img.getCurrentSlice(), 1, 1, img.getStackSize() + 1);
        preview_scrollbar.addAdjustmentListener(new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                // set the current visible slice to the one selected on the bar
                img.setSlice(preview_scrollbar.getValue());
            }
        });
        preview_scrollbar.setUnitIncrement(1);
        preview_scrollbar.setBlockIncrement(1);

        /* button to generate preview of the detected particles */
        final Button preview = new Button("Preview Detected");
        preview.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                previewHandler.preview(e);
            }
        });

        /* button to save the detected particles */
        final Button save_detected = new Button("Save Detected");
        save_detected.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                previewHandler.saveDetected(e);
            }
        });
        final Label seperation = new Label("______________", Label.CENTER);
        final Label previewLabel = new Label("");
        gridbag.setConstraints(preview, c);
        preview_panel.add(preview);
        gridbag.setConstraints(preview_scrollbar, c);
        preview_panel.add(preview_scrollbar);
        gridbag.setConstraints(save_detected, c);
        preview_panel.add(save_detected);
        gridbag.setConstraints(previewLabel, c);
        preview_panel.add(previewLabel);
        gridbag.setConstraints(seperation, c);
        preview_panel.add(seperation);
        return preview_panel;
    }
}
