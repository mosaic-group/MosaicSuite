package mosaic.plugins;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.macro.Interpreter;
import ij.process.ImageProcessor;
import mosaic.bregman.Files;
import mosaic.bregman.Files.FileInfo;
import mosaic.bregman.Files.FileType;
import mosaic.bregman.Parameters;
import mosaic.bregman.RScript;
import mosaic.bregman.SquasshLauncher;
import mosaic.bregman.GUI.GenericGUI;
import mosaic.core.cluster.ClusterSession;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Segmentation;
import mosaic.core.utils.ShellCommand;
import mosaic.utils.ImgUtils;
import mosaic.utils.SysOps;
import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.SerializedDataFile;

public class BregmanGLM_Batch implements Segmentation {
    private static final Logger logger = Logger.getLogger(BregmanGLM_Batch.class);
    
    private static final String PluginName = "Squassh";
    private static final String SettingsFilepath = SysOps.getTmpPath() + "spb_settings.dat";
    private static final String ConfigPrefix = "===> Conf: ";
    
    private enum DataSource {
        IMAGE,            // image provided by Fiji/ImageJ via plugin interface 
        WORK_DIR_OR_FILE, // working dir for path/file privided by macro options or from config file
        NONE              // not specified - user must enter its own image source in GUI
    }
    
    public enum RunMode {
        CLUSTER,          // files will be sent on cluster
        LOCAL,            // files will be processed locally 
        ERROR             // there is an error and plug-in will be stopped
    }
    
    private ImagePlus iInputImage;
    private Parameters iParameters;
    
    @Override
    public void run(ImageProcessor imp) {}

    @Override
    public int setup(String aArgs, ImagePlus aInputImage) {
        iInputImage = aInputImage;

        // ==============================================================================
        // Read settings 
        boolean isMacro = IJ.isMacro() || Interpreter.batchMode;
        if (isMacro) {
            aArgs = Macro.getOptions();
        }
        logger.info("Input options: [" + aArgs + "]");
        iParameters = readConfiguration(aArgs);
        iParameters.nthreads = readNumberOfThreads(aArgs);
        double normalizationMin = readNormalizationMinParams(aArgs);
        double normalizationMax = readNormalizationMaxParams(aArgs);
        boolean iProcessOnCluster = readClusterFlag(aArgs);
        String workDir = readWorkingDirectoryFromArgs(aArgs);

        // ==============================================================================
        // Decide what input take into segmentation. Currently priorities are:
        // 1. input file/dir provided in arguments, if not provided then
        // 2. take input image provided by Fiji (active window), if null then
        // 3. try to read old working directory from config file (or from provided file), if null
        // 4. run anyway, user may want to enter dir/file manually into window
        // It may be chnaged later by user via editing input field
        logger.info(ConfigPrefix + "Working directory read from args: [" + (workDir == null ? "<null>" : workDir) + "]");
        logger.info(ConfigPrefix + "Input image = [" + iInputImage + "][" + ImgUtils.getImageAbsolutePath(iInputImage) + "]");
        logger.info(ConfigPrefix + "Working directory read from config file (or default) = [" + iParameters.wd + "]");
        DataSource iSourceData = null;
        if (workDir != null) {
            iSourceData = DataSource.WORK_DIR_OR_FILE;
        } else if (iInputImage != null) {
            iSourceData = DataSource.IMAGE;
            String imgPath = ImgUtils.getImageAbsolutePath(iInputImage);
            workDir = imgPath != null ? imgPath : (iInputImage != null) ? "Input Image: " + iInputImage.getTitle() : null;
        }
        else if (iParameters.wd != null) {
            iSourceData = DataSource.WORK_DIR_OR_FILE;
            workDir = iParameters.wd;
        } else {
            iSourceData = DataSource.NONE;
            workDir = "Input Image: <path to file or folder>, or press button below.";
        }
        logger.info(ConfigPrefix + "Working mode = " + iSourceData + ", Working directory = [" + workDir + "]");

        // ==============================================================================
        // Run GUI and take input from user
        boolean isHeadlessMode = GraphicsEnvironment.isHeadless();
        boolean iGuiModeEnabled = !(isMacro || isHeadlessMode);
        logger.info("headlessMode = " + isHeadlessMode + ", isMacro = " + IJ.isMacro() + ", batchMode = " + Interpreter.batchMode);
        GenericGUI window = new GenericGUI(iInputImage, iGuiModeEnabled, iParameters);
        RunMode runMode = window.drawStandardWindow(workDir, iProcessOnCluster);
        
        logger.info("Runmode = " + runMode);
        if (runMode == RunMode.ERROR) {
            if (!iGuiModeEnabled) Macro.abort();
            return DONE;
        }
        
        // ==============================================================================
        // Update workDir with one read from GUI and check if we have something to process 
        String guiWorkDir = window.getInput();
        logger.info(ConfigPrefix + "Working directory (from GUI) = [" + guiWorkDir + "]");
        boolean isWorkingDirectoryCorrect = isWorkingDirectoryCorrect(guiWorkDir); 
        if (!guiWorkDir.equals(workDir)) {
            if (isWorkingDirectoryCorrect) {
                // User has changed file/dir to process so update and continue
                iSourceData = DataSource.WORK_DIR_OR_FILE;
                workDir = guiWorkDir;
            }
            else {
                return DONE;
            }
        }
        if (!(iSourceData == DataSource.IMAGE || iSourceData == DataSource.WORK_DIR_OR_FILE)) {
            logger.info("No image to process!");
            return DONE;
        }

        // ==============================================================================
        // Save parameters and run segmentation. 
        // If workDir is correct then also update it.
        if (isWorkingDirectoryCorrect) iParameters.wd = guiWorkDir;
        saveConfig(SettingsFilepath, iParameters);
        
        switch(runMode) {
            case LOCAL:
                runLocally(workDir, iSourceData, normalizationMin, normalizationMax);
                break;
            case CLUSTER:
                runOnCluster(workDir, iSourceData);
                break;
            case ERROR:
            default:
                logger.error("Code should never end up in this place (" + runMode + ")");
        }

        return DONE;
    }

