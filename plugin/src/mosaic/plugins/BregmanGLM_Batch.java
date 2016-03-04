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
import mosaic.bregman.BLauncher;
import mosaic.bregman.Files;
import mosaic.bregman.Files.FileInfo;
import mosaic.bregman.Files.Type;
import mosaic.bregman.Parameters;
import mosaic.bregman.RScript;
import mosaic.bregman.GUI.GenericGUI;
import mosaic.bregman.GUI.GenericGUI.RunMode;
import mosaic.bregman.output.CSVOutput;
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
    
    // constant settings for plug-in
    private static final String PluginName = "Squassh";
    private static final String SettingsFilepath = SysOps.getTmpPath() + "spb_settings.dat";
    private static final String ConfigPrefix = "===> Conf: ";
    
    private enum SourceData {
        Image,      // image provided by Fiji/ImageJ via plugin interface 
        WorkDirOrFile,    // working dir for path/file privided by macro options or from config file
        None        // not specified - user must enter its own image source in GUI
    }
    
    // Input parameters
    ImagePlus iInputImage;
    
    
    @Override
    public int setup(String aArgs, ImagePlus aInputImage) {
        iInputImage = aInputImage;
        
        boolean isMacro = IJ.isMacro() || Interpreter.batchMode;
        if (isMacro) {
            aArgs = Macro.getOptions();
        }
        logger.info("Input options: [" + aArgs + "]");
        
        // Initialize CSV format
        // TODO: This is ridiculous and must be refactored
        CSVOutput.initCSV(1 /* oc_s */);

        // Read settings
        readConfiguration(aArgs);
        System.out.println(mosaic.utils.Debug.getJsonString(BLauncher.iParameters));
        
        readNormalizationParams(aArgs);
        boolean iProcessOnCluster = readClusterFlag(aArgs);
        readNumberOfThreads(aArgs);
        
        // Decide what input take into segmentation. Currently priorities are:
        // 1. input file/dir provided in arguments, if not provided then
        // 2. take input image provided by Fiji (active window), if null then
        // 3. try to read old working directory from config file (or from provided file), if null
        // 4. run anyway, user may want to enter dir/file manually into window
        // It may be chnaged later by user via editing input field
        logger.info(ConfigPrefix + "Working directory read from config file (or default) = [" + BLauncher.iParameters.wd + "]");
        logger.info(ConfigPrefix + "Input image = [" + iInputImage + "][" + ImgUtils.getImageAbsolutePath(iInputImage) + "]");
        String workDir = readWorkingDirectoryFromArgs(aArgs);
        SourceData iSourceData = null;
        if (workDir != null) {
            iSourceData = SourceData.WorkDirOrFile;
        } else if (iInputImage != null) {
            iSourceData = SourceData.Image;
            String imgPath = ImgUtils.getImageAbsolutePath(iInputImage);
            workDir = imgPath != null ? imgPath : (iInputImage != null) ? "Input Image: " + iInputImage.getTitle() : null;
        }
        else if (BLauncher.iParameters.wd != null) {
            iSourceData = SourceData.WorkDirOrFile;
            workDir = BLauncher.iParameters.wd;
        } else {
            iSourceData = SourceData.None;
            workDir = "Input Image: <path to file or folder>, or press button below.";
        }
        logger.info(ConfigPrefix + "Working directory = [" + workDir + "]");
        logger.info(ConfigPrefix + "Working directory mode = [" + iSourceData + "]");
        
        boolean iIsHeadlessMode = GraphicsEnvironment.isHeadless();
        boolean iGuiModeEnabled = !(isMacro || iIsHeadlessMode);
        boolean iUseClusterMode = iIsHeadlessMode;
        logger.info("iIsHeadlessMode = " + iIsHeadlessMode);
        logger.info("input img = [" + (iInputImage != null ? iInputImage.getTitle() : "<no img>") + "]");
        logger.info("run(...)           clustermode = " + iUseClusterMode);
        logger.info("                  IJ.isMacro() = " + IJ.isMacro());
        logger.info("         Interpreter.batchMode = " + Interpreter.batchMode);
        logger.info("               iGuiModeEnabled = " + iGuiModeEnabled);
        logger.info("             iProcessOnCluster = " + iProcessOnCluster);
        logger.info("                       imgPath = [" + workDir + "]");

        GenericGUI window = new GenericGUI(iInputImage);
        RunMode rm = window.drawStandardWindow(workDir, iProcessOnCluster);
        logger.info("                       runmode = " + rm);

        if (rm == RunMode.STOP) {
            if (!iGuiModeEnabled) Macro.abort();
            return DONE;
        }
        
        // Update workDir with one read from GUI. 
        String workDirOut = window.getInput();
        logger.info(ConfigPrefix + "Working directory (from GUI) = [" + workDirOut + "]");
        boolean isWorkingDirectoryCorrect = isWorkingDirectoryCorrect(workDirOut); 
        if (!workDirOut.equals(workDir)) {
            if (isWorkingDirectoryCorrect) {
                iSourceData = SourceData.WorkDirOrFile;
            }
            else {
                return DONE;
            }
        }

        // Save parameters before segmentation. If workDir is correct then update it also.
        if (isWorkingDirectoryCorrect) BLauncher.iParameters.wd = workDirOut;
        saveConfig(SettingsFilepath, BLauncher.iParameters);

        logger.info("Parameters: " + BLauncher.iParameters);
        if (iSourceData != SourceData.Image && iSourceData != SourceData.WorkDirOrFile) {
            logger.info("No image to process!");
            return DONE;
        }

        if (iUseClusterMode || rm != RunMode.CLUSTER) {
            runLocally(workDirOut, iSourceData);
        }
        else {
            runOnCluster(workDir, iSourceData);
        }

        return DONE;
    }

    private void runLocally(String workDirOut, SourceData aSourceData) {
        // Two different way to run the Segmentation and colocalization
        String file1 = null;
        String file2 = null;
        String file3 = null;
        // We run locally
        String savePath = null;
        if (aSourceData == SourceData.Image) {
            logger.info("Taking input Image. WD NULL or EMPTY");
            savePath = ImgUtils.getImageDirectory(iInputImage) + File.separator;
            BLauncher bl = new BLauncher(iInputImage);
            Set<FileInfo> savedFiles = bl.getSavedFiles();
            if (savedFiles.size() == 0) return;

            Files.moveFilesToOutputDirs(savedFiles, savePath);

            String titleNoExt = SysOps.removeExtension(iInputImage.getTitle());
            file1 = savePath + Files.getMovedFilePath(Type.ObjectsData, titleNoExt, 1);
            file2 = savePath + Files.getMovedFilePath(Type.ObjectsData, titleNoExt, 2);
            file3 = savePath + Files.getMovedFilePath(Type.ImagesData, titleNoExt);
        }
        else {
            logger.debug("WD with PATH: " + workDirOut);
            final File inputFile = new File(workDirOut);
            File[] files = (inputFile.isDirectory()) ? inputFile.listFiles() : new File[] {inputFile};
            Arrays.sort(files);
            Set<FileInfo> allFiles = new LinkedHashSet<FileInfo>();
            for (final File f : files) {
                // If it is the directory/Rscript/hidden/csv file then skip it
                if (f.isDirectory() == true || f.getName().equals("R_analysis.R") || f.getName().startsWith(".") || f.getName().endsWith(".csv")) {
                    continue;
                }
                System.out.println("BLAUNCHER FILE: [" + f.getAbsolutePath() + "]");
                BLauncher bl = new BLauncher(MosaicUtils.openImg(f.getAbsolutePath()));
                allFiles.addAll(bl.getSavedFiles());
            }
            if (allFiles.size() == 0) return;

            final File fl = new File(workDirOut);
            savePath = fl.isDirectory() ? workDirOut : fl.getParent();
            savePath += File.separator;

            if (fl.isDirectory() == true) {
                Set<FileInfo> movedFilesNames = Files.moveFilesToOutputDirs(allFiles, savePath);

                file1 = savePath + Files.createTitleWithExt(Type.ObjectsData, "stitch_", 1);
                file2 = savePath + Files.createTitleWithExt(Type.ObjectsData, "stitch_", 2);
                file3 = savePath + Files.createTitleWithExt(Type.ImagesData, "stitch_");
                Files.stitchCsvFiles(movedFilesNames, savePath, null);
            }
            else {
                // TODO: It would be nice to be consistent and also move files to subdirs. But it is
                //       currently violated by cluster mode.
                //Files.moveFilesToOutputDirs(allFiles, savePath);

                String titleNoExt = SysOps.removeExtension(fl.getName());
                file1 = savePath + Files.createTitleWithExt(Type.ObjectsData, titleNoExt, 1);
                file2 = savePath + Files.createTitleWithExt(Type.ObjectsData, titleNoExt, 2);
                file3 = savePath + Files.createTitleWithExt(Type.ImagesData, titleNoExt);
            }
        }


        if (new File(file1).exists() && new File(file2).exists()) {
            if (BLauncher.iParameters.save_images) {
                new RScript(savePath, file1, file2, file3, BLauncher.iParameters.nbconditions, BLauncher.iParameters.nbimages, BLauncher.iParameters.groupnames, BLauncher.iParameters.ch1, BLauncher.iParameters.ch2);
                // Try to run the R script
                // TODO: Output seems to be completely wrong. Must be investigated. Currently turned off.
                try {
                    logger.debug("================ RSCRIPT BEGIN ====================");
                    logger.debug("CMD: " + "cd " + savePath + "; Rscript " + savePath + File.separator + "R_analysis.R");
                    ShellCommand.exeCmdString("cd " + savePath + "; Rscript " + savePath + File.separator + "R_analysis.R");
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

    private void readNumberOfThreads(String aArgs) {
        final String noOfThreads = MosaicUtils.parseString("nthreads", aArgs);
        if (noOfThreads != null) {
            logger.info(ConfigPrefix + "Number of threads provided in arguments = [" + noOfThreads + "]");
            BLauncher.iParameters.nthreads = Integer.parseInt(noOfThreads);
        }
        else {
            BLauncher.iParameters.nthreads = Runtime.getRuntime().availableProcessors();
            logger.info(ConfigPrefix + "Number of threads taken from system = [" + BLauncher.iParameters.nthreads + "]");
        }
    }

    private boolean readClusterFlag(String aArgs) {
        boolean processOnCluster = MosaicUtils.parseCheckbox("process", aArgs);
        logger.info(ConfigPrefix + "Process on cluster flag set to: " + processOnCluster);
        return processOnCluster;
    }

    private void readNormalizationParams(String aArgs) {
        String normmin = MosaicUtils.parseString("min", aArgs);
        if (normmin != null) {
            logger.info(ConfigPrefix + "Min normalization provided in arguments = [" + normmin + "]");
            BLauncher.norm_min = Double.parseDouble(normmin);
        }

        String normmax = MosaicUtils.parseString("max", aArgs);
        if (normmax != null) {
            logger.info(ConfigPrefix + "Max normalization provided in arguments = [" + normmax + "]");
            BLauncher.norm_max = Double.parseDouble(normmax);
        }
    }

    private String readWorkingDirectoryFromArgs(String aArgs) {
        final String filepath = MosaicUtils.parseString(ClusterSession.DefaultInputParameterName, aArgs);
        if (filepath != null) {
            logger.info(ConfigPrefix + "Working directory provided in arguments (" + ClusterSession.DefaultInputParameterName + ") =  [" + filepath + "]");
        }
        return filepath;
    }

    private void readConfiguration(String aArgs) {
        String config = MosaicUtils.parseString("config", aArgs);
        if (config != null) {
            logger.info(ConfigPrefix + "Reading config provided in arguments [" + config + "]");
        }
        else {
            config = SettingsFilepath;
            logger.info(ConfigPrefix + "Reading default config [" + config + "]");
        }
        BLauncher.iParameters = getConfigHandler().LoadFromFile(config, Parameters.class, BLauncher.iParameters);
    }

    @Override
    public void run(ImageProcessor imp) {}

    /**
     * Returns handler for (un)serializing Parameters objects.
     */
    public static DataFile<Parameters> getConfigHandler() {
        return new SerializedDataFile<Parameters>();
    }

    /**
     * Saves Parameters object
     *
     * @param aFullFileName - absolute path and file name
     * @param aParams - object to be serialized
     */
    private void saveConfig(String aFullFileName, Parameters aParams) {
        getConfigHandler().SaveToFile(aFullFileName, aParams);
    }

    private void saveParamitersForCluster(final Parameters aParameters) {
        saveConfig(ClusterSession.DefaultSettingsFileName, aParameters);
    }

    private void runOnCluster(String aWorkDir, SourceData aSourceData) {
        saveParamitersForCluster(BLauncher.iParameters);
        ClusterSession.setPreferredSlotPerProcess(4);
        String backgroundImageFile = null;
        String outputPath = null;
        switch (aSourceData) {
            case WorkDirOrFile:
                File fl = new File(aWorkDir);
                if (fl.isDirectory() == true) {
                    File[] fileslist = fl.listFiles();
                    ClusterSession.processFiles(fileslist, PluginName, "", Files.outSuffixesCluster);
                    outputPath = fl.getAbsolutePath();
                }
                else if (fl.isFile()) {
                    ClusterSession.processFile(fl, PluginName, "", Files.outSuffixesCluster);
                    backgroundImageFile = fl.getAbsolutePath();
                    outputPath = SysOps.getPathToFile(backgroundImageFile);
                }
                else {
                    // Nothing to do just get the result
                    ClusterSession.getFinishedJob(Files.outSuffixesCluster, PluginName);
                    
                    // Ask for a directory
                    fl = new File(IJ.getDirectory("Select output directory"));
                    outputPath = fl.getAbsolutePath();
                }
                break;
            case Image:
                ClusterSession.processImage(iInputImage, PluginName, "nthreads=4", Files.outSuffixesCluster);
                backgroundImageFile = ImgUtils.getImageDirectory(iInputImage) + File.separator + iInputImage.getTitle();
                outputPath = ImgUtils.getImageDirectory(iInputImage);
                break;
            case None:
            default:
                logger.error("Wrong source data for image processing: " + aSourceData);
        }

        // Get output format and Stitch the output in the output selected
        logger.info(mosaic.utils.Debug.getString("testMode", outputPath, iInputImage, aWorkDir));
        final File dir = ClusterSession.processJobsData(outputPath);
        MosaicUtils.StitchJobsCSV(dir.getAbsolutePath(), Files.outSuffixesCluster, backgroundImageFile);
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
        return new String[] { Files.getMovedFilePath(Type.Mask, titleNoExt, 1),
                              Files.getMovedFilePath(Type.Mask, titleNoExt, 2) };
    }
    
    /**
     * Get CSV regions list name output
     * @param aImp image
     * @return set of possible output
     */
    @Override
    public String[] getRegionList(ImagePlus aImp) {
        String titleNoExt = SysOps.removeExtension(aImp.getTitle());
        return new String[] { Files.getMovedFilePath(Type.ObjectsData, titleNoExt, 1),
                              Files.getMovedFilePath(Type.ObjectsData, titleNoExt, 2), 
                              // This is produced if there is a stitch operation
                              Files.createTitleWithExt(Type.ObjectsData, "stitch_", 1), 
                              Files.createTitleWithExt(Type.ObjectsData, "stitch_", 2) };
    }
    
    /**
     * Get name of the plugin
     */
    @Override
    public String getName() {
        return PluginName;
    }
}
