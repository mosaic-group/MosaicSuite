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
    
    // Global settings for plugin
    static final String SettingsFilepath = SysOps.getTmpPath() + "spb_settings.dat";
    
    // Input paramters
    ImagePlus iInputImage;
    
    // Internal settings
    static boolean iGuiModeEnabled = true;
    
    @Override
    public int setup(String aArgs, ImagePlus aInputImage) {
        iInputImage = aInputImage;
        boolean isMacro = IJ.isMacro() || Interpreter.batchMode;
        boolean isHeadlessMode = GraphicsEnvironment.isHeadless();
        iGuiModeEnabled = !(isMacro || isHeadlessMode);

        if (isMacro) {
            // if is a macro get the arguments from macro arguments
            aArgs = Macro.getOptions();
            logger.info("Macro options: [" + aArgs + "]");
        }

        // Read settings
        final String path = MosaicUtils.parseString("config", aArgs);
        if (path != null) {
            BLauncher.iParameters = getConfigHandler().LoadFromFile(path, Parameters.class, BLauncher.iParameters);
        }
        else {
            BLauncher.iParameters = getConfigHandler().LoadFromFile(SettingsFilepath, Parameters.class, BLauncher.iParameters);
        }

        final String filepath = MosaicUtils.parseString("filepath", aArgs);
        if (filepath != null) {
            BLauncher.iParameters.wd = filepath;
            logger.info("wd (cluster) = [" + BLauncher.iParameters.wd + "]");
        }
        
        String normmin = MosaicUtils.parseString("min", aArgs);
        if (normmin != null) {
            BLauncher.norm_min = Double.parseDouble(normmin);
            System.out.println("min norm " + BLauncher.norm_min);
        }

        String normmax = MosaicUtils.parseString("max", aArgs);
        if (normmax != null) {
            BLauncher.norm_max = Double.parseDouble(normmax);
            System.out.println("max norm " + BLauncher.norm_max);
        }
        
        boolean processOnCluster = MosaicUtils.parseCheckbox("process", aArgs);
        if (processOnCluster) {
            System.out.println("Process on cluster: " + processOnCluster);
        }

        final String noOfThreads = MosaicUtils.parseString("nthreads", aArgs);
        if (noOfThreads != null) {
            BLauncher.iParameters.nthreads = Integer.parseInt(noOfThreads);
            logger.info("nthreads = [" + noOfThreads + "]");
        }
        else {
            BLauncher.iParameters.nthreads = Runtime.getRuntime().availableProcessors();
        }
        
        // Initialize CSV format
        CSVOutput.initCSV(1 /* oc_s */);

        logger.info("isHeadless = " + isHeadlessMode);
        logger.info("settings dir = [" + IJ.getDirectory("temp") + "]");
        logger.info("config path = [" + path + "]");
        logger.info("norm min = [" + normmin + "]");
        logger.info("norm max = [" + normmax + "]");
        logger.info("input img = [" + (aInputImage != null ? aInputImage.getTitle() : "<no img>") + "]");
        
        run();
        saveConfig(SettingsFilepath, BLauncher.iParameters);

        return DONE;
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
        // For the cluster we have to nullify the directory option
        String oldWorkDir = aParameters.wd;
        aParameters.wd = null;

        saveConfig("/tmp/settings.dat", aParameters);
        
        aParameters.wd = oldWorkDir;
    }
    
    public void run() {
        GenericGUI window = new GenericGUI(iInputImage);
        
        String imgPath = ImgUtils.getImageAbsolutePath(iInputImage);
        imgPath = imgPath != null ? imgPath : (iInputImage != null) ? "Input Image: " + iInputImage.getTitle() : null;
        boolean iUseClusterMode = GraphicsEnvironment.isHeadless();
        mosaic.utils.Debug.print("imgPath", imgPath);
        logger.info("run(...)           clustermode = " + iUseClusterMode);
        logger.info("                  IJ.isMacro() = " + IJ.isMacro());
        logger.info("         Interpreter.batchMode = " + Interpreter.batchMode);
        logger.info("                        useGUI = " + iGuiModeEnabled);
        

        RunMode rm = (iGuiModeEnabled) ? window.drawStandardWindow(imgPath) : window.drawBatchWindow();
        logger.info("runmode = " + rm);
        if (rm == RunMode.STOP) {
            if (!iGuiModeEnabled) Macro.abort();
            return;
        }
        logger.info("Parameters: " + BLauncher.iParameters);
        
        // Two different way to run the Segmentation and colocalization
        String file1;
        String file2;
        String file3;
        
        if (iUseClusterMode || rm != RunMode.CLUSTER) {
            // We run locally
            String savePath = null;

            if (iInputImage != null && ( BLauncher.iParameters.wd == null || BLauncher.iParameters.wd.startsWith("Input Image:") || BLauncher.iParameters.wd.isEmpty())) {
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
            else if (BLauncher.iParameters.wd != null && !BLauncher.iParameters.wd.equals("")) {
                logger.debug("WD with PATH: " + BLauncher.iParameters.wd);
                final File inputFile = new File(BLauncher.iParameters.wd);
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
                
                final File fl = new File(BLauncher.iParameters.wd);
                savePath = fl.isDirectory() ? BLauncher.iParameters.wd : fl.getParent();
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
            else {
                logger.info("No image to process!");
                return;
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
        else {
            runOnCluster(iInputImage);
        }
    }

    private void runOnCluster(ImagePlus aImp) {
        // We run on cluster
        saveParamitersForCluster(BLauncher.iParameters);
        ClusterSession.setPreferredSlotPerProcess(4);
        String backgroundImageFile = null;
        String outputPath = null;
        
        if (aImp == null) {
            File fl = new File(BLauncher.iParameters.wd);
            if (fl.isDirectory() == true) {
                File[] fileslist = fl.listFiles();
                ClusterSession.processFiles(fileslist, "Squassh", "", Files.outSuffixesCluster);
                outputPath = fl.getAbsolutePath();
            }
            else if (fl.isFile()) {
                ClusterSession.processFile(fl, "Squassh", "", Files.outSuffixesCluster);
                backgroundImageFile = fl.getAbsolutePath();
                outputPath = SysOps.getPathToFile(backgroundImageFile);
            }
            else {
                // Nothing to do just get the result
                ClusterSession.getFinishedJob(Files.outSuffixesCluster, "Squassh");

                // Ask for a directory
                fl = new File(IJ.getDirectory("Select output directory"));
                outputPath = fl.getAbsolutePath();
            }
        }
        else {
            // It is a file
            ClusterSession.processImage(aImp, "Squassh", "nthreads=4", Files.outSuffixesCluster);
            backgroundImageFile = ImgUtils.getImageDirectory(aImp) + File.separator + aImp.getTitle();
            outputPath = ImgUtils.getImageDirectory(aImp);
        }

        // Get output format and Stitch the output in the output selected
        logger.info(mosaic.utils.Debug.getString("testMode", outputPath, aImp, BLauncher.iParameters.wd));
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
        return "Squassh";
    }
}
