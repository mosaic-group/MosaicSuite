package mosaic.bregman.GUI;


import ij.ImagePlus;
import ij.gui.GenericDialog;

import java.awt.Button;
import java.awt.Font;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import mosaic.bregman.Analysis;
import mosaic.bregman.GenericGUI;


public class BackgroundSubGUI
{
    ImagePlus imgch1;
    ImagePlus imgch2;
    int ni,nj,nz,nc;
    int posx, posy;

    public BackgroundSubGUI(int ParentPosx, int ParentPosy)
    {
        posx= ParentPosx+20;
        posy= ParentPosy+20;
    }


    public void run(String arg)
    {
        getParameters();
    }

    static public int getParameters()
    {
        final GenericDialog gd = new GenericDialog("Background subtractor options");

        Font bf = new Font(null, Font.BOLD,12);

        gd.setInsets(-10,0,3);
        gd.addMessage("Background subtractor",bf);

        Panel p = new Panel();
        Button help_b = new Button("help");

        p.add(help_b);

        help_b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                Point p =gd.getLocationOnScreen();

                new BackgroundSubHelp(p.x,p.y);

            }});

        gd.addPanel(p);

        gd.addCheckbox("Remove_background", Analysis.p.removebackground);

        gd.addNumericField("rolling_ball_window_size_(in_pixels)", Analysis.p.size_rollingball,0);

        if (GenericGUI.bypass_GUI == false)
        {
            gd.showDialog();
            if (gd.wasCanceled()) {
                return -1;
            }

            //general options
            Analysis.p.removebackground=gd.getNextBoolean();

            //IJ.log("rem back:" +  Analysis.p.removebackground);
            Analysis.p.size_rollingball=(int) gd.getNextNumber();
        }
        //Analysis.p.usePSF=gd.getNextBoolean();
        return 0;
    }

}
