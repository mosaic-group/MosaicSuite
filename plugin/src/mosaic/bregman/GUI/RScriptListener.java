package mosaic.bregman.GUI;


import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.gui.GenericDialog;
import mosaic.bregman.BLauncher;


class RScriptListener implements ActionListener {

    private final GenericDialog gd;
    private final int posx;
    private final int posy;

    public RScriptListener(GenericDialog gd, int ParentPosx, int ParentPosy) {
        this.gd = gd;
        posx = ParentPosx;
        posy = ParentPosy;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int nbgroups = new Integer(((TextField) gd.getNumericFields().elementAt(0)).getText());
        if (nbgroups > 5) {
            BLauncher.iParameters.nbimages = new int[nbgroups];
            for (int i = 0; i < nbgroups; i++) {
                BLauncher.iParameters.nbimages[i] = 1;
            }
            
            BLauncher.iParameters.groupnames = new String[nbgroups];
            for (int i = 0; i < nbgroups; i++) {
                BLauncher.iParameters.groupnames[i] = "Condition " + (i + 1) + " name";
            }
            
            BLauncher.iParameters.nbconditions = nbgroups;
        }

        new RScriptWindow(nbgroups, posx, posy).run();
    }
}
