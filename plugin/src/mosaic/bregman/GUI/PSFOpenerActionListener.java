package mosaic.bregman.GUI;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import mosaic.bregman.GenericDialogCustom;

public class PSFOpenerActionListener implements ActionListener
{
    GenericDialogCustom gd;

    public PSFOpenerActionListener(GenericDialogCustom gd)
    {
        this.gd=gd;
        //this.ta=ta;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Point p =gd.getLocationOnScreen();
        //IJ.log("plugin location :" + p.toString());
        new PSFWindow(p.x, p.y, gd);
    }
}