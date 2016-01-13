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
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import mosaic.bregman.Analysis;
import mosaic.bregman.BLauncher;
import mosaic.bregman.Parameters;
import mosaic.bregman.RScript;
import mosaic.core.GUI.HelpGUI;
import mosaic.core.cluster.ClusterSession;
import mosaic.core.utils.MosaicUtils;
import mosaic.plugins.BregmanGLM_Batch;


public class GenericGUI {
    private static final Logger logger = Logger.getLogger(GenericGUI.class);
    
    private enum run_mode {
        USE_CLUSTER, LOCAL, STOP
    }

    public static boolean bypass_GUI = false;
    private final boolean clustermode;
    protected ImagePlus imgch1;
    protected ImagePlus imgch2;

    protected int posx, posy;
    private static int screensizex, screensizey;
    private BLauncher hd;
    private boolean gui_use_cluster = false;

    public GenericGUI(boolean mode, ImagePlus img_p) {
        imgch1 = img_p;
        clustermode = mode;
    }

    /**
     * Use the cluster
     *
     * @param bl true to use the cluster option
     */
    public void setUseCluster(boolean bl) {
        gui_use_cluster = bl;
    }

    /**
     * Close all opened image
     */
    public void closeAll() {
        if (hd != null) {
            hd.closeAllImages();
        }
    }

    static public void setwindowlocation(int x, int y, Window w) {
        int wx, wy;
        wx = w.getWidth();
        wy = w.getHeight();
        w.setLocation(Math.min(x, screensizex - wx), Math.min(y, screensizey - wy));

    }

    /**
     * Draw a window if we are running as a macro, basically does not draw
     * any window, but just get the parameters from the command line
     *
     * @param gd Generic dialog where to draw
     */
    private run_mode drawBatchWindow() {
        // No visualization is active by default
        if (GenericGUI.bypass_GUI == false) {
            Analysis.p.livedisplay = false;
            Analysis.p.dispcolors = false;
            Analysis.p.dispint = false;
            Analysis.p.displabels = false;
            Analysis.p.dispoutline = false;
            Analysis.p.dispSoftMask = false;
            Analysis.p.save_images = true;
        }

        System.out.println("Batch window");

        final GenericDialog gd = new GenericDialog("Batch window");

        addTextArea(gd);

        if (GenericGUI.bypass_GUI == false) {
            gd.showDialog();
            if (gd.wasCanceled()) {
                return run_mode.STOP;
            }
            
            Analysis.p.wd = gd.getNextText();
            logger.debug("wd = [" + Analysis.p.wd + "]");
        }

        if (BackgroundSubGUI.getParameters() == -1) {
            return run_mode.STOP;
        }
        if (SegmentationGUI.getParameters() == -1) {
            return run_mode.STOP;
        }
        if (VisualizationGUI.getParameters() == -1) {
            return run_mode.STOP;
        }

        if (gui_use_cluster == true) {
            return run_mode.USE_CLUSTER;
        }

        return run_mode.LOCAL;
    }

    /**
     * Add text are for file path
     *
     * @param gd
     */
    private void addTextArea(GenericDialog gd) {
        if (Analysis.p.wd == null) {
            gd.addTextAreas("Input Image: \n" + "insert Path to file or folder, " + "or press Button below.", null, 2, 50);
        }
        else {
            gd.addTextAreas(Analysis.p.wd, null, 2, 50);
        }
    }

