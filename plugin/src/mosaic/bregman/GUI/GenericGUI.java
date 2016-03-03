package mosaic.bregman.GUI;


import java.awt.Button;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.macro.Interpreter;
import mosaic.core.GUI.HelpGUI;


public class GenericGUI {
    private static final Logger logger = Logger.getLogger(GenericGUI.class);
    
    public enum RunMode {
        CLUSTER, LOCAL, STOP
    }
    
    // Input params
    protected ImagePlus iInputImage;

    // Read params
    private String iInputField = "";
    
    protected ImagePlus imgch2; // TODO: it is not used currently (never assigned)
    static boolean useGUI = true;
    
    public GenericGUI(ImagePlus aInputImg) {
        iInputImage = aInputImg;
        useGUI = !(IJ.isMacro() || Interpreter.batchMode);
    }

    /**
     * Draw the standard squassh main window
     * @param Active imagePlus
     * @return run mode, -1 when cancelled
     */
    public RunMode drawStandardWindow(String aImgPath, boolean aRunOnCluster) {
        iInputField = aImgPath;

        final GenericDialog gd = new GenericDialog("Squassh");
        gd.setInsets(-10, 0, 3);
        
        gd.addStringField("Input:", iInputField, 50);
    
        Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 75, 3));
        addButton(p, "Select File/Folder", new FileOpenerActionListener(gd.getTextArea1()));
        addButton(p, "Help", new HelpOpenerActionListener(gd));
        gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));
    
        p = new Panel();
        addLabel(p, "Background subtraction");
        addButton(p, "Options", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                BackgroundSubGUI.getParameters();
            }
        });
        gd.addPanel(p);
    
        p = new Panel();
        addLabel(p, "Segmentation parameters");
        addButton(p, "Options", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                SegmentationGUI.getParameters();
            }
        });        
        gd.addPanel(p);
    
        p = new Panel();
        addLabel(p, "Colocalization (two channels images)");
        addButton(p, "Options", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                final ColocalizationGUI gds = new ColocalizationGUI(iInputImage, imgch2);
                gds.run();
            }
        });  
        gd.addPanel(p);
    
        p = new Panel();
        addLabel(p, "Vizualization and output");
        addButton(p, "Options", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                VisualizationGUI.getParameters();
            }
        });
        gd.addPanel(p);
    
        gd.addCheckbox("Process on computer cluster", aRunOnCluster);
    
        p = new Panel();
        p.add(new JLabel("<html>Please refer to and cite:<br><br> G. Paul, J. Cardinale, and I. F. Sbalzarini.<br>" + "Coupling image restoration and segmentation:<br>"
                + "A generalized linear model/Bregman<br>" + "perspective. Int. J. Comput. Vis., 104(1):69–93, 2013.<br>" + "<br>" + "A. Rizk, G. Paul, P. Incardona, M. Bugarski, M. Mansouri,<br>"
                + "A. Niemann, U. Ziegler, P. Berger, and I. F. Sbalzarini.<br>" + "Segmentation and quantification of subcellular structures<br>"
                + "in fluorescence microscopy images using Squassh.<br>" + "Nature Protocols, 9(3):586–596, 2014. </html>"));
        gd.addPanel(p);
    
        gd.showDialog();
        if (gd.wasCanceled()) {
            return RunMode.STOP;
        }
    
        iInputField = gd.getNextString();
    
        RunMode runMode = (gd.getNextBoolean() == true) ? RunMode.CLUSTER : RunMode.LOCAL;
        
        return runMode;
    }

    public String getInput() { return iInputField; }
    
    private void addLabel(Panel p, String aLabel) {
        if (useGUI) {
            Label label = new Label(aLabel);
            label.setFont(new Font(null, Font.BOLD, 12));
            p.add(label);
        }
    }

    static void addButton(Panel aPanel, String aLabel, ActionListener aActionListener) {
        // Do not create buttons if not in GUI mode. It might be case of running on cluster with
        // headless mode and creating button will throw an exception. Anyway, in batch or macro mode
        // it is a pointless to have buttons since interaction with user is not possible.
        if (useGUI) {
            final Button b = new Button(aLabel);
            b.addActionListener(aActionListener);
            aPanel.add(b);
        }
    }

    private class FileOpenerActionListener implements ActionListener {
        TextArea ta;

        public FileOpenerActionListener(TextArea ta) {
            this.ta = ta;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (iInputImage != null) {
                iInputImage.close();
                iInputImage = null;
            }
            if (imgch2 != null) {
                imgch2.close();
                imgch2 = null;
            }// close previosuly opened images

            final JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.showOpenDialog(null);
            final File selFile = fc.getSelectedFile();
            if (selFile == null) {
                return;
            }
            String path = selFile.getAbsolutePath();
            final boolean isFile = selFile.isFile();
            if (isFile) {
                // should be checked with isSymbolicLink also (only in 1.7). Since  link to directory is considered as a file.
                final ImagePlus img2 = IJ.openImage(path);
                img2.show();
            }
            ta.setText(path);
        }
    }

    private class HelpOpenerActionListener implements ActionListener {
        GenericDialog gd;

        public HelpOpenerActionListener(GenericDialog gd) {
            this.gd = gd;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Point p = gd.getLocationOnScreen();
            new Helpwindow(p.x, p.y);
        }
    }

    private class Helpwindow extends HelpGUI {
        public Helpwindow(int x, int y) {
            final JPanel pref = new JPanel(new GridBagLayout());
            setPanel(pref);
            setHelpTitle("Squassh");
            createTutorial(null);
            createArticle("http://mosaic.mpi-cbg.de/docs/Paul2013a.pdf");
            String desc = new String("Background subtraction is performed first, as the segmentation model assumes locally "
                    + "homogeneous intensities. Background variations are non-specific signals that are not accounted for by "
                    + "this model. We subtract the image background using the rolling-ball algorithm");
            createField("Background subtraction", desc, null);
            desc = new String("Model-based segmentation aim at finding the segmentation that best explains the image. In other words, "
                    + "they compute the segmentation that has the highest probability of resulting in the actually observed " + "image when imaged with the specific microscope used.");
            createField("Segmentation", desc, null);
            desc = new String("Object-based colocalization is computed after segmenting the objects using information about the shapes "
                    + "and intensities of all objects in both channels. This allows straightforward calculation of the degree of " + "overlap between objects from the different channels.");
            createField("Colocalization", desc, null);
            desc = new String("Select one or more output and visualization options");
            createField("Visualization and output", desc, null);

            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            panel.setPreferredSize(new Dimension(575, 720));
            panel.add(pref);
            
            JFrame frame = new JFrame();
            frame.setSize(555, 480);
            frame.setLocation(x + 500, y - 50);
            frame.add(panel);
            frame.setVisible(true);
        }
    }
}
