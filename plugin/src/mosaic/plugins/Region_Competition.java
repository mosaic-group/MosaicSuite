package mosaic.plugins;


import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.HashMap;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.io.Opener;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import mosaic.core.psf.GeneratePSF;
import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.MosaicUtils;
import mosaic.plugins.utils.PlugInFilterExt;
import mosaic.region_competition.Algorithm;
import mosaic.region_competition.ClusterModeRC;
import mosaic.region_competition.LabelImageRC;
import mosaic.region_competition.LabelInformation;
import mosaic.region_competition.Settings;
import mosaic.region_competition.GUI.Controller;
import mosaic.region_competition.GUI.GenericDialogGUI;
import mosaic.region_competition.GUI.StatisticsTable;
import mosaic.region_competition.energies.E_CV;
import mosaic.region_competition.energies.E_CurvatureFlow;
import mosaic.region_competition.energies.E_Deconvolution;
import mosaic.region_competition.energies.E_Gamma;
import mosaic.region_competition.energies.E_KLMergingCriterion;
import mosaic.region_competition.energies.E_PS;
import mosaic.region_competition.energies.Energy;
import mosaic.region_competition.energies.ImageModel;
import mosaic.region_competition.initializers.BoxInitializer;
import mosaic.region_competition.initializers.BubbleInitializer;
import mosaic.region_competition.initializers.MaximaBubbles;
import mosaic.region_competition.utils.IntConverter;
import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.JsonDataFile;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;


/**
 * @author Stephan Semmler, ETH Zurich
 * @version 2012.06.11
 */
public class Region_Competition implements PlugInFilterExt {

    public enum InitializationType {
        Rectangle, Bubbles, LocalMax, ROI_2D, File
    }

    public enum EnergyFunctionalType {
        e_PC, e_PS, e_DeconvolutionPC
    }

    public enum RegularizationType {
        Sphere_Regularization, Approximative, None,
    }

    // Output file names
    // TODO: It is not nice way of defining output files and should be refactored.
    private final String[] out = { "*_ObjectsData_c1.csv", "*_seg_c1.tif" };

    // Settings
    private Settings settings = null;
    private String output = null;
    private boolean normalize_ip = true;
    private boolean showGUI = true;
    
    // Algorithm and its input stuff
    protected Algorithm algorithm;
    private LabelImageRC labelImage;
    private IntensityImage intensityImage;
    private ImageModel imageModel;
    
    // User interface and images
    private Calibration cal;
    private ImagePlus originalIP; // IP of the input image
    private SegmentationProcessWindow stackProcess;
    private GenericDialogGUI userDialog;
    
    @Override
    public int setup(String aArgs, ImagePlus aImp) {
        showGUI = !(IJ.isMacro() || Interpreter.batchMode);
        
        initSettingsAndParseMacroOptions();
    
        // Save input stuff
        originalIP = aImp;
    
        // Get information from user
        userDialog = new GenericDialogGUI(settings, originalIP);
        userDialog.showDialog();
        if (!userDialog.processInput()) {
            return DONE;
        }
        
        // Save new settings from user input.
        getConfigHandler().SaveToFile(configFilePath(), settings);
        
        if (userDialog.getInputImage() != null) {
            originalIP = userDialog.getInputImage();
            cal = originalIP.getCalibration();
        } 
    
        if (userDialog.useCluster() == true) {
            ClusterModeRC.runClusterMode(aImp, 
                                         settings, 
                                         out, 
                                         userDialog.getLabelImageFilename(), 
                                         userDialog.getInputImageFilename(), 
                                         userDialog.getLabelImage());
            return NO_IMAGE_REQUIRED;
        }
        else {
            if (aImp != null) {
                // if is 3D save the originalIP
                if (aImp.getNSlices() != 1) {
                    originalIP = aImp;
                }
            }
            else {
                originalIP = null;
                return NO_IMAGE_REQUIRED;
            }
        }
    
        return DOES_ALL + NO_CHANGES;
    }

    @Override
    public void run(ImageProcessor aImageP) {
        doRC();
    
        if (output == null) {
            final String folder = MosaicUtils.ValidFolderFromImage(originalIP);
            String fileName = MosaicUtils.removeExtension(originalIP.getTitle());
            fileName += "_seg_c1.tif";
    
            output = folder + File.separator + fileName;
        }
        labelImage.save(output);
    
        labelImage.calculateRegionsCenterOfMass();
    
        if (userDialog.showAndSaveStatistics() || test_mode == true) {
            // TODO: Handle images that are created and not saved yet. There have no directory information and
            // below we receive null
            String directory = MosaicUtils.ValidFolderFromImage(originalIP);
            final String fileNameNoExt = MosaicUtils.removeExtension(originalIP.getTitle());
            String absoluteFileName = directory + File.separator + fileNameNoExt + "_ObjectsData_c1.csv";
    
            // TODO: Is this needed? Can it be case when file is drag and dropped?
            if (absoluteFileName.indexOf("file:") >= 0) {
                absoluteFileName = absoluteFileName.substring(absoluteFileName.indexOf("file:") + 5);
            }
    
            StatisticsTable statisticsTable = new StatisticsTable(algorithm.getLabelMap().values());
            statisticsTable.save(absoluteFileName);
            if (showGUI) statisticsTable.show("statistics");
    
            // TODO: if is headless reorganize (why?) Is this leftover of cluster mode?
            final boolean headless_check = GraphicsEnvironment.isHeadless();
            if (headless_check == false) {
                MosaicUtils.reorganize(out, fileNameNoExt, directory, 1);
            }
        }
    }

