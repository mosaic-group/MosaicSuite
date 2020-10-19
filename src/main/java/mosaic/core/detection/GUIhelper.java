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
    private static boolean isClijAvailable() {
        try {
            net.haesleinhuepf.clij2.CLIJ2.getInstance();
            return true;
        }
        catch (final NoClassDefFoundError err) {
            return false;
        }
    }

    /**
     * gd has to be shown with showDialog and handles the fields added to the dialog
     * with addUserDefinedParamtersDialog(gd).
     * 
     * @param gd <code>GenericDialog</code> at which the UserDefinedParameter fields where added.
     * @return true if user changed the parameters and false if the user didn't changed them.
     */
    public static boolean getUserDefinedParameters(GenericDialog gd, FeaturePointDetector fpd) {
        final int rad = (int) gd.getNextNumber();
        final double cut = gd.getNextNumber();
        final float per = ((float) gd.getNextNumber()) / 100;
        final float intThreshold = per * 100;
        final boolean absolute = gd.getNextBoolean();
        final boolean useCLIJ = isClijAvailable() && gd.getNextBoolean();

        return fpd.setDetectionParameters(cut, per, rad, intThreshold, absolute, useCLIJ);
    }

    /**
     * Gets user defined params that are necessary to display the preview of particle detection.
     * This is the only way to re-read values without touching internal GenericDialgo counters
     */
    public static Boolean getUserDefinedPreviewParams(GenericDialog gd, FeaturePointDetector fpd) {

        @SuppressWarnings("unchecked")
        final Vector<TextField> vec = gd.getNumericFields();
        @SuppressWarnings("unchecked")
        final Vector<Checkbox> vecb = gd.getCheckboxes();
        
        final int rad = Integer.parseInt((vec.elementAt(0)).getText());
        final double cut = Double.parseDouble((vec.elementAt(1)).getText());
        final float per = (Float.parseFloat((vec.elementAt(2)).getText())) / 100;
        final float intThreshold = per * 100;
        final boolean absolute = vecb.elementAt(0).getState();
        final boolean useClij = isClijAvailable() && vecb.elementAt(1).getState();

        return fpd.setDetectionParameters(cut, per, rad, intThreshold, absolute, useClij);
    }
    
    public static void addUserDefinedParametersDialog(GenericDialog gd, FeaturePointDetector fpd) {
        gd.addMessage("Particle Detection:");
        // These 3 params are only relevant for non text_files_mode
        gd.addNumericField("Radius", fpd.getRadius(), 0, 7, null);
        gd.addNumericField("Cutoff [0-1]", fpd.getCutoff(), 3, 7, null);
        gd.addNumericField("Per/Abs", fpd.getPercentile() * 100, 3, 7, null);

        gd.addCheckbox("Absolute", fpd.getThresholdMode() == FeaturePointDetector.Mode.ABS_THRESHOLD_MODE);
        if (isClijAvailable()) {
            gd.addCheckbox("Accelerate_with_CLIJ2 (experimental)", false);
        }
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
        preview_panel.setLayout(gridbag);
        
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;

        int[] position = img.convertIndexToPosition(img.getCurrentSlice());
        
        // scroll bar to navigate through the frames of the movie
        final Scrollbar preview_scrollbar = new Scrollbar(Scrollbar.HORIZONTAL, position[2], 1, 1, img.getNFrames() + 1);
        preview_scrollbar.addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                // set the current visible slice to the one selected on the bar
                int[] position = img.convertIndexToPosition(img.getCurrentSlice());
                img.setPosition(position[0], position[1],preview_scrollbar.getValue());
            }
        });
        preview_scrollbar.setUnitIncrement(1);
        preview_scrollbar.setBlockIncrement(1);

        // Add second scrollbar for depth only if we have 3D image.
        final Scrollbar preview_scrollbarZ;
        final int nDepth = img.getNSlices();
        if (nDepth > 1) {
            preview_scrollbarZ = new Scrollbar(Scrollbar.HORIZONTAL, position[1], 1, 1, img.getNSlices() + 1);
            preview_scrollbarZ.addAdjustmentListener(new AdjustmentListener() {
                @Override
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    // set the current visible slice to the one selected on the bar
                    //                img.setSlice(preview_scrollbar.getValue());
                    int[] position = img.convertIndexToPosition(img.getCurrentSlice());
                    img.setPosition(position[0], preview_scrollbarZ.getValue(), position[2]);
                }
            });
            preview_scrollbarZ.setUnitIncrement(1);
            preview_scrollbarZ.setBlockIncrement(1);
        } else {
            preview_scrollbarZ = null;
        }
            
        // button to generate preview of the detected particles
        final Button preview = new Button("Preview Detected");
        preview.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] position = img.convertIndexToPosition(img.getCurrentSlice());
                previewHandler.preview(e, position[1]);
            }
        });

        // button to save the detected particles
        final Button save_detected = new Button("Save Detected");
        save_detected.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                previewHandler.saveDetected(e);
            }
        });
        
        gridbag.setConstraints(preview, c);
        preview_panel.add(preview);
        gridbag.setConstraints(preview_scrollbar, c);
        preview_panel.add(preview_scrollbar);
        if (nDepth > 1) {
            gridbag.setConstraints(preview_scrollbarZ, c);
            preview_panel.add(preview_scrollbarZ);
        }
        gridbag.setConstraints(save_detected, c);
        preview_panel.add(save_detected);
        final Label previewLabel = new Label("");
        gridbag.setConstraints(previewLabel, c);
        preview_panel.add(previewLabel);
        final Label seperation = new Label("______________", Label.CENTER);
        gridbag.setConstraints(seperation, c);
        preview_panel.add(seperation);
        
        return preview_panel;
    }
}