    private void runLocally(String aPathToFileOrDir, DataSource aSourceData, double aNormalizationMin, double aNormalizationMax) {
        String objectsDataFile = null;
        String imagesDataFile = null;
        String outputSaveDir = null;
        
        logger.info("Running locally with data source (" + aSourceData + ") and working dir [" + aPathToFileOrDir + "]");
        
        if (aSourceData == DataSource.IMAGE) {
            String imageDirectory = ImgUtils.getImageDirectory(iInputImage);
            outputSaveDir = (imageDirectory != null) ? imageDirectory + File.separator : IJ.getDirectory("Select output directory");
            if (outputSaveDir == null) return;
            logger.info("Output save dir: [" + outputSaveDir + "]");
            Set<FileInfo> savedFiles = new SquasshLauncher(iInputImage, iParameters, outputSaveDir, aNormalizationMin, aNormalizationMax).getSavedFiles();
            if (savedFiles.size() == 0) return;

            Files.moveFilesToOutputDirs(savedFiles, outputSaveDir);

            String titleNoExt = SysOps.removeExtension(iInputImage.getTitle());
            objectsDataFile = outputSaveDir + Files.getMovedFilePath(FileType.ObjectsDataNew, titleNoExt);
            imagesDataFile = outputSaveDir + Files.getMovedFilePath(FileType.ImagesDataNew, titleNoExt);
        }
        else {
            final File inputFile = new File(aPathToFileOrDir);
            File[] files = (inputFile.isDirectory()) ? inputFile.listFiles() : new File[] {inputFile};
            Arrays.sort(files);
            if (files.length == 0) {
                logger.info("No files to process in given direcotry. Exiting.");
                return;
            }
            outputSaveDir = (inputFile.isDirectory()) ? aPathToFileOrDir + File.separator : inputFile.getParent() + File.separator;
            Set<FileInfo> allFiles = new LinkedHashSet<FileInfo>();
            for (final File f : files) {
                // If it is the directory/Rscript/hidden/csv file then skip it
                if (f.isDirectory() == true || f.getName().equals(RScript.ScriptName) || f.getName().startsWith(".") || f.getName().endsWith(".csv")) {
                    continue;
                }
                logger.info("Opening file for segmenting: [" + f.getAbsolutePath() + "]");
                allFiles.addAll(new SquasshLauncher(MosaicUtils.openImg(f.getAbsolutePath()), iParameters, outputSaveDir, aNormalizationMin, aNormalizationMax).getSavedFiles());
            }
            if (allFiles.size() == 0) {
                logger.debug("No files have been written. Exiting.");
                return;
            }

            String titlePrefix = null;
            if (inputFile.isDirectory() == true) {
                titlePrefix = "stitch_";
                
                Set<FileInfo> movedFilesNames = Files.moveFilesToOutputDirs(allFiles, outputSaveDir);
                Files.stitchCsvFiles(movedFilesNames, outputSaveDir, null);
            }
            else {
                // TODO: It would be nice to be consistent and also move files to subdirs. But it is
                //       currently violated by cluster mode.
                // Files.moveFilesToOutputDirs(allFiles, savePath);
                titlePrefix = SysOps.removeExtension(inputFile.getName());
            }
            
            objectsDataFile = outputSaveDir + Files.createTitleWithExt(FileType.ObjectsDataNew, titlePrefix);
            imagesDataFile = outputSaveDir + Files.createTitleWithExt(FileType.ImagesDataNew, titlePrefix);
        }

        runRscript(outputSaveDir, objectsDataFile, imagesDataFile);
    }

