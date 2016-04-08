package mosaic.bregman.GUI;


import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

import javax.swing.JDialog;
import javax.swing.JPanel;

import mosaic.core.GUI.HelpGUI;


class BackgroundSubHelp extends HelpGUI {

    public BackgroundSubHelp(int x, int y) {
        JDialog frame = new JDialog();
        frame.setTitle("Background Sub Help");
        frame.setSize(500, 220);
        frame.setLocation(x + 500, y - 50);
        frame.setModal(true);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setPreferredSize(new Dimension(500, 220));

        final JPanel pref = new JPanel(new GridBagLayout());
        setPanel(pref);
        setHelpTitle("Background Subtraction");
        final String desc = new String("Reduce background fluorescence using the rolling ball algorithm " + "by selecting “Remove Background“ and entering the window edge-length "
                + "in units of pixels. This length should be large enough so that" + "a square with that edge length cannot fit inside the objects to be detected,"
                + " but is smaller than the length scale of background variations");
        createField("Background subtraction window size", desc, null);

        panel.add(pref);
        frame.add(panel);
        frame.setVisible(true);
    }
}
