package mosaic.core.GUI;


import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

import ij.macro.Interpreter;


/**
 * It is a Dialog with a progress bar
 *
 * @author Pietro Incardona
 */

public class ProgressBarWin  {
    private JDialog jd;
    private JPanel contentPane;
    private JProgressBar Prog_s;
    private JLabel Status;

    public void SetStatusMessage(String Message) {
        if (Interpreter.batchMode == true) return;
        Status.setText(Message);

        // Measure the text
        final Font font = Status.getFont();

        // get metrics from the graphics
        if (contentPane.getGraphics() == null) {
            jd.setVisible(true);
        }

        final FontMetrics metrics = contentPane.getGraphics().getFontMetrics(font);
        // get the height of a line of text in this
        // font and render context
        final int hgt = metrics.getHeight();
        // get the advance of my text in this font
        // and render context
        final int adv = metrics.stringWidth(Message);
        // calculate the size of a box to hold the
        // text with some padding.
        final Dimension size = new Dimension(adv + 2, hgt + 2);

        jd.setSize(2 * size.width + 50, jd.getSize().height);
    }

    public void SetProgress(int p) {
        if (Interpreter.batchMode == true) return;
        Prog_s.setString(((Integer) p).toString() + " %");
        Prog_s.setValue(p);
    }

    public void dispose() {
        if (Interpreter.batchMode == true) return;
        jd.dispose();
    }
    
    public ProgressBarWin() {
        if (Interpreter.batchMode == true) return;
        jd = new JDialog();
        jd.setTitle("Processing...");
        jd.setBounds(100, 100, 400, 100);
        jd.setVisible(true);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        jd.setContentPane(contentPane);
        contentPane.setLayout(new GridBagLayout());

        // Status message
        final JPanel contentMessage = new JPanel();
        contentMessage.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentMessage.setLayout(new GridLayout(0, 2, 0, 0));
        JLabel lblNewLabel = new JLabel("Status: ");
        contentMessage.add(lblNewLabel);
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        contentPane.add(contentMessage, c);

        lblNewLabel = new JLabel("Warming up");
        Status = lblNewLabel;
        Status.setSize(new Dimension(50, 300));
        contentMessage.add(lblNewLabel);

        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JProgressBar pbar = new JProgressBar();
        pbar.setStringPainted(true);
        Prog_s = pbar;
        contentPane.add(pbar, c);
    }
}