    private void runOnCluster(String aWorkDir, DataSource aSourceData) {
        saveParamitersForCluster(iParameters);
        ClusterSession.setPreferredSlotPerProcess(4);
        
        String backgroundImageFile = null;
        String outputPath = null;
        switch (aSourceData) {
            case WORK_DIR_OR_FILE:
                File fl = new File(aWorkDir);
                if (fl.isDirectory() == true) {
                    File[] fileslist = fl.listFiles();
                    ClusterSession.processFiles(fileslist, PluginName, "nthreads=4", Files.outSuffixesCluster);
                    outputPath = fl.getAbsolutePath();
                }
                else if (fl.isFile()) {
                    ClusterSession.processFile(fl, PluginName, "nthreads=4", Files.outSuffixesCluster);
                    backgroundImageFile = fl.getAbsolutePath();
                    outputPath = SysOps.getPathToFile(backgroundImageFile);
                }
                else {
                    // Nothing to do just get the result
                    ClusterSession.getFinishedJob(Files.outSuffixesCluster, PluginName);
                    fl = new File(IJ.getDirectory("Select output directory"));
                    outputPath = fl.getAbsolutePath();
                }
                break;
            case IMAGE:
                ClusterSession.processImage(iInputImage, PluginName, "nthreads=4", Files.outSuffixesCluster);
                backgroundImageFile = ImgUtils.getImageDirectory(iInputImage) + File.separator + iInputImage.getTitle();
                outputPath = ImgUtils.getImageDirectory(iInputImage);
                break;
            case NONE:
            default:
                logger.error("Wrong source data for image processing: " + aSourceData);
        }
    
        // Get output format and Stitch the output in the output selected
        logger.info("cluster mode output path [" + outputPath + "], background image file [" + backgroundImageFile + "]");
        final File dir = ClusterSession.processJobsData(outputPath);
        MosaicUtils.StitchJobsCSV(dir.getAbsolutePath(), Files.outSuffixesCluster, backgroundImageFile);
    }

