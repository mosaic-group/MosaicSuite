package mosaic.bregman.GUI;


import java.awt.Button;
import java.awt.Font;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.gui.GenericDialog;
import mosaic.bregman.BLauncher;


class BackgroundSubGUI {
    public static int getParameters() {
        final GenericDialog gd = new GenericDialog("Background subtractor options");
        gd.setInsets(-10, 0, 3);
        
        gd.addMessage("Background subtractor", new Font(null, Font.BOLD, 12));

        final Button help_b = new Button("help");
        help_b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                final Point p = gd.getLocationOnScreen();
                new BackgroundSubHelp(p.x, p.y);
            }
        });
        
        final Panel p = new Panel();
        p.add(help_b);
        gd.addPanel(p);

        gd.addCheckbox("Remove_background", BLauncher.iParameters.removebackground);
        gd.addNumericField("rolling_ball_window_size_(in_pixels)", BLauncher.iParameters.size_rollingball, 0);

        if (GenericGUI.bypass_GUI == false) {
            gd.showDialog();
            if (gd.wasCanceled()) {
                return -1;
            }

            BLauncher.iParameters.removebackground = gd.getNextBoolean();
            BLauncher.iParameters.size_rollingball = (int) gd.getNextNumber();
        }
        
        return 0;
    }
}
