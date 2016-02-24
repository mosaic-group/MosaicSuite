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
import java.util.Arrays;
import java.util.Set;
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
import mosaic.bregman.BLauncher;
import mosaic.bregman.Files;
import mosaic.bregman.Files.FileInfo;
import mosaic.bregman.Parameters;
import mosaic.bregman.RScript;
import mosaic.core.GUI.HelpGUI;
import mosaic.core.cluster.ClusterSession;
import mosaic.core.utils.MosaicUtils;
import mosaic.plugins.BregmanGLM_Batch;
import mosaic.utils.ImgUtils;
import mosaic.utils.SysOps;


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
     * Draw a window if we are running as a macro, basically does not draw
     * any window, but just get the parameters from the command line
     *
     * @param gd Generic dialog where to draw
     */
    private run_mode drawBatchWindow() {
        // No visualization is active by default
        if (GenericGUI.bypass_GUI == false) {
            BLauncher.iParameters.livedisplay = false;
            BLauncher.iParameters.dispcolors = false;
            BLauncher.iParameters.dispint = false;
            BLauncher.iParameters.displabels = false;
            BLauncher.iParameters.dispoutline = false;
            BLauncher.iParameters.dispSoftMask = false;
            BLauncher.iParameters.save_images = true;
        }

        System.out.println("Batch window");

        final GenericDialog gd = new GenericDialog("Batch window");

        addTextArea(gd);

        if (GenericGUI.bypass_GUI == false) {
            gd.showDialog();
            if (gd.wasCanceled()) {
                return run_mode.STOP;
            }
            
            BLauncher.iParameters.wd = gd.getNextText();
            logger.debug("wd = [" + BLauncher.iParameters.wd + "]");
        }

        if (BackgroundSubGUI.getParameters() == -1 || SegmentationGUI.getParameters() == -1 || VisualizationGUI.getParameters() == -1) {
            return run_mode.STOP;
        }

        if (gui_use_cluster == true) {
            return run_mode.USE_CLUSTER;
        }

        return run_mode.LOCAL;
    }

    /**
     * Add text are for file path
     */
    private void addTextArea(GenericDialog gd) {
        if (BLauncher.iParameters.wd == null) {
            gd.addTextAreas("Input Image: \n" + "insert Path to file or folder, " + "or press Button below.", null, 2, 50);
        }
        else {
            gd.addTextAreas(BLauncher.iParameters.wd, null, 2, 50);
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

        gd.addChoice("Input image", new String[] { "" }, "");
        MosaicUtils.chooseImage(gd, aImp);

        Label label = new Label("Background subtraction");
        label.setFont(bf);
        p = new Panel();
        p.add(label);
        
        final Button backOption = new Button("Options");
        p.add(backOption);
        backOption.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                BackgroundSubGUI.getParameters();
            }
        });
        gd.addPanel(p);

        label = new Label("Segmentation parameters");
        label.setFont(bf);
        p = new Panel();
        p.add(label);
        
        final Button segOption = new Button("Options");
        p.add(segOption);
        segOption.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                SegmentationGUI.getParameters();
            }
        });
        gd.addPanel(p);

        label = new Label("Colocalization (two channels images)");
        label.setFont(bf);
        p = new Panel();
        p.add(label);
        
        final Button colOption = new Button("Options");
        p.add(colOption);
        colOption.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final ColocalizationGUI gds = new ColocalizationGUI(imgch1, imgch2, posx, posy);
                gds.run();
            }
        });
        gd.addPanel(p);

        label = new Label("Vizualization and output");
        label.setFont(bf);
        p = new Panel();
        p.add(label);
        
        final Button visOption = new Button("Options");
        p.add(visOption);
        visOption.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                VisualizationGUI.getParameters();
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

        BLauncher.iParameters.wd = gd.getNextText();

        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        BLauncher.iParameters.nthreads = availableProcessors;
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

            BLauncher.iParameters.nthreads = (int) gd.getNextNumber();
            BLauncher.iParameters = BregmanGLM_Batch.getConfigHandler().LoadFromFile(gd.getNextString(), Parameters.class, BLauncher.iParameters);
            BLauncher.iParameters.wd = gd.getNextString();
        }

        System.out.println("Parameters: " + BLauncher.iParameters);

        logger.debug("use_cluster = " + use_cluster);
        // Two different way to run the Segmentation and colocalization
        if (clustermode || use_cluster == false) {
            // We run locally
            String savePath = null;

            if (BLauncher.iParameters.wd == null || BLauncher.iParameters.wd.startsWith("Input Image:") || BLauncher.iParameters.wd.isEmpty()) {
                savePath = ImgUtils.getImageDirectory(aImp);
                BLauncher bl = new BLauncher(aImp);
                Set<FileInfo> savedFiles = bl.getSavedFiles();
                if (savedFiles.size() == 0) return;
                
                Files.moveFilesToOutputDirs(savedFiles, savePath);
                
                file1 = savePath + File.separator + Files.outSuffixesLocal[0].replace("*", "_") + File.separator + SysOps.removeExtension(aImp.getTitle()) + "_ObjectsData_c1.csv";
                file2 = savePath + File.separator + Files.outSuffixesLocal[1].replace("*", "_") + File.separator + SysOps.removeExtension(aImp.getTitle()) + "_ObjectsData_c2.csv";
                file3 = savePath + File.separator + Files.outSuffixesLocal[4].replace("*", "_") + File.separator + SysOps.removeExtension(aImp.getTitle()) + "_ImagesData.csv";
            }
            else {
                logger.debug("WD with PATH: " + BLauncher.iParameters.wd);
                final Vector<String> iProcessedFiles = new Vector<String>();
                final File inputFile = new File(BLauncher.iParameters.wd);
                File[] files = (inputFile.isDirectory()) ? inputFile.listFiles() : new File[] {inputFile};
                Arrays.sort(files);
                
                for (final File f : files) {
                    // If it is the directory/Rscript/hidden/csv file then skip it
                    if (f.isDirectory() == true || f.getName().equals("R_analysis.R") || f.getName().startsWith(".") || f.getName().endsWith(".csv")) {
                        continue;
                    }
                    
                    iProcessedFiles.add(f.getName());
                    new BLauncher(MosaicUtils.openImg(f.getAbsolutePath()));
                }
                
                final File fl = new File(BLauncher.iParameters.wd);
                // TODO: Files should be processed only if input was a directory. Strange behaviour but this comes
                // from old code. Should be refactored.
                final Vector<String> pf = fl.isDirectory() ? iProcessedFiles : new Vector<String>();
                savePath = fl.isDirectory() ? BLauncher.iParameters.wd : fl.getParent();

                if (fl.isDirectory() == true) {
                    file1 = BLauncher.iParameters.wd + File.separator + "stitch__ObjectsData_c1.csv";
                    file2 = BLauncher.iParameters.wd + File.separator + "stitch__ObjectsData_c2.csv";
                    file3 = BLauncher.iParameters.wd + File.separator + "stitch_ImagesData.csv";

                    MosaicUtils.reorganize(Files.outSuffixesLocal, pf, BLauncher.iParameters.wd);
                    MosaicUtils.StitchCSV(fl.getAbsolutePath(), Files.outSuffixesCluster, null);
                }
                else {
                    file1 = fl.getParent() + File.separator + Files.outSuffixesLocal[0].replace("*", "_") + File.separator + SysOps.removeExtension(fl.getName()) + "_ObjectsData_c1.csv";
                    file2 = fl.getParent() + File.separator + Files.outSuffixesLocal[1].replace("*", "_") + File.separator + SysOps.removeExtension(fl.getName()) + "_ObjectsData_c2.csv";
                    file3 = fl.getParent() + File.separator + Files.outSuffixesLocal[4].replace("*", "_") + File.separator + SysOps.removeExtension(fl.getName()) + "_ImagesData.csv";

                    MosaicUtils.reorganize(Files.outSuffixesLocal, pf, new File(BLauncher.iParameters.wd).getParent());
                }
            }
            
            if (new File(file1).exists() && new File(file2).exists()) {
                if (BLauncher.iParameters.save_images) {
                    new RScript(savePath, file1, file2, file3, BLauncher.iParameters.nbconditions, BLauncher.iParameters.nbimages, BLauncher.iParameters.groupnames, BLauncher.iParameters.ch1, BLauncher.iParameters.ch2);
                    // Try to run the R script
                    // TODO: Output seems to be completely wrong. Must be investigated. Currently turned off.
//                    try {
//                        logger.debug("================ RSCRIPT BEGIN ====================");
//                        logger.debug("CMD: " + "cd " + savepath + "; Rscript " + savepath + File.separator + "R_analysis.R");
//                        ShellCommand.exeCmdString("cd " + savepath + "; Rscript " + savepath + File.separator + "R_analysis.R");
//                        logger.debug("================ RSCRIPT END ====================");
//                    }
//                    catch (final IOException e) {
//                        e.printStackTrace();
//                    }
//                    catch (final InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }
            }
        }
        else {
            // We run on cluster
            saveParamitersForCluster(BLauncher.iParameters);
            ClusterSession.setPreferredSlotPerProcess(4);
            String backgroundImageFile = null;

            if (aImp == null) {
                File fl = new File(BLauncher.iParameters.wd);
                if (fl.isDirectory() == true) {
                    File[] fileslist = fl.listFiles();
                    ClusterSession.processFiles(fileslist, "Squassh", "", Files.outSuffixesCluster);
                }
                else if (fl.isFile()) {
                    ClusterSession.processFile(fl, "Squassh", "", Files.outSuffixesCluster);
                    backgroundImageFile = fl.getAbsolutePath();
                }
                else {
                    // Nothing to do just get the result
                    ClusterSession.getFinishedJob(Files.outSuffixesCluster, "Squassh");

                    // Ask for a directory
                    fl = new File(IJ.getDirectory("Select output directory"));
                }
            }
            else {
                // It is a file
                ClusterSession.processImage(aImp, "Squassh", "", Files.outSuffixesCluster);
                backgroundImageFile = ImgUtils.getImageDirectory(aImp) + File.separator + aImp.getTitle();
            }

            // Get output format and Stitch the output in the output selected
            String path = ImgUtils.getImageDirectory(aImp);
            if (BregmanGLM_Batch.test_mode == true) {
                // TODO: Artifact from "old test system". Must be refactored!!!
                path = BregmanGLM_Batch.test_path;
            }
            final File dir = ClusterSession.processJobsData(path);
            MosaicUtils.StitchJobsCSV(dir.getAbsolutePath(), Files.outSuffixesCluster, backgroundImageFile);
        }
    }

    private void saveParamitersForCluster(final Parameters aParameters) {
        // save for the cluster
        // For the cluster we have to nullify the directory option
        String oldWorkDir = aParameters.wd;
        aParameters.wd = null;

        // TODO: Why settings are saved twice to two different files? To be investigated.
        BregmanGLM_Batch.saveConfig("/tmp/settings.dat", aParameters);
        // save locally
        BregmanGLM_Batch.saveConfig("/tmp/spb_settings.dat", aParameters);
        
        // revert wd
        aParameters.wd = oldWorkDir;
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
            JFrame frame = new JFrame();
            frame.setSize(555, 480);
            frame.setLocation(x + 500, y - 50);

            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
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
