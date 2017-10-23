package mosaic.plugins;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import mosaic.core.utils.MosaicUtils;
import mosaic.region_competition.PluginSettingsDRS;
import mosaic.region_competition.DRS.AlgorithmDRS;
import mosaic.region_competition.DRS.SettingsDRS;
import mosaic.region_competition.GUI.Controller;
import mosaic.region_competition.GUI.GUI_DRS;
import mosaic.utils.ImgUtils;
import mosaic.utils.SysOps;
import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.JsonDataFile;

public class DiscreteRegionSampling extends Region_Competition implements PlugInFilter {
    private static final Logger logger = Logger.getLogger(DiscreteRegionSampling.class);
    
    static String ConfigFilename = "drs_settings.json";
    
    private PluginSettingsDRS iSettings = null;
    
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
                iSettings = getConfigHandler().LoadFromFile(path, PluginSettingsDRS.class);
            }

            outputSegmentedImageLabelFilename = MosaicUtils.parseString("output", options);
        }

        if (iSettings == null) {
            // load default config file
            iSettings = getConfigHandler().LoadFromFile(configFilePath(), PluginSettingsDRS.class, new PluginSettingsDRS());
        }
    }
    
    /**
     * Returns handler for (un)serializing Settings objects.
     */
    public static DataFile<PluginSettingsDRS> getConfigHandler() {
        return new JsonDataFile<PluginSettingsDRS>();
    }

    
    @Override
    protected String configFilePath() {
        return SysOps.getTmpPath() + ConfigFilename;
    }
    
    @Override
    public int setup(String arg, ImagePlus imp) {
        logger.info("Starting DiscreteRegionSampling");
        
        // Read settings and macro options
        initSettingsAndParseMacroOptions();
        userDialog = new GUI_DRS(iSettings, imp, false);
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

    @Override
    public void run(ImageProcessor aIp) {
        runDeep();
    }
    
    @Override
    protected void runIt() {
        initInputImage();
        initLabelImage(iSettings.labelImageInitType, iSettings.l_BoxRatio, iSettings.m_BubblesRadius, iSettings.m_BubblesDispl, iSettings.l_Sigma, iSettings.l_Tolerance, iSettings.l_BubblesRadius, iSettings.l_RegionTolerance);
        initEnergies(iSettings.m_EnergyFunctional, iSettings.m_RegionMergingThreshold, iSettings.m_GaussPSEnergyRadius, iSettings.m_BalloonForceCoeff, iSettings.regularizationType, iSettings.m_CurvatureMaskRadius, iSettings.m_EnergyContourLengthCoeff);
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
    }
    
    @Override
    protected void saveSegmentedImage() {
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
            if (outputProbabilityImgFileNameNoExt != null) {
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

}
