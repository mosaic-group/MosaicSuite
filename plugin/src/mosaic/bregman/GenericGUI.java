package mosaic.bregman;


import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;

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

import mosaic.bregman.GUI.BackgroundSubGUI;
import mosaic.bregman.GUI.ColocalizationGUI;
import mosaic.bregman.GUI.SegmentationGUI;
import mosaic.bregman.GUI.VisualizationGUI;
import mosaic.core.GUI.HelpGUI;
import mosaic.core.cluster.ClusterSession;
import mosaic.core.utils.MosaicTest;
import mosaic.core.utils.MosaicUtils;
import mosaic.plugins.BregmanGLM_Batch;


public class GenericGUI {

    private enum run_mode {
        USE_CLUSTER, LOCAL, STOP
    }

    public static boolean bypass_GUI = false;
    boolean clustermode;
    ImagePlus imgch1;
    ImagePlus imgch2;
    int ni, nj, nz, nc;
    int posx, posy;
    static int screensizex, screensizey;
    BLauncher hd;
    boolean gui_use_cluster = false;

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
            hd.closeAll();
        }
    }

    public static void setwindowlocation(int x, int y, Window w) {
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

        GenericDialogCustom gd = new GenericDialogCustom("Batch window");

        addTextArea(gd);

        if (GenericGUI.bypass_GUI == false) {
            gd.showDialog();
            if (gd.wasCanceled()) {
                return run_mode.STOP;
            }
            Analysis.p.wd = gd.getNextText();
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
        Font bf = new Font(null, Font.BOLD, 12);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screensizex = (int) screenSize.getWidth();
        screensizey = (int) screenSize.getHeight();

        gd.setInsets(-10, 0, 3);

        addTextArea(gd);

        // FlowLayout fl = new FlowLayout(FlowLayout.LEFT,335,3);
        FlowLayout fl = new FlowLayout(FlowLayout.LEFT, 75, 3);
        Panel p = new Panel(fl);

        Button b = new Button("Select File/Folder");
        b.addActionListener(new FileOpenerActionListener(p, gd, gd.getTextArea1()));
        p.add(b);

        Button bh = new Button("Help");
        bh.addActionListener(new HelpOpenerActionListener(p, gd));
        p.add(bh);

        gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));

        // Image chooser

        gd.addChoice("Input image", new String[] { "" }, "");
        MosaicUtils.chooseImage(gd, "Image", aImp);

        // Background Options

        Button backOption = new Button("Options");
        Label label = new Label("Background subtraction");
        label.setFont(bf);
        p = new Panel();
        p.add(label);
        p.add(backOption);
        backOption.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                BackgroundSubGUI gds = new BackgroundSubGUI(posx, posy);
                gds.run("");
            }
        });
        gd.addPanel(p);

        // seg Option button

        Button segOption = new Button("Options");
        label = new Label("Segmentation parameters");
        label.setFont(bf);
        p = new Panel();
        p.add(label);
        p.add(segOption);
        segOption.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                SegmentationGUI gds = new SegmentationGUI(posx, posy);
                gds.run("");
            }
        });
        gd.addPanel(p);

        Button colOption = new Button("Options");
        label = new Label("Colocalization (two channels images)");
        label.setFont(bf);
        p = new Panel();
        p.add(label);
        p.add(colOption);
        colOption.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                ColocalizationGUI gds = new ColocalizationGUI(imgch1, imgch2, posx, posy);
                gds.run("");
            }
        });
        gd.addPanel(p);

        // gd.addMessage("");

        Button visOption = new Button("Options");
        label = new Label("Vizualization and output");
        label.setFont(bf);
        p = new Panel();
        p.add(label);
        p.add(visOption);
        visOption.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                VisualizationGUI gds = new VisualizationGUI(posx, posy);
                gds.run("");
            }
        });
        gd.addPanel(p);

        gd.addCheckbox("Process on computer cluster", gui_use_cluster);

        gd.centerDialog(false);
        posx = 100;
        posy = 120;
        gd.setLocation(posx, posy);

        // Introduce a label with reference

        JLabel labelJ = new JLabel("<html>Please refer to and cite:<br><br> G. Paul, J. Cardinale, and I. F. Sbalzarini.<br>" + "Coupling image restoration and segmentation:<br>"
                + "A generalized linear model/Bregman<br>" + "perspective. Int. J. Comput. Vis., 104(1):69–93, 2013.<br>" + "<br>" + "A. Rizk, G. Paul, P. Incardona, M. Bugarski, M. Mansouri,<br>"
                + "A. Niemann, U. Ziegler, P. Berger, and I. F. Sbalzarini.<br>" + "Segmentation and quantification of subcellular structures<br>"
                + "in fluorescence microscopy images using Squassh.<br>" + "Nature Protocols, 9(3):586–596, 2014. </html>");
        p = new Panel();
        p.add(labelJ);
        gd.addPanel(p);

        // ////////////////////////////////

        gd.showDialog();
        if (gd.wasCanceled()) {
            return run_mode.STOP;
        }

        Analysis.p.wd = gd.getNextText();

        Runtime runtime = Runtime.getRuntime();
        int nrOfProcessors = runtime.availableProcessors();
        // IJ.log("Number of processors available to the Java Virtual Machine: " + nrOfProcessors);
        Analysis.p.nthreads = nrOfProcessors;

        run_mode rm = run_mode.LOCAL;
        if (gd.getNextBoolean() == true) {
            rm = run_mode.USE_CLUSTER;
        }

        return rm;
    }

    public void run(String arg, ImagePlus aImp) {
        Boolean use_cluster = false;
        // String sgroup1[] = {"activate second step", ".. with subpixel resolution"};
        // boolean bgroup1[] = {false, false};

        // for rscript generation
        Analysis.p.initrsettings = true;

        if (!clustermode) {
            run_mode rm = null;

            if (IJ.isMacro() == true) {
                new GenericDialogCustom("Squassh");
                // Draw a batch system window

                rm = drawBatchWindow();
                if (rm == run_mode.STOP) {
                    Macro.abort();
                }

            }
            else {
                GenericDialog gd = new NonBlockingGenericDialog("Squassh");

                // Draw a standard window

                rm = drawStandardWindow(gd, aImp);
            }

            use_cluster = (rm == run_mode.USE_CLUSTER);

            if (rm == run_mode.STOP) {
                return;
            }
        }
        else {
            GenericDialogCustom gd = new GenericDialogCustom("Squassh");

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

        System.out.println("Paramenters: " + Analysis.p);

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

                    Analysis.p.file1 = savepath + File.separator + "stitch_ObjectsData_c1" + ".csv";
                    Analysis.p.file2 = savepath + File.separator + "stitch_ObjectsData_c2" + ".csv";
                    Analysis.p.file3 = savepath + File.separator + "stitch_ImagesData" + ".csv";
                }
                else {
                    Analysis.p.file1 = savepath + File.separator + Analysis.out_w[0].replace("*", "_") + File.separator + MosaicUtils.removeExtension(aImp.getTitle()) + "_ObjectsData_c1" + ".csv";
                    Analysis.p.file2 = savepath + File.separator + Analysis.out_w[1].replace("*", "_") + File.separator + MosaicUtils.removeExtension(aImp.getTitle()) + "_ObjectsData_c2" + ".csv";
                    Analysis.p.file3 = savepath + File.separator + Analysis.out_w[4].replace("*", "_") + File.separator + MosaicUtils.removeExtension(aImp.getTitle()) + "_ImagesData" + ".csv";
                }
            }
            else {
                hd = new BLauncher(Analysis.p.wd);

                Vector<String> pf = hd.getProcessedFiles();
                File fl = new File(Analysis.p.wd);
                if (fl.isDirectory() == true) {
                    savepath = Analysis.p.wd;
                }
                else {
                    savepath = fl.getParent();
                }

                if (fl.isDirectory() == true) {
                    MosaicUtils.reorganize(Analysis.out_w, pf, Analysis.p.wd);
                    MosaicUtils.StitchCSV(fl.getAbsolutePath(), Analysis.out, null);

                    Analysis.p.file1 = Analysis.p.wd + File.separator + "stitch__ObjectsData_c1" + ".csv";
                    Analysis.p.file2 = Analysis.p.wd + File.separator + "stitch__ObjectsData_c2" + ".csv";
                    Analysis.p.file3 = Analysis.p.wd + File.separator + "stitch_ImagesData" + ".csv";
                }
                else {
                    Analysis.p.file1 = fl.getParent() + File.separator + Analysis.out_w[0].replace("*", "_") + File.separator + MosaicUtils.removeExtension(fl.getName()) + "_ObjectsData_c1" + ".csv";
                    Analysis.p.file2 = fl.getParent() + File.separator + Analysis.out_w[1].replace("*", "_") + File.separator + MosaicUtils.removeExtension(fl.getName()) + "_ObjectsData_c2" + ".csv";
                    Analysis.p.file3 = fl.getParent() + File.separator + Analysis.out_w[4].replace("*", "_") + File.separator + MosaicUtils.removeExtension(fl.getName()) + "_ImagesData" + ".csv";

                    MosaicUtils.reorganize(Analysis.out_w, pf, new File(Analysis.p.wd).getParent());
                }
            }

            // hd.bcolocheadless(imagePlus);*/

            if (Analysis.p.nchannels == 2) {
                if (Analysis.p.save_images) {
                    RScript script = new RScript(savepath, Analysis.p.file1, Analysis.p.file2, Analysis.p.file3, Analysis.p.nbconditions, Analysis.p.nbimages, Analysis.p.groupnames, Analysis.p.ch1,
                            Analysis.p.ch2);
                    script.writeScript();
                }
            }
        }
        else {
            // We run on cluster

            Parameters p = new Parameters(Analysis.p);

            // disabling display options

            p.dispwindows = false;

            // save for the cluster

            // For the cluster we have to nullify the directory option

            p.wd = null;

            // TODO: Why settings are saved twice to two different files? To be investigated.
            BregmanGLM_Batch.saveConfig("/tmp/settings.dat", p);

            // save locally
            BregmanGLM_Batch.saveConfig("/tmp/spb_settings.dat", p);

            // Check if we selected a directory
            @SuppressWarnings("unused")
            ClusterSession ss = null;
            File[] fileslist = null;
            File fl = null;

            ClusterSession.setPreferredSlotPerProcess(4);
            String Background = null;

            if (aImp == null) {
                fl = new File(Analysis.p.wd);
                if (fl.isDirectory() == true) {
                    // we have a directory

                    fileslist = fl.listFiles();
                    ss = ClusterSession.processFiles(fileslist, "Squassh", "", Analysis.out);
                }
                else if (fl.isFile()) {
                    // we process an image

                    ss = ClusterSession.processFile(fl, "Squassh", "", Analysis.out);
                    Background = fl.getAbsolutePath();
                }
                else {
                    // Nothing to do just get the result

                    ss = ClusterSession.getFinishedJob(Analysis.out, "Squassh");

                    // Ask for a directory

                    fl = new File(IJ.getDirectory("Select output directory"));
                }
            }
            else {
                // It is a file

                ss = ClusterSession.processImage(aImp, "Squassh", "", Analysis.out);
                Background = MosaicUtils.ValidFolderFromImage(aImp) + File.separator + aImp.getTitle();
            }

            // Get output format and Stitch the output in the output selected

            String outcsv[] = { "*_ObjectsData_c1.csv" };
            String path = MosaicUtils.ValidFolderFromImage(aImp);
            if (BregmanGLM_Batch.test_mode == true) {
                path = MosaicTest.getTestEnvironment();
            }
            File dir = ClusterSession.processJobsData(outcsv, path);

            // if background is != null it mean that is a video or is an image so try to stitch
            if (Background != null) {
                MosaicUtils.StitchJobsCSV(dir.getAbsolutePath(), Analysis.out, Background);
            }
            else {
                MosaicUtils.StitchJobsCSV(dir.getAbsolutePath(), Analysis.out, null);
            }

            // //////////////
        }

        // Analysis.load2channels(imagePlus);
    }

    private class FileOpenerActionListener implements ActionListener {

        TextArea ta;

        public FileOpenerActionListener(Panel p, GenericDialog gd, TextArea ta) {
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

            String path;
            // with JFileChooser
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.showOpenDialog(null);
            File selFile = fc.getSelectedFile();
            if (selFile == null) {
                return;
            }
            path = selFile.getAbsolutePath();

            boolean processdirectory = (new File(path)).isDirectory();
            if (!processdirectory) {

                ImagePlus img2 = IJ.openImage(path);

                img2.show();

            }

            if (path != null)// (file!=null && dir!=null)
            {
                ta.setText(path);
            }
        }
    }

    private class HelpOpenerActionListener implements ActionListener {

        GenericDialog gd;

        public HelpOpenerActionListener(Panel p, GenericDialog gd) {
            this.gd = gd;

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Point p = gd.getLocationOnScreen();
            // IJ.log("plugin location :" + p.toString());
            new Helpwindow(p.x, p.y);
        }
    }

    private class Helpwindow extends HelpGUI {

        JFrame frame;
        private JPanel panel;

        public Helpwindow(int x, int y) {
            frame = new JFrame();
            frame.setSize(555, 480);
            frame.setLocation(x + 500, y - 50);
            // frame.toFront();
            // frame.setResizable(false);
            // frame.setAlwaysOnTop(true);

            panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            panel.setPreferredSize(new Dimension(575, 720));

            JPanel pref = new JPanel(new GridBagLayout());
            // pref.setPreferredSize(new Dimension(555, 550));
            // pref.setSize(pref.getPreferredSize());

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

            // JPanel panel = new JPanel(new BorderLayout());

            panel.add(pref);
            // panel.add(label, BorderLayout.NORTH);

            frame.add(panel);

            frame.setVisible(true);
        }

    }

}
