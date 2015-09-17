package mosaic.bregman.GUI;

import ij.gui.GenericDialog;

import java.awt.Font;

import mosaic.bregman.Analysis;

class RScriptWindow {

    int nbgroups;
    int posx, posy;
    public RScriptWindow(int nbgroups, int ParentPosx, int ParentPosy)
    {
        this.nbgroups=nbgroups;
        posx= ParentPosx+20;
        posy= ParentPosy+20;
    }

    public void run(String arg)
    {
        Font bf = new Font(null, Font.BOLD,12);

        GenericDialog  gd = new GenericDialog("Visualization and output options");

        gd.setInsets(-10,0,3);
        gd.addMessage("Channel names",bf);

        gd.addStringField("Channel_1", Analysis.p.ch1, 20);
        gd.addStringField("Channel_2", Analysis.p.ch2, 20);

        gd.addMessage("Number of images per condition",bf);

        for (int i=0; i < nbgroups; i++)
        {
            gd.addNumericField("Conditon_" + (i+1), Analysis.p.nbimages[i], 0);
        }


        gd.addMessage("Condition names",bf);
        for (int i=0; i < nbgroups; i++)
        {
            gd.addStringField("Conditon_" + (i+1), Analysis.p.groupnames[i], 20);
        }

        gd.centerDialog(false);
        gd.setLocation(posx, posy);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        Analysis.p.ch1=gd.getNextString();
        Analysis.p.ch2=gd.getNextString();

        for (int i=0; i < nbgroups; i++)
        {
            Analysis.p.groupnames[i]=gd.getNextString();
        }

        for (int i=0; i < nbgroups; i++)
        {
            Analysis.p.nbimages[i]= (int) gd.getNextNumber();
        }

    }


}
