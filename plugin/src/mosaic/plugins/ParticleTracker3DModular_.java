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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JLabel;

import org.apache.log4j.Logger;

import ij.IJ;
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
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import ij.process.StackStatistics;
import mosaic.core.GUI.ParticleTrackerHelp;
import mosaic.core.detection.FeaturePointDetector;
import mosaic.core.detection.FeaturePointDetector.Mode;
import mosaic.core.detection.GUIhelper;
import mosaic.core.detection.MyFrame;
import mosaic.core.detection.MyFrame.DrawType;
import mosaic.core.detection.Particle;
import mosaic.core.detection.PreviewCanvas;
import mosaic.core.detection.PreviewInterface;
import mosaic.core.particleLinking.ParticleLinker;
import mosaic.core.particleLinking.ParticleLinkerBestOnePerm;
import mosaic.core.particleLinking.ParticleLinkerHun;
import mosaic.core.particleLinking.linkerOptions;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.MosaicUtils.SegmentationInfo;
import mosaic.core.utils.MosaicUtils.ToARGB;
import mosaic.particleTracker.FocusStackWin;
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
 * <p>
 * This class implements a to 3d extended feature point detection and tracking algorithm as described in: <br>
 * I. F. Sbalzarini and P. Koumoutsakos. <br>
 * Feature point tracking and trajectory analysis for video imaging in cell biology. <br>
 * J. Struct. Biol., 151(2), 2005.
 * <p>
 * Any publications that made use of this plugin should cite the above reference. <br>
 * This helps to ensure the financial support of our project at ETH and will
 * enable us to provide further updates and support. <br>
 * Thanks for your help!
 * </p>
 * For more information go <a href="http://weeman.inf.ethz.ch/particletracker/">here</a>
 * <p>
 * <b>Disclaimer</b><br>
 * IN NO EVENT SHALL THE ETH BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
 * OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND
 * ITS DOCUMENTATION, EVEN IF THE ETH HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * THE ETH SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE ETH HAS NO
 * OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 * <p>
 *
 * @version 1.3, November 2010 (requires: ImageJ 1.36b and Java 5 or higher)
 * @author Guy Levy - Academic guest CBL
 *         2d algorithm and GUI implementation
 * @author Janick Cardinale, PhD Student at Mosaic
 *         <a href="http://www.mosaic.ethz.ch/">Mosaic Group, Inst. of theoretical computer science<a>, ETH Zurich
 *         3D extension, memory efficiency, parallelism, algorithmic speed up, batch processing
 *         feature extension made by:
 * @version 1.6 November 2010 (requires: ImageJ 1.44 or higher)
 * @author Kota Miura - CMCI, EMBL Heidelberg (http://cmci.embl.de)
 *         add functionality to automatically transfer resulting data to result table in ImageJ,
 * @version 1.7 (require: ImgLib2)
 * @author Pietro Incardona
 *         add Dynamic model in the linker, new 3D/2D visualization system, CSV reading format, for Region based tracking
 */

public class ParticleTracker3DModular_ implements PlugInFilter, Measurements, PreviewInterface {
    private static final Logger logger = Logger.getLogger(ParticleTracker3DModular_.class);
    
    public Img<ARGBType> out;
    public ImagePlus iInputImage;
    public String resultFilesTitle;
    public MyFrame[] iFrames;
    public Vector<Trajectory> all_traj;
    public int number_of_trajectories;
    
    /* user defined parameters for linking */
    public int linkrange = 2; // default
    public double displacement = 10.0; // default
    
    /* results display and file */
    public int magnification_factor = 4;
    public int chosen_traj = -1;
    public ResultsWindow results_window;
    
    // file input
    private int f_size = 0;
    private double f_intensity = 0.0;
    private String file_sel;

    private String background;
    private boolean force;
    private boolean straight_line;
    protected float l_s = 1.0f; 
    protected float l_f = 1.0f;
    protected float l_d = 1.0f;
    private ImageStack stack;
    private int frames_number;
    private int slices_number;
    
    private FeaturePointDetector detector;
    private ParticleLinker iParticleLinker;
    protected NonBlockingGenericDialog gd;
    private PreviewCanvas preview_canvas = null;
    
    /* vars for text_files_mode */
    private String files_dir;
    private String[] files_list;
    private boolean one_file_multiple_frame;
    private boolean csv_format = false;
    private File Csv_region_list;
    private boolean create_bck_image = true;
    private boolean creating_traj_image = false;
    
    private FileInfo vFI = null;
    private boolean isGuiMode = false;
    private boolean text_files_mode = false;
    private boolean only_detect = false;
    private boolean frames_processed = false;
    
    /**
     * This method sets up the plugin filter for use. <br>
     * It is called by ImageJ upon selection of the plugin from the menus and the returned value is
     * used to call the <code>run(ImageProcessor ip)</code> method. <br>
     * The <code>arg</code> is a string passed as an argument to the plugin, can also be an empty string. <br>
     * Different commands from the plugin menu call the same plugin class with a different argument.
     * <ul>
     * <li>"" (empty String) - the plugin will work in regular full default mode
     * <li>"only_detect" - the plugin will work in detector only mode and unlike the regular mode will allow input of only one image
     * </ul>
     * The argument <code>imp</code> is passed by ImageJ - the currently active image is passed.
     * 
     * @param aInputArgs A string command that determines the mode of this plugin - can be empty
     * @param aInputImage The ImagePlus that is the original input image sequence -
     *                    if null then <code>text_files_mode</code> is activated after an OK from the user
     */
    @Override
    public int setup(String aInputArgs, ImagePlus aInputImage) {
        isGuiMode = !(IJ.isMacro() || Interpreter.batchMode);

        // Handle input stuff
        if (aInputArgs.equals("only_detect")) {
            only_detect = true;
        }
        iInputImage = aInputImage;

        // Setup detection/linking things
        iParticleLinker = new ParticleLinkerBestOnePerm();

        if (iInputImage == null && !only_detect) {
            if (IJ.showMessageWithCancel("Text Files Mode", "Do you want to load particles positions from text files?")) {
                text_files_mode = true;
                return NO_IMAGE_REQUIRED;
            }
            IJ.error("You must load an Image Sequence or Movie first");
            return DONE;
        }
        if (aInputImage == null) {
            IJ.error("You must load an Image Sequence or Movie first");
            return DONE;
        }
        else {
            vFI = iInputImage.getOriginalFileInfo();
            create_bck_image = false;
        }

        // Check if there are segmentation information
        if (MosaicUtils.checkSegmentationInfo(aInputImage, null)) {
            boolean shouldProceed = false;
            if (isGuiMode) {
                final YesNoCancelDialog YN_dialog = new YesNoCancelDialog(null, "Segmentation", "A segmentation has been founded for this image, do you want to track the regions");
                if (YN_dialog.yesPressed() == true) shouldProceed = true;
            }
            else {
                // Macro mode
                shouldProceed = true;
            }
            if (shouldProceed) {
                SegmentationInfo info = MosaicUtils.getSegmentationInfo(aInputImage);
                vFI = aInputImage.getOriginalFileInfo();
                logger.debug("Taking input from segmentation results (file = [" + aInputImage.getTitle() + "], dir = [" + vFI.directory + "]) ");
                
                resultFilesTitle = aInputImage.getTitle();
                text_files_mode = true;
                csv_format = true;
                Csv_region_list = info.RegionList;
                create_bck_image = false;

                return NO_IMAGE_REQUIRED;
            }
        }

        // If you have an image with n slice and one frame is quite suspicious
        // that the time information is stored in the slice data, prompt if the data are 2D or 3D
        if (aInputImage.getStackSize() > 1 && aInputImage.getNFrames() == 1) {
            final GenericDialog gd = new GenericDialog("Data dimension");

            final String ad[] = { "No", "Yes" };
            gd.addChoice("Are these 3D data ?", ad, "No");
            gd.showDialog();

            String saved_options = null;
            if (!isGuiMode && Macro.getOptions() != null && !Macro.getOptions().trim().isEmpty()) {
                saved_options = Macro.getOptions();
            }

            if (!gd.wasCanceled() && gd.getNextChoice().equals("No")) {
                IJ.run(aInputImage, "Stack to Hyperstack...", "order=xyczt(default) channels=1 slices=1 frames=" + aInputImage.getNSlices() + " display=Composite");
            }

            if (saved_options != null) {
                Macro.setOptions(saved_options);
            }
        }

        if (only_detect && this.iInputImage.getStackSize() == 1) {
            return DOES_ALL + NO_CHANGES;
        }
        
        return DOES_ALL + NO_CHANGES + PARALLELIZE_STACKS;
    }

    /**
     * This method runs the plugin, what implemented here is what the plugin actually
     * does. It takes the image processor it works on as an argument. <br>
     * In this implementation the processor is not used so that the original image is left unchanged. <br>
     * The original image is locked while the plugin is running. <br>
     * This method is called by ImageJ after <code>setup(String arg, ImagePlus imp)</code> returns
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(ImageProcessor ip) {
        initializeMembers();
        if (!text_files_mode && isGuiMode) {
            preview_canvas = GUIhelper.generatePreviewCanvas(iInputImage);
        }

        /* get user defined params and set more initial params accordingly */
        if (!getUserDefinedParams()) {
            return;
        }

        if (!processFrames()) {
            return;
        }

        if (iInputImage != null) {
            iInputImage.show();
        }

        IJ.showStatus("Linking Particles");
        if (linkParticles() == false) {
            return;
        }

        IJ.showStatus("Generating Trajectories");
        generateTrajectories();
        if (!isGuiMode) {
            writeDataToDisk();
        }
        else {
            results_window = new ResultsWindow(this, "Results");
            results_window.configuration_panel.append(getConfiguration().toString());
            results_window.configuration_panel.append(getInputFramesInformation().toString());
            results_window.text_panel.appendLine("Particle Tracker DONE!");
            results_window.text_panel.appendLine("Found " + number_of_trajectories + " Trajectories");
            results_window.setVisible(true);

            IJ.showStatus("Creating trajectory image ...");
            creating_traj_image = true;

            out = createHyperStackFromFrames();
        }
    }

    public boolean linkParticles() {
        final linkerOptions lo = new linkerOptions();
        lo.linkrange = linkrange;
        lo.displacement = (float) displacement;
        lo.force = force;
        lo.straight_line = straight_line;
        lo.l_s = l_s;
        lo.l_f = l_f;
        lo.l_d = l_d;
        return iParticleLinker.linkParticles(iFrames, lo);
    }

    private MyFrame[] convertIntoFrames(Vector<Particle> p) {
        // Read the first background
        Calibration cal = null;
        if (background != null) {
            if (iInputImage == null) {
                iInputImage = new Opener().openImage(new File(background.replace("*", "1")).getAbsolutePath());
            }

            if (iInputImage != null) {
                cal = iInputImage.getCalibration();
                rescaleWith(cal, p);
            }
        }

        if (cal == null) {
            cal = new Calibration();
            cal.pixelDepth = 1.0f;
            cal.pixelHeight = 1.0f;
            cal.pixelWidth = 1.0f;
            rescaleWith(cal, p);
        }
        return MyFrame.createFrames(p, linkrange);
    }

    /**
     * Initializes some members needed before going to previews on the user param dialog.
     */
    private void initializeMembers() {
        if (!text_files_mode) {
            // initialize ImageStack stack

            stack = iInputImage.getStack();
            resultFilesTitle = iInputImage.getTitle();

            // get global minimum and maximum
            final StackStatistics stack_stats = new StackStatistics(iInputImage);
            final float global_max = (float) stack_stats.max;
            final float global_min = (float) stack_stats.min;
            frames_number = iInputImage.getNFrames();
            slices_number = iInputImage.getNSlices();

            detector = new FeaturePointDetector(global_max, global_min);
        }
        else {
            slices_number = 1;
        }
    }

    private void featureFilteringStage() {
        final GenericDialog gd = new GenericDialog("Filter");

        gd.addMessage("Filter particle");

        gd.addNumericField("Size bigger (m0)", 0.0, 1);
        gd.addNumericField("Intensity bigger (m2)", 0.0, 3);

        gd.showDialog();

        if (gd.wasCanceled() == false) {
            f_size = (int) gd.getNextNumber();
            f_intensity = gd.getNextNumber();
        }
    }

    /**
     * Rescale the particle according to the spacing and filter it by
     * size and intensity
     *
     * @param cal spacing
     * @param p Particle vector
     */
    private void rescaleWith(Calibration cal, Vector<Particle> vp) {
        // ask for feature filtering stage
        featureFilteringStage();

        // Convert to a List
        final List<Particle> pl = new ArrayList<Particle>();
        for (int i = 0; i < vp.size(); i++) {
            pl.add(vp.get(i));
        }

        // rescale and filter
        Iterator<Particle> i = pl.iterator();
        while (i.hasNext()) {
            final Particle p = i.next();

            if (p.m0 >= f_size && p.m2 > f_intensity) {
                p.iX *= cal.pixelWidth;
                p.iY *= cal.pixelHeight;
                p.iZ *= cal.pixelDepth;
            }
            else {
                i.remove();
            }
        }

        // clear the vector and recreate it
        vp.clear();
        i = pl.iterator();
        while (i.hasNext()) {
            vp.add(i.next());
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

        if (csv_format == true) {
            IJ.showStatus("Reading CSV Regions data ...");
            final CSV<Particle> P_csv = new CSV<Particle>(Particle.class);

            P_csv.setCSVPreferenceFromFile(files_dir + File.separator + file_sel);
            final Vector<Particle> p = P_csv.Read(files_dir + File.separator + file_sel, null);

            vFI = new FileInfo();
            vFI.directory = files_dir;
            
            if (p.size() == 0) {
                IJ.error("No regions defined for this image,nothing to do");
                return false;
            }
            background = P_csv.getMetaInformation("background");

            IJ.showStatus("Creating frames with particles ...");

            iFrames = convertIntoFrames(p);
            // It can happen that the segmentation algorithm produce double output on the CSV
            for (int i = 0; i < iFrames.length; i++) {
                iFrames[i].removeDoubleParticles();
            }

            frames_number = iFrames.length;

            /* create an ImagePlus object to hold the particle information from the text files */
            if (create_bck_image == true) {
                IJ.showStatus("Creating background image ...");

                final Img<ARGBType> iw = createHyperStackFromFrames();
                if (iw != null) {
                    iInputImage = ImageJFunctions.wrap(iw, "Video");
                    iInputImage.show();
                }
                else {
                    return false;
                }
            }
        }
        else if (one_file_multiple_frame == false) {
            if (text_files_mode) {
                vFI = new FileInfo();
                vFI.directory = files_dir;
            }
            iFrames = new MyFrame[frames_number];
            for (int frame_i = 0, file_index = 0; frame_i < frames_number; frame_i++, file_index++) {
                MyFrame current_frame = null;
                if (text_files_mode) {
                    if (files_list[file_index].startsWith(".") || files_list[file_index].endsWith("~")) {
                        frame_i--;
                        continue;
                    }

                    // text_files_mode:
                    // construct each frame from the corresponding text file
                    IJ.showStatus("Reading Particles from file " + files_list[file_index] + "(" + (frame_i) + "/" + files_list.length + ")");
                    current_frame = new MyFrame(files_dir + files_list[file_index]);
                    if (current_frame.getParticles() == null) {
                        return false;
                    }

                }
                else {
                    // sequence of images mode:
                    // construct each frame from the corresponding image
                    current_frame = new MyFrame(MosaicUtils.GetSubStackInFloat(stack, (frame_i) * slices_number + 1, (frame_i + 1) * slices_number), frame_i, linkrange);

                    // Detect feature points in this frame
                    IJ.showStatus("Detecting Particles in Frame " + (frame_i + 1) + "/" + frames_number);
                    detector.featurePointDetection(current_frame);
                }
                if (current_frame.frame_number >= iFrames.length) {
                    IJ.showMessage("Error, frame " + current_frame.frame_number + "  is out of range, enumeration must not have hole, and must start from 0");
                    return false;
                }
                iFrames[current_frame.frame_number] = current_frame;
            } // for

            // Here check that all frames are created
            for (int i = 0; i < iFrames.length; i++) {
                if (iFrames[i] == null) {
                    IJ.showMessage("Error, frame: " + i + " does not exist");
                    return false;
                }
            }

            if (create_bck_image == true) {
                IJ.showStatus("Creating background image ...");

                final Img<ARGBType> iw = createHyperStackFromFrames();
                if (iw != null) {
                    iInputImage = ImageJFunctions.wrap(iw, "Video");
                    iInputImage.show();
                }
            }
        }
        else {
            throw new RuntimeException("Unsupported input file");
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
     * @see #makeKernel(int)
     * @see #generateBinaryMask(int)
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

        one_file_multiple_frame = false;
        boolean convert = false;
        if (text_files_mode) {
            if (Csv_region_list == null) {
                GenericDialog text_mode_gd;
                text_mode_gd = new GenericDialog("input files info", IJ.getInstance());
                text_mode_gd.addMessage("Please specify the info provided for the Particles...");
                text_mode_gd.addCheckbox("multiple frame files", true);
                text_mode_gd.addCheckbox("CSV File", false);

                // text_mode_gd.addCheckbox("3rd or 5th position and on- all other data", true);
                text_mode_gd.showDialog();
                if (text_mode_gd.wasCanceled()) {
                    return false;
                }

                one_file_multiple_frame = text_mode_gd.getNextBoolean();
                csv_format = text_mode_gd.getNextBoolean();

                // This is just a quick fix to not mislead users. one_file_multiple_frame and its functionality
                // should be removed from code. Unfortunately to load multiples files both checkboxes must have been
                // unchecked (!) which was not so user friendly.
                if (one_file_multiple_frame == true && csv_format == false) {
                    one_file_multiple_frame = false;
                }
                else if (one_file_multiple_frame == false && csv_format == false || one_file_multiple_frame == true && csv_format == true) {
                    return false;
                }

                // gets the input files directory form
                files_list = getFilesList();
                if (files_list == null) {
                    return false;
                }

                if (csv_format == true) {
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

                this.resultFilesTitle = "text_files";
                frames_number = 0;
                // EACH!! file in the given directory is considered as a frame
                for (int i = 0; i < files_list.length; i++) {
                    if (!files_list[i].startsWith(".") && !files_list[i].endsWith("~")) {
                        frames_number++;
                    }
                }
            }
            else {
                files_dir = Csv_region_list.getParent();
                file_sel = Csv_region_list.getName();
            }
        }
        else {
            GUIhelper.addUserDefinedParametersDialog(gd, detector);

            gd.addPanel(GUIhelper.makePreviewPanel(this, iInputImage), GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));

            // check if the original images are not GRAY8, 16 or 32
            if (this.iInputImage.getType() != ImagePlus.GRAY8 && this.iInputImage.getType() != ImagePlus.GRAY16 && this.iInputImage.getType() != ImagePlus.GRAY32) {
                gd.addCheckbox("Convert to Gray8 (recommended)", true);
                convert = true;
            }
        }

        if (!only_detect) {
            gd.addMessage("Particle Linking:\n");
            // These 2 params are relevant for both working modes
            gd.addNumericField("Link Range", 2, 0);
            gd.addNumericField("Displacement", 10.0, 2);
        }

        final String d_pos[] = { "Brownian", "straight", "constant velocity" };
        gd.addChoice("Dynamics: ", d_pos, d_pos[0]);

        // Create advanced option panel
        final Button a_opt = new Button("Advanced options");
        a_opt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                createLinkFactorDialog();
            }
        });
        if (!isGuiMode) {
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
        if (!text_files_mode) {
            final Boolean changed = GUIhelper.getUserDefinedParameters(gd, detector);
            // even if the frames were already processed (particles detected) but
            // the user changed the detection params then the frames needs to be processed again
            if (changed) {
                if (this.frames_processed) {
                    this.iFrames = null;
                    this.frames_processed = false;
                }
            }

            // add the option to convert only if images are not GRAY8, 16 or 32
            if (convert) {
                convert = gd.getNextBoolean();
            }
        }
        if (only_detect) {
            return false;
        }
        this.linkrange = (int) gd.getNextNumber();
        this.displacement = gd.getNextNumber();


        final String dm = gd.getNextChoice();

        if (dm.equals("Brownian")) {
            this.force = false;
            this.straight_line = false;
        }
        else if (dm.equals("Straight lines")) {
            this.force = false;
            this.straight_line = true;
        }
        else if (dm.equals("Constant velocity")) {
            this.force = true;
            this.straight_line = true;
        }

        // if user choose to convert reset stack, title, frames number and global min, max
        if (convert) {
            StackConverter sc = new StackConverter(iInputImage);
            sc.convertToGray8();
            stack = iInputImage.getStack();
            this.resultFilesTitle = iInputImage.getTitle();
            final StackStatistics stack_stats = new StackStatistics(iInputImage);
            detector.setGlobalMax((float) stack_stats.max);
            detector.setGlobalMin((float) stack_stats.min);
            frames_number = iInputImage.getNFrames(); // ??maybe not necessary
        }
        return true;
    }

    /**
     * Generates <code>Trajectory</code> objects according to the information
     * available in each MyFrame and Particle. <br>
     * Populates the <code>all_traj</code> Vector.
     */
    public void generateTrajectories() {
        int i, j, k;
        int found, n, m;

        final Vector<Particle> curr_traj_particles = new Vector<Particle>(frames_number);
        all_traj = new Vector<Trajectory>();
        number_of_trajectories = 0;
        int cur_traj_start = 0;

        for (i = 0; i < frames_number; i++) {
            for (j = 0; j < this.iFrames[i].getParticles().size(); j++) {
                if (!this.iFrames[i].getParticles().elementAt(j).special) {
                    this.iFrames[i].getParticles().elementAt(j).special = true;
                    found = -1;
                    // go over all particles that this particle (particles[j]) is linked to
                    for (n = 0; n < this.linkrange; n++) {
                        // if it is NOT a dummy particle - stop looking
                        if (this.iFrames[i].getParticles().elementAt(j).next[n] != -1) {
                            found = n;
                            break;
                        }
                    }
                    // if this particle is not linked to any other go to next particle and dont add a trajectory
                    if (found == -1) {
                        continue;
                    }

                    // Added by Guy Levy, 18.08.06 - A change form original implementation
                    // if this particle is linkd to a "real" paritcle that was already linked
                    // break the trajectory and start again from the next particle. dont add a trajectory
                    if (this.iFrames[i + n + 1].getParticles().elementAt(this.iFrames[i].getParticles().elementAt(j).next[n]).special) {
                        continue;
                    }

                    // this particle is linked to another "real" particle that is not already linked
                    // so we have a trajectory
                    number_of_trajectories++;

                    if (curr_traj_particles.size() == 0) {
                        cur_traj_start = i;
                    }

                    curr_traj_particles.add(this.iFrames[i].getParticles().elementAt(j));
                    k = i;
                    m = j;
                    do {
                        found = -1;
                        for (n = 0; n < this.linkrange; n++) {
                            if (this.iFrames[k].getParticles().elementAt(m).next[n] != -1) {
                                // If this particle is linked to a "real" particle that is NOT already linked, continue with building the trajectory
                                if (this.iFrames[k + n + 1].getParticles().elementAt(this.iFrames[k].getParticles().elementAt(m).next[n]).special == false) {
                                    found = n;
                                    break;
                                    // Added by Guy Levy, 18.08.06 - A change form original implementation
                                    // If this particle is linked to a "real" particle that is already linked, stop building the trajectory
                                }
                                else {
                                    break;
                                }
                            }
                        }
                        if (found == -1) {
                            break;
                        }
                        m = this.iFrames[k].getParticles().elementAt(m).next[found];
                        k += (found + 1);
                        curr_traj_particles.add(this.iFrames[k].getParticles().elementAt(m));
                        this.iFrames[k].getParticles().elementAt(m).special = true;
                    } while (m != -1);

                    // Create the current trajectory
                    final Particle[] curr_traj_particles_array = new Particle[curr_traj_particles.size()];
                    Trajectory curr_traj = new Trajectory(curr_traj_particles.toArray(curr_traj_particles_array), iInputImage);

                    // set current trajectory parameters
                    curr_traj.serial_number = number_of_trajectories;
                    curr_traj.setFocusArea();
                    curr_traj.setMouseSelectionArea();
                    curr_traj.populateGaps();
                    curr_traj.start_frame = cur_traj_start;
                    curr_traj.stop_frame = curr_traj.existing_particles[curr_traj.existing_particles.length - 1].getFrame();
                    all_traj.add(curr_traj);
                    curr_traj_particles.removeAllElements();
                }
            }
        }

        final Vector<Color> vI = generateColors();
        for (int s = 0; s < all_traj.size(); s++) {
            all_traj.elementAt(s).color = vI.elementAt(s);
        }
    }

    private Vector<Color> generateColors() {
        /* Assign random unique color to trajectory (KNOWN BUG YOU SHOULD AVOID GREY) */
        final Vector<Color> vI = new Vector<Color>();
        int base = 257; // R = 0 G = 1 B = 1
        int step = 0;
        if (all_traj.size() >= 1) {
            step = (255 * 255 * 255 - base) / all_traj.size();
        }
        else {
            step = (255 * 255 * 255 - base) / 1;
        }

        // Well we cannot create unique trajectory color, recover as we can
        if (step == 0) {
            step = 1;
        }

        for (int s = 0; s < all_traj.size(); s++) {
            vI.add(new Color(base));
            base += step;
        }
        Collections.shuffle(vI);
        
        return vI;
    }
    private String getUnit() {
        final Calibration cal = iInputImage.getCalibration();
        return cal.getUnit();
    }

    /*
     * Get scaling factor
     */
    public double[] getScaling() {
        final Calibration cal = iInputImage.getCalibration();
        final double scaling[] = new double[3];
        scaling[0] = cal.pixelWidth;
        scaling[1] = cal.pixelHeight;
        scaling[2] = cal.pixelDepth;

        return scaling;
    }

    /**
     * Generate a String header of each colum
     */
    public String trajectoryHeader() {
        final Calibration cal = iInputImage.getCalibration();

        return new String("%% frame x (" + cal.getUnit() + ")     y (" + cal.getUnit() + ")    z (" + cal.getUnit() + ")      m0 " + "        m1 " + "          m2 " + "          m3 " + "          m4 "
                + "          s \n");
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
        traj_info.append("%%\t 2nd column: x coordinate top-down" + "(" + getUnit() + ")" + "\n");
        traj_info.append("%%\t 3rd column: y coordinate left-right" + "(" + getUnit() + ")" + "\n");
        traj_info.append("%%\t 4th column: z coordinate bottom-top" + "(" + getUnit() + ")" + "\n");
        if (text_files_mode) {
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

        final Iterator<Trajectory> iter = all_traj.iterator();
        while (iter.hasNext()) {
            final Trajectory curr_traj = iter.next();
            traj_info.append("%% Trajectory " + curr_traj.serial_number + "\n");

            // Uncomment these lines if you want to add trajectory analysis to final report:
            // -----------------------------------------------------------------------------
            // TrajectoryAnalysis ta = new TrajectoryAnalysis(curr_traj);
            // ta.setLengthOfAPixel(pixelDimensions);
            // ta.setTimeInterval(timeInterval);
            // if (ta.calculateAll() == TrajectoryAnalysis.SUCCESS) {
            // traj_info.append("%% MSS(slope): " + String.format("%4.3f", ta.getMSSlinear()) +
            // " MSD(slope): " + String.format("%4.3f", ta.getGammasLogarithmic()[1]) + "\n");
            // }
            // else {
            // traj_info.append("%% Calculating MSS/MSD not possible for this trajectory.\n");
            // }
            // -----------------------------------------------------------------------------

            traj_info.append(trajectoryHeader());
            traj_info.append(curr_traj.toStringBuffer());
        }

        return traj_info;
    }

    private void writeDataToDisk() {
        if (vFI == null) {
            IJ.error("You're running a macro. Data are written to disk at the directory where your image is stored. Please store youre image first.");
            return;
        }
        MosaicUtils.write2File(vFI.directory, "Traj_" + resultFilesTitle + ".txt", getFullReport().toString());
        if (!text_files_mode) new TrajectoriesReportXML(new File(vFI.directory, "report.xml").getAbsolutePath(), this);
        final ResultsTable rt = transferTrajectoriesToResultTable();
        try {
            rt.saveAs(new File(vFI.directory, "Traj_" + resultFilesTitle + ".csv").getAbsolutePath());
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the radius of the particles
     * @return the radius of the particles, return -1 if this parameter is not set (like segmented data)
     */
    public int getRadius() {
        int radius = -1;
        if (detector != null) {
            radius = detector.getRadius();
        }

        return radius;
    }

    /**
     * Detects particles in the current displayed frame according to the parameters currently set
     * Draws dots on the positions of the detected partciles on the frame and circles them
     * 
     * @see #getUserDefinedPreviewParams()
     * @see MyFrame#featurePointDetection()
     * @see PreviewCanvas
     */
    private synchronized void preview() {
        if (iInputImage == null) {
            return;
        }

        GUIhelper.getUserDefinedPreviewParams(gd, detector);

        final ImagePlus frame = MosaicUtils.getImageFrame(iInputImage, iInputImage.getFrame());

        final MyFrame preview_frame = new MyFrame(frame.getStack(), iInputImage.getFrame(), linkrange);

        detector.featurePointDetection(preview_frame);
        final Img<FloatType> backgroundImg = ImagePlusAdapter.convertFloat(frame);

        preview_frame.setParticleRadius(getRadius());
        final Img<ARGBType> img_frame = preview_frame.createImage(backgroundImg, frame.getCalibration());

        ImageJFunctions.wrap(img_frame, "Preview detection").show();
    }

    /**
     * Displays a dialog for filtering trajectories according to minimum length
     * <br>
     * The <code>to_display</code> parameter of all trajectories will be set according to their length
     * and the user choice.
     * <br>
     * Trajectories with shorter or equal length then given by the user will be set to false.
     * For the rest, it will be set the true.
     * <br>
     * Displays a text line on the result windows text panel with the number of trajectories after filter
     * 
     * @return true if user gave an input and false if the user cancelled the operation
     */
    public boolean filterTrajectories() {
        final GenericDialog fod = new GenericDialog("Filter Options...", IJ.getInstance());
        // default is not to filter any trajectories (min length of zero)
        fod.addNumericField("Only keep trajectories longer than", 0, 0, 10, "frames");

        fod.showDialog();
        if (fod.wasCanceled()) {
            return false;
        }
        final int min_length_to_display = (int) fod.getNextNumber();

        int passed_traj = 0;
        final Iterator<Trajectory> iter = all_traj.iterator();
        while (iter.hasNext()) {
            final Trajectory curr_traj = iter.next();
            if (curr_traj.length <= min_length_to_display) {
                curr_traj.to_display = false;
            }
            else {
                curr_traj.to_display = true;
                passed_traj++;
            }
        }
        results_window.text_panel.appendLine(passed_traj + " trajectories remained after filter");
        return true;
    }

    /**
     * Resets the trajectories filter so no trajectory is filtered by
     * setting the <code>to_display</code> param of each trajectory to true
     */
    public void resetTrajectoriesFilter() {
        final Iterator<Trajectory> iter = all_traj.iterator();
        while (iter.hasNext()) {
            (iter.next()).to_display = true;
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
        if (!this.text_files_mode) {
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
        configuration.append(this.displacement);
        configuration.append("\n");
        configuration.append("% \tLinkrange    : ");
        configuration.append(this.linkrange);
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
        if (this.text_files_mode) {
            return new StringBuffer("Frames info was loaded from text files");
        }
        final StringBuffer info = new StringBuffer("% Frames information:\n");
        info.append("% \tWidth : ");
        info.append(stack.getWidth());
        info.append(" pixel\n");
        info.append("% \tHeight: ");
        info.append(stack.getHeight());
        info.append(" pixel\n");
        info.append("% \tDepth: ");
        info.append(slices_number);
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
        final String new_title = "All Trajectories Visual";

        if (duplicated_imp == null) {
            if (out == null) {
                if (creating_traj_image == true) {
                    IJ.error("One moment please ..., we are computing the image");
                    return;
                }
                else {
                    IJ.error("An internal error has occurred we are not able to compute the result image");
                    return;
                }
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
    public void generateTrajFocusView(int trajectory_index, int magnification) {
        // create a title
        final String new_title = "[Trajectory number " + (trajectory_index + 1) + "]";

        // get the trajectory at the given index
        final Trajectory traj = (all_traj.elementAt(trajectory_index));
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
        min[img.numDimensions() - 1] = traj.start_frame;
        max[img.numDimensions() - 1] = traj.stop_frame;

        final FinalInterval in = new FinalInterval(min, max);

        final Img<ARGBType> focus_view = copyScaleConvertToRGB(img, in, magnification);

        IJ.showStatus("Creating frames ... ");
        final Vector<Trajectory> vt = new Vector<Trajectory>();
        vt.add(traj);
        final Calibration cal = iInputImage.getCalibration();
        MyFrame.updateImage(focus_view, traj.focus_area.getBounds(), traj.start_frame, vt, cal, DrawType.TRAJECTORY_HISTORY, getRadius());

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

        this.files_dir = od.getDirectory();
        this.file_sel = od.getFileName();
        if (files_dir == null) {
            return null;
        }
        final String[] list = new File(od.getDirectory()).list();
        return list;
    }

    public int[] getParticlesRange() {
        int vMax[] = new int[3];

        /* find the max coordinates for each coordinate */
        for (int i = 0; i < iFrames.length; i++) {
            for (int p = 0; p < iFrames[i].getParticles().size(); p++) {
                final Particle vParticle = iFrames[i].getParticles().elementAt(p);
                if (vParticle.iX > vMax[0]) {
                    vMax[0] = (int) Math.ceil(vParticle.iX);
                }
                if (vParticle.iY > vMax[1]) {
                    vMax[1] = (int) Math.ceil(vParticle.iY);
                }
                if (vParticle.iZ > vMax[2]) {
                    vMax[2] = (int) Math.ceil(vParticle.iZ);
                }
            }
        }

        // is 2D
        if (vMax[2] == 0.0) {
            final int vMax_t[] = new int[2];
            vMax_t[0] = vMax[0];
            vMax_t[1] = vMax[1];
            vMax = vMax_t;
        }

        return vMax;
    }

    /**
     * Create an image stack or hyperstack from frames
     * @return
     */
    public Img<ARGBType> createHyperStackFromFrames() {
        return createHyperStackFromFrames(background);
    }
    
    private Img<ARGBType> createHyperStackFromFrames(String aBackgroundFilename) {
        int[] vMax = null;
        Img<ARGBType> out_f = null;
        Img<ARGBType> out_fs = null;

        vMax = getParticlesRange();

        for (int i = 0; i < vMax.length; i++) {
            vMax[i] += 1;
        }

        // Create time Image

        if (text_files_mode == true) {
            if (aBackgroundFilename == null) {
                final long vMaxp1[] = new long[vMax.length + 1];

                for (int i = 0; i < vMax.length; i++) {
                    vMaxp1[i] = vMax[i];
                }
                vMaxp1[vMax.length] = this.iFrames.length;

                final ImgFactory<ARGBType> imgFactory = new CellImgFactory<ARGBType>();
                out_fs = imgFactory.create(vMaxp1, new ARGBType());
            }
            else {
                // Open first background to get the size

                if (iInputImage == null) {
                    final File file = new File(aBackgroundFilename.replace("*", Integer.toString(1)));

                    // open a file with ImageJ
                    iInputImage = new Opener().openImage(file.getAbsolutePath());
                }

                long vMaxp1[] = null;

                if (iInputImage != null) {
                    ImagePlus imp = null;
                    if (iInputImage.getNFrames() > 1) {
                        imp = MosaicUtils.getImageFrame(iInputImage, 1);
                    }
                    else {
                        imp = iInputImage;
                    }

                    final Img<UnsignedByteType> backgroundImg = ImagePlusAdapter.wrap(imp);

                    vMaxp1 = new long[backgroundImg.numDimensions() + 1];

                    for (int i = 0; i < backgroundImg.numDimensions(); i++) {
                        vMaxp1[i] = backgroundImg.dimension(i);
                    }
                    vMaxp1[backgroundImg.numDimensions()] = this.iFrames.length;
                }
                else {
                    // Cannot open the background

                    IJ.error("Cannot open the background " + aBackgroundFilename);
                    creating_traj_image = false;
                    return null;
                }

                final ImgFactory<ARGBType> imgFactory = new CellImgFactory<ARGBType>();
                out_fs = imgFactory.create(vMaxp1, new ARGBType());
            }
        }
        else {
            // Open original image

            final Img<UnsignedByteType> backgroundImg = ImagePlusAdapter.wrap(iInputImage);

            final long vMaxp1[] = new long[backgroundImg.numDimensions()];

            for (int i = 0; i < backgroundImg.numDimensions(); i++) {
                vMaxp1[i] = backgroundImg.dimension(i);
            }

            final ImgFactory<ARGBType> imgFactory = new CellImgFactory<ARGBType>();
            out_fs = imgFactory.create(vMaxp1, new ARGBType());
        }

        /* for each frame we have to add a stack to the image */
        for (int i = 0; i < iFrames.length; i++) {
            IJ.showStatus("Creating frame " + (i + 1));
            if (text_files_mode == true) {
                // Create frame image

                if (aBackgroundFilename != null) {
                    ImagePlus imp = null;

                    if (iInputImage.getNFrames() > 1 && i < iInputImage.getNFrames()) {
                        imp = MosaicUtils.getImageFrame(iInputImage, i + 1);
                    }
                    else {
                        if (iInputImage.getNChannels() >= 1 && i < iInputImage.getNChannels()) {
                            imp = MosaicUtils.getImageSlice(iInputImage, i + 1);
                        }
                    }

                    final Calibration cal = iInputImage.getCalibration();

                    if (imp == null) {
                        IJ.error("Cannot find the background image or wrong format");
                        creating_traj_image = false;
                        return null;
                    }

                    // wrap it into an ImgLib image (no copying)
                    final Img<UnsignedByteType> backgroundImg = ImagePlusAdapter.wrap(imp);

                    out_f = iFrames[i].createImage(backgroundImg, all_traj, cal, i, DrawType.TRAJECTORY_HISTORY);

                    // It failed end
                    if (out_f == null) {
                        break;
                    }
                }
                else {
                    out_f = iFrames[i].createImage(vMax, all_traj, i, DrawType.TRAJECTORY_HISTORY);

                    // It failed end
                    if (out_f == null) {
                        break;
                    }
                }
            }
            else {
                final Calibration cal = iInputImage.getCalibration();

                // wrap it into an ImgLib image (no copying)

                final ImagePlus timp = MosaicUtils.getImageFrame(iInputImage, i + 1);
                final Img<UnsignedByteType> backgroundImg = ImagePlusAdapter.wrap(timp);

                iFrames[i].setParticleRadius(getRadius());
                out_f = iFrames[i].createImage(backgroundImg, all_traj, cal, i, DrawType.TRAJECTORY_HISTORY);
            }

            MosaicUtils.copyEmbedded(out_fs, out_f, i);
        }

        IJ.showStatus("Done");
        return out_fs;
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
        report.append(this.getConfiguration());
        report.append(this.getInputFramesInformation());
        report.append("\n");

        /* detected particles infomation per frame */
        report.append("%\tPer frame information (verbose output):\n");
        for (int i = 0; i < iFrames.length; i++) {
            report.append(this.iFrames[i].getFullFrameInfo());
        }

        /* Add linking info */
        report.append("% Trajectory linking (verbose output):\n");
        for (int i = 0; i < iFrames.length; i++) {
            report.append(this.iFrames[i].toStringBuffer());
        }

        /* all trajectories info */
        report.append("\n");
        report.append(getTrajectoriesInfo());

        return report;
    }

    @Override
    public void preview(ActionEvent e) {
        // set the original_imp window position next to the dialog window
        this.iInputImage.getWindow().setLocation((int) gd.getLocationOnScreen().getX() + gd.getWidth(), (int) gd.getLocationOnScreen().getY());
        // do preview
        this.preview();
        preview_canvas.repaint();
        return;
    }

    @Override
    public void saveDetected(ActionEvent e) {
        /* set the user defined pramars according to the valus in the dialog box */
        GUIhelper.getUserDefinedPreviewParams(gd, detector);

        /* detect particles and save to files */
        if (this.processFrames()) { // process the frames
            saveDetected(iFrames);
        }
        preview_canvas.repaint();
        return;
    }

    public void saveDetected(MyFrame[] frames) {
        final SaveDialog sd = new SaveDialog("Save Detected Particles", IJ.getDirectory("image"), "frame", "");
        if (sd.getDirectory() == null || sd.getFileName() == null) {
            return;
        }

        // for each frame - save the detected particles
        for (int i = 0; i < frames.length; i++) {
            if (!MosaicUtils.write2File(sd.getDirectory(), sd.getFileName() + "_" + i, frames[i].frameDetectedParticlesForSave(false).toString())) {
                IJ.log("Problem occured while writing to file.");
                return;
            }
        }

        return;
    }
    
    /**
     * Extracts spot segmentation results <br>
     * and show in in ImageJ static Results Table
     * <br>
     * Invoked by clicking button in ParticleTracker Results Window.
     * 
     * @author Kota Miura <a href="http://cmci.embl.de">cmci.embl.de</a>
     * @see ResultsWindow
     */
    public void transferParticlesToResultsTable() {
        final ResultsTable rt = getResultsTable();

        if (rt != null) {
            int rownum = 0;
            for (int i = 0; i < iFrames.length; i++) {
                final Vector<Particle> particles = iFrames[i].getParticles();
                for (final Particle p : particles) {
                    rt.incrementCounter();
                    rownum = rt.getCounter() - 1;
                    addParticleInfo(rt, rownum, p);
                }
            }
            rt.show("Results");
        }
    }

    /**
     * Extracts tracking results <br>
     * and show in in ImageJ static Results Table
     * <br>
     * Invoked by clicking button in ParticleTracker Results Window.
     * 
     * @author Kota Miura <a href="http://cmci.embl.de">cmci.embl.de</a>
     * @see ResultsWindow
     */
    public ResultsTable transferTrajectoriesToResultTable() {
        final ResultsTable rt = getResultsTable();
        if (rt != null) {
            final Iterator<Trajectory> iter = all_traj.iterator();
            while (iter.hasNext()) {
                final Trajectory curr_traj = iter.next();
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
        int rownum;
        final Particle[] pts = curr_traj.existing_particles;
        for (final Particle p : pts) {
            rt.incrementCounter();
            rownum = rt.getCounter() - 1;
            rt.setValue("Trajectory", rownum, curr_traj.serial_number);
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
            rt.setValue("Trajectory", rownum, currentTrajectory.serial_number);
            rt.setValue("Trajectory length", rownum, currentTrajectory.length);
            rt.setValue("MSS: slope", rownum, ta.getMSSlinear());
            rt.setValue("MSS: y-axis intercept", rownum, ta.getMSSlinearY0());
            // second element in 'gammas' array is an order=2 (MSD)
            final int secondOrder = 1;
            rt.setValue("MSD: slope", rownum, ta.getGammasLogarithmic()[secondOrder]);
            rt.setValue("MSD: y-axis intercept", rownum, ta.getGammasLogarithmicY0()[secondOrder]);
            rt.setValue("Diffusion Coefficient D2", rownum, ta.getDiffusionCoefficients()[secondOrder]);
            rt.setValue("Pixel size", rownum, aPixelDimensions);
            rt.setValue("Time interval", rownum, aTimeInterval);
        }
    }

    public ResultsTable mssTrajectoryResultsToTable(Trajectory aTrajectory, double aPixelDimensions, double aTimeInterval) {
        final ResultsTable rt = getResultsTable();

        if (rt != null) {
            computeMssForOneTrajectory(rt, aTrajectory, aPixelDimensions, aTimeInterval);

            if (isGuiMode == true) {
                rt.show("Results");
            }
        }

        return rt;
    }

    public ResultsTable mssAllResultsToTable(double aPixelDimensions, double aTimeInterval) {
        final ResultsTable rt = getResultsTable();

        if (rt != null) {
            final Iterator<Trajectory> iter = all_traj.iterator();
            while (iter.hasNext()) {
                final Trajectory currentTrajectory = iter.next();
                computeMssForOneTrajectory(rt, currentTrajectory, aPixelDimensions, aTimeInterval);
            }

            if (isGuiMode == true) {
                rt.show("Results");
            }
        }

        return rt;
    }

    public int getLinkRange() {
        return linkrange;
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
        else {
            return "Unknown";
        }
    }

    public String getThresholdValue() {
        if (detector.getThresholdMode() == Mode.PERCENTILE_MODE) {
            return "" + (detector.getPercentile() * 100);
        }
        else if (detector.getThresholdMode() == Mode.ABS_THRESHOLD_MODE) {
            return "" + detector.getAbsIntensityThreshold();
        }
        else {
            return "0";
        }
    }

    public int getWidth() {
        return stack.getWidth();
    }

    public int getHeight() {
        return stack.getHeight();
    }

    public int getNumberOfSlices() {
        return slices_number;
    }

    public float getGlobalMinimum() {
        return detector.getGlobalMin();
    }

    public float getGlobalMaximum() {
        return detector.getGlobalMax();
    }

    public int getNumberOfFrames() {
        return frames_number;
    }

    void createLinkFactorDialog() {
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
        final String dm = gd.getNextChoice();

        if (dm.equals("Greedy")) {
            iParticleLinker = new ParticleLinkerBestOnePerm();
        }
        else {
            iParticleLinker = new ParticleLinkerHun();
        }
    }
}
