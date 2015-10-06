package mosaic.plugins;


import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JFrame;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import mosaic.core.ImagePatcher.ImagePatch;
import mosaic.core.ImagePatcher.ImagePatcher;
import mosaic.core.cluster.ClusterGUI;
import mosaic.core.cluster.ClusterSession;
import mosaic.core.psf.GeneratePSF;
import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Segmentation;
import mosaic.region_competition.Algorithm;
import mosaic.region_competition.LabelImageRC;
import mosaic.region_competition.LabelInformation;
import mosaic.region_competition.Settings;
import mosaic.region_competition.GUI.ControllerFrame;
import mosaic.region_competition.GUI.GenericDialogGUI;
import mosaic.region_competition.GUI.InputReadable;
import mosaic.region_competition.energies.E_CV;
import mosaic.region_competition.energies.E_CurvatureFlow;
import mosaic.region_competition.energies.E_Deconvolution;
import mosaic.region_competition.energies.E_Gamma;
import mosaic.region_competition.energies.E_KLMergingCriterion;
import mosaic.region_competition.energies.E_PS;
import mosaic.region_competition.energies.Energy;
import mosaic.region_competition.energies.EnergyFunctionalType;
import mosaic.region_competition.energies.ImageModel;
import mosaic.region_competition.energies.RegularizationType;
import mosaic.region_competition.initializers.BoxInitializer;
import mosaic.region_competition.initializers.BubbleInitializer;
import mosaic.region_competition.initializers.MaximaBubbles;
import mosaic.region_competition.utils.IntConverter;
import mosaic.region_competition.utils.Timer;
import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.JsonDataFile;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;


/**
 * @author Stephan Semmler, ETH Zurich
 * @version 2012.06.11
 */
public class Region_Competition implements Segmentation {
    /**
     * enum to determine type of initialization
     */
    public enum InitializationType {
        Rectangle, Bubbles, LocalMax, ROI_2D, File, File_Patcher
    }
    
    private static final Logger logger = Logger.getLogger(Region_Competition.class);

    private final String[] out = { "*_ObjectsData_c1.csv", "*_seg_c1.tif" };

    private Region_Competition MVC; // interface to image application (imageJ)
    public Settings settings;

    protected Algorithm algorithm;
    private LabelImageRC labelImage; // data structure mapping pixels to labels
    private IntensityImage intensityImage;
    private ImageModel imageModel;
    private Calibration cal;
    private ImagePlus originalIP; // IP of the input image
    private final Vector<ImagePlus> OpenedImages;

    protected ImageStack stack; // stack saving the segmentation progress images
    protected ImagePlus stackImPlus; // IP showing the stack
    private boolean stackKeepFrames = false;
    private boolean normalize_ip = false;

    private ImageStack initialStack; // copy of the initial guess (without contour/boundary)

    private InputReadable userDialog;
    private JFrame controllerFrame;

    /**
     * Return the dimension of a file
     *
     * @param f file
     * @return
     */
    private int getDimension(File f) {
        final Opener o = new Opener();
        final ImagePlus ip = o.openImage(f.getAbsolutePath());

        return getDimension(ip);
    }

    /**
     * Return the dimension of an image
     *
     * @param aImp image
     * @return
     */
    private int getDimension(ImagePlus aImp) {
        if (aImp.getNSlices() == 1) {
            return 2;
        }
        else {
            return 3;
        }
    }

    private String getOptions(File f) {
        String par = new String();

        // get file dimension
        final int d = getDimension(f);
        par += "Dimensions=" + d + " ";

        // if deconvolving create a PSF generator window
        if (settings.m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC) {
            final GeneratePSF psf = new GeneratePSF();
            psf.generate(d);
            par += psf.getParameters();
        }

        return par;
    }

    private String getOptions(ImagePlus aImp) {
        String par = new String();

        // get file dimension
        final int d = getDimension(aImp);
        par += "Dimensions=" + d + " ";

        // if deconvolving create a PSF generator window
        final GeneratePSF psf = new GeneratePSF();
        psf.generate(d);
        par += psf.getParameters();

        return par;
    }

    /**
     * Run the segmentation on ImgLib2
     *
     * @param aArgs arguments
     * @param img Image
     * @param lbl Label image
     * @return
     */
    private <T extends RealType<T>, E extends IntegerType<E>> void runOnImgLib2(Img<T> img, Img<E> lbl) {
        initAndParse();

        // Run Region Competition as usual
        RCImageFilter(img, lbl);
    }

