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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JLabel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import ij.process.StackStatistics;
import mosaic.core.GUI.ParticleTrackerHelp;
import mosaic.core.detection.FeaturePointDetector;
import mosaic.core.detection.MyFrame;
import mosaic.core.detection.MyFrame.DrawType;
import mosaic.core.detection.Particle;
import mosaic.core.detection.PreviewCanvas;
import mosaic.core.detection.PreviewInterface;
import mosaic.core.particleLinking.ParticleLinker;
import mosaic.core.particleLinking.ParticleLinkerBestOnePerm;
import mosaic.core.particleLinking.ParticleLinkerHun;
import mosaic.core.particleLinking.linkerOptions;
import mosaic.core.test.PlugInFilterExt;
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
 * This class implements a to 3d extended feature point detection and tracking algorithm as described in:
 * <br>
 * I. F. Sbalzarini and P. Koumoutsakos.
 * <br>
 * Feature point tracking and trajectory analysis for video imaging in cell biology.
 * <br>
 * J. Struct. Biol., 151(2), 2005.
 * <p>
 * Any publications that made use of this plugin should cite the above reference.
 * <br>
 * This helps to ensure the financial support of our project at ETH and will
 * enable us to provide further updates and support.
 * <br>
 * Thanks for your help!
 * </p>
 * <br>
 * For more information go <a href="http://weeman.inf.ethz.ch/particletracker/">here</a>
 * <p>
 * <b>Disclaimer</b>
 * <br>
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
 *         <a href="http://www.mosaic.ethz.ch/">Mosaic Group, Inst. of theoretical computer
 *         science<a>, ETH Zurich
 *         3D extension, memory efficiency, parallelism, algorithmic speed up, batch processing
 *         feature extension made by:
 * @version 1.6 November 2010 (requires: ImageJ 1.44 or higher)
 * @author Kota Miura - CMCI, EMBL Heidelberg (http://cmci.embl.de)
 *         add functionality to automatically transfer resulting data to result table in ImageJ,
 * @version 1.7 (require: ImgLib2)
 * @author Pietro Incardona
 *         add Dynamic model in the linker, new 3D/2D visualization system, CSV reading format,
 *         for Region based tracking
 */

public class ParticleTracker3DModular_ implements PlugInFilterExt, Measurements, PreviewInterface {

    public Img<ARGBType> out;
    public String background;
    public boolean force;
    public boolean straight_line;
    protected float l_s = 1.0f;
    protected float l_f = 1.0f;
    protected float l_d = 1.0f;
    private ImageStack stack;
    private StackConverter sc;
    public ImagePlus original_imp;
    public MyFrame[] frames;
    public Vector<Trajectory> all_traj;// = new Vector();
    public int number_of_trajectories;
    public int frames_number;
    private int slices_number;
    private FeaturePointDetector detector;
    public ParticleLinker linker;
    public String title;

    /* user defined parameters for linking */
    public int linkrange = 2; // default
    public double displacement = 10.0; // default

    // Fields required by trajectory analysis
    public double pixelDimensions; // physical pixel dimensions in meters
    public double timeInterval; // physical time interval between frames in seconds

    protected NonBlockingGenericDialog gd;

    /* flags */
    public boolean text_files_mode = false;
    private boolean only_detect = false;
    private boolean frames_processed = false;

    /* results display and file */
    public int magnification_factor = 4;
    public int chosen_traj = -1;
    public ResultsWindow results_window;
    private PreviewCanvas preview_canvas = null;

    /* preview vars */
    // public Button preview, save_detected;
    // public Scrollbar preview_scrollbar;
    // public Label previewLabel = new Label("");

    /* vars for text_files_mode */
    private String files_dir;
    private String[] files_list;
    private boolean one_file_multiple_frame;
    private boolean csv_format = false;
    private File Csv_region_list;
    private boolean create_bck_image = true;
    private boolean creating_traj_image = false;

    /**
     * This method sets up the plugin filter for use.
     * <br>
     * It is called by ImageJ upon selection of the plugin from the menus and the returned value is
     * used to call the <code>run(ImageProcessor ip)</code> method.
     * <br>
     * The <code>arg</code> is a string passed as an argument to the plugin, can also be an empty string.
     * <br>
     * Different commands from the plugin menu call the same plugin class with a different argument.
     * <ul>
     * <li>"" (empty String) - the plugin will work in regular full default mode
     * <li>"about" - will call the <code>showAbout()</code> method and return <code>DONE</code>,
     * meaning without doing anything else
     * <li>"only_detect" - the plugin will work in detector only mode and unlike the regular
     * mode will allow input of only one image
     * </ul>
     * The argument <code>imp</code> is passed by ImageJ - the currently active image is passed.
     * 
     * @param arg A string command that determines the mode of this plugin - can be empty
     * @param imp The ImagePlus that is the original input image sequence -
     *            if null then <code>text_files_mode</code> is activated after an OK from the user
     * @return a flag word that represents the filters capabilities according to arg String argument
     * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
     */
    @Override
    public int setup(String arg, ImagePlus imp) {
        if (MosaicUtils.checkRequirement() == false) {
            return DONE;
        }

        if (arg.equals("about")) {
            showAbout();
            return DONE;
        }

        if (arg.equals("only_detect")) {
            only_detect = true;
        }

        MyFrame.initCache();
        this.original_imp = imp;

        // Initialite the linker

        linker = new ParticleLinkerBestOnePerm();

        if (imp == null && !only_detect) {
            if (IJ.showMessageWithCancel("Text Files Mode", "Do you want to load particles positions from text files?")) {
                text_files_mode = true;
                return NO_IMAGE_REQUIRED;
            }
            IJ.error("You must load an Image Sequence or Movie first");
            return DONE;
        }
        if (imp == null) {
            IJ.error("You must load an Image Sequence or Movie first");
            return DONE;
        }
        else {
            // we have an image, we do not need to create an image

            create_bck_image = false;
        }

        // Check if there are segmentation information

        if (MosaicUtils.checkSegmentationInfo(imp, null)) {
            final YesNoCancelDialog YN_dialog = new YesNoCancelDialog(null, "Segmentation", "A segmentation has been founded for this image, do you want to track the regions");

            if (YN_dialog.yesPressed() == true) {
                SegmentationInfo info;
                info = MosaicUtils.getSegmentationInfo(imp);

                text_files_mode = true;
                csv_format = true;
                Csv_region_list = info.RegionList;
                create_bck_image = false;

                return NO_IMAGE_REQUIRED;
            }
        }

        // If you have an image with n slice and one frame is quite suspicious
        // that the time information is stored in the slice data, prompt if the data
        // are 2D or 3D

        if (imp.getStackSize() > 1 && imp.getNFrames() == 1) {
            final GenericDialog gd = new GenericDialog("Data dimension");

            final String ad[] = { "No", "Yes" };
            gd.addChoice("Are these 3D data ?", ad, "No");
            gd.showDialog();

            String saved_options = null;
            if (IJ.isMacro() && Macro.getOptions() != null && !Macro.getOptions().trim().isEmpty()) {
                saved_options = Macro.getOptions();
            }

            if (!gd.wasCanceled() && gd.getNextChoice().equals("No")) {
                IJ.run(imp, "Stack to Hyperstack...", "order=xyczt(default) channels=1 slices=1 frames=" + imp.getNSlices() + " display=Composite");
            }

            if (saved_options != null) {
                Macro.setOptions(saved_options);
            }
        }

        if (only_detect && this.original_imp.getStackSize() == 1) {
            return DOES_ALL + NO_CHANGES + SUPPORTS_MASKING;
        }
        return DOES_ALL + NO_CHANGES + SUPPORTS_MASKING + PARALLELIZE_STACKS;
    }

    /**
     * This method runs the plugin, what implemented here is what the plugin actually
     * does. It takes the image processor it works on as an argument.
     * <br>
     * In this implementation the processor is not used so that the original image is left unchanged.
     * <br>
     * The original image is locked while the plugin is running.
     * <br>
     * This method is called by ImageJ after <code>setup(String arg, ImagePlus imp)</code> returns
     * 
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(ImageProcessor ip) {
        initializeMembers();
        System.out.println("IJ macro is running: " + IJ.isMacro());
        if (!text_files_mode && !IJ.isMacro()) {
            preview_canvas = detector.generatePreviewCanvas(original_imp);
        }

        /* get user defined params and set more initial params accordingly */
        if (!getUserDefinedParams()) {
            return;
        }

        if (!processFrames()) {
            return;
        }

        if (original_imp != null) {
            original_imp.show();
        }

        /* link the particles found */
        IJ.showStatus("Linking Particles");

        final linkerOptions lo = new linkerOptions();
        lo.linkrange = linkrange;
        lo.displacement = (float) displacement;
        lo.force = force;
        lo.straight_line = straight_line;
        lo.l_s = l_s;
        lo.l_f = l_f;
        lo.l_d = l_d;
        if (linker.linkParticles(frames, frames_number, lo) == false) {
            return;
        }

        /* generate trajectories */
        IJ.showStatus("Generating Trajectories");
        generateTrajectories();

        if (IJ.isMacro()) {
            /* Write data to disk */
            writeDataToDisk();
        }
        else {
            /* Display results window */
            results_window = new ResultsWindow(this, "Results");
            results_window.configuration_panel.append(getConfiguration().toString());
            results_window.configuration_panel.append(getInputFramesInformation().toString());
            results_window.text_panel.appendLine("Particle Tracker DONE!");
            results_window.text_panel.appendLine("Found " + this.number_of_trajectories + " Trajectories");
            results_window.setVisible(true);

            IJ.showStatus("Creating trajectory image ...");
            creating_traj_image = true;

            out = createHyperStackFromFrames(background);
        }
    }

    private MyFrame[] convertIntoFrames(Vector<Particle> p) {

        // Read the first background
        Calibration cal = null;
        if (background != null) {
            if (original_imp == null) {
                original_imp = new Opener().openImage(new File(background.replace("*", "1")).getAbsolutePath());
            }

            if (original_imp != null) {
                cal = original_imp.getCalibration();
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

            stack = original_imp.getStack();
            this.title = original_imp.getTitle();

            // get global minimum and maximum
            final StackStatistics stack_stats = new StackStatistics(original_imp);
            final float global_max = (float) stack_stats.max;
            final float global_min = (float) stack_stats.min;
            frames_number = original_imp.getNFrames();
            slices_number = original_imp.getNSlices();

            detector = new FeaturePointDetector(global_max, global_min);
        }
        else {
            slices_number = 1;
        }
    }

    private int f_size = 0;
    private double f_intensity = 0.0;
    private String file_sel;

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

        // Get iterator

        Iterator<Particle> i = pl.iterator();

        // rescale and filter

        while (i.hasNext()) {
            final Particle p = i.next();

            if (p.m0 >= f_size && p.m2 > f_intensity) {
                p.x *= cal.pixelWidth;
                p.y *= cal.pixelHeight;
                p.z *= cal.pixelDepth;
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
     * Iterates through all frames(ImageProcessors or text files).
     * <br>
     * Creates a <code>MyFrame</code> object for each frame according to the input.
     * <br>
     * If non text mode: gets particles by applying <code>featurePointDetection</code> method on the current frame
     * <br>
     * if text mode set particles according to input files
     * <br>
     * Adds every <code>MyFrame</code> created to the <code>frames</code> array
     * <br>
     * Setes the <code>frames_processed</code> flag to true
     * <br>
     * If the frames were already processed do nothing and return true
     * 
     * @see MyFrame
     * @see MyFrame#featurePointDetection()
     */
    private boolean processFrames() {
        if (frames_processed) {
            return true;
        }

        /* Initialise frames array */

        MyFrame current_frame = null;

        if (csv_format == true) {
            IJ.showStatus("Reading CSV Regions data ...");
            final CSV<Particle> P_csv = new CSV<Particle>(Particle.class);

            P_csv.setCSVPreferenceFromFile(files_dir + File.separator + file_sel);
            final Vector<Particle> p = P_csv.Read(files_dir + File.separator + file_sel, null);
            
            if (p.size() == 0) {
                IJ.error("No regions defined for this image,nothing to do");
                return false;
            }

            background = P_csv.getMetaInformation("background");

            IJ.showStatus("Creating frames with particles ...");

            frames = convertIntoFrames(p);

            // It can happen that the segmentation algorithm produce
            // double output on the CSV

            for (int i = 0; i < frames.length; i++) {
                frames[i].removeDoubleParticles();
            }

            frames_number = frames.length;

            // Create image if needed

            /* create an ImagePlus object to hold the particle information from the text files */

            if (create_bck_image == true) {
                IJ.showStatus("Creating background image ...");

                final Img<ARGBType> iw = createHyperStackFromFrames(background);
                if (iw != null) {
                    original_imp = ImageJFunctions.wrap(iw, "Video");
                    original_imp.show();
                    // reslice
                }
                else {
                    return false;
                }
            }
        }
        else if (one_file_multiple_frame == false) {
            frames = new MyFrame[frames_number];
            for (int frame_i = 0, file_index = 0; frame_i < frames_number; frame_i++, file_index++) {

                if (text_files_mode) {
                    if (files_list[file_index].startsWith(".") || files_list[file_index].endsWith("~")) {
                        frame_i--;
                        continue;
                    }

                    // text_files_mode:
                    // construct each frame from the conrosponding text file
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
                if (current_frame.frame_number >= frames.length) {
                    IJ.showMessage("Error, frame " + current_frame.frame_number + "  is out of range, enumeration must not have hole, and must start from 0");
                    return false;
                }
                frames[current_frame.frame_number] = current_frame;
            } // for

            // Here check that all frames are created

            for (int i = 0; i < frames.length; i++) {
                if (frames[i] == null) {
                    IJ.showMessage("Error, frame: " + i + " does not exist");
                    return false;
                }
            }

            if (create_bck_image == true) {
                IJ.showStatus("Creating background image ...");

                final Img<ARGBType> iw = createHyperStackFromFrames(background);
                if (iw != null) {
                    original_imp = ImageJFunctions.wrap(iw, "Video");
                    original_imp.show();
                }
            }
        }
        else {
            final Vector<MyFrame> tmf = new Vector<MyFrame>();
            BufferedReader r = null;
            try {
                r = new BufferedReader(new FileReader(this.files_dir + files_list[0]));
            }
            catch (final Exception e) {
                IJ.error(e.getMessage());
                return false;
            }
            finally {
                if (r != null) try {
                    r.close();
                }
                catch (final IOException e) {
                    e.printStackTrace();
                }
            }

            tmf.add(new MyFrame(r));
            while (tmf.lastElement().getParticles() != null) {
                tmf.add(new MyFrame(r));
            }

            // remove any null object

            tmf.remove(tmf.size() - 1);

            // copy the frames and discharge the vector

            frames = new MyFrame[tmf.size()];

            for (int i = 0; i < tmf.size(); i++) {
                frames[i] = tmf.get(i);
            }
            frames_number = frames.length;
        }

        frames_processed = true;

        return true;
    }

    /**
     * Displays a dialog window to get user defined params and selections,
     * also initialize and sets other params according to the work mode.
     * <ul>
     * <br>
     * For a sequence of images:
     * <ul>
     * <li>Gets user defined params:<code> radius, cutoff, precentile, linkrange, displacement</code>
     * <li>Displays the preview Button and slider
     * <li>Gives the option to convert the image seq to 8Bit if its color
     * <li>Initialize and sets params:<code> stack, title, global_max, global_min, mask, kernel</code>
     * <br>
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
        GenericDialog text_mode_gd;
        one_file_multiple_frame = false;
        boolean convert = false;

        // Add help panel

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

        //

        if (text_files_mode) {
            if (Csv_region_list == null) {
                text_mode_gd = new GenericDialog("input files info", IJ.getInstance());
                text_mode_gd.addMessage("Please specify the info provided for the Particles...");
                // text_mode_gd.addCheckbox("one file multiple frame (deprecated)", false);
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
                    int nf = 0;

                    final Vector<String> v = new Vector<String>();

                    for (int i = 0; i < files_list.length; i++) {
                        final File f = new File(files_dir + File.separator + files_list[i]);
                        if (files_list[i].endsWith("csv") == true && f.exists() && f.isDirectory() == false) {
                            v.add(files_list[i]);
                            nf++;
                        }
                    }

                    files_list = new String[nf];
                    v.toArray(files_list);
                }

                this.title = "text_files";
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
            detector.addUserDefinedParametersDialog(gd);

            gd.addPanel(detector.makePreviewPanel(this, original_imp), GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));

            // check if the original images are not GRAY8, 16 or 32
            if (this.original_imp.getType() != ImagePlus.GRAY8 && this.original_imp.getType() != ImagePlus.GRAY16 && this.original_imp.getType() != ImagePlus.GRAY32) {
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
        //
        // gd.addMessage("Trajectory Analysis Data :\n");
        /// These 2 params are relevant for both working modes
        // gd.addNumericField("Length of pixel (in mm)", 1, 3);
        // gd.addNumericField("Time interval between frames (in s)", 1.0, 3);

        // Create advanced option panel

        final Button a_opt = new Button("Advanced options");
        a_opt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final GenericDialog gd = new GenericDialog("Link factor");

                gd.addMessage("weight of different contributions for linking\n relative to the distance normalized to one");

                gd.addNumericField("Object feature", l_f, 3);
                gd.addNumericField("Dynamics", l_d, 3);

                final String sc[] = new String[2];

                sc[0] = new String("Greedy");
                sc[1] = new String("Hungarian");

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
                    linker = new ParticleLinkerBestOnePerm();
                }
                else {
                    linker = new ParticleLinkerHun();
                }
            }
        });

        final Panel preview_panel = new Panel();

        preview_panel.add(a_opt);
        gd.addPanel(preview_panel);

        // Introduce a label with reference

        final JLabel labelJ = new JLabel("<html>Please refer to and cite:<br><br>" + "I. F. Sbalzarini and P. Koumoutsakos.<br> Feature Point "
                + "Tracking and<br> Trajectory Analysis for<br>Video Imaging in Cell Biology,<br>" + "Journal of Structural Biology<br> 151(2):182-195, 2005.<br>" + "</html>");

        p = new Panel();
        p.add(labelJ);
        gd.addPanel(p);

        gd.showDialog();

        // retrieve params from user
        if (!text_files_mode) {
            final Boolean changed = detector.getUserDefinedParameters(gd);
            // even if the frames were already processed (particles detected) but
            // the user changed the detection params then the frames needs to be processed again
            if (changed) {
                if (this.frames_processed) {
                    this.frames = null;
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

        // this.pixelDimensions = gd.getNextNumber()/1000.0;
        // this.timeInterval = gd.getNextNumber();
        this.pixelDimensions = 1;
        this.timeInterval = 1;

        // if Cancel button was clicked
        if (gd.wasCanceled()) {
            return false;
        }

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
            sc = new StackConverter(original_imp);
            sc.convertToGray8();
            stack = original_imp.getStack();
            this.title = original_imp.getTitle();
            final StackStatistics stack_stats = new StackStatistics(original_imp);
            detector.global_max = (float) stack_stats.max;
            detector.global_min = (float) stack_stats.min;
            frames_number = original_imp.getNFrames(); // ??maybe not necessary
        }
        return true;
    }

    // private void thresholdModeChanged(int aThresholdMode) {
    // setThresholdMode(aThresholdMode);
    // if (aThresholdMode == ABS_THRESHOLD_MODE) {
    // int defaultIntensity = (int)(global_max - (global_max-global_min) / 5);
    // ((TextField)gd.getNumericFields().elementAt(2)).setText("" + defaultIntensity);
    // }
    // if (aThresholdMode == PERCENTILE_MODE) {
    // ((TextField)gd.getNumericFields().elementAt(2)).setText(IJ.d2s(0.1, 5));
    // }
    //
    // }

    /**
     * Shows an ImageJ message with info about this plugin
     */
    private void showAbout() {
        IJ.showMessage("ParticleTracker...",
                "An ImageJ Plugin for particles detection and tracking from digital videos.\n"
                        + "The plugin implements an extended version of the feature point detection and tracking algorithm as described in:\n" + "I. F. Sbalzarini and P. Koumoutsakos.\n"
                        + "Feature point tracking and trajectory analysis for video imaging in cell biology.\n" + "J. Struct. Biol., 151(2): 182195, 2005.\n"
                        + "Any publications that made use of this plugin should cite the above reference.\n"
                        + "This helps to ensure the financial support of our project at ETH and will enable us to provide further updates and support.\n" + "Thanks for your help!\n"
                        + "Written by: Guy Levy, Extended by: Janick Cardinale" + "Version: 1.0. January, 2008\n" + "Requires: ImageJ 1.38u or higher and Java 5\n"
                        + "For more information go to http://weeman.inf.ethz.ch/particletracker/");
    }


    /**
     * Generates <code>Trajectory</code> objects according to the information
     * available in each MyFrame and Particle.
     * <br>
     * Populates the <code>all_traj</code> Vector.
     */
    public void generateTrajectories() {

        int i, j, k;
        int found, n, m;
        // Bank of colors from which the trajectories color will be selected

        Trajectory curr_traj;
        // temporary vector to hold particles for current trajectory
        final Vector<Particle> curr_traj_particles = new Vector<Particle>(frames_number);
        // initialize trajectories vector
        all_traj = new Vector<Trajectory>();
        this.number_of_trajectories = 0;
        int cur_traj_start = 0;

        for (i = 0; i < frames_number; i++) {
            for (j = 0; j < this.frames[i].getParticles().size(); j++) {
                if (!this.frames[i].getParticles().elementAt(j).special) {
                    this.frames[i].getParticles().elementAt(j).special = true;
                    found = -1;
                    // go over all particles that this particle (particles[j]) is linked to
                    for (n = 0; n < this.linkrange; n++) {
                        // if it is NOT a dummy particle - stop looking
                        if (this.frames[i].getParticles().elementAt(j).next[n] != -1) {
                            found = n;
                            break;
                        }
                    }
                    // if this particle is not linked to any other
                    // go to next particle and dont add a trajectory
                    if (found == -1) {
                        continue;
                    }

                    // Added by Guy Levy, 18.08.06 - A change form original implementation
                    // if this particle is linkd to a "real" paritcle that was already linked
                    // break the trajectory and start again from the next particle. dont add a trajectory
                    if (this.frames[i + n + 1].getParticles().elementAt(this.frames[i].getParticles().elementAt(j).next[n]).special) {
                        continue;
                    }

                    // this particle is linked to another "real" particle that is not already linked
                    // so we have a trajectory
                    this.number_of_trajectories++;

                    if (curr_traj_particles.size() == 0) {
                        cur_traj_start = i;
                    }

                    curr_traj_particles.add(this.frames[i].getParticles().elementAt(j));
                    k = i;
                    m = j;
                    do {
                        found = -1;
                        for (n = 0; n < this.linkrange; n++) {
                            if (this.frames[k].getParticles().elementAt(m).next[n] != -1) {
                                // If this particle is linked to a "real" particle that
                                // that is NOT already linked, continue with building the trajectory
                                if (this.frames[k + n + 1].getParticles().elementAt(this.frames[k].getParticles().elementAt(m).next[n]).special == false) {
                                    found = n;
                                    break;
                                    // Added by Guy Levy, 18.08.06 - A change form original implementation
                                    // If this particle is linked to a "real" particle that
                                    // that is already linked, stop building the trajectory
                                }
                                else {
                                    break;
                                }
                            }
                        }
                        if (found == -1) {
                            break;
                        }
                        m = this.frames[k].getParticles().elementAt(m).next[found];
                        k += (found + 1);
                        curr_traj_particles.add(this.frames[k].getParticles().elementAt(m));
                        this.frames[k].getParticles().elementAt(m).special = true;
                    } while (m != -1);

                    // Create the current trajectory
                    final Particle[] curr_traj_particles_array = new Particle[curr_traj_particles.size()];
                    curr_traj = new Trajectory(curr_traj_particles.toArray(curr_traj_particles_array), original_imp);

                    // set current trajectory parameters
                    curr_traj.serial_number = this.number_of_trajectories;
                    curr_traj.setFocusArea();
                    curr_traj.setMouseSelectionArea();
                    curr_traj.populateGaps();
                    curr_traj.start_frame = cur_traj_start;
                    curr_traj.stop_frame = curr_traj.existing_particles[curr_traj.existing_particles.length - 1].getFrame();
                    // add current trajectory to all_traj vactor
                    all_traj.add(curr_traj);
                    // clear temporary vector
                    curr_traj_particles.removeAllElements();
                }
            }
        }

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

        for (int s = 0; s < all_traj.size(); s++) {
            all_traj.elementAt(s).color = vI.elementAt(s);
        }
    }

    private String getUnit() {
        final Calibration cal = original_imp.getCalibration();

        return cal.getUnit();
    }

    /*
     * Get scaling factor
     */
    public double[] getScaling() {
        final Calibration cal = original_imp.getCalibration();
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
        final Calibration cal = original_imp.getCalibration();

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
        // get original file location
        final FileInfo vFI = this.original_imp.getOriginalFileInfo();
        if (vFI == null) {
            IJ.error("You're running a macro. Data are written to disk at the directory where your image is stored. Please store youre image first.");
            return;
        }

        // create new directory
        // File newDir = new File(vFI.directory,"ParticleTracker3DResults");
        // if (!newDir.mkdir() && !newDir.exists()) {
        // IJ.error("You probably do not have the permission to write in the directory where your image is stored. Data are not written to disk.");
        // return;
        // }
        /// write data to file
        // write2File(newDir.getAbsolutePath(), vFI.fileName + "PT3D.txt", getFullReport().toString());
        MosaicUtils.write2File(vFI.directory, "Traj_" + title + ".txt", getFullReport().toString());
        writeXMLFormatReport(new File(vFI.directory, title + "_r_" + getRadius() + "_c_" + detector.cutoff + "_perc_" + detector.percentile + "_PT3Dresults.xml").getAbsolutePath());
        new TrajectoriesReportXML(new File(vFI.directory, "report.xml").getAbsolutePath(), this);
        final ResultsTable rt = transferTrajectoriesToResultTable();
        try {
            rt.saveAs(new File(vFI.directory, "Traj_" + title + ".csv").getAbsolutePath());
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the radius of the particles
     *
     * @return the radius of the particles, return -1 if this parameter is not set (like segmented data)
     */

    public int getRadius() {
        int radius = -1;
        if (detector != null) {
            radius = detector.radius;
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
        if (original_imp == null) {
            return;
        }

        detector.getUserDefinedPreviewParams(gd);

        final ImagePlus frame = MosaicUtils.getImageFrame(original_imp, original_imp.getFrame());

        final MyFrame preview_frame = new MyFrame(frame.getStack(), original_imp.getFrame(), linkrange);

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
        int passed_traj = 0;
        final GenericDialog fod = new GenericDialog("Filter Options...", IJ.getInstance());
        // default is not to filter any trajectories (min length of zero)
        fod.addNumericField("Only keep trajectories longer than", 0, 0, 10, "frames");

        // fod.addNumericField("Only keep the", this.number_of_trajectories, 0, 10, "longest trajectories");
        fod.showDialog();
        final int min_length_to_display = (int) fod.getNextNumber();
        // this.trajectories_longer = (int)fod.getNextNumber();

        if (fod.wasCanceled()) {
            return false;
        }

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
            configuration.append(detector.cutoff);
            configuration.append("\n");
            if (detector.threshold_mode == FeaturePointDetector.PERCENTILE_MODE) {
                configuration.append("% \tPercentile: ");
                configuration.append((detector.percentile * 100));
                configuration.append("\n");
            }
            else if (detector.threshold_mode == FeaturePointDetector.ABS_THRESHOLD_MODE) {
                configuration.append("% \tAbsolute threshold: ");
                configuration.append((detector.absIntensityThreshold));
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
        info.append(detector.global_min);
        info.append("\n");
        info.append("% \tGlobal maximum: ");
        info.append(detector.global_max);
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
            // if (this.text_files_mode) {
            /// there is no original image so set magnification to default(1)
            // magnification = 1;
            // } else {
            /// Set magnification to the one of original_imp
            // magnification = original_imp.getWindow().getCanvas().getMagnification();
            // }
        }
        else {
            // if the view is generated on an already existing image,
            // set the updated view scale (magnification) to be the same as in the existing image
            // magnification = duplicated_imp.getWindow().getCanvas().getMagnification();

            duplicated_imp.setImage(ImageJFunctions.wrap(out, new_title));
        }

        // Create a new window to hold the image and canvas
        new TrajectoryStackWin(this, duplicated_imp, duplicated_imp.getWindow().getCanvas(), out);

        // zoom the window until its magnification will reach the set magnification magnification
        /*
         * while (tsw.getCanvas().getMagnification() < magnification) {
         * tc.zoomIn(0,0);
         * }
         */
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
        // Clean Circle Mask cache

        MyFrame.cleanCache();

        // create a title
        final String new_title = "[Trajectory number " + (trajectory_index + 1) + "]";

        // get the trajectory at the given index
        final Trajectory traj = (all_traj.elementAt(trajectory_index));

        // Here we check the if the magnification if compatible with the Available memory

        final long AvaMem = IJ.maxMemory() - IJ.currentMemory();

        final Rectangle r = traj.focus_area.getBounds();

        final long cropped_size = r.height * r.width * 4 * traj.length * original_imp.getNSlices();

        final long ReqMem = cropped_size * magnification * magnification;

        if (ReqMem >= AvaMem * 3 / 4) {
            magnification = (int) Math.sqrt((AvaMem * 3 / 4) / cropped_size);
        }

        if (magnification <= 1) {
            IJ.error("No available memory to create a focus view");
        }

        // Create a cropped rescaled image

        final Img<UnsignedByteType> img = ImagePlusAdapter.wrap(original_imp);

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
        final Calibration cal = original_imp.getCalibration();
        MyFrame.updateImage(focus_view, traj.focus_area.getBounds(), traj.start_frame, vt, cal, DrawType.TRAJECTORY_HISTORY, getRadius());

        final ImagePlus imp = ImageJFunctions.show(focus_view);
        imp.setTitle(new_title);

        new FocusStackWin(imp, traj, (float) cal.pixelDepth);

        IJ.showStatus("Done");
    }

    /**
     * Generates and displays a new <code>StackWindow</code> with rescaled (magnified)
     * view of the Roi that was selected on ImageJs currently active window.
     * <br>
     * The new Stack will be made of RGB ImageProcessors upon which the trajectories in the Roi
     * will be drawn
     * <br>
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
        // traj_stack = duplicated_imp.getStack();

        // Reset the active imageJ window to the one the ROI was selected on - info from the Roi is still needed
        IJ.selectWindow(roi_image_id);

        IJ.selectWindow(duplicated_imp.getID());

    }

    /**
     * Opens a dialog where the user can select a text mode particles file
     * 
     * @return an array of All the file names in the selected folder
     *         or null if the user cancelled the selection.
     *         is some O.S (e.g. Linux) this may include '.' and '..'
     * @see ij.io.OpenDialog#OpenDialog(java.lang.String, java.lang.String, java.lang.String)
     * @see java.io.File#list()
     */

    /**
     * Opens an 'open file' dialog where the user can select a folder
     * 
     * @return an array of All the file names in the selected folder
     *         or null if the user cancelled the selection.
     *         is some O.S (e.g. Linux) this may include '.' and '..'
     * @see ij.io.OpenDialog#OpenDialog(java.lang.String, java.lang.String, java.lang.String)
     * @see java.io.File#list()
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
        for (int i = 0; i < frames.length; i++) {
            for (int p = 0; p < frames[i].getParticles().size(); p++) {
                final Particle vParticle = frames[i].getParticles().elementAt(p);
                if (vParticle.x > vMax[0]) {
                    vMax[0] = (int) Math.ceil(vParticle.x);
                }
                if (vParticle.y > vMax[1]) {
                    vMax[1] = (int) Math.ceil(vParticle.y);
                }
                if (vParticle.z > vMax[2]) {
                    vMax[2] = (int) Math.ceil(vParticle.z);
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
     *
     * @return
     */

    public Img<ARGBType> createHyperStackFromFrames(String background) {
        int[] vMax = null;
        Img<ARGBType> out_f = null;
        Img<ARGBType> out_fs = null;

        vMax = getParticlesRange();

        for (int i = 0; i < vMax.length; i++) {
            vMax[i] += 1;
        }

        // Create time Image

        if (text_files_mode == true) {
            if (background == null) {
                final long vMaxp1[] = new long[vMax.length + 1];

                for (int i = 0; i < vMax.length; i++) {
                    vMaxp1[i] = vMax[i];
                }
                vMaxp1[vMax.length] = this.frames.length;

                final ImgFactory<ARGBType> imgFactory = new CellImgFactory<ARGBType>();
                out_fs = imgFactory.create(vMaxp1, new ARGBType());
            }
            else {
                // Open first background to get the size

                if (original_imp == null) {
                    final File file = new File(background.replace("*", Integer.toString(1)));

                    // open a file with ImageJ
                    original_imp = new Opener().openImage(file.getAbsolutePath());
                }

                long vMaxp1[] = null;

                if (original_imp != null) {
                    ImagePlus imp = null;
                    if (original_imp.getNFrames() > 1) {
                        imp = MosaicUtils.getImageFrame(original_imp, 1);
                    }
                    else {
                        imp = original_imp;
                    }

                    final Img<UnsignedByteType> backgroundImg = ImagePlusAdapter.wrap(imp);

                    vMaxp1 = new long[backgroundImg.numDimensions() + 1];

                    for (int i = 0; i < backgroundImg.numDimensions(); i++) {
                        vMaxp1[i] = backgroundImg.dimension(i);
                    }
                    vMaxp1[backgroundImg.numDimensions()] = this.frames.length;
                }
                else {
                    // Cannot open the background

                    IJ.error("Cannot open the background " + background);
                    creating_traj_image = false;
                    return null;
                }

                final ImgFactory<ARGBType> imgFactory = new CellImgFactory<ARGBType>();
                out_fs = imgFactory.create(vMaxp1, new ARGBType());
            }
        }
        else {
            // Open original image

            final Img<UnsignedByteType> backgroundImg = ImagePlusAdapter.wrap(original_imp);

            final long vMaxp1[] = new long[backgroundImg.numDimensions()];

            for (int i = 0; i < backgroundImg.numDimensions(); i++) {
                vMaxp1[i] = backgroundImg.dimension(i);
            }

            final ImgFactory<ARGBType> imgFactory = new CellImgFactory<ARGBType>();
            out_fs = imgFactory.create(vMaxp1, new ARGBType());
        }

        /* for each frame we have to add a stack to the image */
        for (int i = 0; i < frames.length; i++) {
            IJ.showStatus("Creating frame " + (i + 1));
            if (text_files_mode == true) {
                // Create frame image

                if (background != null) {
                    ImagePlus imp = null;

                    if (original_imp.getNFrames() > 1 && i < original_imp.getNFrames()) {
                        imp = MosaicUtils.getImageFrame(original_imp, i + 1);
                    }
                    else {
                        if (original_imp.getNChannels() >= 1 && i < original_imp.getNChannels()) {
                            imp = MosaicUtils.getImageSlice(original_imp, i + 1);
                        }
                    }

                    final Calibration cal = original_imp.getCalibration();

                    if (imp == null) {
                        IJ.error("Cannot find the background image or wrong format");
                        creating_traj_image = false;
                        return null;
                    }

                    // wrap it into an ImgLib image (no copying)
                    final Img<UnsignedByteType> backgroundImg = ImagePlusAdapter.wrap(imp);

                    out_f = frames[i].createImage(backgroundImg, all_traj, cal, i, DrawType.TRAJECTORY_HISTORY);

                    // It failed end
                    if (out_f == null) {
                        break;
                    }
                }
                else {
                    out_f = frames[i].createImage(vMax, all_traj, i, DrawType.TRAJECTORY_HISTORY);

                    // It failed end
                    if (out_f == null) {
                        break;
                    }
                }
            }
            else {
                final Calibration cal = original_imp.getCalibration();

                // wrap it into an ImgLib image (no copying)

                final ImagePlus timp = MosaicUtils.getImageFrame(original_imp, i + 1);
                final Img<UnsignedByteType> backgroundImg = ImagePlusAdapter.wrap(timp);

                frames[i].setParticleRadius(getRadius());
                out_f = frames[i].createImage(backgroundImg, all_traj, cal, i, DrawType.TRAJECTORY_HISTORY);
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
     * @see #getConfiguration()
     * @see #getInputFramesInformation()
     * @see MyFrame#getFullFrameInfo()
     * @see MyFrame#toStringBuffer() * @see #getTrajectoriesInfo()
     */
    public StringBuffer getFullReport() {

        /* initial infomation to output */
        final StringBuffer report = new StringBuffer();
        report.append(this.getConfiguration());
        report.append(this.getInputFramesInformation());
        report.append("\n");

        /* detected particles infomation per frame */
        report.append("%\tPer frame information (verbose output):\n");
        for (int i = 0; i < frames.length; i++) {
            report.append(this.frames[i].getFullFrameInfo());
        }

        /* Add linking info */
        report.append("% Trajectory linking (verbose output):\n");
        for (int i = 0; i < frames.length; i++) {
            report.append(this.frames[i].toStringBuffer());
        }

        /* all trajectories info */
        report.append("\n");
        report.append(getTrajectoriesInfo());

        return report;
    }

    private void writeXMLFormatReport(String aFilename) {

        try {

            final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            final Document doc = docBuilder.newDocument();
            final Element rootElement = doc.createElement("root");
            doc.appendChild(rootElement);

            // staff elements
            final Element TrackContest = doc.createElement("TrackContestISBI2012");
            rootElement.appendChild(TrackContest);

            // set attributes
            // TrackContest.setAttribute("SNR", "__SNR__");
            // TrackContest.setAttribute("density", "__density__");
            // SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm::ss z yyyy");
            // TrackContest.setAttribute("generationDateTime", sdf.format(Calendar.getInstance().getTime()));
            // TrackContest.setAttribute("info", "http://bioimageanalysis.org/track/");
            // TrackContest.setAttribute("scenario", "__scenario__");

            // for each trajectory, generate a (ISBI-)particle element:
            final Iterator<Trajectory> iter = all_traj.iterator();
            boolean isFirst = true;
            while (iter.hasNext() && isFirst) {
                isFirst = false;
                final Element particleElement = doc.createElement("particle");
                final Trajectory curr_traj = iter.next();

                for (final Particle vP : curr_traj.existing_particles) {
                    final Element detectionElement = doc.createElement("detection");
                    detectionElement.setAttribute("t", "" + vP.getFrame());
                    detectionElement.setAttribute("x", "" + (vP.y - 0.5f));
                    detectionElement.setAttribute("y", "" + (vP.x - 0.5f));
                    detectionElement.setAttribute("z", "" + vP.z);
                    particleElement.appendChild(detectionElement);
                }
                TrackContest.appendChild(particleElement);
            }

            // write the content into xml file
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();
            final DOMSource source = new DOMSource(doc);
            final StreamResult result = new StreamResult(new File(aFilename));

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);

            transformer.transform(source, result);

        }
        catch (final ParserConfigurationException pce) {
            pce.printStackTrace();
        }
        catch (final TransformerException tfe) {
            tfe.printStackTrace();
        }
    }

    @Override
    public void preview(ActionEvent e) {
        // set the original_imp window position next to the dialog window
        this.original_imp.getWindow().setLocation((int) gd.getLocationOnScreen().getX() + gd.getWidth(), (int) gd.getLocationOnScreen().getY());
        // do preview
        this.preview();
        preview_canvas.repaint();
        return;
    }

    @Override
    public void saveDetected(ActionEvent e) {
        /* set the user defined pramars according to the valus in the dialog box */
        detector.getUserDefinedPreviewParams(gd);

        /* detect particles and save to files */
        if (this.processFrames()) { // process the frames
            detector.saveDetected(this.frames);
        }
        preview_canvas.repaint();
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
            for (int i = 0; i < frames.length; i++) {
                final Vector<Particle> particles = this.frames[i].getParticles();
                for (final Particle p : particles) {
                    rt.incrementCounter();
                    rownum = rt.getCounter() - 1;
                    rt.setValue("frame", rownum, p.getFrame());
                    rt.setValue("x", rownum, p.x);
                    rt.setValue("y", rownum, p.y);
                    rt.setValue("z", rownum, p.z);
                    rt.setValue("m0", rownum, p.m0);
                    rt.setValue("m1", rownum, p.m1);
                    rt.setValue("m2", rownum, p.m2);
                    rt.setValue("m3", rownum, p.m3);
                    rt.setValue("m4", rownum, p.m4);
                    rt.setValue("NPscore", rownum, p.score);
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
            int rownum = 0;
            while (iter.hasNext()) {
                final Trajectory curr_traj = iter.next();
                final Particle[] pts = curr_traj.existing_particles;
                for (final Particle p : pts) {
                    rt.incrementCounter();
                    rownum = rt.getCounter() - 1;
                    rt.setValue("Trajectory", rownum, curr_traj.serial_number);
                    rt.setValue("Frame", rownum, p.getFrame());
                    rt.setValue("x", rownum, p.x);
                    rt.setValue("y", rownum, p.y);
                    rt.setValue("z", rownum, p.z);
                    rt.setValue("m0", rownum, p.m0);
                    rt.setValue("m1", rownum, p.m1);
                    rt.setValue("m2", rownum, p.m2);
                    rt.setValue("m3", rownum, p.m3);
                    rt.setValue("m4", rownum, p.m4);
                    rt.setValue("NPscore", rownum, p.score);
                }

            }
            if (IJ.isMacro() == false) {
                rt.show("Results");
            }
        }
        return rt;
    }

    /**
     * corrdinates of selected trajectory (as argument) will be copied to
     * ImageJ results table.
     *
     * @param traj
     */
    public void transferSelectedTrajectoriesToResultTable(Trajectory traj) {
        final ResultsTable rt = getResultsTable();

        if (rt != null) {
            final Trajectory curr_traj = traj;
            int rownum = 0;
            final Particle[] pts = curr_traj.existing_particles;

            for (final Particle p : pts) {
                rt.incrementCounter();
                rownum = rt.getCounter() - 1;
                rt.setValue("Trajectory", rownum, curr_traj.serial_number);
                rt.setValue("Frame", rownum, p.getFrame());
                rt.setValue("x", rownum, p.x);
                rt.setValue("y", rownum, p.y);
                rt.setValue("z", rownum, p.z);
                rt.setValue("m0", rownum, p.m0);
                rt.setValue("m1", rownum, p.m1);
                rt.setValue("m2", rownum, p.m2);
                rt.setValue("m3", rownum, p.m3);
                rt.setValue("m4", rownum, p.m4);
                rt.setValue("NPscore", rownum, p.score);
            }
            rt.show("Results");
        }
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

    private void computeMssForOneTrajectory(ResultsTable rt, Trajectory currentTrajectory) {
        final TrajectoryAnalysis ta = new TrajectoryAnalysis(currentTrajectory);
        ta.setLengthOfAPixel(pixelDimensions);
        ta.setTimeInterval(timeInterval);
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
            rt.setValue("Pixel size", rownum, pixelDimensions);
            rt.setValue("Time interval", rownum, timeInterval);
        }
    }

    public ResultsTable mssTrajectoryResultsToTable(Trajectory aTrajectory) {
        final ResultsTable rt = getResultsTable();

        if (rt != null) {
            computeMssForOneTrajectory(rt, aTrajectory);

            if (IJ.isMacro() == false) {
                rt.show("Results");
            }
        }

        return rt;
    }

    public ResultsTable mssAllResultsToTable() {
        final ResultsTable rt = getResultsTable();

        if (rt != null) {
            final Iterator<Trajectory> iter = all_traj.iterator();
            while (iter.hasNext()) {
                final Trajectory currentTrajectory = iter.next();
                computeMssForOneTrajectory(rt, currentTrajectory);
            }

            if (IJ.isMacro() == false) {
                rt.show("Results");
            }
        }

        return rt;
    }

    public int getLinkRange() {
        return linkrange;
    }

    public double getCutoffRadius() {
        return detector.cutoff;
    }

    public String getThresholdMode() {
        if (detector.threshold_mode == FeaturePointDetector.PERCENTILE_MODE) {
            return "percentile";
        }
        else if (detector.threshold_mode == FeaturePointDetector.ABS_THRESHOLD_MODE) {
            return "Absolute";
        }
        else {
            return "Unknown";
        }
    }

    public String getThresholdValue() {
        if (detector.threshold_mode == FeaturePointDetector.PERCENTILE_MODE) {
            return "" + (detector.percentile * 100);
        }
        else if (detector.threshold_mode == FeaturePointDetector.ABS_THRESHOLD_MODE) {
            return "" + detector.absIntensityThreshold;
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
        return detector.global_min;
    }

    public float getGlobalMaximum() {
        return detector.global_max;
    }

    public int getNumberOfFrames() {
        return frames_number;
    }

    @Override
    public void closeAll() {
        // Close all the images
        original_imp.close();
    }

    boolean test_mode;

    @Override
    public void setIsOnTest(boolean test) {
        test_mode = test;

    }

    @Override
    public boolean isOnTest() {
        return test_mode;
    }
}
