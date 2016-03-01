package mosaic.bregman.GUI;


import java.awt.Font;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import mosaic.bregman.BLauncher;


class SegmentationGUI {
    static int getParameters() {
        final Font bf = new Font(null, Font.BOLD, 12);

        final GenericDialog gd = new GenericDialog("Segmentation options");
        gd.setInsets(-10, 0, 3);
        gd.addMessage("    Segmentation parameters ", bf);

        final Panel pp = new Panel();
        GenericGUI.addButton(pp, "help", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                new SegmentationGUIHelp(gd.getLocationOnScreen().x, gd.getLocationOnScreen().y);
            }
        });
        gd.addPanel(pp);
        
        gd.addNumericField("Regularization_(>0)_ch1", BLauncher.iParameters.lreg_[0], 3);
        gd.addNumericField("Regularization_(>0)_ch2", BLauncher.iParameters.lreg_[1], 3);

        gd.addNumericField("Minimum_object_intensity_channel_1_(0_to_1)", BLauncher.iParameters.min_intensity, 3);
        gd.addNumericField("                        _channel_2_(0_to_1)", BLauncher.iParameters.min_intensityY, 3);

        gd.addCheckbox("Subpixel_segmentation", BLauncher.iParameters.subpixel);
        gd.addCheckbox("Exclude_Z_edge", BLauncher.iParameters.exclude_z_edges);

        final String choice1[] = { "Automatic", "Low", "Medium", "High" };
        gd.addChoice("Local_intensity_estimation ", choice1, choice1[BLauncher.iParameters.mode_intensity]);

        final String choice2[] = { "Poisson", "Gauss" };
        gd.addChoice("Noise_Model ", choice2, choice2[BLauncher.iParameters.noise_model]);

        gd.addMessage("PSF model (Gaussian approximation)", bf);

        gd.addNumericField("standard_deviation_xy (in pixels)", BLauncher.iParameters.sigma_gaussian, 2);
        gd.addNumericField("standard_deviation_z  (in pixels)", BLauncher.iParameters.sigma_gaussian / BLauncher.iParameters.zcorrec, 2);

        gd.addMessage("Region filter", bf);
        gd.addNumericField("Remove_region_with_intensities_<", BLauncher.iParameters.min_region_filter_intensities, 0);

        Panel p = new Panel();
        GenericGUI.addButton(p, "Patch position", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                final OpenDialog od = new OpenDialog("(Patch file", "");
                final String directory = od.getDirectory();
                final String name = od.getFileName();
                if (directory != null && name != null) BLauncher.iParameters.patches_from_file = directory + name;
            }

        });
        gd.addPanel(p);

        p = new Panel();
        GenericGUI.addButton(p, "Estimate PSF from objective properties", new PSFOpenerActionListener(gd));
        gd.addPanel(p);

        gd.centerDialog(false);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return -1;
        }

        BLauncher.iParameters.lreg_[0] = gd.getNextNumber();
        BLauncher.iParameters.lreg_[1] = gd.getNextNumber();
        BLauncher.iParameters.min_intensity = gd.getNextNumber();
        BLauncher.iParameters.min_intensityY = gd.getNextNumber();
        BLauncher.iParameters.subpixel = gd.getNextBoolean();
        BLauncher.iParameters.exclude_z_edges = gd.getNextBoolean();
        BLauncher.iParameters.sigma_gaussian = gd.getNextNumber();
        BLauncher.iParameters.zcorrec = BLauncher.iParameters.sigma_gaussian / gd.getNextNumber();
        BLauncher.iParameters.min_region_filter_intensities = gd.getNextNumber();
        BLauncher.iParameters.mode_intensity = gd.getNextChoiceIndex();
        BLauncher.iParameters.noise_model = gd.getNextChoiceIndex();

        return 0;
    }
}