    private String sv = null;

    private void initAndParse() {
        final String options = Macro.getOptions();

        normalize_ip = true;
        if (options != null) {
            // Command line interface search for config file

            // normalize
            String normalizeString = MosaicUtils.parseString("normalize", options);
            if (normalizeString != null) {
                normalize_ip = Boolean.parseBoolean(normalizeString);
            }

            // config
            String tmp = null;
            String path;
            if ((tmp = MosaicUtils.parseString("config", options)) != null) {
                path = tmp;
                settings = getConfigHandler().LoadFromFile(path, Settings.class);
            }
            else {
                // load config file

                final String dir = IJ.getDirectory("temp");
                sv = dir + "rc_settings.dat";
                settings = getConfigHandler().LoadFromFile(sv, Settings.class);
            }

            output = MosaicUtils.parseString("output", options);

            // no config file open the GUI
        }
        else {
            // load config file

            final String dir = IJ.getDirectory("temp");
            sv = dir + "rc_settings.dat";
            settings = getConfigHandler().LoadFromFile(sv, Settings.class);
        }

        if (settings == null) {
            settings = new Settings();
        }

        MVC = this;
    }

    private Img<FloatType> image_psf;

    @Override
    public int setup(String aArgs, ImagePlus aImp) {
        if (MosaicUtils.checkRequirement() == false) {
            return DONE;
        }

        initAndParse();

        originalIP = aImp;
        userDialog = new GenericDialogGUI(this.settings, this.getOriginalImPlus());
        userDialog.showDialog();

        final boolean success = userDialog.processInput();
        if (!success) {
            return DONE;
        }

        if (userDialog.getInputImage() != null) {
            originalIP = (ImagePlus) userDialog.getInputImage();
            if (originalIP != null) {
                cal = originalIP.getCalibration();
            }
        }

        if (userDialog.useCluster() == true) {
            // We run on cluster
            // Copying parameters
            final Settings p = new Settings(settings);

            // saving config file
            getConfigHandler().SaveToFile("/tmp/settings.dat", p);

            final ClusterGUI cg = new ClusterGUI();
            ClusterSession ss = cg.getClusterSession();
            ss.setInputArgument("text1");
            ss.setSlotPerProcess(1);
            File[] fileslist = null;

            // Check if we selected a directory
            if (aImp == null) {
                final File fl = new File(userDialog.getInputImageFilename());
                final File fl_l = new File(userDialog.getLabelImageFilename());
                if (fl.isDirectory() == true) {
                    // we have a directory

                    String opt = getOptions(fl);
                    if (settings.labelImageInitType == InitializationType.File) {
                        // upload label images

                        ss = cg.getClusterSession();
                        fileslist = fl_l.listFiles();
                        final File dir = new File("label");
                        ss.upload(dir, fileslist);
                        opt += " text2=" + ss.getClusterDirectory() + File.separator + dir.getPath();
                    }

                    fileslist = fl.listFiles();

                    ss = ClusterSession.processFiles(fileslist, "Region Competition", opt + " show_and_save_statistics", out, cg);
                }
                else if (fl.isFile()) {
                    String opt = getOptions(fl);
                    if (settings.labelImageInitType == InitializationType.File) {
                        // upload label images
                        ss = cg.getClusterSession();
                        fileslist = new File[1];
                        fileslist[0] = fl_l;
                        ss.upload(fileslist);
                        opt += " text2=" + ss.getClusterDirectory() + File.separator + fl_l.getName();
                    }

                    ss = ClusterSession.processFile(fl, "Region Competition", opt + " show_and_save_statistics", out, cg);
                }
                else {
                    ss = ClusterSession.getFinishedJob(out, "Region Competition", cg);
                }
            }
            else {
                // It is an image
                String opt = getOptions(aImp);

                if (settings.labelImageInitType == InitializationType.File) {
                    // upload label images

                    ss = cg.getClusterSession();
                    ss.splitAndUpload((ImagePlus) userDialog.getLabelImage(), new File("label"), null);
                    opt += " text2=" + ss.getClusterDirectory() + File.separator + "label" + File.separator + ss.getSplitAndUploadFilename(0);
                }

                ss = ClusterSession.processImage(aImp, "Region Competition", opt + " show_and_save_statistics", out, cg);
            }

            // Get output format and Stitch the output in the output selected
            final File f = ClusterSession.processJobsData(MosaicUtils.ValidFolderFromImage(aImp));

            if (aImp != null) {
                MosaicUtils.StitchCSV(MosaicUtils.ValidFolderFromImage(aImp), out, MosaicUtils.ValidFolderFromImage(aImp) + File.separator + aImp.getTitle());
            }
            else {
                MosaicUtils.StitchCSV(f.getParent(), out, null);
            }

            return NO_IMAGE_REQUIRED;
        }
        else {
            getConfigHandler().SaveToFile(sv, settings);

            // if is 3D save the originalIP
            if (aImp != null) {
                if (aImp.getNSlices() != 1) {
                    originalIP = aImp;

                }
            }
            else {
                originalIP = null;
                return NO_IMAGE_REQUIRED;
            }

            if (settings.m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC) {
                // Here, no PSF has been set by the user. Hence, Generate it
                final GeneratePSF gPsf = new GeneratePSF();

                if (aImp.getNSlices() == 1) {
                    image_psf = gPsf.generate(2);
                }
                else {
                    image_psf = gPsf.generate(3);
                }
            }
        }
        return DOES_ALL + NO_CHANGES;
    }

