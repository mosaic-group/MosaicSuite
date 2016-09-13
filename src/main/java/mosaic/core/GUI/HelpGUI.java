package mosaic.core.GUI;


import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * Class to create help windows + link to respective documentation
 * @author Pietro Incardona
 */
public class HelpGUI {

    private JPanel pref;
    private int gridy = 0;

    protected void setPanel(JPanel pref_) {
        pref = pref_;
    }

    protected void setHelpTitle(String title) {
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = gridy;
        c.gridwidth = 2;
        pref.add(new JLabel("<html>" + "<h1> " + title + " </h1>" + "</html>"), c);

        gridy++;
    }

    protected void createArticle(final String link) {
        if (link == null) {
            return;
        }

        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = gridy;
        c.anchor = GridBagConstraints.CENTER;
        pref.add(new JLabel("<html>Article: </html>"), c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        final JButton button = createLinkButton("<html><font color=\"blue\">click here</font></hmtl>", link);
        pref.add(button, c);

        gridy++;
    }

    protected void createTutorial(final String link) {
        if (link == null) {
            return;
        }

        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = gridy;
        c.gridwidth = 1;
        pref.add(new JLabel("<html>Tutorial: </html>"), c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        final JButton button = createLinkButton("<html><font color=\"blue\">click here</font></hmtl>", link);
        pref.add(button, c);

        gridy++;
    }

    protected void createSection(String sc, final String link) {
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = gridy;

        if (link == null) {
            c.gridwidth = 2;
            pref.add(new JLabel("<html><h1> " + sc + " </h1>"), c);
        }
        else {
            c.gridwidth = 1;
            pref.add(new JLabel("<html><h1> " + sc + " </h1>"), c);

            c.gridx = 1;
            final JButton button = createLinkButton("<html><font color=\"blue\">more info</font></hmtl>", link);
            pref.add(button, c);
        }
        
        gridy++;
    }

    protected void createField(String fld, String desc, final String link) {
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = gridy;
        c.gridwidth = 2;
        pref.add(new JLabel("<html>" + "<h2> <font color=\"red\"> " + fld + " </font></h2>" + "<div style=\"width:400px\">" + desc + "</div>"), c);

        gridy++;

        if (link != null) {
            c.gridx = 0;
            c.gridy = gridy;
            c.gridwidth = 2;
            final JButton button = createLinkButton("<html><font color=\"blue\">more info</font></hmtl>", link);
            pref.add(button, c);
        }

        gridy++;
    }
    
    private JButton createLinkButton(String buttonStr, final String link) {
        final JButton button = new JButton(buttonStr);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setBackground(Color.GRAY);
        button.setToolTipText(link);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                open(link);
            }
        });
        
        return button;
    }

    protected static void open(String aUriString) {
        try {
            open(new URI(aUriString));
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static void open(URI uri) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(uri);
            }
            catch (final IOException e) { /* Intentionally ignored - nothing to do in such case */}
        }
    }
}
