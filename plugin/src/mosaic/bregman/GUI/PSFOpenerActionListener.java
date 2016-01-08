package mosaic.bregman.GUI;


import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.gui.GenericDialog;


class PSFOpenerActionListener implements ActionListener {
    private final GenericDialog gd;

    PSFOpenerActionListener(GenericDialog gd) {
        this.gd = gd;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Point p = gd.getLocationOnScreen();
        new PSFWindow(p.x, p.y, gd);
    }
}