    /**
     * Returns handler for (un)serializing Settings objects.
     */
    public static DataFile<Settings> getConfigHandler() {
        return new JsonDataFile<Settings>();
    }

    private String output;

    /**
     * Run region competition plugins
     */

    @Override
    public void run(ImageProcessor aImageP) {
        if (settings.labelImageInitType == InitializationType.File_Patcher) {
            // Open the label and intensity image with imgLib2

            initInputImage();
            initLabelImage();

            // Patches the image
            final int margins[] = new int[intensityImage.getDim()];

            for (int i = 0; i < margins.length; i++) {
                margins[i] = (int) (image_psf.dimension(i));
                if (margins[i] < 50) {
                    margins[i] = 50;
                }
            }

            final ImagePatcher<FloatType, IntType> ip = new ImagePatcher<FloatType, IntType>(intensityImage.getImgLib2(FloatType.class), labelImage.getImgLib2(IntType.class), margins);

            // for each patch run region competition
            @SuppressWarnings("unchecked")
            final ImagePatch<FloatType, IntType>[] ips = ip.getPathes();

            for (int i = 1; i < ips.length; i++) {
                settings.labelImageInitType = InitializationType.File;

                // Run region competition on the patches image

                ips[i].show();
                final Region_Competition RC = new Region_Competition();
                RC.settings = settings;
                RC.image_psf = image_psf;
                RC.cal = cal;
                RC.runOnImgLib2(ips[i].getImage(), ips[i].getLabelImage());
                RC.labelImage.eliminateForbidden();
                ips[i].setResult(RC.labelImage.getImgLib2(IntType.class));
                ips[i].showResult();
            }

            // Assemble result
            ImageJFunctions.show(ip.assemble(IntType.class, 1));

        }
        else {
            // Run Region Competition as usual
            try {
                RCImageFilter();
            }
            catch (final Exception e) {
                if (controllerFrame != null) {
                    controllerFrame.dispose();
                }
                e.printStackTrace();
            }
        }

        final String folder = MosaicUtils.ValidFolderFromImage(MVC.getOriginalImPlus());

        // Remove eventually extension

        if (labelImage == null) {
            return;
        }

        if (output == null) {
            String fileName = MosaicUtils.removeExtension(MVC.getOriginalImPlus().getTitle());
            fileName += "_seg_c1.tif";

            labelImage.save(folder + File.separator + fileName);
        }
        else {
            labelImage.save(output);
        }

        labelImage.calculateRegionsCenterOfMass();

        if (userDialog.showAndSaveStatistics() || test_mode == true) {
            showAndSaveStatistics(algorithm.getLabelMap());
        }
    }

    private final boolean hide_p = false;

    /**
     * Hide get hide processing status
     */

    public boolean getHideProcess() {
        return hide_p;
    }

    /**
     * Initialize the energy function
     */

