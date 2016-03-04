package mosaic.bregman.GUI;


import java.awt.Font;

import ij.gui.GenericDialog;
import mosaic.bregman.Parameters;


class RScriptWindow {

    public static int getParameters(int nbgroups, int ParentPosx, int ParentPosy, Parameters aParameters) {
        final Font bf = new Font(null, Font.BOLD, 12);

        final GenericDialog gd = new GenericDialog("Visualization and output options");
        gd.setInsets(-10, 0, 3);
        
        gd.addMessage("Channel names", bf);

        gd.addStringField("Channel_1", aParameters.ch1, 20);
        gd.addStringField("Channel_2", aParameters.ch2, 20);

        gd.addMessage("Number of images per condition", bf);
        for (int i = 0; i < nbgroups; i++) {
            gd.addNumericField("Conditon_" + (i + 1), aParameters.nbimages[i], 0);
        }

        gd.addMessage("Condition names", bf);
        for (int i = 0; i < nbgroups; i++) {
            gd.addStringField("Conditon_" + (i + 1), aParameters.groupnames[i], 20);
        }

        gd.centerDialog(false);
        gd.setLocation(ParentPosx + 20, ParentPosy + 20);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return -1;
        }

        aParameters.ch1 = gd.getNextString();
        aParameters.ch2 = gd.getNextString();
        for (int i = 0; i < nbgroups; i++) {
            aParameters.groupnames[i] = gd.getNextString();
        }
        for (int i = 0; i < nbgroups; i++) {
            aParameters.nbimages[i] = (int) gd.getNextNumber();
        }
        return 0;
    }
}
