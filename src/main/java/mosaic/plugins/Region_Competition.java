package mosaic.plugins;


import java.awt.GraphicsEnvironment;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.psf.GeneratePSF;
import mosaic.core.utils.MosaicUtils;
import mosaic.region_competition.Algorithm;
import mosaic.region_competition.ClusterModeRC;
import mosaic.region_competition.Settings;
import mosaic.region_competition.GUI.Controller;
import mosaic.region_competition.GUI.GenericDialogGUI;
import mosaic.region_competition.GUI.SegmentationProcessWindow;
import mosaic.region_competition.GUI.StatisticsTable;
import mosaic.region_competition.energies.E_CV;
import mosaic.region_competition.energies.E_CurvatureFlow;
import mosaic.region_competition.energies.E_Deconvolution;
import mosaic.region_competition.energies.E_Gamma;
import mosaic.region_competition.energies.E_KLMergingCriterion;
import mosaic.region_competition.energies.E_PS;
import mosaic.region_competition.energies.Energy.ExternalEnergy;
import mosaic.region_competition.energies.Energy.InternalEnergy;
import mosaic.region_competition.energies.ImageModel;
import mosaic.region_competition.initializers.BoxInitializer;
import mosaic.region_competition.initializers.BubbleInitializer;
import mosaic.region_competition.initializers.MaximaBubbles;
import mosaic.utils.Debug;
import mosaic.utils.ImgUtils;
import mosaic.utils.SysOps;
import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.JsonDataFile;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;


