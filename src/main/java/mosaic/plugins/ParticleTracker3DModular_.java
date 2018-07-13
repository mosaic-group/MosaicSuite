package mosaic.plugins;


import java.awt.Button;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JLabel;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Roi;
import ij.gui.YesNoCancelDialog;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.io.SaveDialog;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import ij.process.StackStatistics;
import mosaic.core.detection.FeaturePointDetector;
import mosaic.core.detection.FeaturePointDetector.Mode;
import mosaic.core.detection.GUIhelper;
import mosaic.core.detection.MyFrame;
import mosaic.core.detection.MyFrame.DrawType;
import mosaic.core.detection.Particle;
import mosaic.core.detection.PreviewCanvas;
import mosaic.core.detection.PreviewInterface;
import mosaic.core.particleLinking.LinkerOptions;
import mosaic.core.particleLinking.ParticleLinker;
import mosaic.core.particleLinking.ParticleLinkerGreedy;
import mosaic.core.particleLinking.ParticleLinkerHungarian;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.MosaicUtils.SegmentationInfo;
import mosaic.core.utils.MosaicUtils.ToARGB;
import mosaic.particleTracker.FocusStackWin;
import mosaic.particleTracker.ParticleTrackerHelp;
import mosaic.particleTracker.ResultsWindow;
import mosaic.particleTracker.TrajectoriesReportXML;
import mosaic.particleTracker.Trajectory;
import mosaic.particleTracker.TrajectoryAnalysis;
import mosaic.particleTracker.TrajectoryStackWin;
import mosaic.utils.io.csv.CSV;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;


/**
 * <h2>ParticleTracker</h2>
 * <h3>An ImageJ Plugin for particles detection and tracking from digital videos</h3>
 * This class implements a to 3d extended feature point detection and tracking algorithm as described in: <br>
 * I. F. Sbalzarini and P. Koumoutsakos.
 * "Feature point tracking and trajectory analysis for video imaging in cell biology."
 * J. Struct. Biol., 151(2), 2005.
 */

public class ParticleTracker3DModular_ implements PlugInFilter, PreviewInterface {
    private static final Logger logger = Logger.getLogger(ParticleTracker3DModular_.class);

    // General configuration
    private boolean iIsInGuiMode = false;
    private boolean iSaveMss = false;
    
    // Text files input 
    private boolean iInputModeTextFile = false;
    private boolean iInputModeTextFileCsv = false;
    private boolean iInputModeTextFileFrames = false;
    private File iInputModeTextFileSegmentationInfo = null;
    
    // Input parameters
    public ImagePlus iInputImage;
    private FileInfo iInputImageFileInfo = null;

    // Output files
    public String iResultFilesBaseTitle;
    
    // ------------ 
    
    public Img<ARGBType> iTrajImg;
    public MyFrame[] iFrames;
    public Vector<Trajectory> iTrajectories;
    
    /* user defined parameters for linking */
    public int iLinkRange = 2;
    public double displacement = 10.0;
    private boolean force;
    private boolean straight_line;
    private float l_s = 1.0f; 
    private float l_f = 1.0f;
    private float l_d = 1.0f;
    private ParticleLinker iParticleLinker = new ParticleLinkerGreedy();
    
    /* results display and file */
    public int chosen_traj = -1;
    public ResultsWindow results_window;
    
    // file input
    private String files_dir;
    private String file_sel;

    private String background;
    private int iNumOfFrames;
    private int iNumOfSlices;
    
    private FeaturePointDetector detector;
    protected NonBlockingGenericDialog gd;
    private PreviewCanvas preview_canvas = null;
    
    /* vars for text_files_mode */
    private String[] files_list;
    private boolean iCreateImageFromParticles = false;
    private boolean creating_traj_image = false;
    
    private boolean frames_processed = false;
    private ImagePlus iPreviewImage = null;
    
    @Override
    public int setup(String aInputArgs, ImagePlus aInputImage) {
        iIsInGuiMode = !(IJ.isMacro() || Interpreter.batchMode);

        final String options = Macro.getOptions();
        logger.info("Macro Options: [" + options + "]");
        if (options != null) {
            if (MosaicUtils.parseCheckbox("saveMss", options)) {
                logger.info("Save MSS results = true");
                iSaveMss = true;
            }
        }
        
        iInputImage = aInputImage;
        if (iInputImage == null) {
            if (IJ.showMessageWithCancel("Text Files Mode", "Do you want to load particles positions from text files?")) {
                iInputModeTextFile = true;
                return NO_IMAGE_REQUIRED;
            }
            IJ.error("You must load an Image Sequence or Movie first");
            return DONE;
        }
        
        iInputImageFileInfo = iInputImage.getOriginalFileInfo();
        iResultFilesBaseTitle = iInputImage.getTitle();
        
        // Check if there are segmentation information (use it in macro mode or check with user if OK in GUI)
        if (MosaicUtils.checkSegmentationInfo(iInputImage, null)) {
            if (!iIsInGuiMode || new YesNoCancelDialog(null, "Segmentation", "A segmentation has been founded for this image, do you want to track the regions").yesPressed()) { 
                SegmentationInfo info = MosaicUtils.getSegmentationInfo(aInputImage);
                logger.debug("Taking input from segmentation results (file = [" + iInputImage.getTitle() + "], dir = [" + iInputImageFileInfo.directory + "]) ");

                iInputModeTextFile = true;
                iInputModeTextFileCsv = true;
                iInputModeTextFileSegmentationInfo = info.RegionList;

                return NO_IMAGE_REQUIRED;
            }
        }
        
        // If you have an image with n slice and one frame is quite suspicious
        // that the time information is stored in the slice data, prompt if the data are 2D or 3D
        if (iInputImage.getStackSize() > 1 && iInputImage.getNFrames() == 1) {
            final GenericDialog gd = new GenericDialog("Data dimension (this functionality will be removed soon)");
            gd.addChoice("Are these 3D data ?", new String[] { "No", "Yes" }, "No");
            gd.showDialog();

            if (!gd.wasCanceled() && gd.getNextChoice().equals("No")) {
                String saved_options = null;
                if (!iIsInGuiMode && Macro.getOptions() != null && !Macro.getOptions().trim().isEmpty()) {
                    saved_options = Macro.getOptions();
                }
                IJ.run(aInputImage, "Stack to Hyperstack...", "order=xyczt(default) channels=1 slices=1 frames=" + aInputImage.getNSlices() + " display=Composite");
                if (saved_options != null) {
                    Macro.setOptions(saved_options);
                }
            }
        }

        return DOES_ALL + NO_CHANGES;
    }

    @Override
    public void run(ImageProcessor ip) {
        initializeMembers();

        if (!iInputModeTextFile && iIsInGuiMode) {
            preview_canvas = GUIhelper.generatePreviewCanvas(iInputImage);
        }

        if (!getUserDefinedParams()) return;
        if (!processFrames()) return;
        if (linkParticles() == false) return;

        generateTrajectories();
        assignColorsToTrajectories();
        
        if (!iIsInGuiMode) {
            writeDataToDisk();
        }
        else {
            results_window = new ResultsWindow(this, "Results");
            results_window.configuration_panel.append(getConfiguration().toString());
            results_window.configuration_panel.append(getInputFramesInformation().toString());
            results_window.text_panel.appendLine("Particle Tracker DONE!");
            results_window.text_panel.appendLine("Found " + iTrajectories.size() + " Trajectories");
            results_window.setVisible(true);

            IJ.showStatus("Creating trajectory image ...");
            creating_traj_image = true;

            iTrajImg = createHyperStackFromFrames();
        }
    }

