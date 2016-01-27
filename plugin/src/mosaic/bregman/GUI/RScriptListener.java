package mosaic.bregman.GUI;


import ij.gui.GenericDialog;

import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import mosaic.bregman.Analysis;


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
            Analysis.iParameters.nbimages = new int[nbgroups];
            for (int i = 0; i < nbgroups; i++) {
                Analysis.iParameters.nbimages[i] = 1;
            }
            
            Analysis.iParameters.groupnames = new String[nbgroups];
            for (int i = 0; i < nbgroups; i++) {
                Analysis.iParameters.groupnames[i] = "Condition " + (i + 1) + " name";
            }
            
            Analysis.iParameters.nbconditions = nbgroups;
        }

        new RScriptWindow(nbgroups, posx, posy).run();
    }
}