    private void runRscript(String outputSavePath, String objectsDataFile, String imagesDataFile) {
        if (new File(objectsDataFile).exists() && new File(imagesDataFile).exists()) {
            if (iParameters.save_images) {
                // TODO: Now both channels are kept in one csv file so below stuff should be changed.
                RScript.makeRScript(outputSavePath, objectsDataFile, objectsDataFile, imagesDataFile, iParameters.nbconditions, iParameters.nbimages, iParameters.groupnames, iParameters.ch1, iParameters.ch2);
                // Try to run the R script
                // TODO: Output seems to be completely wrong. Must be investigated.
                try {
                    logger.debug("================ RSCRIPT BEGIN ====================");
                    String command = "cd " + outputSavePath + "; Rscript " + outputSavePath + File.separator + RScript.ScriptName;
                    logger.debug("Command: [" + command + "]");
                    ShellCommand.exeCmdString(command);
                    logger.debug("================ RSCRIPT END ====================");
                }
                catch (final IOException e) {
                    e.printStackTrace();
                }
                catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isWorkingDirectoryCorrect(String workingDir) {
        return !(workingDir == null || workingDir.startsWith("Input Image:") || workingDir.isEmpty() || !(new File(workingDir).exists()));
    }

    private int readNumberOfThreads(String aArgs) {
        int num = 1;
        final String noOfThreads = MosaicUtils.parseString("nthreads", aArgs);
        if (noOfThreads != null) {
            logger.info(ConfigPrefix + "Number of threads provided in arguments = [" + noOfThreads + "]");
            num = Integer.parseInt(noOfThreads);
        }
        else {
            num = Runtime.getRuntime().availableProcessors();
            logger.info(ConfigPrefix + "Number of threads taken from runtime = [" + num + "]");
        }
        return num;
    }

    private boolean readClusterFlag(String aArgs) {
        boolean processOnCluster = MosaicUtils.parseCheckbox("process", aArgs);
        logger.info(ConfigPrefix + "Process on cluster flag set to: " + processOnCluster);
        return processOnCluster;
    }

    private double readNormalizationMinParams(String aArgs) {
        String normmin = MosaicUtils.parseString("min", aArgs);
        if (normmin != null) {
            logger.info(ConfigPrefix + "Min normalization provided in arguments = [" + normmin + "]");
            return Double.parseDouble(normmin);
        }
        return 0.0;
    }
    
    private double readNormalizationMaxParams(String aArgs) {
        String normmax = MosaicUtils.parseString("max", aArgs);
        if (normmax != null) {
            logger.info(ConfigPrefix + "Max normalization provided in arguments = [" + normmax + "]");
            return Double.parseDouble(normmax);
        }
        return 0.0;
    }

    private String readWorkingDirectoryFromArgs(String aArgs) {
        final String filepath = MosaicUtils.parseString(ClusterSession.DefaultInputParameterName, aArgs);
        if (filepath != null) {
            logger.info(ConfigPrefix + "Working directory provided in arguments (" + ClusterSession.DefaultInputParameterName + ") =  [" + filepath + "]");
        }
        return filepath;
    }

    private Parameters readConfiguration(String aArgs) {
        String config = MosaicUtils.parseString("config", aArgs);
        if (config != null) {
            logger.info(ConfigPrefix + "Reading config provided in arguments [" + config + "]");
        }
        else {
            config = SettingsFilepath;
            logger.info(ConfigPrefix + "Reading default config [" + config + "]");
        }
        Parameters parameters = getConfigHandler().LoadFromFile(config, Parameters.class, new Parameters());
        logger.info(mosaic.utils.Debug.getJsonString(parameters));
        return parameters;
    }

    /**
     * Returns handler for (un)serializing Parameters objects.
     */
    private static DataFile<Parameters> getConfigHandler() {
        return new SerializedDataFile<Parameters>();
    }

    /**
     * Saves aParams object as a aFullFileName file
     */
    private void saveConfig(String aFullFileName, Parameters aParams) {
        getConfigHandler().SaveToFile(aFullFileName, aParams);
    }

    /**
     * Saves parameters for cluster session (with default name used by cluster session)
     */
    private void saveParamitersForCluster(final Parameters aParameters) {
        saveConfig(ClusterSession.DefaultSettingsFileName, aParameters);
    }

    // =================== Implementation of Segmentation interface ===========
    /**
     * Get Mask images name output
     * @param aImp image
     * @return set of possible output
     */
    @Override
    public String[] getMask(ImagePlus aImp) {
        String titleNoExt = SysOps.removeExtension(aImp.getTitle());
        return new String[] { Files.getMovedFilePath(FileType.Mask, titleNoExt, 1),
                              Files.getMovedFilePath(FileType.Mask, titleNoExt, 2) };
    }
    
    /**
     * Get CSV regions list name output
     * @param aImp image
     * @return set of possible output
     */
    @Override
    public String[] getRegionList(ImagePlus aImp) {
        String titleNoExt = SysOps.removeExtension(aImp.getTitle());
        return new String[] { Files.getMovedFilePath(FileType.ObjectsDataNew, titleNoExt),
                              // This is produced if there is a stitch operation
                              Files.createTitleWithExt(FileType.ObjectsDataNew, "stitch_") };
    }
    
    /**
     * Get name of the plugin
     */
    @Override
    public String getName() {
        return PluginName;
    }
}