    private void initEnergies() {
        Energy e_data = null;
        Energy e_length = null;
        Energy e_merge = null;

        final HashMap<Integer, LabelInformation> labelMap = labelImage.getLabelMap();

        final EnergyFunctionalType type = settings.m_EnergyFunctional;

        final Energy e_merge_KL = new E_KLMergingCriterion(labelMap, labelImage.bgLabel, settings.m_RegionMergingThreshold);

        switch (type) {
            case e_PC: {
                e_data = new E_CV(labelMap);
                e_merge = e_merge_KL;
                break;
            }
            case e_PS: {
                e_data = new E_PS(labelImage, intensityImage, labelMap, settings.m_GaussPSEnergyRadius, settings.m_RegionMergingThreshold);
                // e_merge == null
                break;
            }
            case e_DeconvolutionPC: {
                final int dims[] = intensityImage.getDimensions();
                e_data = new E_Deconvolution(intensityImage, labelMap, new ArrayImgFactory<FloatType>(), dims);
                break;
            }
            default: {
                final String s = "Unsupported Energy functional";
                IJ.showMessage(s);
                throw new RuntimeException(s);
            }
        }

        final RegularizationType rType = settings.regularizationType;
        switch (rType) {
            case Sphere_Regularization: {
                final int rad = (int) settings.m_CurvatureMaskRadius;
                e_length = new E_CurvatureFlow(labelImage, rad, cal);
                break;
            }
            case Approximative: {
                e_length = new E_Gamma(labelImage);
                break;
            }
            case None: {
                // e_length = null;
                break;
            }
            default: {
                final String s = "Unsupported Regularization";
                IJ.showMessage(s);
                throw new RuntimeException(s);
            }
        }

        imageModel = new ImageModel(e_data, e_length, e_merge, settings);

    }

    private void initAlgorithm() {
        algorithm = new Algorithm(intensityImage, labelImage, imageModel, settings, this);
    }

    private void initInputImage() {
        ImagePlus ip = null;

        final String file = userDialog.getInputImageFilename();
        final ImagePlus choiceIP = (ImagePlus) userDialog.getInputImage();

        // first try: filepath of inputReader
        if (file != null && !file.isEmpty()) {
            final Opener o = new Opener();
            ip = o.openImage(file);
            if (ip != null) {
                final FileInfo fi = ip.getFileInfo();
                fi.directory = file.substring(0, file.lastIndexOf(File.separator));
                ip.setFileInfo(fi);
            }
        }
        else // selected opened file
        {
            ip = choiceIP;
        }

        // next try: opened image
        if (ip == null) {
            ip = originalIP;
        }

        // debug
        // next try: default image
        if (ip == null) {
            new Opener();
        }

        if (ip != null) {
            originalIP = ip;

            if (normalize_ip) {
                intensityImage = new IntensityImage(originalIP);
            }
            else {
                intensityImage = new IntensityImage(originalIP, false);
            }

            // image loaded
            final boolean showOriginal = true;
            if (showOriginal && userDialog != null) {
                originalIP.show();
            }
        }

        if (ip == null) {
            // failed to load anything
            originalIP = null;
            // TODO maybe show image opener dialog
            IJ.noImage();
            throw new RuntimeException("Failed to load an input image.");
        }

    }

