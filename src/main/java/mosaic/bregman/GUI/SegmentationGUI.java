package mosaic.bregman.GUI;


import java.awt.Font;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import mosaic.bregman.Parameters;


class SegmentationGUI {
    static int getParameters(final Parameters aParameters) {
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
        
        gd.addNumericField("Regularization_(>0)_ch1", aParameters.lreg_[0], 3);
        gd.addNumericField("Regularization_(>0)_ch2", aParameters.lreg_[1], 3);

        gd.addNumericField("Minimum_object_intensity_channel_1_(0_to_1)", aParameters.min_intensity, 3);
        gd.addNumericField("                        _channel_2_(0_to_1)", aParameters.min_intensityY, 3);

        gd.addCheckbox("Subpixel_segmentation", aParameters.subpixel);
        gd.addCheckbox("Exclude_Z_edge", aParameters.exclude_z_edges);

        final String choice1[] = { "Automatic", "Low", "Medium", "High" };
        gd.addChoice("Local_intensity_estimation ", choice1, choice1[aParameters.mode_intensity]);

        final String choice2[] = { "Poisson", "Gauss" };
        gd.addChoice("Noise_Model ", choice2, choice2[aParameters.noise_model]);

        gd.addMessage("PSF model (Gaussian approximation)", bf);

        gd.addNumericField("standard_deviation_xy (in pixels)", aParameters.sigma_gaussian, 2);
        gd.addNumericField("standard_deviation_z  (in pixels)", aParameters.sigma_gaussian / aParameters.zcorrec, 2);

        gd.addMessage("Region filter", bf);
        gd.addNumericField("Remove_region_with_intensities_<", aParameters.min_region_filter_intensities, 0);
        gd.addNumericField("Remove_region_with_size_<", aParameters.min_region_filter_size, 0, 6, "pixels");

        Panel p = new Panel();
        GenericGUI.addButton(p, "Patch position", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                final OpenDialog od = new OpenDialog("(Patch file", "");
                final String directory = od.getDirectory();
                final String name = od.getFileName();
                if (directory != null && name != null) aParameters.patches_from_file = directory + name;
                else aParameters.patches_from_file = null;
            }

        });
        gd.addPanel(p);
        
        Panel p2 = new Panel();
        GenericGUI.addButton(p2, "Objects Mask (2D)", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                final OpenDialog od = new OpenDialog("(Mask file", "");
                final String directory = od.getDirectory();
                final String name = od.getFileName();
                if (directory != null && name != null) aParameters.mask_from_file = directory + name;
                else aParameters.mask_from_file = null;
            }

        });
        gd.addPanel(p2);

        p = new Panel();
        GenericGUI.addButton(p, "Estimate PSF from objective properties", new PSFOpenerActionListener(gd));
        gd.addPanel(p);

        gd.centerDialog(false);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return -1;
        }

        aParameters.lreg_[0] = gd.getNextNumber();
        aParameters.lreg_[1] = gd.getNextNumber();
        aParameters.min_intensity = gd.getNextNumber();
        aParameters.min_intensityY = gd.getNextNumber();
        aParameters.subpixel = gd.getNextBoolean();
        aParameters.exclude_z_edges = gd.getNextBoolean();
        aParameters.sigma_gaussian = gd.getNextNumber();
        aParameters.zcorrec = aParameters.sigma_gaussian / gd.getNextNumber();
        aParameters.min_region_filter_intensities = gd.getNextNumber();
        aParameters.min_region_filter_size = (int) gd.getNextNumber();
        aParameters.mode_intensity = gd.getNextChoiceIndex();
        aParameters.noise_model = gd.getNextChoiceIndex();

        return 0;
    }
}