    public boolean linkParticles() {
        IJ.showStatus("Linking Particles");
        logger.debug("Link Particles");
        final LinkerOptions lo = new LinkerOptions();
        lo.linkRange = iLinkRange;
        lo.maxDisplacement = (float) displacement;
        lo.force = force;
        lo.straightLine = straight_line;
        lo.lSpace = l_s;
        lo.lFeature = l_f;
        lo.lDynamic = l_d;
        int length = iFrames.length;
        List<Vector<Particle>> particles = new ArrayList<Vector<Particle>>(length);
        for (int i = 0; i < length; ++i) {
            particles.add(iFrames[i].getParticles());
        }
        return iParticleLinker.linkParticles(particles, lo);
    }

    private MyFrame[] convertIntoFrames(Vector<Particle> p) {
        // Read the background if available
        if (background != null && iInputImage == null) {
            iInputImage = new Opener().openImage(new File(background.replace("*", "1")).getAbsolutePath());
        }

        filterParticles(p);
        return createFrames(p);
    }
    
    /**
     * Initializes some members needed before going to previews on the user param dialog.
     */
    private void initializeMembers() {
        if (!iInputModeTextFile) {
            // get global minimum and maximum
            final StackStatistics stack_stats = new StackStatistics(iInputImage);
            final float global_max = (float) stack_stats.max;
            final float global_min = (float) stack_stats.min;
            iNumOfFrames = iInputImage.getNFrames();
            iNumOfSlices = iInputImage.getNSlices();

            detector = new FeaturePointDetector(global_max, global_min);
        }
        else {
            iNumOfSlices = 1;
        }
    }

    /**
     * Iterates through all frames(ImageProcessors or text files). <br>
     * Creates a <code>MyFrame</code> object for each frame according to the input. <br>
     * If non text mode: gets particles by applying <code>featurePointDetection</code> method on the current frame <br>
     * if text mode set particles according to input files <br>
     * Adds every <code>MyFrame</code> created to the <code>frames</code> array <br>
     * Sets the <code>frames_processed</code> flag to true <br>
     * If the frames were already processed do nothing and return true
     */
    private boolean processFrames() {
        if (frames_processed) {
            return true;
        }

        // text file mode starts here
        if (iInputModeTextFile && iInputModeTextFileCsv == true) {
            IJ.showStatus("Reading CSV Regions data ...");
            final CSV<Particle> P_csv = new CSV<Particle>(Particle.class);

            P_csv.setCSVPreferenceFromFile(files_dir + File.separator + file_sel);
            final Vector<Particle> p = P_csv.Read(files_dir + File.separator + file_sel, null);

            iInputImageFileInfo = new FileInfo();
            iInputImageFileInfo.directory = files_dir;

            if (p.size() == 0) {
                IJ.error("No regions defined for this image,nothing to do");
                return false;
            }
            background = P_csv.getMetaInformation("background");

            IJ.showStatus("Creating frames with particles ...");

            iFrames = convertIntoFrames(p);
            // It can happen that the segmentation algorithm produce double output on the CSV
            for (int i = 0; i < iFrames.length; i++) {
                iFrames[i].removeDuplicatedParticles();
            }

            iNumOfFrames = iFrames.length;
        }
        else {
            if (iInputModeTextFile) {
                iInputImageFileInfo = new FileInfo();
                iInputImageFileInfo.directory = files_dir;
            }
            iFrames = new MyFrame[iNumOfFrames];
            for (int frame_i = 0, file_index = 0; frame_i < iNumOfFrames; frame_i++, file_index++) {
                MyFrame current_frame = null;
                if (iInputModeTextFileFrames) {
                    if (files_list[file_index].startsWith(".") || files_list[file_index].endsWith("~")) {
                        frame_i--;
                        continue;
                    }

                    // text_files_mode: construct each frame from the corresponding text file
                    IJ.showStatus("Reading Particles from file " + files_list[file_index] + "(" + (frame_i) + "/" + files_list.length + ")");
                    current_frame = new MyFrame(files_dir + files_list[file_index]);
                    if (current_frame.getParticles() == null) {
                        return false;
                    }
                }
                else {
                    // sequence of images mode: construct each frame from the corresponding image
                    ImageStack frameStack = MosaicUtils.getSubStackInFloat(iInputImage.getStack(), (frame_i) * iNumOfSlices + 1, (frame_i + 1) * iNumOfSlices, false /*duplicate*/);
                    current_frame = new MyFrame(frame_i);

                    // Detect feature points in this frame
                    IJ.showStatus("Detecting Particles in Frame " + (frame_i + 1) + "/" + iNumOfFrames);
                    logger.info("Detecting particles in frame: " + (frame_i + 1) + "/" + iNumOfFrames);
                    Vector<Particle> detectedParticles = detector.featurePointDetection(frameStack);
                    current_frame.setParticles(detectedParticles);
                }
                if (current_frame.iFrameNumber >= iFrames.length) {
                    IJ.showMessage("Error, frame " + current_frame.iFrameNumber + "  is out of range, enumeration must not have hole, and must start from 0");
                    return false;
                }
                iFrames[current_frame.iFrameNumber] = current_frame;
            } // for

            // Here check that all frames are created
            for (int i = 0; i < iFrames.length; ++i) {
                if (iFrames[i] == null) {
                    IJ.showMessage("Error, frame: " + i + " does not exist");
                    return false;
                }
            }
        }
        
        if (iCreateImageFromParticles == true) {
            IJ.showStatus("Creating image from particles and trajectories ...");

            final Img<ARGBType> iw = createHyperStackFromFrames();
            if (iw != null) {
                iInputImage = ImageJFunctions.wrap(iw, "Video");
                iInputImage.show();
            }
            else {
                return false;
            }
        }
        
        frames_processed = true;

        return true;
    }