    private void initLabelImage() {
        labelImage = new LabelImageRC(intensityImage.getDimensions());
        InitializationType input;

        if (userDialog != null) {
            input = userDialog.getLabelImageInitType();
        }
        else {
            input = settings.labelImageInitType;
        }

        switch (input) {
            case ROI_2D: {
                System.out.println("manualSelect");
                manualSelect(labelImage);
                break;
            }
            case Rectangle: {
                final BoxInitializer bi = new BoxInitializer(labelImage);
                bi.initRatio(settings.l_BoxRatio);
                break;
            }
            case Bubbles: {
                final BubbleInitializer bi = new BubbleInitializer(labelImage);
                bi.initSizePaddig(settings.m_BubblesRadius, settings.m_BubblesDispl);
                break;
            }
            case LocalMax: {
                final MaximaBubbles mb = new MaximaBubbles(intensityImage, labelImage, settings.l_BubblesRadius, settings.l_Sigma, settings.l_Tolerance, settings.l_RegionTolerance);
                mb.initFloodFilled();
                break;
            }
            case File_Patcher:
            case File: {
                ImagePlus ip = null;

                final String fileName = userDialog.getLabelImageFilename();
                final ImagePlus choiceIP = (ImagePlus) userDialog.getLabelImage();

                // first priority: filename was entered
                if (fileName != null && !fileName.isEmpty()) {
                    final Opener o = new Opener();
                    ip = o.openImage(fileName);
                    if (ip == null) {
                        ip = choiceIP;
                    }
                }
                else // no filename. fileName == null || fileName()
                {
                    ip = choiceIP;
                }

                if (ip != null) {
                    labelImage.initWithIP(ip);
                    labelImage.initBoundary();
                    labelImage.connectedComponents();
                }
                else {
                    labelImage = null;
                    final String msg = "Failed to load LabelImage (" + fileName + ")";
                    IJ.showMessage(msg);
                    throw new RuntimeException(msg);
                }

                break;
            }
            default: {
                // was aborted
                labelImage = null;
                throw new RuntimeException("No valid input option in User Input. Abort");
            }
        }

        if (labelImage == null) {
            throw new RuntimeException("Not able to build a LabelImage.");
        }

        // TODO sts 3D_comment
        // if (labelImage.getDim()==2)
        {
            initialStack = IntConverter.intArrayToStack(labelImage.dataLabel, labelImage.getDimensions());
        }

        saveInitialLabelImage();

        labelImage.initBoundary();
    }

    private void saveInitialLabelImage() {
        // save the initial guess (random/user defined/whatever) to a tiff
        // so we can reuse it for debugging
        final boolean doSaveGuess = false;
        if (doSaveGuess) {
            final FileInfo fi = originalIP.getOriginalFileInfo();
            if (fi != null) {
                final String d = fi.directory;
                final ImagePlus ip = new ImagePlus("", labelImage.getLabelImageProcessor());
                final FileSaver fs = new FileSaver(ip);
                fs.saveAsTiff(d + "initialLabelImage.tiff");
            }
            else {
                System.out.println("image was created using file/new. initial label was not saved");
            }
        }
    }

    private void initStack() {
        if (IJ.isMacro() == true || hide_p == true) {
            return;
        }

        if (userDialog != null) {
            stackKeepFrames = userDialog.showAllFrames();
        }
        else {
            stackKeepFrames = false;
        }

        ImageProcessor labelImageProc;

        final int[] dims = labelImage.getDimensions();
        final int width = dims[0];
        final int height = dims[1];

        labelImageProc = new ShortProcessor(width, height);
        final ImagePlus labelImPlus = new ImagePlus("dummy", labelImageProc);
        stack = labelImPlus.createEmptyStack();

        stackImPlus = new ImagePlus(null, labelImageProc);
        stackImPlus.show();

        // add a windowlistener to

        if (IJ.isMacro() == false) {
            stackImPlus.getWindow().addWindowListener(new StackWindowListener());
        }

        // first stack image without boundary&contours
        for (int i = 1; i <= initialStack.getSize(); i++) {
            final Object pixels = initialStack.getPixels(i);
            final short[] shortData = IntConverter.intToShort((int[]) pixels);
            addSliceToStackAndShow("init", shortData);
        }

        // next stack image is start of algo
        addSlice(labelImage, "init");

        IJ.setMinAndMax(stackImPlus, 0, maxLabel);
        IJ.run(stackImPlus, "3-3-2 RGB", null); // stack has to contain at least
        // 2 slices so this LUT applies
        // to all future slices.
    }

