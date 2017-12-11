package mosaic.plugins;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.Recorder;
import ij.process.ImageProcessor;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.utils.MosaicUtils;
import mosaic.regions.RegionsUtils;
import mosaic.regions.DRS.AlgorithmDRS;
import mosaic.regions.DRS.PluginSettingsDRS;
import mosaic.regions.DRS.SettingsDRS;
import mosaic.regions.GUI.Controller;
import mosaic.regions.GUI.GuiDrs;
import mosaic.regions.energies.ImageModel;
import mosaic.utils.Debug;
import mosaic.utils.ImgUtils;
import mosaic.utils.SysOps;
import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.JsonDataFile;

public class DiscreteRegionSampling implements PlugInFilter {
    private static final Logger logger = Logger.getLogger(DiscreteRegionSampling.class);
    
    // Settings
    private static PluginSettingsDRS iMacroSettings = null;
    private PluginSettingsDRS iSettings = null;
    private boolean iNormalizeInputImg = true;
    private boolean iShowGui = true;
    private int iPadSize = 1;

    // User images to be processed
    private ImagePlus iInputImageChosenByUser;
    private ImagePlus iInputLabelImageChosenByUser;
    
    // User interfaces
    private GuiDrs iUserGui;
    private LabelImage iLabelImage;
    private ImagePlus iOutputLabelImage;
    private ImagePlus iProbabilityImage;
    
    @Override
    public int setup(String aInputArgs, ImagePlus aInputImg) {
        logger.info("Starting DiscreteRegionSampling");
        iInputImageChosenByUser = aInputImg;
        
        initSettingsAndParseMacroOptions();

        return DOES_ALL + NO_CHANGES;
    }

    @Override
    public void run(ImageProcessor aIp) {
        iShowGui = !(IJ.isMacro() || Interpreter.batchMode);
        logger.info("showGui: " + iShowGui + ", normalize: " + iNormalizeInputImg);
        
        if (iShowGui) {
            iUserGui = new GuiDrs(iSettings, iInputImageChosenByUser);
            iUserGui.showDialog();
            if (iUserGui.wasCanceled()) {
                logger.debug("Execution cancelled");
                return;
            }
            iInputLabelImageChosenByUser = iUserGui.getInputLabelImage();
        }
        else {
            if (iSettings.initFileName != null) {
                iInputLabelImageChosenByUser = WindowManager.getImage(iSettings.initFileName);
            }
        }
        
        logger.info("Input image [" + iInputImageChosenByUser.getTitle() + "]");
        logger.info(ImgUtils.getImageInfo(iInputImageChosenByUser));
        logger.info("Label image [" + (iInputLabelImageChosenByUser != null ? iInputLabelImageChosenByUser.getTitle() : "<no file>") + "]");
        logger.debug("Settings:\n" + Debug.getJsonString(iSettings));
        
        // Save new settings from user input.
        getConfigHandler().SaveToFile(configFilePath(), iSettings);
        if (Recorder.record) {
            Recorder.recordString("call('mosaic.plugins.DiscreteRegionSampling.macroSetup', '"+ new Gson().toJson(iSettings) + "');\n");
        }
        
        if (runSegmentation()) {
            saveSegmentedImage();
        }
    }
    
    private void initSettingsAndParseMacroOptions() {
        iSettings = iMacroSettings;
        
        // Set static macro settings to null. This is needed since we would remember old macro settings 
        // and treat it as a new one at next run. (as happens in unit testing).
        iMacroSettings = null;
        
        final String options = Macro.getOptions();
        logger.info("Macro Options: [" + options + "]");
        if (options != null) {
            String normalizeString = MosaicUtils.parseString("normalize", options);
            if (normalizeString != null) {
                iNormalizeInputImg = Boolean.parseBoolean(normalizeString);
            }
            
            if (iSettings == null) {
                String path = MosaicUtils.parseString("config", options);
                if (path != null) {
                    logger.debug("Reading settings from file provided in macro options.");
                    iSettings = getConfigHandler().LoadFromFile(path, PluginSettingsDRS.class);
                }
            }
        }

        if (iSettings == null) {
            logger.debug("Trying to read config file from default location [" + configFilePath() + "]");
            iSettings = getConfigHandler().LoadFromFile(configFilePath(), PluginSettingsDRS.class, new PluginSettingsDRS());
        }
    }
    
