package mosaic.bregman.GUI;


import java.awt.Button;
import java.awt.Font;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;

import ij.gui.GenericDialog;
import mosaic.bregman.Analysis;


class SegmentationGUI {
    void run() {
        getParameters();
    }

    static int getParameters() {
        final Font bf = new Font(null, Font.BOLD, 12);

        final GenericDialog gd = new GenericDialog("Segmentation options");
        gd.setInsets(-10, 0, 3);
        gd.addMessage("    Segmentation parameters ", bf);

        final Button help_b = new Button("help");
        help_b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final Point p = gd.getLocationOnScreen();
                new SegmentationGUIHelp(p.x, p.y);
            }
        });

        final Panel pp = new Panel();
        pp.add(help_b);
        gd.addPanel(pp);

        gd.addNumericField("Regularization_(>0)_ch1", Analysis.p.lreg_[0], 3);
        gd.addNumericField("Regularization_(>0)_ch2", Analysis.p.lreg_[1], 3);

        gd.addNumericField("Minimum_object_intensity_channel_1_(0_to_1)", Analysis.p.min_intensity, 3);
        gd.addNumericField("                        _channel_2_(0_to_1)", Analysis.p.min_intensityY, 3);

        gd.addCheckbox("Subpixel_segmentation", Analysis.p.subpixel);
        gd.addCheckbox("Exclude_Z_edge", Analysis.p.exclude_z_edges);

        final String choice1[] = { "Automatic", "Low", "Medium", "High" };
        gd.addChoice("Local_intensity_estimation ", choice1, choice1[Analysis.p.mode_intensity]);

        final String choice2[] = { "Poisson", "Gauss" };
        gd.addChoice("Noise_Model ", choice2, choice2[Analysis.p.noise_model]);

        gd.addMessage("PSF model (Gaussian approximation)", bf);

        gd.addNumericField("standard_deviation_xy (in pixels)", Analysis.p.sigma_gaussian, 2);
        gd.addNumericField("standard_deviation_z  (in pixels)", Analysis.p.sigma_gaussian / Analysis.p.zcorrec, 2);

        gd.addMessage("Region filter", bf);
        gd.addNumericField("Remove_region_with_intensities_<", Analysis.p.min_region_filter_intensities, 0);

        Panel p = new Panel();
        final Button b = new Button("Patch position");
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                fc.showOpenDialog(null);
                final File selFile = fc.getSelectedFile();
                Analysis.p.patches_from_file = selFile.getAbsolutePath();
            }

        });
        p.add(b);
        gd.addPanel(p);

        final Button bp = new Button("Estimate PSF from objective properties");
        bp.addActionListener(new PSFOpenerActionListener(gd));
        p = new Panel();
        p.add(bp);
        gd.addPanel(p);

        gd.centerDialog(false);

        if (GenericGUI.bypass_GUI == false) {
            gd.showDialog();
            if (gd.wasCanceled()) {
                return -1;
            }

            Analysis.p.lreg_[0] = gd.getNextNumber();
            Analysis.p.lreg_[1] = gd.getNextNumber();
            Analysis.p.min_intensity = gd.getNextNumber();
            Analysis.p.min_intensityY = gd.getNextNumber();
            Analysis.p.subpixel = gd.getNextBoolean();
            Analysis.p.exclude_z_edges = gd.getNextBoolean();
            Analysis.p.sigma_gaussian = gd.getNextNumber();
            Analysis.p.zcorrec = Analysis.p.sigma_gaussian / gd.getNextNumber();
            Analysis.p.min_region_filter_intensities = gd.getNextNumber();
            Analysis.p.mode_intensity = gd.getNextChoiceIndex();
            Analysis.p.noise_model = gd.getNextChoiceIndex();
        }

        Analysis.p.betaMLEindefault = 1;
        Analysis.p.regionthresh = Analysis.p.min_intensity;
        Analysis.p.regionthreshy = Analysis.p.min_intensityY;
        Analysis.p.refinement = true;
        Analysis.p.max_nsb = 151;
        Analysis.p.regionSegmentLevel = 1;// not used
        Analysis.p.minves_size = 2;

        if (!Analysis.p.subpixel) {
            Analysis.p.oversampling2ndstep = 1;
            Analysis.p.interpolation = 1;
        }

        return 0;
    }
}
