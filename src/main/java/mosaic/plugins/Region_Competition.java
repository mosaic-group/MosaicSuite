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
import mosaic.region_competition.Settings;
import mosaic.region_competition.Settings.SegmentationType;
import mosaic.region_competition.DRS.AlgorithmDRS;
import mosaic.region_competition.DRS.SettingsDRS;
import mosaic.region_competition.GUI.Controller;
import mosaic.region_competition.GUI.GUI;
import mosaic.region_competition.GUI.SegmentationProcessWindow;
import mosaic.region_competition.GUI.StatisticsTable;
import mosaic.region_competition.RC.AlgorithmRC;
import mosaic.region_competition.RC.ClusterModeRC;
import mosaic.region_competition.RC.SettingsRC;
import mosaic.region_competition.energies.E_CV;
import mosaic.region_competition.energies.E_CurvatureFlow;
import mosaic.region_competition.energies.E_Deconvolution;
import mosaic.region_competition.energies.E_Gamma;
import mosaic.region_competition.energies.E_KLMergingCriterion;
import mosaic.region_competition.energies.E_PC_Gauss;
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
        e_PC, e_PS, e_DeconvolutionPC, e_PC_Gauss
    }

    public enum RegularizationType {
        Sphere_Regularization, Approximative, None,
    }
    
    // Output file names
    private final String[] outputFileNamesSuffixes = { "*_ObjectsData_c1.csv", "*_seg_c1.tif", "*_prob_c1.tif" };

    // Settings
    private Settings iSettings = null;
    private String outputSegmentedImageLabelFilename = null;
    private boolean normalize_ip = true;
    private boolean showGUI = true;
    private boolean useCluster = false;
    private SegmentationType segmentationType = SegmentationType.RC;
    
    // Get some more settings and images from dialog
    private boolean showAndSaveStatistics; 
    private boolean showAllFrames;

    // Images to be processed
    private Calibration inputImageCalibration;
    private ImagePlus originalInputImage;
    private ImagePlus inputImageChosenByUser;
    private ImagePlus inputLabelImageChosenByUser;
    private int iPadSize = 1;
    
    // Algorithm and its input stuff
    private LabelImage labelImage;
    private IntensityImage intensityImage;
    private ImageModel imageModel;
    
    // User interfaces
    private SegmentationProcessWindow stackProcess;
    private GUI userDialog;
    
    
    @Override
    public int setup(String aArgs, ImagePlus aImp) {
        // Save input stuff
        originalInputImage = aImp;

        // Read settings and macro options
        initSettingsAndParseMacroOptions();
    
        // Get information from user
        userDialog = new GUI(iSettings, originalInputImage);
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
        segmentationType = userDialog.getSegmentationType();
        normalize_ip = userDialog.getNormalize();
        
        logger.info("Input image [" + (inputImageChosenByUser != null ? inputImageChosenByUser.getTitle() : "<no file>") + "]");
        if (inputImageChosenByUser != null) logger.info(ImgUtils.getImageInfo(inputImageChosenByUser));
        logger.info("Label image [" + (inputLabelImageChosenByUser != null ? inputLabelImageChosenByUser.getTitle() : "<no file>") + "]");
        logger.info("showAndSaveStatistics: " + showAndSaveStatistics + 
                    ", showAllFrames: " + showAllFrames + 
                    ", useCluster: " + useCluster +
                    ", segmentationType: " + segmentationType +
                    ", showGui: " + showGUI +
                    ", normalize: " + normalize_ip);
        logger.debug("Settings:\n" + Debug.getJsonString(iSettings));
        
        // Save new settings from user input.
        getConfigHandler().SaveToFile(configFilePath(), iSettings);
    
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
                                         iSettings, 
                                         outputFileNamesSuffixes);
            
            // Finish - nothing to do more here...
            return;
        }
        
        switch (segmentationType) {
            case RC:
                runRegionCompetion();
                break;
            case DRS:
                runDiscreteRegionSampling();
                break;
            default:
                new RuntimeException("Uknown SegmentationType: [" + segmentationType + "]");
                break;
        }
        
        // ================= Save segmented image =========================
        //
        saveSegmentedImage();
        
        final boolean headless_check = GraphicsEnvironment.isHeadless();
        if (headless_check == false) {
            final String directory = ImgUtils.getImageDirectory(inputImageChosenByUser);
            final String fileNameNoExt = SysOps.removeExtension(inputImageChosenByUser.getTitle());
            MosaicUtils.reorganize(outputFileNamesSuffixes, fileNameNoExt, directory, 1);
        }
    }
    
    /**
     * Returns handler for (un)serializing Settings objects.
     */
    public static DataFile<Settings> getConfigHandler() {
        return new JsonDataFile<Settings>();
    }

    private void saveStatistics(AlgorithmRC algorithm) {
        if (showAndSaveStatistics) {
            String absoluteFileNameNoExt= ImgUtils.getImageAbsolutePath(inputImageChosenByUser, true);
            if (absoluteFileNameNoExt == null) {
                logger.error("Cannot save segmentation statistics. Filename for saving not available!");
                return;
            }
            String absoluteFileName = absoluteFileNameNoExt + outputFileNamesSuffixes[0].replace("*", "");
    
            algorithm.calculateRegionsCenterOfMass();
            StatisticsTable statisticsTable = new StatisticsTable(algorithm.getLabelStatistics().values(), iPadSize);
            logger.info("Saving segmentation statistics [" + absoluteFileName + "]");
            statisticsTable.save(absoluteFileName);
            if (showGUI) {
                statisticsTable.show("statistics");
            }
        }
    }

    private void saveSegmentedImage() {
        String absoluteFileNameNoExt= ImgUtils.getImageAbsolutePath(inputImageChosenByUser, true);
        String outputProbabilityImgFileNameNoExt = null;
        logger.debug("Absolute file name with dir: " + absoluteFileNameNoExt);
        if (outputSegmentedImageLabelFilename == null && absoluteFileNameNoExt != null) {
                outputSegmentedImageLabelFilename = absoluteFileNameNoExt + outputFileNamesSuffixes[1].replace("*", "");
                outputProbabilityImgFileNameNoExt = absoluteFileNameNoExt + outputFileNamesSuffixes[2].replace("*", "");
        }
        if (outputSegmentedImageLabelFilename != null) { 
            logger.info("Saving segmented image [" + outputSegmentedImageLabelFilename + "]");
            ImagePlus outImg = labelImage.convertToImg("ResultWindow");
            outImg.setStack(ImgUtils.crop(outImg.getStack(), iPadSize, labelImage.getNumOfDimensions() > 2));
            IJ.save(outImg, outputSegmentedImageLabelFilename);
            
            //TODO: save probability image
            if (segmentationType == SegmentationType.DRS && outputProbabilityImgFileNameNoExt != null) {
                ImagePlus outImgStack = stackProcess.getImage();
                outImgStack.setStack(ImgUtils.crop(outImgStack.getStack(), iPadSize, labelImage.getNumOfDimensions() > 2));
                logger.debug("Probability file name with dir: " + outputProbabilityImgFileNameNoExt);
                IJ.save(stackProcess.getImage(), outputProbabilityImgFileNameNoExt);
            }
        }
        else {
            logger.error("Cannot save segmentation result. Filename for saving not available!");
        }
    }

    private void initSettingsAndParseMacroOptions() {
        iSettings = null;
        
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
                iSettings = getConfigHandler().LoadFromFile(path, Settings.class);
            }

            outputSegmentedImageLabelFilename = MosaicUtils.parseString("output", options);
        }

        if (iSettings == null) {
            // load default config file
            iSettings = getConfigHandler().LoadFromFile(configFilePath(), Settings.class, new Settings());
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
        switch (iSettings.m_EnergyFunctional) {
            case e_PC: {
                e_data = new E_CV();
                e_merge = new E_KLMergingCriterion(LabelImage.BGLabel, iSettings.m_RegionMergingThreshold);
                break;
            }
            case e_PS: {
                e_data = new E_PS(labelImage, intensityImage, iSettings.m_GaussPSEnergyRadius, iSettings.m_BalloonForceCoeff, iSettings.m_RegionMergingThreshold);
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
            case e_PC_Gauss: {
                e_data = new E_PC_Gauss();
                e_merge = new E_KLMergingCriterion(LabelImage.BGLabel, iSettings.m_RegionMergingThreshold);
                break;
            }
            default: {
                final String s = "Unsupported Energy functional";
                IJ.showMessage(s);
                throw new RuntimeException(s);
            }
        }

        InternalEnergy e_length;
        switch (iSettings.regularizationType) {
            case Sphere_Regularization: {
                e_length = new E_CurvatureFlow(labelImage, (int)iSettings.m_CurvatureMaskRadius, inputImageCalibration);
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

        imageModel = new ImageModel(e_data, e_length, e_merge, iSettings.m_EnergyContourLengthCoeff);
    }

    private void initInputImage() {
        // We should have a image or...
        if (inputImageChosenByUser != null) {
            int c = inputImageChosenByUser.getNChannels();
            int f = inputImageChosenByUser.getNFrames();
            if (c != 1 || f != 1) {
                String s = "Region Competition is not able to segment correctly multichannel or multiframe images.\n" +
                        "Current input file info: number of channels=" + c +
                        "number of frames=" + f + "\nPlease use as a input only 2D or 3D single image.";
                IJ.showMessage(s);
                throw new RuntimeException(s);
            }
            ImagePlus workImg = inputImageChosenByUser;
//            if (segmentationType == SegmentationType.RC) {
                ImageStack padedIs = ImgUtils.pad(inputImageChosenByUser.getStack(), iPadSize, inputImageChosenByUser.getNDimensions() > 2);
                workImg = inputImageChosenByUser.duplicate();
                workImg.setStack(padedIs);
//            }
            intensityImage = new IntensityImage(workImg, normalize_ip);
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

        InitializationType input = iSettings.labelImageInitType;

        switch (input) {
            case ROI_2D: {
                initializeRoi(labelImage);
                break;
            }
            case Rectangle: {
                final BoxInitializer bi = new BoxInitializer(labelImage);
                bi.initialize(iSettings.l_BoxRatio);
                break;
            }
            case Bubbles: {
                final BubbleInitializer bi = new BubbleInitializer(labelImage);
                bi.initialize(iSettings.m_BubblesRadius, iSettings.m_BubblesDispl);
                break;
            }
            case LocalMax: {
                final MaximaBubbles mb = new MaximaBubbles(intensityImage, labelImage, iSettings.l_Sigma, iSettings.l_Tolerance, iSettings.l_BubblesRadius, iSettings.l_RegionTolerance);
                mb.initialize();
                break;
            }
            case File: {
                if (inputLabelImageChosenByUser != null) {
                    ImagePlus labelImg = inputLabelImageChosenByUser;
//                    if (segmentationType == SegmentationType.RC) {
                        ImageStack padedIs = ImgUtils.pad(inputLabelImageChosenByUser.getStack(), iPadSize, inputLabelImageChosenByUser.getNDimensions() > 2);
                        labelImg = inputLabelImageChosenByUser.duplicate();
                        labelImg.setStack(padedIs);
//                    }
                    labelImage.initWithImg(labelImg);
                    labelImage.initBorder();
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
        
        stackProcess = new SegmentationProcessWindow(width, height, showAllFrames);
        stackProcess.setImageTitle("Stack_" + (inputImageChosenByUser.getTitle() == null ? "DRS" : inputImageChosenByUser.getTitle()));
      
        stackProcess.addSliceToStack(labelImage, "init without contours", 0);
        labelImage.initBorder();
        stackProcess.addSliceToStack(labelImage, "init with contours", 0);
    }

    private void runRegionCompetion() {
        initInputImage();
        initLabelImage();
        initEnergies();
        initStack();
        
        Controller iController = new Controller(/* aShowWindow */ showGUI);

        // Run segmentation
        SettingsRC rcSettings = new SettingsRC(iSettings.m_AllowFusion, 
                                               iSettings.m_AllowFission, 
                                               iSettings.m_AllowHandles, 
                                               iSettings.m_MaxNbIterations, 
                                               iSettings.m_OscillationThreshold, 
                                               iSettings.m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC);
        
        AlgorithmRC algorithm = new AlgorithmRC(intensityImage, labelImage, imageModel, rcSettings);
        
        boolean isDone = false;
        int iteration = 0;
        while (iteration < iSettings.m_MaxNbIterations && !isDone) {
            // Perform one iteration of RC
            ++iteration;
            IJ.showStatus("Iteration: " + iteration + "/" + iSettings.m_MaxNbIterations);
            IJ.showProgress(iteration, iSettings.m_MaxNbIterations);
            isDone = algorithm.performIteration();
            
            // Check if we should pause for a moment or if simulation is not aborted by user
            // If aborted pretend that we have finished segmentation (isDone=true)
            isDone = iController.hasAborted() ? true : isDone;

            // Add slice with iteration output
            stackProcess.addSliceToStack(labelImage, "iteration " + iteration, algorithm.getBiggestLabel());
        }
        IJ.showProgress(iSettings.m_MaxNbIterations, iSettings.m_MaxNbIterations);

        // Do some post process stuff
        stackProcess.addSliceToStack(labelImage, "final image iteration " + iteration, algorithm.getBiggestLabel());
        
        ImagePlus show = labelImage.show("LabelRC");
        show.setStack(ImgUtils.crop(show.getStack(), iPadSize, labelImage.getNumOfDimensions() > 2));
        
        iController.close();
        saveStatistics(algorithm);
    }

    /**
     * Initializes labelImage with ROI <br>
     */
    private void initializeRoi(final LabelImage labelImg) {
        labelImg.initLabelsWithRoi(inputImageChosenByUser.getRoi());
        labelImg.initBorder();
        labelImg.connectedComponents();
    }
    
    private void runDiscreteRegionSampling() {
        initInputImage();
        initLabelImage();
        initEnergies();
//        initStack();
        Controller iController = new Controller(/* aShowWindow */ showGUI);

        // Run segmentation
        SettingsDRS drsSettings = new SettingsDRS(iSettings.m_AllowFusion, 
                                                  iSettings.m_AllowFission, 
                                                  iSettings.m_AllowHandles, 
                                                  iSettings.m_MaxNbIterations, 
                                                  iSettings.offBoundarySampleProbability,
                                                  iSettings.useBiasedProposal,
                                                  iSettings.usePairProposal,
                                                  iSettings.burnInFactor,
                                                  iSettings.m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC);
        
        AlgorithmDRS algorithm = new AlgorithmDRS(intensityImage, labelImage, imageModel, drsSettings);
        
        
        int modulo = iSettings.m_MaxNbIterations / 20; // 5% steps
        if (modulo < 1) modulo = 1;
        
        boolean isDone = false;
        int iteration = 0;
        while (iteration < iSettings.m_MaxNbIterations && !isDone) {
            // Perform one iteration of RC
            ++iteration;
            if (iteration % modulo == 0) {
                logger.debug("Iteration progress: " + ((iteration * 100) /  iSettings.m_MaxNbIterations) + "%");
                IJ.showStatus("Iteration: " + iteration + "/" + iSettings.m_MaxNbIterations);
                IJ.showProgress(iteration, iSettings.m_MaxNbIterations);
            }
            isDone = algorithm.performIteration();
            
            // Check if we should pause for a moment or if simulation is not aborted by user
            // If aborted pretend that we have finished segmentation (isDone=true)
            isDone = iController.hasAborted() ? true : isDone;
        }
        IJ.showProgress(iSettings.m_MaxNbIterations, iSettings.m_MaxNbIterations);

        // Do some post process stuff
        iController.close();
        
        stackProcess = algorithm.createProbabilityImage();
        
        ImagePlus show = labelImage.show("LabelDRS");
        show.setStack(ImgUtils.crop(show.getStack(), iPadSize, labelImage.getNumOfDimensions() > 2));
        
//        intensityImage.show("IntenDRS");
//        edgeImage.show("EdgeDRS");
    }
}
