package mosaic.plugins;

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
import mosaic.regions.PluginSettingsDRS;
import mosaic.regions.RegionsUtils;
import mosaic.regions.DRS.AlgorithmDRS;
import mosaic.regions.DRS.SettingsDRS;
import mosaic.regions.GUI.Controller;
import mosaic.regions.GUI.GuiDRS;
import mosaic.regions.GUI.SegmentationProcessWindow;
import mosaic.regions.energies.ImageModel;
import mosaic.utils.Debug;
import mosaic.utils.ImgUtils;
import mosaic.utils.SysOps;
import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.JsonDataFile;

public class DiscreteRegionSampling implements PlugInFilter {
    private static final Logger logger = Logger.getLogger(DiscreteRegionSampling.class);
    
    // Settings
    private PluginSettingsDRS iSettings = null;
    private boolean iNormalizeInputImg = true;
    private boolean iShowGui = true;
    private int iPadSize = 1;

    // User images to be processed
    private ImagePlus iInputImageChosenByUser;
    private ImagePlus iInputLabelImageChosenByUser;
    
    // User interfaces
    private GuiDRS iUserGui;
    private LabelImage iLabelImage;
    private SegmentationProcessWindow iProbabilityImage;
    
    @Override
    public int setup(String aInputArgs, ImagePlus aInputImg) {
        logger.info("Starting DiscreteRegionSampling");
        
        // Read settings and macro options
        initSettingsAndParseMacroOptions();
        iUserGui = new GuiDRS(iSettings, aInputImg);
        
        // Get information from user
        iUserGui.showDialog();
        if (!iUserGui.configurationValid()) {
            return DONE;
        }
        
        // Get some more settings and images
        iShowGui = !(IJ.isMacro() || Interpreter.batchMode);
        iInputLabelImageChosenByUser = iUserGui.getInputLabelImage();
        iInputImageChosenByUser = iUserGui.getInputImage();
        iNormalizeInputImg = iUserGui.getNormalize();
        
        logger.info("Input image [" + (iInputImageChosenByUser != null ? iInputImageChosenByUser.getTitle() : "<no file>") + "]");
        if (iInputImageChosenByUser != null) logger.info(ImgUtils.getImageInfo(iInputImageChosenByUser));
        logger.info("Label image [" + (iInputLabelImageChosenByUser != null ? iInputLabelImageChosenByUser.getTitle() : "<no file>") + "]");
        logger.info("showGui: " + iShowGui +
                    ", normalize: " + iNormalizeInputImg);
        logger.debug("Settings:\n" + Debug.getJsonString(iSettings));
        
        // Save new settings from user input.
        getConfigHandler().SaveToFile(configFilePath(), iSettings);
        
        // If there were no input image when plugin was started but user selected it after then return NO_IMAGE_REQUIRED
        return (iInputImageChosenByUser != null  && aInputImg == null) ? NO_IMAGE_REQUIRED : DOES_ALL + NO_CHANGES;
    }

    @Override
    public void run(ImageProcessor aIp) {
        if (runSegmentation()) {
            saveSegmentedImage();
        }
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
                iSettings = getConfigHandler().LoadFromFile(path, PluginSettingsDRS.class);
            }
        }

        if (iSettings == null) {
            // load default config file
            iSettings = getConfigHandler().LoadFromFile(configFilePath(), PluginSettingsDRS.class, new PluginSettingsDRS());
        }
    }
    
    /**
     * Returns handler for (un)serializing Settings objects.
     */
    private static DataFile<PluginSettingsDRS> getConfigHandler() {
        return new JsonDataFile<PluginSettingsDRS>();
    }

    private String configFilePath() {
        return SysOps.getTmpPath() + "drs_settings.json";
    }
    
    private boolean runSegmentation() {
        IntensityImage iIntensityImage = RegionsUtils.initInputImage(iInputImageChosenByUser, iNormalizeInputImg, iPadSize);
        if (iIntensityImage == null) return false; // Abort execution
        iLabelImage = RegionsUtils.initLabelImage(iIntensityImage, iInputImageChosenByUser, iInputLabelImageChosenByUser, iPadSize, iSettings.labelImageInitType, iSettings.l_BoxRatio, iSettings.m_BubblesRadius, iSettings.m_BubblesDispl, iSettings.l_Sigma, iSettings.l_Tolerance, iSettings.l_BubblesRadius, iSettings.l_RegionTolerance);
        if (iLabelImage == null) return false; // Abort execution
        ImageModel iImageModel = RegionsUtils.initEnergies(iIntensityImage, iLabelImage, iInputImageChosenByUser.getCalibration(), iSettings.m_EnergyFunctional, 0 /* merging not used in DRS */, iSettings.m_GaussPSEnergyRadius, iSettings.m_BalloonForceCoeff, iSettings.regularizationType, iSettings.m_CurvatureMaskRadius, iSettings.m_EnergyContourLengthCoeff);
        Controller iController = new Controller(/* aShowWindow */ iShowGui);

        // Run segmentation
        SettingsDRS drsSettings = new SettingsDRS(iSettings.m_AllowFusion, 
                                                  iSettings.m_AllowFission, 
                                                  iSettings.m_AllowHandles, 
                                                  iSettings.m_MaxNbIterations, 
                                                  iSettings.offBoundarySampleProbability,
                                                  iSettings.useBiasedProposal,
                                                  iSettings.usePairProposal,
                                                  iSettings.burnInFactor,
                                                  iSettings.m_EnergyFunctional == RegionsUtils.EnergyFunctionalType.e_DeconvolutionPC);
        
        AlgorithmDRS algorithm = new AlgorithmDRS(iIntensityImage, iLabelImage, iImageModel, drsSettings);
        
        
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
        
        iProbabilityImage = algorithm.createProbabilityImage();
        
        ImagePlus show = iLabelImage.show("LabelDRS");
        show.setStack(ImgUtils.crop(show.getStack(), iPadSize, iLabelImage.getNumOfDimensions() > 2));
        
        return true;
    }
    
    private void saveSegmentedImage() {
        final String directory = ImgUtils.getImageDirectory(iInputImageChosenByUser);
        
        if (directory != null) {
            final String fileNameNoExt = SysOps.removeExtension(iInputImageChosenByUser.getTitle());
            
            // Probability image
            ImagePlus outImgStack = iProbabilityImage.getImage();
            outImgStack.setStack(ImgUtils.crop(outImgStack.getStack(), iPadSize, iLabelImage.getNumOfDimensions() > 2));
            saveImage(directory, fileNameNoExt, "_prob_c1.tif", outImgStack);
            
            // Label image
            ImagePlus outImg = iLabelImage.convertToImg("ResultWindow");
            outImg.setStack(ImgUtils.crop(outImg.getStack(), iPadSize, iLabelImage.getNumOfDimensions() > 2));
            saveImage(directory, fileNameNoExt, "_seg_c1.tif", outImg);
        }
    }
    
    private void saveImage(String aOutputDir, String aFileNameNoExt, String aTag, ImagePlus aImage) {
        String dirNameProb = aOutputDir + "_" + aTag;
        SysOps.createDir(dirNameProb);
        String outputImgFileName = dirNameProb + "/" + aFileNameNoExt + aTag;
        logger.debug("Saving file: [" + outputImgFileName + "]");
        IJ.save(aImage, outputImgFileName);
    }
}
