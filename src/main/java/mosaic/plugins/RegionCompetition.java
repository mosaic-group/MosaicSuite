package mosaic.plugins;

import java.awt.GraphicsEnvironment;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.macro.Interpreter;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.utils.MosaicUtils;
import mosaic.regions.PluginSettingsRC;
import mosaic.regions.RegionsUtils;
import mosaic.regions.GUI.Controller;
import mosaic.regions.GUI.GuiRC;
import mosaic.regions.GUI.SegmentationProcessWindow;
import mosaic.regions.GUI.StatisticsTable;
import mosaic.regions.RC.AlgorithmRC;
import mosaic.regions.RC.ClusterModeRC;
import mosaic.regions.RC.SettingsRC;
import mosaic.regions.energies.ImageModel;
import mosaic.utils.Debug;
import mosaic.utils.ImgUtils;
import mosaic.utils.SysOps;
import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.JsonDataFile;

public class RegionCompetition implements PlugInFilter {
    private static final Logger logger = Logger.getLogger(RegionCompetition.class);
    
    // Output file names
    private final static String[] iOutputFileNamesSuffixes = { "*_ObjectsData_c1.csv", "*_seg_c1.tif" };
    
    private PluginSettingsRC iSettings = null;
    private boolean iNormalizeInputImg = true;
    private boolean iShowGui = true;
    private boolean iUseCluster = false;
    private boolean iShowAndSaveStatistics; 
    private boolean iShowAllSteps;
    private int iPadSize = 1;
    private String iOutputSegmentedImageLabelFilename = null;

    // Images to be processed
    private ImagePlus iInputImageChosenByUser;
    private ImagePlus iInputLabelImageChosenByUser;
    
    // User interfaces
    private GuiRC iUserGui;
    private LabelImage iLabelImage;
    private SegmentationProcessWindow iLabelImageStack;

    @Override
    public int setup(String arg, ImagePlus originalInputImage) {
        logger.info("Starting RegionCompetition");
        
        // Read settings and macro options
        initSettingsAndParseMacroOptions();
        
        iUserGui = new GuiRC(iSettings, originalInputImage);
        
        // Get information from user
        iUserGui.showDialog();
        if (!iUserGui.configurationValid()) {
            return DONE;
        }
        
        // Get some more settings and images
        iShowGui = !(IJ.isMacro() || Interpreter.batchMode);
        iShowAndSaveStatistics = iUserGui.showAndSaveStatistics();
        iShowAllSteps = iUserGui.showAllFrames();
        iInputLabelImageChosenByUser = iUserGui.getInputLabelImage();
        iInputImageChosenByUser = iUserGui.getInputImage();
        iUseCluster = iUserGui.useCluster();
        iNormalizeInputImg = iUserGui.getNormalize();
        
        logger.info("Input image [" + (iInputImageChosenByUser != null ? iInputImageChosenByUser.getTitle() : "<no file>") + "]");
        if (iInputImageChosenByUser != null) logger.info(ImgUtils.getImageInfo(iInputImageChosenByUser));
        logger.info("Label image [" + (iInputLabelImageChosenByUser != null ? iInputLabelImageChosenByUser.getTitle() : "<no file>") + "]");
        logger.info("showAndSaveStatistics: " + iShowAndSaveStatistics + 
                    ", showAllFrames: " + iShowAllSteps + 
                    ", useCluster: " + iUseCluster +
                    ", showGui: " + iShowGui +
                    ", normalize: " + iNormalizeInputImg);
        logger.debug("Settings:\n" + Debug.getJsonString(iSettings));
        
        // Save new settings from user input.
        getConfigHandler().SaveToFile(configFilePath(), iSettings);
        
        // If there were no input image when plugin was started but user selected it after then return NO_IMAGE_REQUIRED
        return (iInputImageChosenByUser != null  && originalInputImage == null) ? NO_IMAGE_REQUIRED : DOES_ALL + NO_CHANGES;
    }

    @Override
    public void run(ImageProcessor aIp) {
        if (iUseCluster == true) {
            ClusterModeRC.runClusterMode(iInputImageChosenByUser, 
                                         iInputLabelImageChosenByUser,
                                         iSettings, 
                                         iOutputFileNamesSuffixes);
            
            // Finish - nothing to do more here...
            return;
        }
        
        // Running locally
        runSegmentation();
        saveSegmentedImage();
    }
    
    private void initSettingsAndParseMacroOptions() {
        iSettings = null;
        
        final String options = Macro.getOptions();
        logger.info("Macro Options: [" + options + "]");
        if (options != null) {
            String normalizeString = MosaicUtils.parseString("normalize", options);
            if (normalizeString != null) {
                iNormalizeInputImg = Boolean.parseBoolean(normalizeString);
            }

            String path = MosaicUtils.parseString("config", options);
            if (path != null) {
                iSettings = getConfigHandler().LoadFromFile(path, PluginSettingsRC.class);
            }

            iOutputSegmentedImageLabelFilename = MosaicUtils.parseString("output", options);
        }

        if (iSettings == null) {
            // load default config file
            iSettings = getConfigHandler().LoadFromFile(configFilePath(), PluginSettingsRC.class, new PluginSettingsRC());
        }
    }
    