/**
 * Region Competion plugin implementation
 * 
 * @author Stephan Semmler, ETH Zurich
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class Region_Competition implements PlugInFilter {
    private static final Logger logger = Logger.getLogger(Region_Competition.class);

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
    private final String[] outputFileNamesSuffixes = { "*_ObjectsData_c1.csv", "*_seg_c1.tif" };

    // Settings
    private Settings settings = null;
    private String outputSegmentedImageLabelFilename = null;
    private boolean normalize_ip = true;
    private boolean showGUI = true;
    private boolean useCluster = false;
    
    // Get some more settings and images from dialog
    private boolean showAndSaveStatistics; 
    private boolean showAllFrames;

    // Images to be processed
    private Calibration inputImageCalibration;
    private ImagePlus originalInputImage;
    private ImagePlus inputImageChosenByUser;
    private ImagePlus inputLabelImageChosenByUser; 
    
    // Algorithm and its input stuff
    protected Algorithm algorithm;
    private LabelImage labelImage;
    private IntensityImage intensityImage;
    private ImageModel imageModel;
    
    // User interfaces
    private SegmentationProcessWindow stackProcess;
    private GenericDialogGUI userDialog;
    
    
    @Override
    public int setup(String aArgs, ImagePlus aImp) {
        // Save input stuff
        originalInputImage = aImp;

        // Read settings and macro options
        initSettingsAndParseMacroOptions();
    
        // Get information from user
        userDialog = new GenericDialogGUI(settings, originalInputImage);
        userDialog.showDialog();
        if (!userDialog.configurationValid()) {
            return DONE;
        }
        
        // Get some more settings and images
        showGUI = !(IJ.isMacro() || Interpreter.batchMode);
        showAndSaveStatistics = userDialog.showAndSaveStatistics();
        showAllFrames = userDialog.showAllFrames();
        inputLabelImageChosenByUser = userDialog.getInputLabelImage();
        inputImageChosenByUser = userDialog.getInputImage();
        if (inputImageChosenByUser != null) inputImageCalibration = inputImageChosenByUser.getCalibration();
        useCluster = userDialog.useCluster();
        
        logger.info("Input image [" + (inputImageChosenByUser != null ? inputImageChosenByUser.getTitle() : "<no file>") + "]");
        logger.info("Label image [" + (inputLabelImageChosenByUser != null ? inputLabelImageChosenByUser.getTitle() : "<no file>") + "]");
        logger.info("showAndSaveStatistics: " + showAndSaveStatistics + 
                    ", showAllFrames: " + showAllFrames + 
                    ", useCluster: " + useCluster +
                    ", showGui: " + showGUI);
        logger.debug("Settings:\n" + Debug.getJsonString(settings));
        
        // Save new settings from user input.
        getConfigHandler().SaveToFile(configFilePath(), settings);
    
        // If there were no input image when plugin was started then return NO_IMAGE_REQUIRED 
        // since user must have chosen image in dialog - they will be processed later.
        if (inputImageChosenByUser != null && originalInputImage == null) {
            return NO_IMAGE_REQUIRED;
        } 
        return DOES_ALL + NO_CHANGES;
    }

    @Override
    public void run(ImageProcessor aImageP) {
        // ================= Run segmentation ==============================
        //
        if (useCluster == true) {
            ClusterModeRC.runClusterMode(inputImageChosenByUser, 
                                         inputLabelImageChosenByUser,
                                         settings, 
                                         outputFileNamesSuffixes);
            
            // Finish - nothing to do more here...
            return;
        }
        runRegionCompetion();
        
        // ================= Save segmented image and statistics ==========
        //
        String absoluteFileNameNoExt= MosaicUtils.getAbsolutFileName(inputImageChosenByUser, /* remove ext */ true);
        
        saveSegmentedImage(absoluteFileNameNoExt);
        saveStatistics(absoluteFileNameNoExt);
    }
    
    /**
     * Returns handler for (un)serializing Settings objects.
     */
    public static DataFile<Settings> getConfigHandler() {
        return new JsonDataFile<Settings>();
    }

    private void saveStatistics(String absoluteFileNameNoExt) {
        if (showAndSaveStatistics) {
            if (absoluteFileNameNoExt == null) {
                logger.error("Cannot save segmentation statistics. Filename for saving not available!");
                return;
            }
            String absoluteFileName = absoluteFileNameNoExt + outputFileNamesSuffixes[0].replace("*", "");
    
            algorithm.calculateRegionsCenterOfMass();
            StatisticsTable statisticsTable = new StatisticsTable(algorithm.getLabelStatistics().values());
            logger.info("Saving segmentation statistics [" + absoluteFileName + "]");
            statisticsTable.save(absoluteFileName);
            if (showGUI) {
                statisticsTable.show("statistics");
            }
    
            final boolean headless_check = GraphicsEnvironment.isHeadless();
            if (headless_check == false) {
                final String directory = ImgUtils.getImageDirectory(inputImageChosenByUser);
                final String fileNameNoExt = SysOps.removeExtension(inputImageChosenByUser.getTitle());
                MosaicUtils.reorganize(outputFileNamesSuffixes, fileNameNoExt, directory, 1);
            }
        }
    }

    private void saveSegmentedImage(String absoluteFileNameNoExt) {
        if (outputSegmentedImageLabelFilename == null && absoluteFileNameNoExt != null) {
                outputSegmentedImageLabelFilename = absoluteFileNameNoExt + outputFileNamesSuffixes[1].replace("*", "");
        }
        if (outputSegmentedImageLabelFilename != null) { 
            logger.info("Saving segmented image [" + outputSegmentedImageLabelFilename + "]");
            labelImage.save(outputSegmentedImageLabelFilename);
        }
        else {
            logger.error("Cannot save segmentation result. Filename for saving not available!");
        }
    }

    private void initSettingsAndParseMacroOptions() {
        settings = null;
        
        final String options = Macro.getOptions();
        logger.info("Macro Options: [" + options + "]");
        if (options != null) {
            // Command line interface
            
            String normalizeString = MosaicUtils.parseString("normalize", options);
            if (normalizeString != null) {
                normalize_ip = Boolean.parseBoolean(normalizeString);
            }

            String path = MosaicUtils.parseString("config", options);
            if (path != null) {
                settings = getConfigHandler().LoadFromFile(path, Settings.class);
            }

            outputSegmentedImageLabelFilename = MosaicUtils.parseString("output", options);
        }

        if (settings == null) {
            // load default config file
            configFilePath();
            settings = getConfigHandler().LoadFromFile(configFilePath(), Settings.class, new Settings());
        }
    }

    private String configFilePath() {
        return IJ.getDirectory("temp") + "rc_settings.dat";
    }

    /**
     * Initialize the energy function
     */
    private void initEnergies() {
        ExternalEnergy e_data;
        ExternalEnergy e_merge = null;
        switch (settings.m_EnergyFunctional) {
            case e_PC: {
                e_data = new E_CV();
                e_merge = new E_KLMergingCriterion(LabelImage.BGLabel, settings.m_RegionMergingThreshold);
                break;
            }
            case e_PS: {
                e_data = new E_PS(labelImage, intensityImage, settings.m_GaussPSEnergyRadius, settings.m_RegionMergingThreshold);
                break;
            }
            case e_DeconvolutionPC: {
                final GeneratePSF gPsf = new GeneratePSF();
                Img<FloatType> image_psf = gPsf.generate(inputImageChosenByUser.getNSlices() == 1 ? 2 : 3);
                
                // Normalize PSF to overall sum equal 1.0
                final double Vol = MosaicUtils.volume_image(image_psf);
                MosaicUtils.rescale_image(image_psf, (float) (1.0f / Vol));

                e_data = new E_Deconvolution(intensityImage, image_psf);
                break;
            }
            default: {
                final String s = "Unsupported Energy functional";
                IJ.showMessage(s);
                throw new RuntimeException(s);
            }
        }

        InternalEnergy e_length;
        switch (settings.regularizationType) {
            case Sphere_Regularization: {
                e_length = new E_CurvatureFlow(labelImage, (int)settings.m_CurvatureMaskRadius, inputImageCalibration);
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
        // We should have a image or...
        if (inputImageChosenByUser != null) {
            intensityImage = new IntensityImage(inputImageChosenByUser, normalize_ip);
            inputImageChosenByUser.show();
        }
        else {
            // ... we have failed to load anything
            IJ.noImage();
            throw new RuntimeException("Failed to load an input image.");
        }
    }

    private void initLabelImage() {
        labelImage = new LabelImage(intensityImage.getDimensions());

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
                if (inputLabelImageChosenByUser != null) {
                    labelImage.initWithImg(inputLabelImageChosenByUser);
                    labelImage.initBoundary();
                    labelImage.connectedComponents();
                }
                else {
                    final String msg = "No valid label image given.";
                    IJ.showMessage(msg);
                    throw new RuntimeException(msg);
                }

                break;
            }
            default: {
                // was aborted
                throw new RuntimeException("No valid input option in User Input. Abort");
            }
        }
    }

    private void initStack() {
        final int[] dims = labelImage.getDimensions();
        final int width = dims[0];
        final int height = dims[1];
        
        ImageStack initialStack = labelImage.getShortStack(true); 
        stackProcess = new SegmentationProcessWindow(width, height, showAllFrames);
      
        // first stack image without boundary&contours
        for (int i = 1; i <= initialStack.getSize(); i++) {
            stackProcess.addSliceToStackAndShow("init without countours", (short[])initialStack.getPixels(i));
            initialStack.getProcessor(i);
        }
        
        // Generate contours and add second image to stack
        labelImage.initBoundary();
        stackProcess.addSliceToStack(labelImage, "init with contours", 0);
    }

    private void runRegionCompetion() {
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
            isDone = algorithm.performIteration();
            
            // Check if we should pause for a moment or if simulation is not aborted by user
            // If aborted pretend that we have finished segmentation (isDone=true)
            isDone = iController.hasAborted() ? true : isDone;

            // Add slice with iteration output
            stackProcess.addSliceToStack(labelImage, "iteration " + iteration, algorithm.getBiggestLabel());
        }
        IJ.showProgress(settings.m_MaxNbIterations, settings.m_MaxNbIterations);

        // Do some post process stuff
        stackProcess.addSliceToStack(labelImage, "final image iteration " + iteration, algorithm.getBiggestLabel());
        labelImage.show("", algorithm.getBiggestLabel());
        
        iController.close();
    }

    /**
     * Initializes labelImage with ROI <br>
     */
    private void initializeRoi(final LabelImage labelImg) {
        labelImg.initLabelsWithRoi(inputImageChosenByUser.getRoi());
        labelImg.initBoundary();
        labelImg.connectedComponents();
    }
}