    /**
     * Returns handler for (un)serializing Settings objects.
     */
    public static DataFile<Settings> getConfigHandler() {
        return new JsonDataFile<Settings>();
    }

    private void initSettingsAndParseMacroOptions() {
        settings = null;
        
        final String options = Macro.getOptions();
        if (options != null) {
            // Command line interface

            // normalize
            String normalizeString = MosaicUtils.parseString("normalize", options);
            if (normalizeString != null) {
                normalize_ip = Boolean.parseBoolean(normalizeString);
            }

            // config file
            String path = MosaicUtils.parseString("config", options);
            if (path != null) {
                settings = getConfigHandler().LoadFromFile(path, Settings.class);
            }

            // output file
            output = MosaicUtils.parseString("output", options);
        }

        if (settings == null) {
            // load default config file
            configFilePath();
            settings = getConfigHandler().LoadFromFile(configFilePath(), Settings.class, new Settings());
        }
    }

    private String configFilePath() {
        final String dir = IJ.getDirectory("temp");
        return dir + "rc_settings.dat";
    }

    /**
     * Initialize the energy function
     */
    private void initEnergies() {
        final HashMap<Integer, LabelInformation> labelMap = labelImage.getLabelMap();
        final Energy e_merge_KL = new E_KLMergingCriterion(labelMap, labelImage.bgLabel, settings.m_RegionMergingThreshold);

        Energy e_data = null;
        Energy e_merge;
        switch (settings.m_EnergyFunctional) {
            case e_PC: {
                e_data = new E_CV(labelMap);
                e_merge = e_merge_KL;
                break;
            }
            case e_PS: {
                e_data = new E_PS(labelImage, intensityImage, labelMap, settings.m_GaussPSEnergyRadius, settings.m_RegionMergingThreshold);
                e_merge = null;
                break;
            }
            case e_DeconvolutionPC: {
                final GeneratePSF gPsf = new GeneratePSF();
                Img<FloatType> image_psf = null;
                if (originalIP.getNSlices() == 1) {
                    image_psf = gPsf.generate(2);
                }
                else {
                    image_psf = gPsf.generate(3);
                }
                // Normalize PSF to overall sum equal 1.0
                final double Vol = IntensityImage.volume_image(image_psf);
                IntensityImage.rescale_image(image_psf, (float) (1.0f / Vol));

                e_data = new E_Deconvolution(intensityImage, labelMap, image_psf);
                e_merge = null;
                break;
            }
            default: {
                final String s = "Unsupported Energy functional";
                IJ.showMessage(s);
                throw new RuntimeException(s);
            }
        }

        Energy e_length;
        switch (settings.regularizationType) {
            case Sphere_Regularization: {
                e_length = new E_CurvatureFlow(labelImage, (int)settings.m_CurvatureMaskRadius, cal);
                break;
            }
            case Approximative: {
                e_length = new E_Gamma(labelImage);
                break;
            }
            case None: {
                e_length = null;
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

    private void initInputImage() {
        final String file = userDialog.getInputImageFilename();
        final ImagePlus choiceIP = userDialog.getInputImage();

        // first try: filepath of inputReader
        ImagePlus ip = null;
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
            intensityImage = new IntensityImage(originalIP, normalize_ip);

            // image loaded
            final boolean showOriginal = true;
            if (showOriginal && userDialog != null) {
                originalIP.show();
            }
        }

        if (ip == null) {
            // failed to load anything
            originalIP = null;
            IJ.noImage();
            throw new RuntimeException("Failed to load an input image.");
        }
    }

    private void initLabelImage() {
        labelImage = new LabelImageRC(intensityImage.getDimensions());

        InitializationType input = settings.labelImageInitType;

        switch (input) {
            case ROI_2D: {
                initializeRoi(labelImage);
                break;
            }
            case Rectangle: {
                final BoxInitializer bi = new BoxInitializer(labelImage);
                bi.initialize(settings.l_BoxRatio);
                break;
            }
            case Bubbles: {
                final BubbleInitializer bi = new BubbleInitializer(labelImage);
                bi.initialize(settings.m_BubblesRadius, settings.m_BubblesDispl);
                break;
            }
            case LocalMax: {
                final MaximaBubbles mb = new MaximaBubbles(intensityImage, labelImage, settings.l_Sigma, settings.l_Tolerance, settings.l_BubblesRadius, settings.l_RegionTolerance);
                mb.initialize();
                break;
            }
            case File: {
                final String fileName = userDialog.getLabelImageFilename();
                final ImagePlus choiceIP = userDialog.getLabelImage();

                // first priority: filename was entered
                ImagePlus ip = null;
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
    }

    private void initStack() {
        boolean stackKeepFrames = false;
        if (userDialog != null) {
            stackKeepFrames = userDialog.showAllFrames();
        }

        final int[] dims = labelImage.getDimensions();
        final int width = dims[0];
        final int height = dims[1];
        
        ImageStack initialStack = IntConverter.intArrayToStack(labelImage.dataLabel, labelImage.getDimensions());
        stackProcess = new SegmentationProcessWindow(width, height, stackKeepFrames);
        
        // first stack image without boundary&contours
        for (int i = 1; i <= initialStack.getSize(); i++) {
            final Object pixels = initialStack.getPixels(i);
            final short[] shortData = IntConverter.intToShort((int[]) pixels);
            stackProcess.addSliceToStackAndShow("init without countours", shortData);
            initialStack.getProcessor(i);
        }

        // Generate contours and add second image to stack
        labelImage.initBoundary();
        stackProcess.addSliceToStack(labelImage, "init with contours", 0);
    }

    private void doRC() {
        initInputImage();
        initLabelImage();
        initEnergies();
        initStack();
        
        Controller iController = new Controller(/* aShowWindow */ showGUI);

        // Run segmentation
        algorithm = new Algorithm(intensityImage, labelImage, imageModel, settings);
        
        boolean isDone = false;
        int iteration = 0;
        while (iteration < settings.m_MaxNbIterations && !isDone) {
            // Perform one iteration of RC
            ++iteration;
            IJ.showStatus("Iteration: " + iteration + "/" + settings.m_MaxNbIterations);
            IJ.showProgress(iteration, settings.m_MaxNbIterations);
            isDone = algorithm.GenerateData();
            
            // Check if we should pause for a moment or if simulation is not aborted by user
            // If aborted pretend that we have finished segmentation (isDone=true)
            isDone = iController.hasAborted() ? true : isDone;

            // Add slice with iteration output
            stackProcess.addSliceToStack(labelImage, "iteration " + iteration, algorithm.getBiggestLabel());
        }
        IJ.showProgress(settings.m_MaxNbIterations, settings.m_MaxNbIterations);

        // Do some post process stuff
        stackProcess.addSliceToStack(labelImage, "final image iteration " + iteration, algorithm.getBiggestLabel());
       
        if (userDialog != null) {
            OpenedImages.add(labelImage.show("", algorithm.getBiggestLabel()));
        }
        
        iController.close();
    }

    /**
     * Initializes labelImage with ROI <br>
     * If there was no ROI in input image, asks user to draw a roi.
     */
    private void initializeRoi(final LabelImageRC labelImg) {
        Roi roi = originalIP.getRoi();
        if (roi == null) {
            roi = getRoiFromUser(labelImg);
        }

        // now we have a roi
        labelImg.getLabelImageProcessor().setValue(1);
        labelImg.getLabelImageProcessor().fill(roi);
        labelImg.initBoundary();
        labelImg.connectedComponents();
    }

    private Roi getRoiFromUser(final LabelImageRC labelImg) {
        final ImageCanvas canvas = originalIP.getCanvas();
       
        // save old keylisteners, remove them (so we can use all keys to select guess ROIs)
        final KeyListener[] kls = canvas.getKeyListeners();
        for (final KeyListener kl : kls) {
            canvas.removeKeyListener(kl);
        }

        final KeyListener keyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                synchronized (labelImg) {
                    labelImg.notifyAll();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {}
        };
        canvas.addKeyListener(keyListener);

        Roi roi = null;
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
        
        return roi;
    }

    // Current test interface methods and variables - will vanish in future
    private boolean test_mode;
    private final Vector<ImagePlus> OpenedImages = new Vector<ImagePlus>();
    
    @Override
    public void closeAll() {
        if (labelImage != null) {
            labelImage.close();
        }
        if (intensityImage != null) {
            intensityImage.close();
        }
        if (stackProcess != null) {
            stackProcess.close();
        }
        if (originalIP != null) {
            originalIP.close();
        }
        for (int i = 0; i < OpenedImages.size(); i++) {
            OpenedImages.get(i).close();
        }
    }

    @Override
    public void setIsOnTest(boolean test) {
        test_mode = test;
    }

    @Override
    public boolean isOnTest() {
        return test_mode;
    }
}