    /**
     * Displays a dialog window to get user defined params and selections,
     * also initialize and sets other params according to the work mode.<br>
     * <ul>
     * For a sequence of images:
     * <ul>
     * <li>Gets user defined params:<code> radius, cutoff, precentile, linkrange, displacement</code>
     * <li>Displays the preview Button and slider
     * <li>Gives the option to convert the image seq to 8Bit if its color
     * <li>Initialize and sets params:<code> stack, title, global_max, global_min, mask, kernel</code>
     * </ul>
     * For text_files_mode:
     * <ul>
     * <li>Gets user defined params:<code> linkrange, displacement </code>
     * <li>Initialize and sets params:<code> files_list, title, frames_number, one_file_multiple_frame </code>
     * </ul>
     * </ul>
     * 
     * @return false if cancel button clicked or problem with input
     */
    private boolean getUserDefinedParams() {
        gd = new NonBlockingGenericDialog("Particle Tracker...");
        Panel p = new Panel();
        final Button help_b = new Button("help");
        p.add(help_b);
        gd.addPanel(p);
        help_b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                final Point p = gd.getLocationOnScreen();
                new ParticleTrackerHelp(p.x, p.y);

            }
        });

        boolean convert = false;
        if (iInputModeTextFile) {
            if (iInputModeTextFileSegmentationInfo == null) {
                // Create dialog 
                GenericDialog textModeDialog = new GenericDialog("input text files type", IJ.getInstance());
                String opts[] = {"multiple frame files", "CSV File"};
                textModeDialog.addRadioButtonGroup("Please specify the info provided for the Particles...", opts, opts.length, 1, opts[0]);
                textModeDialog.addCheckbox("Create initial image from read data", true);
                textModeDialog.showDialog();
                
                // Check what user decided
                if (textModeDialog.wasCanceled()) {
                    return false;
                }
                final String typeOfInputFile = textModeDialog.getNextRadioButton();
                int idx = Arrays.asList(opts).indexOf(typeOfInputFile);
                iInputModeTextFileFrames = (idx == 0) ? true : false;
                iInputModeTextFileCsv = !iInputModeTextFileFrames;
                iCreateImageFromParticles = textModeDialog.getNextBoolean(); // Should create img?
                
                // gets the input files directory form
                files_list = getFilesList();
                if (files_list == null) {
                    return false;
                }

                if (iInputModeTextFileCsv == true) {
                    final Vector<String> v = new Vector<String>();

                    for (int i = 0; i < files_list.length; i++) {
                        final File f = new File(files_dir + File.separator + files_list[i]);
                        if (files_list[i].endsWith("csv") == true && f.exists() && f.isDirectory() == false) {
                            v.add(files_list[i]);
                        }
                    }

                    files_list = new String[v.size()];
                    v.toArray(files_list);
                }

                iResultFilesBaseTitle = "text_files";
                iNumOfFrames = 0;
                // EACH!! file in the given directory is considered as a frame
                for (int i = 0; i < files_list.length; i++) {
                    if (!files_list[i].startsWith(".") && !files_list[i].endsWith("~")) {
                        iNumOfFrames++;
                    }
                }
            }
            else {
                files_dir = iInputModeTextFileSegmentationInfo.getParent();
                file_sel = iInputModeTextFileSegmentationInfo.getName();
                logger.debug("Segmentation Mode, output set to: ["+files_dir +"]["+file_sel+"]");
            }
        }
        else {
            GUIhelper.addUserDefinedParametersDialog(gd, detector);

            gd.addPanel(GUIhelper.makePreviewPanel(this, iInputImage), GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));

            // check if the original images are not GRAY8, 16 or 32
            if (iInputImage.getType() != ImagePlus.GRAY8 && iInputImage.getType() != ImagePlus.GRAY16 && iInputImage.getType() != ImagePlus.GRAY32) {
                gd.addCheckbox("Convert to Gray8 (recommended)", true);
                convert = true;
            }
        }

        gd.addMessage("Particle Linking:\n");
        // These 2 params are relevant for both working modes
        gd.addNumericField("Link Range", 2, 0);
        gd.addNumericField("Displacement", 10.0, 2);

        final String d_pos[] = { "Brownian", "Straight lines", "Constant velocity" };
        gd.addChoice("Dynamics: ", d_pos, d_pos[0]);

        // Create advanced option panel
        final Button a_opt = new Button("Advanced options");
        a_opt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                createLinkFactorDialog();
            }
        });
        if (!iIsInGuiMode) {
            // In no gui mode creating that dialog allows to read parameters from macro arguments
            createLinkFactorDialog();
        }
        final Panel preview_panel = new Panel();
        preview_panel.add(a_opt);
        gd.addPanel(preview_panel);

        final JLabel labelJ = new JLabel("<html>Please refer to and cite:<br><br>" + "I. F. Sbalzarini and P. Koumoutsakos.<br> Feature Point "
                + "Tracking and<br> Trajectory Analysis for<br>Video Imaging in Cell Biology,<br>" + "Journal of Structural Biology<br> 151(2):182-195, 2005.<br>" + "</html>");
        p = new Panel();
        p.add(labelJ);
        gd.addPanel(p);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return false;
        }

        // retrieve params from user
        if (!iInputModeTextFile) {
            final Boolean changed = GUIhelper.getUserDefinedParameters(gd, detector);
            // even if the frames were already processed (particles detected) but
            // the user changed the detection params then the frames needs to be processed again
            if (changed) {
                if (frames_processed) {
                    iFrames = null;
                    frames_processed = false;
                }
            }

            // add the option to convert only if images are not GRAY8, 16 or 32
            if (convert) {
                convert = gd.getNextBoolean();
            }
        }
        iLinkRange = (int) gd.getNextNumber();
        displacement = gd.getNextNumber();

        final String dm = gd.getNextChoice();

        if (dm.equals("Brownian")) {
            force = false;
            straight_line = false;
        }
        else if (dm.equals("Straight lines")) {
            force = false;
            straight_line = true;
        }
        else if (dm.equals("Constant velocity")) {
            force = true;
            straight_line = false;
        }

        // if user choose to convert reset stack, title, frames number and global min, max
        if (convert) {
            StackConverter sc = new StackConverter(iInputImage);
            sc.convertToGray8();
            iResultFilesBaseTitle = iInputImage.getTitle();
            final StackStatistics stack_stats = new StackStatistics(iInputImage);
            detector.setGlobalMax((float) stack_stats.max);
            detector.setGlobalMin((float) stack_stats.min);
            iNumOfFrames = iInputImage.getNFrames(); // ??maybe not necessary
        }
        return true;
    }

    /**
     * Generate a String header of each colum
     */
    public String trajectoryHeader() {
        String unit = "pixel"; //iInputImage.getCalibration().getUnit();
        return new String("%% frame x (" + unit + ")     y (" + unit + ")    z (" + unit + ")      m0 " + "        m1 " + "          m2 " + "          m3 " + "          m4 " + "          s \n");
    }

    /**
     * Generates (in real time) a "ready to print" report with all trajectories info.
     * <br>
     * For each Trajectory:
     * <ul>
     * <li>Its serial number
     * <li>All frames of this trajectory with infomation about the particle in each frame
     * </ul>
     * 
     * @return a <code>StringBuffer</code> that holds this information
     */
    private StringBuffer getTrajectoriesInfo() {
        final StringBuffer traj_info = new StringBuffer("%% Trajectories:\n");
        traj_info.append("%%\t 1st column: frame number\n");
        traj_info.append("%%\t 2nd column: x coordinate top-down" + "(pixel)" + "\n");
        traj_info.append("%%\t 3rd column: y coordinate left-right" + "(pixel)" + "\n");
        traj_info.append("%%\t 4th column: z coordinate bottom-top" + "(pixel)" + "\n");
        if (iInputModeTextFile) {
            traj_info.append("%%\t next columns: other information provided for each particle in the given order\n");
        }
        else {
            traj_info.append("%%\t 4th column: zero-order intensity moment m0\n");
            traj_info.append("%%\t 5th column: first-order intensity moment m1\n");
            traj_info.append("%%\t 6th column: second-order intensity moment m2\n");
            traj_info.append("%%\t 7th column: second-order intensity moment m3\n");
            traj_info.append("%%\t 8th column: second-order intensity moment m4\n");
            traj_info.append("%%\t 9th column: non-particle discrimination score\n");
        }
        traj_info.append("\n");

        for (Trajectory curr_traj : iTrajectories) {
            traj_info.append("%% Trajectory " + curr_traj.iSerialNumber + "\n");
            traj_info.append(trajectoryHeader());
            traj_info.append(curr_traj.toStringBuffer());
        }

        return traj_info;
    }

    private void writeDataToDisk() {
        logger.debug("Writing data to disk");
        if (iInputImageFileInfo == null) {
            IJ.error("You're running a macro. Data are written to disk at the directory where your image is stored. Please store youre image first.");
            return;
        }
        logger.debug("Dir ["+iInputImageFileInfo.directory+"]");
        MosaicUtils.write2File(iInputImageFileInfo.directory, "Traj_" + iResultFilesBaseTitle + ".txt", getFullReport().toString());
        if (!iInputModeTextFile) new TrajectoriesReportXML(new File(iInputImageFileInfo.directory, "Traj_" + iResultFilesBaseTitle + ".xml").getAbsolutePath(), this);
        try {
            if (iSaveMss) {
                CalibrationData calData = getImageCalibrationData();
                if (calData.errorMsg == null) {
                    ResultsTable rtMss = mssAllResultsToTable(calData.pixelDimension, calData.timeInterval);
                    rtMss.saveAs(new File(iInputImageFileInfo.directory, "TrajMss_" + iResultFilesBaseTitle + ".csv").getAbsolutePath());
                }
            }
            final ResultsTable rt = generateResultsTableWithTrajectories();
            rt.saveAs(new File(iInputImageFileInfo.directory, "Traj_" + iResultFilesBaseTitle + ".csv").getAbsolutePath());
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Detects particles in the current displayed frame according to the parameters currently set
     * Draws dots on the positions of the detected partciles on the frame and circles them
     * 
     * @see #getUserDefinedPreviewParams()
     * @see MyFrame#featurePointDetection()
     * @see PreviewCanvas
     */
    private synchronized void preview(int zDepth) {
        if (iInputImage == null) {
            return;
        }

        GUIhelper.getUserDefinedPreviewParams(gd, detector);

        final ImagePlus frame = MosaicUtils.getImageFrame(iInputImage, iInputImage.getFrame());

        ImageStack frameStack = frame.getStack();
        final MyFrame preview_frame = new MyFrame(iInputImage.getFrame());

        Vector<Particle> detectedParticles = detector.featurePointDetection(frameStack);
        preview_frame.setParticles(detectedParticles);
        final Img<FloatType> backgroundImg = ImagePlusAdapter.convertFloat(frame);

        preview_frame.setParticleRadius(getRadius());
        final Img<ARGBType> img_frame = preview_frame.createImage(backgroundImg, frame.getCalibration());

        ImagePlus wrap = ImageJFunctions.wrap(img_frame, "Preview detection");
        if (iPreviewImage == null) iPreviewImage = wrap;
        else iPreviewImage.setImage(wrap);
        iPreviewImage.setSlice(zDepth);
        iPreviewImage.show();
    }

    public void setDrawingParticle(boolean showParticles) {
        for (Trajectory t : iTrajectories) {
            t.showParticles = showParticles;
        }
    }
    
    /**
     * Resets the trajectories filter so no trajectory is filtered by
     * setting the <code>to_display</code> param of each trajectory to true
     */
    public void resetTrajectoriesFilter() {
        for (Trajectory t : iTrajectories) {
            t.to_display = true;
        }
    }

    /**
     * Generates (in real time) a "ready to print" report with information
     * about the user defined parameters:
     * <ul>
     * <li>Radius
     * <li>Cutoff
     * <li>Percentile
     * <li>Displacement
     * <li>Linkrange
     * </ul>
     * 
     * @return a <code>StringBuffer</code> that holds this information
     */
    public StringBuffer getConfiguration() {
        final StringBuffer configuration = new StringBuffer("% Configuration:\n");
        if (!iInputModeTextFile) {
            configuration.append("% \tKernel radius: ");
            configuration.append(getRadius());
            configuration.append("\n");
            configuration.append("% \tCutoff radius: ");
            configuration.append(detector.getCutoff());
            configuration.append("\n");
            if (detector.getThresholdMode() == Mode.PERCENTILE_MODE) {
                configuration.append("% \tPercentile: ");
                configuration.append((detector.getPercentile() * 100));
                configuration.append("\n");
            }
            else if (detector.getThresholdMode() == Mode.ABS_THRESHOLD_MODE) {
                configuration.append("% \tAbsolute threshold: ");
                configuration.append((detector.getAbsIntensityThreshold()));
                configuration.append("\n");
            }

        }
        configuration.append("% \tDisplacement : ");
        configuration.append(displacement);
        configuration.append("\n");
        configuration.append("% \tLinkrange    : ");
        configuration.append(iLinkRange);
        configuration.append("\n");
        return configuration;
    }

    /**
     * Generates (in real time) a "ready to print" report with this information
     * about the frames that were given as input (movie or images):
     * <ul>
     * <li>Width
     * <li>Height
     * <li>Global pixel intensity max
     * <li>Global pixel intensity min
     * </ul>
     * 
     * @return a <code>StringBuffer</code> that holds this information
     */
    public StringBuffer getInputFramesInformation() {
        if (iInputModeTextFile) {
            return new StringBuffer("Frames info was loaded from text files");
        }
        final StringBuffer info = new StringBuffer("% Frames information:\n");
        info.append("% \tWidth : ");
        info.append(iInputImage.getStack().getWidth());
        info.append(" pixel\n");
        info.append("% \tHeight: ");
        info.append(iInputImage.getStack().getHeight());
        info.append(" pixel\n");
        info.append("% \tDepth: ");
        info.append(iNumOfSlices);
        info.append(" slices\n");
        info.append("% \tGlobal minimum: ");
        info.append(detector.getGlobalMin());
        info.append("\n");
        info.append("% \tGlobal maximum: ");
        info.append(detector.getGlobalMax());
        info.append("\n");
        return info;
    }

    /**
     * Creates a new view of the trajectories as an overlay on the given <code>ImagePlus</code>.
     * <br>
     * The new view is an instance of <code>TrajectoryStackWin</code>
     * <br>
     * If the given image is null, a new <code>ImagePlus</code> is duplicated from <code>original_imp</code>
     * <br>
     * If the given image is NOT null, the overlay view is RE-created on top of it
     * <br>
     * The trajectories are drawn on <code>TrajectoryCanvas</code> when it's constructed and not on the image
     * 
     * @param duplicated_imp the image upon which the view will be updated - can be null
     * @see TrajectoryStackWindow
     */
    public void generateView(ImagePlus duplicated_imp, Img<ARGBType> out) {
        if (iTrajectories.size() == 0) {
            IJ.error("There are no any trajectories detected - nothing to visualize.");
            return;
        }
        final String new_title = "All Trajectories Visual";

        if (duplicated_imp == null) {
            if (out == null) {
                if (creating_traj_image == true) {
                    IJ.error("One moment please ..., we are computing the image");
                    return;
                }
                IJ.error("An internal error has occurred we are not able to compute the result image");
                return;
            }

            // if there is no image to generate the view on:
            // generate a new image by duplicating the original image
            duplicated_imp = ImageJFunctions.wrap(out, new_title);
            duplicated_imp.show();
        }
        else {
            // if the view is generated on an already existing image,
            // set the updated view scale (magnification) to be the same as in the existing image
            // magnification = duplicated_imp.getWindow().getCanvas().getMagnification();
            duplicated_imp.setImage(ImageJFunctions.wrap(out, new_title));
        }

        // Create a new window to hold the image and canvas
        new TrajectoryStackWin(this, duplicated_imp, duplicated_imp.getWindow().getCanvas(), out);
    }

    /**
     * Take an image take the ROI, scale (2D) it without interpolation
     * and convert to RGB in one shot
     *
     * @param img is the Image
     * @param r is the Rectangle of focusing
     * @param magnification is the scaling size (suppose to be > 1)
     */

    private <T extends RealType<T>> Img<ARGBType> copyScaleConvertToRGB(RandomAccessibleInterval<T> img, FinalInterval r, int magnification) {
        // Create output image

        final long[] sz = new long[img.numDimensions()];
        r.dimensions(sz);

        sz[0] *= magnification;
        sz[1] *= magnification;

        final ImgFactory<ARGBType> imgFactory = new ArrayImgFactory<ARGBType>();
        final Img<ARGBType> output = imgFactory.create(sz, new ARGBType());

        // Interval

        final IntervalView<T> vi = Views.interval(img, r);
        final IterableInterval<T> fvi = Views.flatIterable(vi);
        final Cursor<T> cur = fvi.cursor();
        final RandomAccess<ARGBType> rc_o = output.randomAccess();

        final ToARGB pixel_converter = MosaicUtils.getConversion(cur.get(), fvi.cursor());

        final int start_frame = (int) r.min(rc_o.numDimensions() - 1);
        final int start_x = (int) r.min(0);
        final int start_y = (int) r.min(1);
        final int loc[] = new int[rc_o.numDimensions()];
        final int loc_p[] = new int[rc_o.numDimensions()];

        while (cur.hasNext()) {
            final ARGBType a = pixel_converter.toARGB(cur.next());
            cur.localize(loc);
            cur.localize(loc_p);

            for (int i = 0; i < magnification; i++) {
                for (int j = 0; j < magnification; j++) {
                    loc_p[0] = (loc[0] - start_x) * magnification + i;
                    loc_p[1] = (loc[1] - start_y) * magnification + j;
                    loc_p[rc_o.numDimensions() - 1] = loc[rc_o.numDimensions() - 1] - start_frame;

                    rc_o.setPosition(loc_p);
                    rc_o.get().set(a);
                }
            }
        }

        return output;
    }

    /**
     * Generates and displays a new <code>StackWindow</code> with rescaled (magnified)
     * view of the trajectory specified by the given <code>trajectory_index</code>.
     * <br>
     * The new Stack will be made of RGB ImageProcessors upon which the trajectory will be drawn
     * 
     * @param trajectory_index the trajectory index in the <code>trajectories</code> Vector (starts with 0)
     * @param magnification the scale factor to use for rescaling the original image
     * @see IJ#run(java.lang.String, java.lang.String)
     * @see ImagePlus#getRoi()
     * @see StackConverter#convertToRGB()
     * @see Trajectory#animate(int)
     */
    public void generateTrajFocusView(int trajectory_index) {
        int magnification = results_window.magnification_factor;
        // create a title
        final String new_title = "[Trajectory number " + (trajectory_index + 1) + "]";

        // get the trajectory at the given index
        final Trajectory traj = (iTrajectories.elementAt(trajectory_index));
        final Rectangle r = traj.focus_area.getBounds();

        // Create a cropped rescaled image
        final Img<UnsignedByteType> img = ImagePlusAdapter.wrap(iInputImage);
        final long min[] = new long[img.numDimensions()];
        final long max[] = new long[img.numDimensions()];

        min[0] = r.x;
        max[0] = r.x + r.width;
        min[1] = r.y;
        max[1] = r.y + r.height;
        for (int i = 2; i < img.numDimensions() - 1; i++) {
            min[i] = 0;
            max[i] = img.dimension(i);
        }
        min[img.numDimensions() - 1] = traj.getStartFrame();
        max[img.numDimensions() - 1] = traj.getStopFrame();

        final FinalInterval in = new FinalInterval(min, max);

        final Img<ARGBType> focus_view = copyScaleConvertToRGB(img, in, magnification);

        IJ.showStatus("Creating frames ... ");
        final Vector<Trajectory> vt = new Vector<Trajectory>();
        vt.add(traj);
        final Calibration cal = iInputImage.getCalibration();
        MyFrame.updateImage(focus_view, traj.focus_area.getBounds(), traj.getStartFrame(), vt, cal, DrawType.TRAJECTORY_HISTORY, getRadius());

        final ImagePlus imp = ImageJFunctions.show(focus_view);
        imp.setTitle(new_title);

        new FocusStackWin(imp, traj, (float) cal.pixelDepth);

        IJ.showStatus("Done");
    }

    /**
     * Generates and displays a new <code>StackWindow</code> with rescaled (magnified)
     * view of the Roi that was selected on ImageJs currently active window. <br>
     * The new Stack will be made of RGB ImageProcessors upon which the trajectories in the Roi will be drawn <br>
     * If Roi was not selected, an imageJ error is displayed and no new window is created
     * 
     * @param magnification the scale factor to use for rescaling the original image
     * @see IJ#run(java.lang.String, java.lang.String)
     * @see ImagePlus#getRoi()
     * @see StackConverter#convertToRGB()
     * @see Trajectory#animate(int)
     */
    public void generateAreaFocusView(int magnification) {

        final String new_title = "[Area Focus]";
        // Save the ID of the last active image window - the one the ROI was selected on
        final int roi_image_id = IJ.getImage().getID();

        // Get the ROI and check its valid
        final Roi user_roi = IJ.getImage().getRoi();
        if (user_roi == null) {
            IJ.error("generateAreaFocusView: No Roi was selected");
            return;
        }

        // ImageJ macro command to rescale and image the select ROI in the active window
        // this will create a new ImagePlus (stack) that will be the active window
        IJ.run("Scale...", "x=" + magnification + " y=" + magnification + " process create title=" + new_title);

        // Get the new-scaled image (stack) and assign it duplicated_imp
        final ImagePlus duplicated_imp = IJ.getImage();

        // Convert the stack to RGB so color can been drawn on it and get its ImageStack
        IJ.run("RGB Color");

        // Reset the active imageJ window to the one the ROI was selected on - info from the Roi is still needed
        IJ.selectWindow(roi_image_id);

        IJ.selectWindow(duplicated_imp.getID());
    }

    /**
     * Opens an 'open file' dialog where the user can select a folder
     * @return an array of All the file names in the selected folder
     *         or null if the user cancelled the selection.
     *         is some O.S (e.g. Linux) this may include '.' and '..'
     */
    private String[] getFilesList() {
        /* Opens an 'open file' with the default directory as the imageJ 'image' directory */
        final OpenDialog od = new OpenDialog("test", IJ.getDirectory("image"), "");

        files_dir = od.getDirectory();
        file_sel = od.getFileName();
        if (files_dir == null) {
            return null;
        }
        final String[] list = new File(od.getDirectory()).list();
        return list;
    }

    /**
     * Create an image stack or hyperstack from frames
     */
    public Img<ARGBType> createHyperStackFromFrames() {
        return createHyperStackFromFrames(background);
    }
    
    private Img<ARGBType> createHyperStackFromFrames(String aBackgroundFilename) {
        int[] maxDims = getParticlesRange();
        for (int i = 0; i < maxDims.length; i++) { maxDims[i] += 1; }
        
        // Create empty image for trajectories visualization
        Img<ARGBType> outputImg = null;
        if (iInputModeTextFile == true) {
            if (aBackgroundFilename == null) {
                // Create dims with additional "number of frames" dimension
                final long extendedDims[] = new long[maxDims.length + 1];
                for (int i = 0; i < maxDims.length; i++) { extendedDims[i] = maxDims[i]; }
                extendedDims[maxDims.length] = iFrames.length;

                outputImg = new CellImgFactory<ARGBType>().create(extendedDims, new ARGBType());
            }
            else {
                // Open first background to get the original size of tracked data
                if (iInputImage == null) {
                    final File file = new File(aBackgroundFilename.replace("*", Integer.toString(1)));
                    iInputImage = new Opener().openImage(file.getAbsolutePath());
                    
                    if (iInputImage != null) {
                        IJ.error("Cannot open the background " + aBackgroundFilename);
                        creating_traj_image = false;
                        return null;
                    }
                }

                // Generate extedned dimensions (single frame dims + number of frames)
                ImagePlus imp = (iInputImage.getNFrames() > 1) ? MosaicUtils.getImageFrame(iInputImage, 1) : iInputImage;
                final Img<UnsignedByteType> backgroundImg = ImagePlusAdapter.wrap(imp);
                long extendedDims[] = new long[backgroundImg.numDimensions() + 1];
                for (int i = 0; i < backgroundImg.numDimensions(); i++) { extendedDims[i] = backgroundImg.dimension(i); }
                extendedDims[backgroundImg.numDimensions()] = iFrames.length;
                
                outputImg = new CellImgFactory<ARGBType>().create(extendedDims, new ARGBType());
            }
        }
        else {
            // Generate extedned dimensions (single frame dims + number of frames) from original image
            final Img<UnsignedByteType> backgroundImg = ImagePlusAdapter.wrap(iInputImage);
            final long extendedDims[] = new long[backgroundImg.numDimensions()];
            for (int i = 0; i < backgroundImg.numDimensions(); i++) { extendedDims[i] = backgroundImg.dimension(i); }
            outputImg = new CellImgFactory<ARGBType>().create(extendedDims, new ARGBType());
        }

        // for each frame we have to add a stack to the image
        Img<ARGBType> frameImg = null;
        for (int i = 0; i < iFrames.length; i++) {
            IJ.showStatus("Creating frame " + (i + 1));
            if (iInputModeTextFile == true) {
                if (aBackgroundFilename != null) {
                    // Get next frame or channel image
                    ImagePlus imp = null;
                    if (iInputImage.getNFrames() > 1 && i < iInputImage.getNFrames()) {
                        imp = MosaicUtils.getImageFrame(iInputImage, i + 1);
                    }
                    else if (iInputImage.getNChannels() >= 1 && i < iInputImage.getNChannels()) {
                        imp = MosaicUtils.getImageSlice(iInputImage, i + 1);
                    }
                    if (imp == null) {
                        IJ.error("Cannot find the background image or wrong format");
                        creating_traj_image = false;
                        return null;
                    }

                    // wrap it into an ImgLib image (no copying)
                    final Img<UnsignedByteType> backgroundImg = ImagePlusAdapter.wrap(imp);
                    frameImg = iFrames[i].createImage(backgroundImg, iTrajectories, iInputImage.getCalibration(), i, DrawType.TRAJECTORY_HISTORY);
                }
                else {
                    frameImg = iFrames[i].createImage(maxDims, iTrajectories, i, DrawType.TRAJECTORY_HISTORY);
                }
            }
            else {
                final ImagePlus timp = MosaicUtils.getImageFrame(iInputImage, i + 1);
                final Img<UnsignedByteType> backgroundImg = ImagePlusAdapter.wrap(timp);
                iFrames[i].setParticleRadius(getRadius());
                frameImg = iFrames[i].createImage(backgroundImg, iTrajectories, iInputImage.getCalibration(), i, DrawType.TRAJECTORY_HISTORY);
            }
            if (frameImg == null) {
                break;
            }

            MosaicUtils.copyEmbedded(outputImg, frameImg, i);
        }

        IJ.showStatus("Done");
        return outputImg;
    }

    /**
     * Generates (in real time) a "ready to print" report with this information:
     * <ul>
     * <li>System configuration
     * <li>Frames general information
     * <li>Per frame information about detected particles
     * <li>Particles linking
     * <li>All trajectories found
     * </ul>
     * 
     * @return a <code>StringBuffer</code> that holds this information
     */
    public StringBuffer getFullReport() {

        /* initial infomation to output */
        final StringBuffer report = new StringBuffer();
        report.append(getConfiguration());
        report.append(getInputFramesInformation());
        report.append("\n");

        /* detected particles infomation per frame */
        report.append("%\tPer frame information (verbose output):\n");
        for (int i = 0; i < iFrames.length; i++) {
            report.append(iFrames[i].getFullFrameInfo());
        }

        /* Add linking info */
        report.append("% Trajectory linking (verbose output):\n");
        for (int i = 0; i < iFrames.length; i++) {
            report.append(iFrames[i].toStringBuffer());
        }

        /* all trajectories info */
        report.append("\n");
        report.append(getTrajectoriesInfo());

        return report;
    }

    @Override
    public void preview(ActionEvent e, int zDepth) {
        // set the original_imp window position next to the dialog window
        iInputImage.getWindow().setLocation((int) gd.getLocationOnScreen().getX() + gd.getWidth(), (int) gd.getLocationOnScreen().getY());
        // do preview
        preview(zDepth);
        preview_canvas.repaint();
    }

    @Override
    public void saveDetected(ActionEvent e) {
        /* set the user defined pramars according to the valus in the dialog box */
        GUIhelper.getUserDefinedPreviewParams(gd, detector);

        /* detect particles and save to files */
        if (processFrames()) { // process the frames
            saveDetected(iFrames);
        }
        preview_canvas.repaint();
    }

    private void saveDetected(MyFrame[] frames) {
        final SaveDialog sd = new SaveDialog("Save Detected Particles", IJ.getDirectory("image"), "frame", "");
        if (sd.getDirectory() == null || sd.getFileName() == null) {
            return;
        }

        // for each frame - save the detected particles
        for (MyFrame f : frames) {
            String fileName = sd.getFileName() + "_" + f.iFrameNumber;
            if (!MosaicUtils.write2File(sd.getDirectory(), fileName, f.frameDetectedParticlesForSave(false).toString())) {
                IJ.log("Problem occured while writing to file. Directory: [" + sd.getDirectory() + "] File: [" + fileName + "]");
                return;
            }
        }
    }
    
    /**
     * Generate ResultsTable with all detected particles.
     */
    public ResultsTable generateResultsTableWithParticles() {
        final ResultsTable rt = getResultsTable();

        if (rt != null) {
            for (MyFrame f : iFrames) {
                for (final Particle p : f.getParticles()) {
                    rt.incrementCounter();
                    int rownum = rt.getCounter() - 1;
                    addParticleInfo(rt, rownum, p);
                }
            }
        }
        
        return rt;
    }

    /**
     * Generate ResultsTable with all trajectories.
     */
    public ResultsTable generateResultsTableWithTrajectories() {
        final ResultsTable rt = getResultsTable();
        
        if (rt != null) {
            for (Trajectory curr_traj : iTrajectories) {
                putOneTrajectoryIntoResultsTable(rt, curr_traj);
            }
        }
        
        return rt;
    }

    /**
     * Coordinates of selected trajectory will be copied to ImageJ results table.
     * @param aSelectedTrajectory
     */
    public ResultsTable transferSelectedTrajectoriesToResultTable(Trajectory aSelectedTrajectory) {
        final ResultsTable rt = getResultsTable();
        if (rt != null) {
            putOneTrajectoryIntoResultsTable(rt, aSelectedTrajectory);
        }
        return rt;
    }

    private void putOneTrajectoryIntoResultsTable(final ResultsTable rt, final Trajectory curr_traj) {
        for (final Particle p : curr_traj.iParticles) {
            rt.incrementCounter();
            int rownum = rt.getCounter() - 1;
            rt.setValue("Trajectory", rownum, curr_traj.iSerialNumber);
            addParticleInfo(rt, rownum, p);
        }
    }

    private void addParticleInfo(final ResultsTable rt, int rownum, final Particle p) {
        rt.setValue("Frame", rownum, p.getFrame());
        rt.setValue("x", rownum, p.iX);
        rt.setValue("y", rownum, p.iY);
        rt.setValue("z", rownum, p.iZ);
        rt.setValue("m0", rownum, p.m0);
        rt.setValue("m1", rownum, p.m1);
        rt.setValue("m2", rownum, p.m2);
        rt.setValue("m3", rownum, p.m3);
        rt.setValue("m4", rownum, p.m4);
        rt.setValue("NPscore", rownum, p.nonParticleDiscriminationScore);
    }

    private ResultsTable getResultsTable() {
        final ResultsTable rt = ResultsTable.getResultsTable();
        if ((rt.getCounter() != 0) || (rt.getLastColumn() != -1)) {
            if (IJ.showMessageWithCancel("Results Table", "Reset Results Table?")) {
                rt.reset();
            }
            else {
                return null;
            }
        }

        return rt;
    }

    private void computeMssForOneTrajectory(ResultsTable rt, Trajectory currentTrajectory, double aPixelDimensions, double aTimeInterval) {
        final TrajectoryAnalysis ta = new TrajectoryAnalysis(currentTrajectory);
        ta.setLengthOfAPixel(aPixelDimensions);
        ta.setTimeInterval(aTimeInterval);
        if (ta.calculateAll() == TrajectoryAnalysis.SUCCESS) {
            rt.incrementCounter();
            final int rownum = rt.getCounter() - 1;
            rt.setValue("Trajectory", rownum, currentTrajectory.iSerialNumber);
            rt.setValue("Trajectory length", rownum, currentTrajectory.getLength());
            rt.setValue("MSS: slope", rownum, ta.getMSSlinear());
            rt.setValue("MSS: y-axis intercept", rownum, ta.getMSSlinearY0());
            // second element in 'gammas' array is an order=2 (MSD)
            final int secondOrder = 1;
            rt.setValue("MSD: slope", rownum, ta.getGammasLogarithmic()[secondOrder]);
            rt.setValue("MSD: y-axis intercept", rownum, ta.getGammasLogarithmicY0()[secondOrder]);
            rt.setValue("Diffusion Coefficient D2 (m^2/s)", rownum, ta.getDiffusionCoefficients()[secondOrder]);
            rt.setValue("Distance (m)", rownum, ta.getDistance());
            rt.setValue("AvgDistance (m/frame)", rownum, ta.getAvgDistance());
            rt.setValue("Straightness", rownum, ta.getStraightness());
            rt.setValue("Bending", rownum, ta.getBending());
            rt.setValue("Bending (linear)", rownum, ta.getBendingLinear());
            rt.setValue("Efficiency", rownum, ta.getEfficiency());
            rt.setValue("Pixel size", rownum, aPixelDimensions);
            rt.setValue("Time interval", rownum, aTimeInterval);
        }
    }

    public ResultsTable mssTrajectoryResultsToTable(Trajectory aTrajectory, double aPixelDimensions, double aTimeInterval) {
        final ResultsTable rt = getResultsTable();

        if (rt != null) {
            for (int i = 4; i < 12; i++) rt.setDecimalPlaces(i, 8);
            computeMssForOneTrajectory(rt, aTrajectory, aPixelDimensions, aTimeInterval);

            if (iIsInGuiMode == true) {
                rt.show("Results");
            }
        }

        return rt;
    }

    public ResultsTable mssAllResultsToTable(double aPixelDimensions, double aTimeInterval) {
        final ResultsTable rt = getResultsTable();
        if (rt != null) {
            rt.reset();
            final Iterator<Trajectory> iter = iTrajectories.iterator();
            while (iter.hasNext()) {
                final Trajectory currentTrajectory = iter.next();
                computeMssForOneTrajectory(rt, currentTrajectory, aPixelDimensions, aTimeInterval);
            }
            logger.info("Computed MSS for " + rt.getCounter() + " trajectories.");

            if (iIsInGuiMode == true) {
                rt.show("Results");
            }
        }

        return rt;
    }

    public class CalibrationData {
        public CalibrationData(Double aPixelDimension, Double aTimeInterval, String aErrorMsg) {
            pixelDimension = aPixelDimension;
            timeInterval = aTimeInterval;
            errorMsg = aErrorMsg;
        }
        public Double pixelDimension;
        public Double timeInterval;
        public String errorMsg;
    }
    /**
     * @return array with [pixelDimensions, timeInterval] - if null there was some error getting cal. data.
     */
    public CalibrationData getImageCalibrationData() {
        // Get all calibration data from image
        final double width = iInputImage.getCalibration().pixelWidth;
        final double height = iInputImage.getCalibration().pixelHeight;
        final double interval = iInputImage.getCalibration().frameInterval;
        final String intervalUnit = iInputImage.getCalibration().getTimeUnit();
        final String unit = iInputImage.getCalibration().getUnit();

        // Do checking and complain if necessary
        String message = "";
        if (width != height) {
            message += "Pixel width is different than height. \n";
        }
        else if (interval == 0) {
            message += "Frame interval is equall to 0. To perform analysis it must have correct value \n";
        }
        else if (!(unit.equals("nm") || unit.equals(IJ.micronSymbol + "m") || unit.equals("um") || unit.equals("mm") || unit.equals("m"))) {
            message += "Dimension unit must be one of: m, mm, um or (" + IJ.micronSymbol + "m), nm";
        }
        else if (!(intervalUnit.equals("us") || intervalUnit.equals(IJ.micronSymbol + "s") || intervalUnit.equals("ms") || intervalUnit.equals("sec") || intervalUnit.equals("s"))) {
            message += "Time interval unit must be one of: s, sec, ms, us, " + IJ.micronSymbol + "m";
        }
        if (!message.equals("")) {
            return new CalibrationData(null, null, message);
        }
        
        // All provided data are correct! Get it and recalculate if needed.

        double pixelDimensions = width;
        double timeInterval = interval;

        // Convert dimension unit to meters
        if (unit.equals("nm")) {
            pixelDimensions /= 1000000000; // convert from nm to m
        }
        else if (unit.equals(IJ.micronSymbol + "m") || unit.equals("um")) {
            pixelDimensions /= 1000000; // convert from um to m
        }
        else if (unit.equals("mm")) {
            pixelDimensions /= 1000; // convert from mm to nm
        }

        // convert time unit to seconds
        if (intervalUnit.equals(IJ.micronSymbol + "s") || intervalUnit.equals("us")) {
            timeInterval /= 1000000; // convert from us to s
        }
        else if (intervalUnit.equals("ms")) {
            timeInterval /= 1000; // convert from ms to s
        }
        return new CalibrationData(pixelDimensions, timeInterval, null);
    }
    
    public double getCutoffRadius() {
        return detector.getCutoff();
    }

    public String getThresholdMode() {
        if (detector.getThresholdMode() == Mode.PERCENTILE_MODE) {
            return "percentile";
        }
        else if (detector.getThresholdMode() == Mode.ABS_THRESHOLD_MODE) {
            return "Absolute";
        }
        return "Unknown";
    }

    public String getThresholdValue() {
        if (detector.getThresholdMode() == Mode.PERCENTILE_MODE) {
            return "" + (detector.getPercentile() * 100);
        }
        else if (detector.getThresholdMode() == Mode.ABS_THRESHOLD_MODE) {
            return "" + detector.getAbsIntensityThreshold();
        }
        return "0";
    }

    public int getWidth() {
        return iInputImage.getStack().getWidth();
    }

    public int getHeight() {
        return iInputImage.getStack().getHeight();
    }

    public int getNumberOfSlices() {
        return iNumOfSlices;
    }

    public float getGlobalMinimum() {
        return detector.getGlobalMin();
    }

    public float getGlobalMaximum() {
        return detector.getGlobalMax();
    }

    public int getNumberOfFrames() {
        return iNumOfFrames;
    }

    protected void createLinkFactorDialog() {
        final GenericDialog gd = new GenericDialog("Link factor");

        gd.addMessage("weight of different contributions for linking\n relative to the distance normalized to one");
        gd.addNumericField("Object feature", l_f, 3);
        gd.addNumericField("Dynamics_", l_d, 3);
        final String sc[] = {"Greedy", "Hungarian"};
        gd.addChoice("Optimizer", sc, sc[0]);

        gd.showDialog();
        if (gd.wasCanceled() == true) {
            return;
        }

        l_s = (float) 1.0;
        l_f = (float) gd.getNextNumber();
        l_d = (float) gd.getNextNumber();
        final String linkerString = gd.getNextChoice();
        iParticleLinker = linkerString.equals("Greedy") ? new ParticleLinkerGreedy() : new ParticleLinkerHungarian();
    }
    
    // ========================================== CLEANED UP ==============================================================
    
    /**
     * Rescale and filter particles
     * @param aCalibration
     * @param aParticles 
     */
    private void filterParticles(Vector<Particle> aParticles) {
        int size = 0;
        double intensity = 0.0;
        
        // ----- Ask user for filtering input
        final GenericDialog gd = new GenericDialog("Filter particles");
        
        gd.addNumericField("Size (m0) >=", 0.0, 1);
        gd.addNumericField("Intensity (m2) >=", 0.0, 3);
        
        gd.showDialog();
        if (gd.wasCanceled() == false) {
            size = (int) gd.getNextNumber();
            intensity = gd.getNextNumber();
        }

        // ----- Filter
        for (int i = aParticles.size()  - 1; i >= 0; --i) {
            final Particle p = aParticles.get(i);

            if (p.m0 < size || p.m2 < intensity) {
                aParticles.remove(i);
            }
        }
    }
    
    /**
     * @return coordinates of maximum x,y(,z) from all particles
     */
    private int[] getParticlesRange() {
        // find the max coordinates for each coordinate
        int x = 0, y = 0, z = 0;
        for (final MyFrame f : iFrames) {
            for (final Particle p : f.getParticles()) {
                if (p.iX > x) x = (int) Math.ceil(p.iX);
                if (p.iY > y) y = (int) Math.ceil(p.iY);
                if (p.iZ > z) z = (int) Math.ceil(p.iZ);
            }
        }

        // create 2D or 3D coordinates
        return (z == 0) ? new int[] {x, y} : new int[] {x, y, z};
    }
    
    /**
     * Get the radius of the particles
     * @return the radius of the particles, return -1 if this parameter is not set (like segmented data)
     */
    public int getRadius() {
        return (detector != null) ? detector.getRadius() : -1;
    }
    
    /**
     * Create a set of frames from a vector of particles
     * @param aParticles - input particles
     */
    private static MyFrame[] createFrames(Vector<Particle> aParticles) {
        int numOfParticles = aParticles.size();
        if (numOfParticles == 0) {
            return new MyFrame[0];
        }
        
        final int numOfFrames = aParticles.get(numOfParticles - 1).getFrame() + 1;
        final MyFrame[] frames = new MyFrame[numOfFrames];
        
        int lastFrameNum = -1;
        for (int i = 0; i < numOfParticles;) {
            // Find all particles belonging to one frame
            final Vector<Particle> particlesInOneFrame = new Vector<Particle>();
            int currFrameNum = aParticles.get(i).getFrame();
            do {
                particlesInOneFrame.add(aParticles.get(i++));
            } while (i < numOfParticles && aParticles.get(i).getFrame() == currFrameNum);
            
            // Add possibly missing frames (in case where there is no particle(s) in some frames)
            for (int f = lastFrameNum + 1; f < currFrameNum; ++f) {
                frames[f] = new MyFrame(new Vector<Particle>(), f); 
            }
            
            // Update current frame
            frames[currFrameNum] = new MyFrame(particlesInOneFrame, currFrameNum);
            lastFrameNum = currFrameNum;
        }

        return frames;
    }
    
    /**
     * Generated trajectories from previously linked particles.
     */
    public void generateTrajectories() {
        IJ.showStatus("Generating Trajectories");
        logger.debug("Generate trajectories");
        iTrajectories = new Vector<Trajectory>();
        
        // Preallocate space for building trajectories.
        final Vector<Particle> currTrajectory = new Vector<Particle>(iNumOfFrames);

        for (int currFrameNum = 0; currFrameNum < iNumOfFrames; ++currFrameNum) {
            Vector<Particle> particles = iFrames[currFrameNum].getParticles();
            for (Particle p : particles) {
                if (!p.special) {
                    currTrajectory.clear();
                    
                    int currTrajFrameNum = currFrameNum;
                    while (true) {
                        p.special = true;
                        currTrajectory.add(p);
                        
                        // ---------- find first not dummy particle that this particle is linked to, if not found go to next particle
                        boolean isNextParticleFound = false;
                        for (int n = 1; n <= iLinkRange; ++n) {
                            if (p.next[n - 1] != -1) {
                                Vector<Particle> p2 = iFrames[currTrajFrameNum + n].getParticles();
                                Particle linkedParticle = p2.elementAt(p.next[n - 1]);
                                // If this particle is linked to a "real" particle that was already linked break the trajectory
                                if (!linkedParticle.special) {
                                    p = linkedParticle;
                                    currTrajFrameNum += n;
                                    isNextParticleFound = true;
                                }
                                break;
                            }
                        }
                        if (!isNextParticleFound) {
                            // no more particles found for that trajectory
                            break;
                        }
                    }
                    
                    if (currTrajectory.size() <= 1) {
                        // Trajectory has not been found for that particle - skip to next particle
                        continue;
                    }

                    // Create the current trajectory
                    iTrajectories.add(new Trajectory(currTrajectory.toArray(new Particle[0]), 
                                                     iTrajectories.size() + 1, // serial number
                                                     iInputImage));
                }
            }
        }
    }
    
    /**
     * Assignees randomly generated colors to trajectories. 
     */
    public void assignColorsToTrajectories() {
        int len = iTrajectories.size();
        final Vector<Color> vI = generateColors(len);
        for (int s = 0; s < len; s++) {
            iTrajectories.elementAt(s).color = vI.elementAt(s);
        }
    }

    /**
     * Generates random colors.
     * @param aNumOfColors - Number of colors to be generated.
     * @return generated colors (randomly shuffled).
     */
    private Vector<Color> generateColors(int aNumOfColors) {
        if (aNumOfColors < 1) return null;
        
        final Vector<Color> colors = new Vector<Color>(aNumOfColors);
        
        int base = 257; // R = 0 G = 1 B = 1
        int step = (256*256*256 - 1 - base) / aNumOfColors;
        step = (step > 0) ? step : 1;
        
        for (int s = 0; s < aNumOfColors; ++s) {
            base += step;
            colors.add(new Color(base));
        }
        Collections.shuffle(colors);
        
        return colors;
    }
    
    /**
     * Filters trajectories basing on length and ID of trajectory (via GUI).
     * @return false if cancelled by user, true otherwise
     */
    public boolean filterTrajectories() {
        final GenericDialog fod = new GenericDialog("Filter Options...", IJ.getInstance());
        fod.addNumericField("Only keep trajectories longer than", 0, 0, 10, "frames (0 means - showAll)");
        fod.addNumericField("Show only trajectory with ID:", 0, 0, 10, " (0 means - show All)");
        fod.showDialog();
        if (fod.wasCanceled()) {
            return false;
        }
        
        int minTrajectoryLength = (int) fod.getNextNumber();
        int idToShow = (int) fod.getNextNumber();
        if (idToShow < 0 || idToShow > iTrajectories.size()) {
            IJ.showMessage("ID of trajectory to filter is not valid. All trajectories will be displayed");
            idToShow = 0;
        }
        
        int numOfVisibleTrajs = 0;
        for (Trajectory t : iTrajectories) {
            if (t.getLength() <= minTrajectoryLength || (idToShow != 0 && t.iSerialNumber != idToShow)) {
                t.to_display = false;
            }
            else {
                t.to_display = true;
                numOfVisibleTrajs++;
            }
        }
        results_window.text_panel.appendLine(numOfVisibleTrajs + " trajectories remained after filter");
        
        return true;
    }

    public static void main(String[] args) {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = ParticleTracker3DModular_.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
        System.setProperty("plugins.dir", pluginsDir);

        new ImageJ();

//        ImagePlus image = IJ.openImage("https://upload.wikimedia.org/wikipedia/commons/3/3f/Bikesgray.jpg");
//        image.show();

        // run the plugin
        IJ.runPlugIn(clazz.getName(), "");
    }
}