    /**
     * Static method used in macro mode for providing settings. With all disadvantages of that way, it has one important advantage:
     * config string in macro looks much better than provided directly to plugin since it does not have to
     * escape all quote signs and there are a lot of those. So it is easier for user to modify config manually.
     * @param aSettingsData - data provided in macro in Json format (will be provided by ImageJ which will call this method)
     */
    static public void macroSetup(String aSettingsData) {
        logger.info("macroSetup() run with: [" + aSettingsData + "]");
        PluginSettingsDRS obj = new Gson().fromJson(aSettingsData, PluginSettingsDRS.class);
        logger.debug("DRS Settings: ["+obj+"]");
        iMacroSettings = obj;
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
        iLabelImage = RegionsUtils.initLabelImage(iIntensityImage, iInputImageChosenByUser, iInputLabelImageChosenByUser, iPadSize, iSettings.initType, iSettings.initBoxRatio, iSettings.initBubblesRadius, iSettings.initBubblesDisplacement, iSettings.initLocalMaxGaussBlurSigma, iSettings.initLocalMaxTolerance, iSettings.initLocalMaxBubblesRadius, iSettings.initLocalMaxMinimumRegionSize);
        if (iLabelImage == null) return false; // Abort execution
        ImageModel iImageModel = RegionsUtils.initEnergies(iIntensityImage, iLabelImage, iInputImageChosenByUser.getCalibration(), iSettings.energyFunctional, 0 /* merging not used in DRS */, iSettings.energyPsGaussEnergyRadius, iSettings.energyPsBalloonForceCoeff, iSettings.regularizationType, iSettings.energyCurvatureMaskRadius, iSettings.energyContourLengthCoeff);
        Controller iController = new Controller(/* aShowWindow */ iShowGui);

        // Run segmentation
        SettingsDRS drsSettings = new SettingsDRS(iSettings.allowFusion, 
                                                  iSettings.allowFission, 
                                                  iSettings.allowHandles, 
                                                  iSettings.maxNumOfIterations, 
                                                  iSettings.offBoundarySampleProbability,
                                                  iSettings.useBiasedProposal,
                                                  iSettings.usePairProposal,
                                                  iSettings.burnInFactor);
        
        AlgorithmDRS algorithm = new AlgorithmDRS(iIntensityImage, iLabelImage, iImageModel, drsSettings);
        
        
        int modulo = iSettings.maxNumOfIterations / 20; // 5% steps
        if (modulo < 1) modulo = 1;
        
        boolean isDone = false;
        int iteration = 0;
        try {
            while (iteration < iSettings.maxNumOfIterations && !isDone) {
                // Perform one iteration of RC
                ++iteration;
                if (iteration % modulo == 0) {
                    logger.debug("Iteration progress: " + ((iteration * 100) /  iSettings.maxNumOfIterations) + "%");
                    IJ.showStatus("Iteration: " + iteration + "/" + iSettings.maxNumOfIterations);
                    IJ.showProgress(iteration, iSettings.maxNumOfIterations);
                }
                algorithm.runOneIteration();

                // Check if we should pause for a moment or if simulation is not aborted by user
                // If aborted pretend that we have finished segmentation (isDone=true)
                isDone = iController.hasAborted();
            }
        }
        catch (IllegalStateException e) {
            IJ.showMessage("DRS stopped because: " + e.getMessage());
            return false;
        }
        finally {
            // Do some post process stuff
            IJ.showProgress(iSettings.maxNumOfIterations, iSettings.maxNumOfIterations);
            iController.close();
        }

        // Produce probability image only if needed        
        if (iSettings.showProbabilityImage || iSettings.saveProbabilityImage) {
            iProbabilityImage = algorithm.createProbabilityImage();
            iProbabilityImage.setStack(ImgUtils.crop(iProbabilityImage.getStack(), iPadSize, iLabelImage.getNumOfDimensions() > 2));
        }
        
        // Produce labelImage only if needed
        if (iSettings.showLabelImage || iSettings.saveLabelImage) {
            iOutputLabelImage = iLabelImage.convertToImg("Label");
            iOutputLabelImage.setStack(ImgUtils.crop(iOutputLabelImage.getStack(), iPadSize, iLabelImage.getNumOfDimensions() > 2));
        }
        
        if (iSettings.showProbabilityImage) iProbabilityImage.show();
        if (iSettings.showLabelImage) iOutputLabelImage.show();
        
        return true;
    }
    
    private void saveSegmentedImage() {
        final String directory = ImgUtils.getImageDirectory(iInputImageChosenByUser);
        
        if (directory != null) {
            final String fileNameNoExt = SysOps.removeExtension(iInputImageChosenByUser.getTitle());
            
            // Probability image
            if (iSettings.saveProbabilityImage) {
                saveImage(directory, fileNameNoExt, "_prob_c1.tif", iProbabilityImage);
            }
            
            // Label image
            if (iSettings.saveLabelImage) {
                saveImage(directory, fileNameNoExt, "_seg_c1.tif", iOutputLabelImage);
            }
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