    private void initControls() {
        // no control when is a script

        if (IJ.isMacro() == true) {
            return;
        }

        controllerFrame = new ControllerFrame(this);
        controllerFrame.setVisible(true);

        // Stop the algorithm if controllerframe is closed
        controllerFrame.addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (algorithm != null) {
                    algorithm.stop();
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
                if (algorithm != null) {
                    algorithm.stop();
                }
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }
        });
    }

    private void doRC() {
        initEnergies();
        initAlgorithm();

        initStack();
        initControls();

        int n = 1;
        if (userDialog != null) {
            n = userDialog.getKBest();
        }
        if (n < 1) {
            n = 1;
        }
        final Timer t = new Timer();

        if (userDialog != null && userDialog.getKBest() > 0) {
            final ArrayList<Long> list = new ArrayList<Long>();

            for (int i = 0; i < userDialog.getKBest(); i++) {
                t.tic();
                labelImage.initMembers();
                labelImage.initWithStack(initialStack);
                initEnergies();

                initAlgorithm();
                if (algorithm.GenerateData(image_psf) == false) {
                    return;
                }
                t.toc();

                updateProgress(settings.m_MaxNbIterations, settings.m_MaxNbIterations);
                list.add(t.lastResult());

                if (stackImPlus != null) {
                    IJ.setMinAndMax(stackImPlus, 0, algorithm.getBiggestLabel());
                }

                if (userDialog != null && output == null) {
                    OpenedImages.add(labelImage.show("", algorithm.getBiggestLabel()));
                }
            }

            logger.debug("--- kbest: (set in GenericDialogGui.kbest): " + Arrays.toString(list.toArray()));
            Collections.sort(list);
            logger.debug("--- sorted: " + Arrays.toString(list.toArray()));

        }
        else // no kbest
        {
            algorithm.GenerateData(image_psf);

            updateProgress(settings.m_MaxNbIterations, settings.m_MaxNbIterations);

            if (stackImPlus != null) {
                IJ.setMinAndMax(stackImPlus, 0, algorithm.getBiggestLabel());
            }
            if (userDialog != null) {
                showFinalResult(labelImage);
            }
        }

        if (IJ.isMacro() == false) {
            controllerFrame.dispose();
        }
    }

    private <T extends RealType<T>, E extends IntegerType<E>> void RCImageFilter(Img<T> img, Img<E> lbl) {
        labelImage = new LabelImageRC(lbl);
        intensityImage = new IntensityImage(img);
        initialStack = IntConverter.intArrayToStack(labelImage.dataLabel, labelImage.getDimensions());
        labelImage.initBoundary();

        doRC();
    }

    private void RCImageFilter() {
        initInputImage();
        initLabelImage();

        doRC();
    }

    private void showFinalResult(LabelImageRC li) {
        OpenedImages.add(li.show("", algorithm.getBiggestLabel()));
    }

    public void showStatus(String s) {
        IJ.showStatus(s);
    }

    /**
     * Invoke this method after done an itation
     */
    public void updateProgress(int iteration, int maxIterations) {
        IJ.showProgress(iteration, maxIterations);
    }

    /**
     * Initializes labelImage with ROI <br>
     * If there was no ROI in input image, asks user to draw a roi.
     */
    private void manualSelect(final LabelImageRC labelImg) {
        Roi roi = null;
        roi = originalIP.getRoi();
        if (roi == null) {
            // System.out.println("no ROIs yet. Get from UserInput");
            final ImageCanvas canvas = originalIP.getCanvas();

            // save old keylisteners, remove them (so we can use all keys to
            // select guess ROIs)
            final KeyListener[] kls = canvas.getKeyListeners();
            for (final KeyListener kl : kls) {
                canvas.removeKeyListener(kl);
            }

            final KeyListener keyListener = new KeyListener() {

                @Override
                public void keyTyped(KeyEvent e) {
                    {
                        synchronized (labelImg) {
                            labelImg.notifyAll();
                        }
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                }

                @Override
                public void keyPressed(KeyEvent e) {
                }
            };
            canvas.addKeyListener(keyListener);

            // try to get a ROI from user
            while (roi == null) {
                synchronized (labelImg) {
                    try {
                        System.out.println("Waiting for user input (pressing space");
                        labelImg.wait();
                    }
                    catch (final InterruptedException e) {
                        e.printStackTrace();
                    }

                    roi = originalIP.getRoi();
                    if (roi == null) {
                        IJ.showMessage("No ROI selcted. maybe wrong window");
                    }
                }
            }
            // we have a roi, remove keylistener and reattach the old ones
            canvas.removeKeyListener(keyListener);
            for (final KeyListener kl : kls) {
                canvas.addKeyListener(kl);
            }
        }

        // now we have a roi
        labelImg.getLabelImageProcessor().setValue(1);
        labelImg.getLabelImageProcessor().fill(roi);
        labelImg.initBoundary();
        labelImg.connectedComponents();

    }

    public void addSlice(LabelImageRC labelImage, String title) {
        final int dim = labelImage.getDim();
        if (dim == 2) {
            addSliceToStackAndShow(title, labelImage.getSlice());
        }
        if (dim == 3) {
            addSliceToHyperstack(title, labelImage.get3DShortStack(false));
        }
    }

    @Override
    public void closeAll() {
        if (labelImage != null) {
            labelImage.close();
        }
        if (intensityImage != null) {
            intensityImage.close();
        }
        if (stackImPlus != null) {
            stackImPlus.close();
        }
        if (originalIP != null) {
            originalIP.close();
        }

        for (int i = 0; i < OpenedImages.size(); i++) {
            OpenedImages.get(i).close();
        }
        algorithm.close();
    }

    /**
     * Adds a new slice pixels to the end of the stack, and sets the new stack position to this slice
     *
     * @param title Title of the stack slice
     * @param pixels data of the new slice (pixel array)
     */
    private void addSliceToStackAndShow(String title, Object pixels) {
        if (stack == null) {
            // stack was closed by user, don't reopen
            return;
        }

        if (!stackKeepFrames) {
            stack.deleteLastSlice();
        }

        stack.addSlice(title, pixels);
        stackImPlus.setStack(stack);
        stackImPlus.setPosition(stack.getSize());

        adjustLUT();
    }

    /**
     * Adds slices for 3D images to stack, overwrites old images.
     */
    private void add3DtoStaticStack(String title, ImageStack stackslice) {

        int oldpos = stackImPlus.getCurrentSlice();
        if (oldpos < 1) {
            oldpos = 1;
        }

        while (stack.getSize() > 0) {
            stack.deleteLastSlice();
        }

        final int nnewslices = stackslice.getSize();
        for (int i = 1; i <= nnewslices; i++) {
            stack.addSlice(title + " " + i, stackslice.getPixels(i));
        }

        stackImPlus.setStack(stack);
        stackImPlus.setPosition(oldpos);

        adjustLUT();

    }

    /**
     * Shows 3D segmentation progress in a hyperstack
     */
    private void addSliceToHyperstack(String title, ImageStack stackslice) {
        if (stack == null) {
            // stack was closed by user, dont reopen
            // System.out.println("stack is null");
            return;
        }

        if (!stackKeepFrames) {
            add3DtoStaticStack(title, stackslice);
            return;
        }

        // clean the stack, hyperstack must not contain additional slices
        while (stack.getSize() % stackslice.getSize() != 0) {
            stack.deleteSlice(1);
        }

        // in first iteration, convert to hyperstack
        if (stackImPlus.getNFrames() <= 2) {
            final ImagePlus imp2 = stackImPlus;
            imp2.setOpenAsHyperStack(true);
            new StackWindow(imp2);
        }

        int lastSlice = stackImPlus.getSlice();
        final int lastFrame = stackImPlus.getFrame();
        final boolean wasLastFrame = lastFrame == stackImPlus.getDimensions()[4];

        for (int i = 1; i <= stackslice.getSize(); i++) {
            stack.addSlice(title + i, stackslice.getProcessor(i));
        }

        final int total = stack.getSize();
        final int depth = stackslice.getSize();
        final int timeSlices = total / depth;

        stackImPlus.setDimensions(1, depth, timeSlices);

        // scroll lock on last frame
        int nextFrame = lastFrame;
        if (wasLastFrame) {
            nextFrame++;
        }

        // go to mid in first iteration
        if (timeSlices <= 2) {
            lastSlice = depth / 2;
        }
        try {
            // sometimes here is a ClassCastException
            // when scrolling in the hyperstack
            // it's a IJ problem... catch the Exception, hope it helps
            stackImPlus.setPosition(1, lastSlice, nextFrame);
        }
        catch (final Exception e) {
            System.out.println(e);
        }

        adjustLUT();

    }

    private int maxLabel = 100;

    private void adjustLUT() {
        if (algorithm.getBiggestLabel() > maxLabel) {
            maxLabel *= 2;
        }
        IJ.setMinAndMax(stackImPlus, 0, maxLabel);
        IJ.run(stackImPlus, "3-3-2 RGB", null);
    }

    public LabelImageRC getLabelImage() {
        return this.labelImage;
    }

    public Algorithm getAlgorithm() {
        return this.algorithm;
    }

    public ImagePlus getStackImPlus() {
        return this.stackImPlus;
    }

    /**
     * Get the original imagePlus
     *
     * @return
     */

    public ImagePlus getOriginalImPlus() {
        return this.originalIP;
    }

    /**
     * This {@link WindowListener} sets stack to null if stackwindow was closed by user. This indicates to not further producing stackframes. For Hyperstacks (for which IJ reopens new Window on each update) it hooks to the new Windows.
     */
    private class StackWindowListener implements WindowListener {

        protected StackWindowListener() {
        }

        @Override
        public void windowClosing(WindowEvent e) {
            stack = null;
        }

        @Override
        public void windowClosed(WindowEvent e) {
            // System.out.println("stackimp closed");
            // hook to new window
            final Window win = stackImPlus.getWindow();
            if (win != null) {
                win.addWindowListener(this);
            }
        }

        @Override
        public void windowOpened(WindowEvent e) {}

        @Override
        public void windowIconified(WindowEvent e) {}

        @Override
        public void windowDeiconified(WindowEvent e) {}

        @Override
        public void windowDeactivated(WindowEvent e) {}

        @Override
        public void windowActivated(WindowEvent e) {}
    }

    public Region_Competition() {
        OpenedImages = new Vector<ImagePlus>();
    }

    /**
     * Show and save statistics
     *
     * @param labelMap HashMap that contain the labels information
     */
    private void showAndSaveStatistics(HashMap<Integer, LabelInformation> labelMap) {
        final ResultsTable rts = createStatistics(labelMap);

        final String folder = MosaicUtils.ValidFolderFromImage(MVC.getOriginalImPlus());

        String fileName = MosaicUtils.removeExtension(MVC.getOriginalImPlus().getTitle());
        fileName += "_ObjectsData_c1.csv";
        saveStatistics(folder + File.separator + fileName, labelMap);

        // if is headless do not show
        final boolean headless_check = GraphicsEnvironment.isHeadless();

        if (headless_check == false) {
            rts.show("statistics");
        }
    }

    /**
     * Save the csv region statistics
     *
     * @param fold where to save
     * @param labelMap HashMap that save the label information
     */
    private void saveStatistics(String fold, HashMap<Integer, LabelInformation> labelMap) {
        // Remove the string file:

        if (fold.indexOf("file:") >= 0) {
            fold = fold.substring(fold.indexOf("file:") + 5);
        }

        final ResultsTable rts = createStatistics(labelMap);

        try {
            rts.saveAs(fold);
        }
        catch (final IOException e) {
            e.printStackTrace();
        }

        final String oip = originalIP.getTitle().substring(0, originalIP.getTitle().lastIndexOf("."));

        final boolean headless_check = GraphicsEnvironment.isHeadless();

        if (headless_check == false) {
            MosaicUtils.reorganize(out, oip, fold.substring(0, fold.lastIndexOf(File.separator)), 1);
        }
    }

    private ResultsTable createStatistics(HashMap<Integer, LabelInformation> labelMap) {
        final ResultsTable rt = new ResultsTable();

        // over all labels
        for (final Entry<Integer, LabelInformation> entry : labelMap.entrySet()) {
            final LabelInformation info = entry.getValue();

            rt.incrementCounter();
            rt.addValue("Image_ID", 0);
            rt.addValue("label", info.label);
            rt.addValue("size", info.count);
            rt.addValue("mean", info.mean);
            rt.addValue("variance", info.var);
            rt.addValue("Coord_X", info.mean_pos[0]);
            rt.addValue("Coord_Y", info.mean_pos[1]);
            if (info.mean_pos.length > 2) {
                rt.addValue("Coord_Z", info.mean_pos[2]);
            }
            else {
                rt.addValue("Coord_Z", 0.0);
            }
        }

        return rt;
    }

    /**
     * Get CSV regions list name output
     *
     * @param aImp image
     * @return set of possible output
     */

    @Override
    public String[] getRegionList(ImagePlus aImp) {
        final String[] gM = new String[1];
        gM[0] = new String(aImp.getTitle() + "_ObjectsData_c1.csv");
        return gM;
    }

    @Override
    public String[] getMask(ImagePlus aImp) {
        final String[] gM = new String[1];
        gM[0] = new String(aImp.getTitle() + "_seg_c1.tif");
        return gM;
    }

    @Override
    public String getName() {
        return new String("Region_Competition");
    }

    private boolean test_mode;

    @Override
    public void setIsOnTest(boolean test) {
        test_mode = test;
    }

    @Override
    public boolean isOnTest() {
        return test_mode;
    }
}
