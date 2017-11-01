package mosaic.plugins;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.macro.Interpreter;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import mosaic.core.utils.MosaicUtils;
import mosaic.region_competition.PluginSettingsRC;
import mosaic.region_competition.GUI.Controller;
import mosaic.region_competition.GUI.GUI_RC;
import mosaic.region_competition.GUI.SegmentationProcessWindow;
import mosaic.region_competition.GUI.StatisticsTable;
import mosaic.region_competition.RC.AlgorithmRC;
import mosaic.region_competition.RC.ClusterModeRC;
import mosaic.region_competition.RC.SettingsRC;
import mosaic.utils.Debug;
import mosaic.utils.ImgUtils;
import mosaic.utils.SysOps;
import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.JsonDataFile;

public class RegionCompetition extends Region_Competition implements PlugInFilter {
    private static final Logger logger = Logger.getLogger(RegionCompetition.class);
    
    static String ConfigFilename = "rc_settings.dat";
    protected GUI_RC userDialog;
    private PluginSettingsRC iSettings = null;
    protected boolean useCluster = false;
    
    // Get some more settings and images from dialog
    protected boolean showAndSaveStatistics; 
    protected boolean showAllFrames;
    
    
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
                iSettings = getConfigHandler().LoadFromFile(path, PluginSettingsRC.class);
            }

            outputSegmentedImageLabelFilename = MosaicUtils.parseString("output", options);
        }

        if (iSettings == null) {
            // load default config file
            iSettings = getConfigHandler().LoadFromFile(configFilePath(), PluginSettingsRC.class, new PluginSettingsRC());
        }
    }
    
    protected void initStack() {
        final int[] dims = labelImage.getDimensions();
        final int width = dims[0];
        final int height = dims[1];
        
        stackProcess = new SegmentationProcessWindow(width, height, showAllFrames);
        stackProcess.setImageTitle("Stack_" + (inputImageChosenByUser.getTitle() == null ? "DRS" : inputImageChosenByUser.getTitle()));
      
        stackProcess.addSliceToStack(labelImage, "init without contours", 0);
        labelImage.initBorder();
        stackProcess.addSliceToStack(labelImage, "init with contours", 0);
    }
    
    protected void saveStatistics(AlgorithmRC algorithm) {
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
    
    
    /**
     * Returns handler for (un)serializing Settings objects.
     */
    public static DataFile<PluginSettingsRC> getConfigHandler() {
        return new JsonDataFile<PluginSettingsRC>();
    }
    
    @Override
    protected String configFilePath() {
        return SysOps.getTmpPath() + ConfigFilename;
    }
    
    @Override
    public int setup(String arg, ImagePlus imp) {
        logger.info("Starting RegionCompetition");
        
        // Read settings and macro options
        initSettingsAndParseMacroOptions();
        
        userDialog = new GUI_RC(iSettings, imp);
        
        if (!setupDeep(imp, iSettings)) return DONE;
        
        // Save new settings from user input.
        getConfigHandler().SaveToFile(configFilePath(), iSettings);
        
        // If there were no input image when plugin was started then return NO_IMAGE_REQUIRED 
        // since user must have chosen image in dialog - they will be processed later.
        if (inputImageChosenByUser != null && originalInputImage == null) {
            return NO_IMAGE_REQUIRED;
        }
        return DOES_ALL + NO_CHANGES;
    }

    public boolean setupDeep(ImagePlus aImp, PluginSettingsRC iSettings) {
        // Save input stuff
        originalInputImage = aImp;

        // Get information from user
//        userDialog = new GUI(iSettings, originalInputImage, aArgs.equals("DRS") ? false : true);
        userDialog.showDialog();
        if (!userDialog.configurationValid()) {
            return false;
        }
        
        // Get some more settings and images
        showGUI = !(IJ.isMacro() || Interpreter.batchMode);
        showAndSaveStatistics = userDialog.showAndSaveStatistics();
        showAllFrames = userDialog.showAllFrames();
        inputLabelImageChosenByUser = userDialog.getInputLabelImage();
        inputImageChosenByUser = userDialog.getInputImage();
        if (inputImageChosenByUser != null) inputImageCalibration = inputImageChosenByUser.getCalibration();
        useCluster = userDialog.useCluster();
        normalize_ip = userDialog.getNormalize();
        
        logger.info("Input image [" + (inputImageChosenByUser != null ? inputImageChosenByUser.getTitle() : "<no file>") + "]");
        if (inputImageChosenByUser != null) logger.info(ImgUtils.getImageInfo(inputImageChosenByUser));
        logger.info("Label image [" + (inputLabelImageChosenByUser != null ? inputLabelImageChosenByUser.getTitle() : "<no file>") + "]");
        logger.info("showAndSaveStatistics: " + showAndSaveStatistics + 
                    ", showAllFrames: " + showAllFrames + 
                    ", useCluster: " + useCluster +
                    ", showGui: " + showGUI +
                    ", normalize: " + normalize_ip);
        logger.debug("Settings:\n" + Debug.getJsonString(iSettings));
        

        return true;
    }
    
    @Override
    public void run(ImageProcessor aIp) {
        if (useCluster == true) {
            ClusterModeRC.runClusterMode(inputImageChosenByUser, 
                                         inputLabelImageChosenByUser,
                                         iSettings, 
                                         outputFileNamesSuffixes);
            
            // Finish - nothing to do more here...
            return;
        }
        runDeep();
    }

    @Override
    protected void runIt() {
        initInputImage();
        initLabelImage(iSettings.labelImageInitType, iSettings.l_BoxRatio, iSettings.m_BubblesRadius, iSettings.m_BubblesDispl, iSettings.l_Sigma, iSettings.l_Tolerance, iSettings.l_BubblesRadius, iSettings.l_RegionTolerance);
        initEnergies(iSettings.m_EnergyFunctional, iSettings.m_RegionMergingThreshold, iSettings.m_GaussPSEnergyRadius, iSettings.m_BalloonForceCoeff, iSettings.regularizationType, iSettings.m_CurvatureMaskRadius, iSettings.m_EnergyContourLengthCoeff);
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
    
    @Override
    protected void saveSegmentedImage() {
        String absoluteFileNameNoExt= ImgUtils.getImageAbsolutePath(inputImageChosenByUser, true);
        logger.debug("Absolute file name with dir: " + absoluteFileNameNoExt);
        if (outputSegmentedImageLabelFilename == null && absoluteFileNameNoExt != null) {
                outputSegmentedImageLabelFilename = absoluteFileNameNoExt + outputFileNamesSuffixes[1].replace("*", "");
        }
        if (outputSegmentedImageLabelFilename != null) { 
            logger.info("Saving segmented image [" + outputSegmentedImageLabelFilename + "]");
            ImagePlus outImg = labelImage.convertToImg("ResultWindow");
            outImg.setStack(ImgUtils.crop(outImg.getStack(), iPadSize, labelImage.getNumOfDimensions() > 2));
            IJ.save(outImg, outputSegmentedImageLabelFilename);
        }
        else {
            logger.error("Cannot save segmentation result. Filename for saving not available!");
        }
    }
}
