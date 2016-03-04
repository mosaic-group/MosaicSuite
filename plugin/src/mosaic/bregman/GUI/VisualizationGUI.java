package mosaic.bregman.GUI;


import java.awt.Font;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.gui.GenericDialog;
import mosaic.bregman.Parameters;
import mosaic.bregman.output.CSVOutput;
import mosaic.bregman.output.SquasshOutputChoose;


class VisualizationGUI {
    
    public static int getParameters(Parameters aParameters) {
        final GenericDialog gd = new GenericDialog("Visualization and output options");
        gd.setInsets(-10, 0, 3);
        
        final Font bf = new Font(null, Font.BOLD, 12);
        gd.addMessage("Visualization and output", bf);

        final String sgroup2[] = { "Intermediate_steps", "Colored_objects", "Objects_intensities", "Labeled_objects", "Outlines_overlay", "Soft_Mask", "Save_objects_characteristics", };
        final boolean bgroup2[] = { aParameters.livedisplay, aParameters.dispcolors, aParameters.dispint, aParameters.displabels, aParameters.dispoutline, aParameters.dispSoftMask, aParameters.save_images };
        gd.addCheckboxGroup(3, 3, sgroup2, bgroup2);

        final Panel p1 = new Panel();
        GenericGUI.addButton(p1, "Output options", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                final OutputGUI og = new OutputGUI();
                CSVOutput.occ = (SquasshOutputChoose) og.visualizeOutput(CSVOutput.oc, 1 /* oc_s */);
            }
        });
        gd.add(p1);
        
        gd.addMessage("    R script data analysis settings", bf);
        gd.addNumericField("Number of conditions", aParameters.nbconditions, 0);

        final Panel p2 = new Panel();
        GenericGUI.addButton(p2, "Set condition names and number of images per condition", new RScriptListener(gd, 0, 0, aParameters));
        gd.addPanel(p2);
        
        
        gd.centerDialog(false);

        // Visualization
        gd.showDialog();
        if (gd.wasCanceled()) {
            return -1;
        }

        aParameters.livedisplay = gd.getNextBoolean();
        aParameters.dispcolors = gd.getNextBoolean();
        aParameters.dispint = gd.getNextBoolean();
        aParameters.displabels = gd.getNextBoolean();
        aParameters.dispoutline = gd.getNextBoolean();
        aParameters.dispSoftMask = gd.getNextBoolean();
        aParameters.save_images = gd.getNextBoolean();

        aParameters.nbconditions = (int) gd.getNextNumber();
        return 0;
    }
    
    static class RScriptListener implements ActionListener {

        private final GenericDialog gd;
        private final int posx;
        private final int posy;
        private final Parameters iParameters;
        
        public RScriptListener(GenericDialog gd, int ParentPosx, int ParentPosy, Parameters aParameters) {
            this.gd = gd;
            posx = ParentPosx;
            posy = ParentPosy;
            iParameters = aParameters;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int nbgroups = new Integer(((TextField) gd.getNumericFields().elementAt(0)).getText());
            if (nbgroups > 1) {
                iParameters.nbimages = new int[nbgroups];
                for (int i = 0; i < nbgroups; i++) {
                    iParameters.nbimages[i] = 1;
                }
                
                iParameters.groupnames = new String[nbgroups];
                for (int i = 0; i < nbgroups; i++) {
                    iParameters.groupnames[i] = "Condition " + (i + 1) + " name";
                }
                
                iParameters.nbconditions = nbgroups;
            }

            RScriptWindow.getParameters(nbgroups, posx, posy, iParameters);
        }
    }
}
