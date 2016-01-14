package mosaic.bregman.GUI;


import java.awt.Button;
import java.awt.Font;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.gui.GenericDialog;
import mosaic.bregman.Analysis;
import mosaic.bregman.output.CSVOutput;
import mosaic.bregman.output.SquasshOutputChoose;


public class VisualizationGUI {
    public VisualizationGUI() {
    }

    public void run() {
        getParameters();
    }

    public static int getParameters() {
        final Font bf = new Font(null, Font.BOLD, 12);

        final GenericDialog gd = new GenericDialog("Visualization and output options");

        gd.setInsets(-10, 0, 3);
        gd.addMessage("Visualization and output", bf);

        final String sgroup2[] = { "Intermediate_steps", "Colored_objects", "Objects_intensities", "Labeled_objects", "Outlines_overlay", "Soft_Mask", "Save_objects_characteristics", };
        final boolean bgroup2[] = { false, false, false, false, false, false, false };

        bgroup2[0] = Analysis.p.livedisplay;
        bgroup2[1] = Analysis.p.dispcolors;
        bgroup2[2] = Analysis.p.dispint;
        bgroup2[3] = Analysis.p.displabels;
        bgroup2[4] = Analysis.p.dispoutline;
        bgroup2[5] = Analysis.p.dispSoftMask;
        bgroup2[6] = Analysis.p.save_images;

        gd.addCheckboxGroup(3, 3, sgroup2, bgroup2);

        final Button b = new Button("Output options");
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                final OutputGUI og = new OutputGUI();

                CSVOutput.occ = (SquasshOutputChoose) og.visualizeOutput(CSVOutput.oc, Analysis.p.oc_s);
            }

        });

        gd.add(b);
        gd.addMessage("    R script data analysis settings", bf);
        gd.addNumericField("Number of conditions", Analysis.p.nbconditions, 0);

        final Button rscript = new Button("Set condition names and number of images per condition");
        final Panel p = new Panel();
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

            Analysis.p.livedisplay = gd.getNextBoolean();
            Analysis.p.dispcolors = gd.getNextBoolean();
            Analysis.p.dispint = gd.getNextBoolean();
            Analysis.p.displabels = gd.getNextBoolean();
            Analysis.p.dispoutline = gd.getNextBoolean();
            Analysis.p.dispSoftMask = gd.getNextBoolean();
            Analysis.p.save_images = gd.getNextBoolean();
        }

        Analysis.p.nbconditions = (int) gd.getNextNumber();

        return 0;
    }
}