    private void initStack() {
        final int width = iLabelImage.getWidth();
        final int height = iLabelImage.getHeight();
        
        iLabelImageStack = new SegmentationProcessWindow(width, height, iShowAllSteps);
        iLabelImageStack.setImageTitle("Stack_" + (iInputImageChosenByUser.getTitle() == null ? "DRS" : iInputImageChosenByUser.getTitle()));
      
        iLabelImageStack.addSliceToStack(iLabelImage, "init without contours", 0);
        iLabelImage.initBorder();
        iLabelImageStack.addSliceToStack(iLabelImage, "init with contours", 0);
    }
    
    private void saveStatistics(AlgorithmRC algorithm) {
        String absoluteFileNameNoExt= ImgUtils.getImageAbsolutePath(iInputImageChosenByUser, true);
        if (absoluteFileNameNoExt == null) {
            logger.error("Cannot save segmentation statistics. Filename for saving not available!");
            return;
        }
        String absoluteFileName = absoluteFileNameNoExt + iOutputFileNamesSuffixes[0].replace("*", "");

        algorithm.calculateRegionsCenterOfMass();
        StatisticsTable statisticsTable = new StatisticsTable(algorithm.getLabelStatistics().values(), iPadSize);
        logger.info("Saving segmentation statistics [" + absoluteFileName + "]");
        statisticsTable.save(absoluteFileName);
        if (iShowGui) {
            statisticsTable.show("statistics");
        }
    }
    
    /**
     * Returns handler for (un)serializing Settings objects.
     */
    public static DataFile<PluginSettingsRC> getConfigHandler() {
        return new JsonDataFile<PluginSettingsRC>();
    }
    
    private String configFilePath() {
        return SysOps.getTmpPath() + "rc_settings.dat";
    }
    
    private void runSegmentation() {
        IntensityImage intensityImage = RegionsUtils.initInputImage(iInputImageChosenByUser, iNormalizeInputImg, iPadSize);
        iLabelImage = RegionsUtils.initLabelImage(intensityImage, iInputImageChosenByUser, iInputLabelImageChosenByUser, iPadSize, iSettings.labelImageInitType, iSettings.l_BoxRatio, iSettings.m_BubblesRadius, iSettings.m_BubblesDispl, iSettings.l_Sigma, iSettings.l_Tolerance, iSettings.l_BubblesRadius, iSettings.l_RegionTolerance);
        ImageModel imageModel = RegionsUtils.initEnergies(intensityImage, iLabelImage, iInputImageChosenByUser.getCalibration(), iSettings.m_EnergyFunctional, iSettings.m_RegionMergingThreshold, iSettings.m_GaussPSEnergyRadius, iSettings.m_BalloonForceCoeff, iSettings.regularizationType, iSettings.m_CurvatureMaskRadius, iSettings.m_EnergyContourLengthCoeff);
        initStack();
        
        Controller iController = new Controller(/* aShowWindow */ iShowGui);

        // Run segmentation
        SettingsRC rcSettings = new SettingsRC(iSettings.m_AllowFusion, 
                                               iSettings.m_AllowFission, 
                                               iSettings.m_AllowHandles, 
                                               iSettings.m_MaxNbIterations, 
                                               iSettings.m_OscillationThreshold, 
                                               iSettings.m_EnergyFunctional == RegionsUtils.EnergyFunctionalType.e_DeconvolutionPC);
        
        AlgorithmRC algorithm = new AlgorithmRC(intensityImage, iLabelImage, imageModel, rcSettings);
        
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
            iLabelImageStack.addSliceToStack(iLabelImage, "iteration " + iteration, algorithm.getBiggestLabel());
        }
        IJ.showProgress(iSettings.m_MaxNbIterations, iSettings.m_MaxNbIterations);

        // Do some post process stuff
        iLabelImageStack.addSliceToStack(iLabelImage, "final image iteration " + iteration, algorithm.getBiggestLabel());
        
        ImagePlus show = iLabelImage.show("LabelRC");
        show.setStack(ImgUtils.crop(show.getStack(), iPadSize, iLabelImage.getNumOfDimensions() > 2));
        
        iController.close();
        if (iShowAndSaveStatistics) saveStatistics(algorithm);
    }
    
    private void saveSegmentedImage() {
        String absoluteFileNameNoExt= ImgUtils.getImageAbsolutePath(iInputImageChosenByUser, true);
        logger.debug("Absolute file name with dir: " + absoluteFileNameNoExt);
        if (iOutputSegmentedImageLabelFilename == null && absoluteFileNameNoExt != null) {
                iOutputSegmentedImageLabelFilename = absoluteFileNameNoExt + iOutputFileNamesSuffixes[1].replace("*", "");
        }
        if (iOutputSegmentedImageLabelFilename != null) { 
            logger.info("Saving segmented image [" + iOutputSegmentedImageLabelFilename + "]");
            ImagePlus outImg = iLabelImage.convertToImg("ResultWindow");
            outImg.setStack(ImgUtils.crop(outImg.getStack(), iPadSize, iLabelImage.getNumOfDimensions() > 2));
            IJ.save(outImg, iOutputSegmentedImageLabelFilename);
        }
        else {
            logger.error("Cannot save segmentation result. Filename for saving not available!");
        }
        
        if (GraphicsEnvironment.isHeadless() == false) { // we are not on cluster
            final String directory = ImgUtils.getImageDirectory(iInputImageChosenByUser);
            final String fileNameNoExt = SysOps.removeExtension(iInputImageChosenByUser.getTitle());
            MosaicUtils.reorganize(iOutputFileNamesSuffixes, fileNameNoExt, directory, 1);
        }
    }
}
