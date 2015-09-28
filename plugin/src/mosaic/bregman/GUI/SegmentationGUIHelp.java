package mosaic.bregman.GUI;


import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

import javax.swing.JDialog;
import javax.swing.JPanel;

import mosaic.core.GUI.HelpGUI;


class SegmentationGUIHelp extends HelpGUI {

    private final JDialog frame;
    private final JPanel panel;

    SegmentationGUIHelp(int x, int y) {
        frame = new JDialog();
        frame.setTitle("Segmentation Help");
        frame.setSize(500, 620);
        frame.setLocation(x + 500, y - 50);
        frame.setModal(true);

        panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setPreferredSize(new Dimension(500, 620));

        final JPanel pref = new JPanel(new GridBagLayout());

        setPanel(pref);
        setHelpTitle("Segmentation");

        String desc = new String("Set a regularization parameter for the segmentation. Use higher values"
                + "to avoid segmenting noise- induced small intensity peaks values are between 0.05 and 0.25.");

        createField("Regularization parameter", desc, null);

        desc = new String("Set the threshold for the minimum object intensity to be considered. Intensity values are normalized"
                + "between 0 for the smallest value occurring in the image and 1 for the largest value");

        createField("Cell mask thresholding", desc, null);

        desc = new String("compute segmentations with sub-pixel resolution." + "The resolution of the segmentation is increased " + "by an over-sampling factor of 8 for 2D images and "
                + "4 for 3D images.");

        createField("Sub-pixel segmentation", desc, null);

        desc = new String("Noise and intensity models");

        createField("Noise model", desc, null);

        desc = new String("Set the microscope PSF. In order to correct for diffraction blur, " + "the software needs information about the PSF of the microscope. "
                + "This can be done in either of the following two ways: " + "a)  Use a theoretical PSF model. Use Estimate PSF " + "b)  Use  Measure the microscope PSF from images of "
                + "fluorescent sub-diffraction beads. " + "Use the menu item Plugins → Mosaic → PSF Tool to measure " + "these parameters from images of beads.");

        createField("PSF", desc, null);

        panel.add(pref);

        frame.add(panel);

        frame.setVisible(true);

    }
}