    /**
     * Draw the standard squassh main window
     *
     * @param gd Generic dialog where to draw
     * @param Active imagePlus
     * @param It output if we have to use the cluster
     * @return run mode, -1 when cancelled
     */
    private run_mode drawStandardWindow(GenericDialog gd, ImagePlus aImp) {
        // font for reference
        final Font bf = new Font(null, Font.BOLD, 12);

        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screensizex = (int) screenSize.getWidth();
        screensizey = (int) screenSize.getHeight();

        gd.setInsets(-10, 0, 3);

        addTextArea(gd);

        final FlowLayout fl = new FlowLayout(FlowLayout.LEFT, 75, 3);
        Panel p = new Panel(fl);

        final Button b = new Button("Select File/Folder");
        b.addActionListener(new FileOpenerActionListener(gd.getTextArea1()));
        p.add(b);

        final Button bh = new Button("Help");
        bh.addActionListener(new HelpOpenerActionListener(gd));
        p.add(bh);

        gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));

        // Image chooser
        gd.addChoice("Input image", new String[] { "" }, "");
        MosaicUtils.chooseImage(gd, aImp);

        // Background Options
        final Button backOption = new Button("Options");
        Label label = new Label("Background subtraction");
        label.setFont(bf);
        p = new Panel();
        p.add(label);
        p.add(backOption);
        backOption.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final BackgroundSubGUI gds = new BackgroundSubGUI();
                gds.run();
            }
        });
        gd.addPanel(p);

        // seg Option button
        final Button segOption = new Button("Options");
        label = new Label("Segmentation parameters");
        label.setFont(bf);
        p = new Panel();
        p.add(label);
        p.add(segOption);
        segOption.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final SegmentationGUI gds = new SegmentationGUI();
                gds.run();
            }
        });
        gd.addPanel(p);

        final Button colOption = new Button("Options");
        label = new Label("Colocalization (two channels images)");
        label.setFont(bf);
        p = new Panel();
        p.add(label);
        p.add(colOption);
        colOption.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final ColocalizationGUI gds = new ColocalizationGUI(imgch1, imgch2, posx, posy);
                gds.run();
            }
        });
        gd.addPanel(p);

        final Button visOption = new Button("Options");
        label = new Label("Vizualization and output");
        label.setFont(bf);
        p = new Panel();
        p.add(label);
        p.add(visOption);
        visOption.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final VisualizationGUI gds = new VisualizationGUI();
                gds.run();
            }
        });
        gd.addPanel(p);

        gd.addCheckbox("Process on computer cluster", gui_use_cluster);

        gd.centerDialog(false);
        posx = 100;
        posy = 120;
        gd.setLocation(posx, posy);

        // Introduce a label with reference
        final JLabel labelJ = new JLabel("<html>Please refer to and cite:<br><br> G. Paul, J. Cardinale, and I. F. Sbalzarini.<br>" + "Coupling image restoration and segmentation:<br>"
                + "A generalized linear model/Bregman<br>" + "perspective. Int. J. Comput. Vis., 104(1):69–93, 2013.<br>" + "<br>" + "A. Rizk, G. Paul, P. Incardona, M. Bugarski, M. Mansouri,<br>"
                + "A. Niemann, U. Ziegler, P. Berger, and I. F. Sbalzarini.<br>" + "Segmentation and quantification of subcellular structures<br>"
                + "in fluorescence microscopy images using Squassh.<br>" + "Nature Protocols, 9(3):586–596, 2014. </html>");
        p = new Panel();
        p.add(labelJ);
        gd.addPanel(p);

        /////////////////////////////////

        gd.showDialog();
        if (gd.wasCanceled()) {
            return run_mode.STOP;
        }

        Analysis.p.wd = gd.getNextText();

        final Runtime runtime = Runtime.getRuntime();
        final int nrOfProcessors = runtime.availableProcessors();
        // IJ.log("Number of processors available to the Java Virtual Machine: " + nrOfProcessors);
        Analysis.p.nthreads = nrOfProcessors;

        run_mode rm = run_mode.LOCAL;
        if (gd.getNextBoolean() == true) {
            rm = run_mode.USE_CLUSTER;
        }

        return rm;
    }

    public void run(ImagePlus aImp) {
        String file1;
        String file2;
        String file3;
        Boolean use_cluster = false;

        logger.debug("clustermode = " + clustermode);
        
        if (!clustermode) {
            run_mode rm = null;

            // TODO: It should be also nice to have " || Interpreter.batchMode == true" but it seems that 
            // it does not work -> unit test fails. Should be investigated why...
            // It seems that we go then always in first 'if' instead of 'else' on most tests since batchMode = true is default
            // for unit testing...
            if (IJ.isMacro() == true || BregmanGLM_Batch.test_mode) {// || Interpreter.batchMode == true) {
                logger.debug("Macro setting for mode");
                new GenericDialog("Squassh");
                // Draw a batch system window

                rm = drawBatchWindow();
                if (rm == run_mode.STOP) {
                    Macro.abort();
                }
            }
            else {
                logger.debug("Non-Macro setting for mode");
                final GenericDialog gd = new NonBlockingGenericDialog("Squassh");
                rm = drawStandardWindow(gd, aImp);
            }
            logger.debug("runmode = " + rm);
            use_cluster = (rm == run_mode.USE_CLUSTER);

            if (rm == run_mode.STOP) {
                return;
            }
        }
        else {
            final GenericDialog gd = new GenericDialog("Squassh");

            gd.addStringField("config", "path", 10);
            gd.addStringField("filepath", "path", 10);

            gd.addNumericField("number of threads", 4, 0);

            gd.showDialog();
            if (gd.wasCanceled()) {
                return;
            }

            Analysis.p.nthreads = (int) gd.getNextNumber();
            Analysis.p = BregmanGLM_Batch.getConfigHandler().LoadFromFile(gd.getNextString(), Parameters.class, Analysis.p);
            Analysis.p.wd = gd.getNextString();
        }

        System.out.println("Parameters: " + Analysis.p);

        if (Analysis.p.mode_voronoi2) {
            // betamleout to be determined by clustering of whole image

            Analysis.p.betaMLEindefault = 1;
            // Analysis.p.betaMLEoutdefault=0.1;
            Analysis.p.regionthresh = Analysis.p.min_intensity;
            Analysis.p.regionthreshy = Analysis.p.min_intensityY;
            Analysis.p.refinement = true;
            Analysis.p.max_nsb = 151;
            Analysis.p.regionSegmentLevel = 1;// not used
            Analysis.p.dispvoronoi = Analysis.p.debug;
            Analysis.p.minves_size = 2;
        }

        if (!Analysis.p.subpixel) {
            Analysis.p.oversampling2ndstep = 1;
            Analysis.p.interpolation = 1;
        }

        if (use_cluster == false && clustermode == false) {
            Analysis.p.dispwindows = true;
        }
        logger.debug("use_cluster = " + use_cluster);
        // Two different way to run the Segmentation and colocalization
        if (clustermode || use_cluster == false) {
            // We run locally
            String savepath = null;

            hd = null;

            if (Analysis.p.wd == null || Analysis.p.wd.startsWith("Input Image:") || Analysis.p.wd.isEmpty()) {
                savepath = MosaicUtils.ValidFolderFromImage(aImp);
                if (aImp == null) {
                    IJ.error("No image to process");
                    return;
                }
                hd = new BLauncher(aImp);

                MosaicUtils.reorganize(Analysis.out_w, aImp.getShortTitle(), savepath, aImp.getNFrames());

                // if it is a video Stitch all the csv

                if (aImp.getNFrames() > 1) {
                    MosaicUtils.StitchCSV(savepath, Analysis.out, savepath + File.separator + aImp.getTitle());

                    file1 = savepath + File.separator + "stitch_ObjectsData_c1" + ".csv";
                    file2 = savepath + File.separator + "stitch_ObjectsData_c2" + ".csv";
                    file3 = savepath + File.separator + "stitch_ImagesData" + ".csv";
                }
                else {
                    file1 = savepath + File.separator + Analysis.out_w[0].replace("*", "_") + File.separator + MosaicUtils.removeExtension(aImp.getTitle()) + "_ObjectsData_c1" + ".csv";
                    file2 = savepath + File.separator + Analysis.out_w[1].replace("*", "_") + File.separator + MosaicUtils.removeExtension(aImp.getTitle()) + "_ObjectsData_c2" + ".csv";
                    file3 = savepath + File.separator + Analysis.out_w[4].replace("*", "_") + File.separator + MosaicUtils.removeExtension(aImp.getTitle()) + "_ImagesData" + ".csv";
                }
            }
            else {
                hd = new BLauncher(Analysis.p.wd);

                final Vector<String> pf = hd.getProcessedFiles();
                final File fl = new File(Analysis.p.wd);
                if (fl.isDirectory() == true) {
                    savepath = Analysis.p.wd;
                }
                else {
                    savepath = fl.getParent();
                }

                if (fl.isDirectory() == true) {
                    MosaicUtils.reorganize(Analysis.out_w, pf, Analysis.p.wd);
                    MosaicUtils.StitchCSV(fl.getAbsolutePath(), Analysis.out, null);

                    file1 = Analysis.p.wd + File.separator + "stitch__ObjectsData_c1" + ".csv";
                    file2 = Analysis.p.wd + File.separator + "stitch__ObjectsData_c2" + ".csv";
                    file3 = Analysis.p.wd + File.separator + "stitch_ImagesData" + ".csv";
                }
                else {
                    file1 = fl.getParent() + File.separator + Analysis.out_w[0].replace("*", "_") + File.separator + MosaicUtils.removeExtension(fl.getName()) + "_ObjectsData_c1" + ".csv";
                    file2 = fl.getParent() + File.separator + Analysis.out_w[1].replace("*", "_") + File.separator + MosaicUtils.removeExtension(fl.getName()) + "_ObjectsData_c2" + ".csv";
                    file3 = fl.getParent() + File.separator + Analysis.out_w[4].replace("*", "_") + File.separator + MosaicUtils.removeExtension(fl.getName()) + "_ImagesData" + ".csv";

                    MosaicUtils.reorganize(Analysis.out_w, pf, new File(Analysis.p.wd).getParent());
                }
            }

            if (Analysis.p.nchannels == 2) {
                if (Analysis.p.save_images) {
                    final RScript script = new RScript(savepath, file1, file2, file3, Analysis.p.nbconditions, Analysis.p.nbimages, Analysis.p.groupnames, Analysis.p.ch1,
                            Analysis.p.ch2);
                    script.writeScript();
                }
            }
        }
        else {
            // We run on cluster
            final Parameters tempParams = new Parameters(Analysis.p);

            // disabling display options
            tempParams.dispwindows = false;

            // save for the cluster
            // For the cluster we have to nullify the directory option
            tempParams.wd = null;

            // TODO: Why settings are saved twice to two different files? To be investigated.
            BregmanGLM_Batch.saveConfig("/tmp/settings.dat", tempParams);
            // save locally
            BregmanGLM_Batch.saveConfig("/tmp/spb_settings.dat", tempParams);

            ClusterSession.setPreferredSlotPerProcess(4);
            String Background = null;

            if (aImp == null) {
                File fl = new File(Analysis.p.wd);
                if (fl.isDirectory() == true) {
                    // we have a directory

                    File[] fileslist = fl.listFiles();
                    ClusterSession.processFiles(fileslist, "Squassh", "", Analysis.out);
                }
                else if (fl.isFile()) {
                    // we process an image

                    ClusterSession.processFile(fl, "Squassh", "", Analysis.out);
                    Background = fl.getAbsolutePath();
                }
                else {
                    // Nothing to do just get the result
                    ClusterSession.getFinishedJob(Analysis.out, "Squassh");

                    // Ask for a directory
                    fl = new File(IJ.getDirectory("Select output directory"));
                }
            }
            else {
                // It is a file
                ClusterSession.processImage(aImp, "Squassh", "", Analysis.out);
                Background = MosaicUtils.ValidFolderFromImage(aImp) + File.separator + aImp.getTitle();
            }

            // Get output format and Stitch the output in the output selected
            String path = MosaicUtils.ValidFolderFromImage(aImp);
            if (BregmanGLM_Batch.test_mode == true) {
                // TODO: Artifact from "old test system". Must be refactored!!!
                path = BregmanGLM_Batch.test_path;
            }
            final File dir = ClusterSession.processJobsData(path);

            // if background is != null it mean that is a video or is an image so try to stitch
            if (Background != null) {
                MosaicUtils.StitchJobsCSV(dir.getAbsolutePath(), Analysis.out, Background);
            }
            else {
                MosaicUtils.StitchJobsCSV(dir.getAbsolutePath(), Analysis.out, null);
            }
        }
    }

    private class FileOpenerActionListener implements ActionListener {

        TextArea ta;

        public FileOpenerActionListener(TextArea ta) {
            this.ta = ta;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (imgch1 != null) {
                imgch1.close();
                imgch1 = null;
            }
            if (imgch2 != null) {
                imgch2.close();
                imgch2 = null;
            }// close previosuly opened images

            // with JFileChooser
            final JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.showOpenDialog(null);
            final File selFile = fc.getSelectedFile();
            if (selFile == null) {
                return;
            }
            String path = selFile.getAbsolutePath();

            final boolean processdirectory = (new File(path)).isDirectory();
            if (!processdirectory) {
                final ImagePlus img2 = IJ.openImage(path);
                img2.show();
            }

            if (path != null) {
                ta.setText(path);
            }
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

        JFrame frame;
        private final JPanel panel;

        public Helpwindow(int x, int y) {
            frame = new JFrame();
            frame.setSize(555, 480);
            frame.setLocation(x + 500, y - 50);

            panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            panel.setPreferredSize(new Dimension(575, 720));

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

            panel.add(pref);
            frame.add(panel);

            frame.setVisible(true);
        }

    }
}
