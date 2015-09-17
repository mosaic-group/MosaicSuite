package mosaic.bregman.GUI;


import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import mosaic.core.GUI.HelpGUI;


public class SegmentationGUIHelp extends HelpGUI implements ActionListener {

    public JDialog frame;
    // Initialize Buttons
    private JPanel panel;
    private JButton Close;

    public SegmentationGUIHelp(int x, int y) {
        frame = new JDialog();
        frame.setTitle("Segmentation Help");
        frame.setSize(500, 620);
        frame.setLocation(x + 500, y - 50);
        frame.setModal(true);
        // frame.toFront();
        // frame.setResizable(false);
        // frame.setAlwaysOnTop(true);

        panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setPreferredSize(new Dimension(500, 620));

        JPanel pref = new JPanel(new GridBagLayout());
        // pref.setPreferredSize(new Dimension(555, 550));
        // pref.setSize(pref.getPreferredSize());

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

        // JPanel panel = new JPanel(new BorderLayout());

        panel.add(pref);
        // panel.add(label, BorderLayout.NORTH);

        frame.add(panel);

        // frame.repaint();

        frame.setVisible(true);
        // frame.requestFocus();
        // frame.setAlwaysOnTop(true);

        // JOptionPane.showMessageDialog(frame,
        // "Eggs are not supposed to be green.\n dsfdsfsd",
        // "A plain message",
        // JOptionPane.PLAIN_MESSAGE);

    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        Object source = ae.getSource(); // Identify Button that was clicked

        if (source == Close) {
            // IJ.log("close called");
            frame.dispose();
        }

    }

}
