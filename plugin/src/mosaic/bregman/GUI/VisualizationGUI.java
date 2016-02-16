package mosaic.bregman.GUI;


import java.awt.Button;
import java.awt.Font;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.gui.GenericDialog;
import mosaic.bregman.BLauncher;
import mosaic.bregman.output.CSVOutput;
import mosaic.bregman.output.SquasshOutputChoose;


class VisualizationGUI {
    
    public static int getParameters() {
        final GenericDialog gd = new GenericDialog("Visualization and output options");
        gd.setInsets(-10, 0, 3);
        
        final Font bf = new Font(null, Font.BOLD, 12);
        gd.addMessage("Visualization and output", bf);

        final String sgroup2[] = { "Intermediate_steps", "Colored_objects", "Objects_intensities", "Labeled_objects", "Outlines_overlay", "Soft_Mask", "Save_objects_characteristics", };
        final boolean bgroup2[] = { BLauncher.iParameters.livedisplay, BLauncher.iParameters.dispcolors, BLauncher.iParameters.dispint, BLauncher.iParameters.displabels, BLauncher.iParameters.dispoutline, BLauncher.iParameters.dispSoftMask, BLauncher.iParameters.save_images };
        gd.addCheckboxGroup(3, 3, sgroup2, bgroup2);

        final Button b = new Button("Output options");
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                final OutputGUI og = new OutputGUI();
                CSVOutput.occ = (SquasshOutputChoose) og.visualizeOutput(CSVOutput.oc, 1 /* oc_s */);
            }
        });
        gd.add(b);
        
        gd.addMessage("    R script data analysis settings", bf);
        gd.addNumericField("Number of conditions", BLauncher.iParameters.nbconditions, 0);

        final Panel p = new Panel();
        final Button rscript = new Button("Set condition names and number of images per condition");
        p.add(rscript);
        rscript.addActionListener(new RScriptListener(gd, 0, 0));

        gd.addPanel(p);
        gd.centerDialog(false);

        // Visualization
        if (GenericGUI.bypass_GUI == false) {
            gd.showDialog();
            if (gd.wasCanceled()) {
                return -1;
            }

            BLauncher.iParameters.livedisplay = gd.getNextBoolean();
            BLauncher.iParameters.dispcolors = gd.getNextBoolean();
            BLauncher.iParameters.dispint = gd.getNextBoolean();
            BLauncher.iParameters.displabels = gd.getNextBoolean();
            BLauncher.iParameters.dispoutline = gd.getNextBoolean();
            BLauncher.iParameters.dispSoftMask = gd.getNextBoolean();
            BLauncher.iParameters.save_images = gd.getNextBoolean();
        }

        BLauncher.iParameters.nbconditions = (int) gd.getNextNumber();

        return 0;
    }
}
