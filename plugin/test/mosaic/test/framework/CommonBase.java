package mosaic.test.framework;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.scijava.Context;
import org.scijava.app.AppService;
import org.scijava.app.StatusService;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import io.scif.SCIFIOService;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;


/**
 * Common stuff for MOSAIC suit testing
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
@Ignore
public class CommonBase extends Info {

    private static final Logger logger = Logger.getLogger(CommonBase.class);
    private static ImgOpener iImgOpener = null;
    protected String tmpPath;
    protected String tcPath;
    
    @BeforeClass
    static public void setUpSuit() {
        logger.info("========================================================");
        logger.info("Starting TestSuit: " + iTestSuiteName);
        logger.info("========================================================");
    }

    @AfterClass
    static public void tearDownSuit() {
        logger.info("========================================================");
        logger.info("Ending TestSuit: " + iTestSuiteName);
        logger.info("========================================================");
        logger.info("");
    }

    @Before
    public void setUpTc() {
        logger.info("-----------------------");
        logger.info("----- TestCase[START]: " + iTestCaseName);
        
        // Prepare needed paths
        logger.debug("Preparing test directory");
        tmpPath = SystemOperations.getCleanTestTmpPath();
    }

    @After
    public void tearDownTc() {
        closeAllImageJimages();
        
        logger.info("----- TestCase[END]:   " + iTestCaseName);
        logger.info("-----------------------");
        logger.info("");
    }

    protected void testPlugin(PlugInFilter aTestedPlugin,
            final String aTcDirName, final String aMacroOptions,
            final String aSetupString, final String aInputFile,
            final String[] aExpectedImgFiles, final String[] aReferenceImgFiles) {
        testPlugin(aTestedPlugin, aTcDirName, aMacroOptions, aSetupString,
                aInputFile,  aExpectedImgFiles, aReferenceImgFiles, null, null);
    }
    /**
     * Tests plugin (one image input, one image output)
     * @param aTestedPlugin Tested plugin
     * @param aTcDirName Directory name with input files
     * @param aMacroOptions Macro options if needed (to not show GUI elements)
     * @param aSetupArg Setup string passed to plugin
     * @param aExpectedImgFiles Expected output files (WindowManager)
     * @param aReferenceImgFiles Reference files used to compare values
     * @param aInputFiles Input files used to test plugin
     */
    protected void testPlugin(PlugInFilter aTestedPlugin,
            final String aTcDirName,
            final String aMacroOptions,
            final String aSetupArg,
            final String aInputFile,
            final String[] aExpectedImgFiles,
            final String[] aReferenceImgFiles,
            final String[] aExpectedFiles,
            final String[] aReferenceFiles) {

        // ===================  Prepare plugin env. =================================
        tcPath = SystemOperations.getTestDataPath() + aTcDirName;
        
        copyTestResources(aInputFile, tcPath, tmpPath);

        // Make IJ running in batch mode (no GUI)
        Interpreter.batchMode = true;

        // ===================  Test plugin =========================================
        // options to plugin
        Thread.currentThread().setName("Run$_" + aTestedPlugin.getClass().getSimpleName());
        Macro.setOptions(Thread.currentThread(), aMacroOptions);

        // Set active image and run plugin
        if (aInputFile != null) {
            logger.debug("Loading image for testing: [" + tmpPath + aInputFile + "]");
            final ImagePlus ip = loadImagePlus(tmpPath + aInputFile);
            logger.debug("Testing plugin");
            WindowManager.setTempCurrentImage(ip);
        }
        // Change name of thread to begin with "Run$_". This is required by IJ to pass later
        new PlugInFilterRunner(aTestedPlugin, "pluginTest", aSetupArg);
        
        // ===================  Verify plugin output================================
        // Show what images are opened in ImageJ internal structures.
        debugOutput();
        
        // compare output from plugin with reference images
        for (int i = 0; i < aExpectedImgFiles.length; ++i) {
            String refFile = tcPath + aReferenceImgFiles[i];
            String testFile = aExpectedImgFiles[i];
            compareImageFromIJ(refFile, testFile);
        }

        if (aExpectedFiles != null && aReferenceFiles != null) {
            for (int i = 0; i < aExpectedFiles.length; ++i) {
                String refFile = tcPath + aReferenceFiles[i];
                String testFile = tmpPath + aExpectedFiles[i];
                compareTextFiles(refFile, testFile);
            }
        }
    }

    /**
     * Compare two images, tested one will be read from IJ by its name (it is usually image generated by plugin)
     * @param aReferenceFileName - absolute path to reference file
     * @param aGeneratedImageWindowName - name of window containing tested image
     */
    private void compareImageFromIJ(String aReferenceFileName, String aGeneratedImageWindowName) {
        logger.debug("Comparing output of two images:");
        logger.debug("    ref: [" + aReferenceFileName + "]");
        logger.debug("    test:[" + aGeneratedImageWindowName +"]");
        final Img<?> referenceImg = loadImage(aReferenceFileName);
        Img<?> processedImg = loadImageByName(aGeneratedImageWindowName);
        if (processedImg == null) {
            processedImg = loadImage(tmpPath + aGeneratedImageWindowName);
        }
        if (processedImg == null) {
            throw new RuntimeException("No img: [" + aGeneratedImageWindowName + "]");
        }
        assertTrue("Reference vs. processed file.", compareImages(referenceImg, processedImg));
    }

    /**
     * Compare two text files.
     * @param refFile - absolute path to reference file
     * @param testFile - absolute path to tested file
     */
    private void compareTextFiles(String refFile, String testFile) {
        logger.debug("Comparing output of two text files:");
        logger.debug("    ref: [" + refFile + "]");
        logger.debug("    test:[" + testFile + "]");
        String expected = readFile(refFile);
        String result = readFile(testFile);
        assertEquals(expected, result);
    }

    /**
     * Copies aInputFileOrDirectory from inputPath to given destinationPath
     * @param aInputFileOrDirectory input file or directory
     * @param aInputPath source test case path
     * @param aDestinationPath destination temporary path
     */
    protected void copyTestResources(final String aInputFileOrDirectory, final String aInputPath, final String aDestinationPath) {
        if (aInputFileOrDirectory != null) {
            logger.debug("Copy [" + aInputPath + aInputFileOrDirectory + "] to [" + aDestinationPath + "]");
            final File in = new File(aInputPath + aInputFileOrDirectory);
            final File out = new File(aDestinationPath);
            if (in.isDirectory()) {
                SystemOperations.copyDirectoryToDirectory(in, out);
            }
            else {
                SystemOperations.copyFileToDirectory(in, out);
            }
            
        }
    }

    /**
     * Compare two imglib2 images
     * @param aRefImg Image1
     * @param aTestedImg Image2
     * @return true if they match, false otherwise
     */
    protected boolean compareImages(Img<?> aRefImg, Img<?> aTestedImg) {
        final Cursor<?> ci1 = aRefImg.cursor();
        final RandomAccess<?> ci2 = aTestedImg.randomAccess();

        final int loc[] = new int[aRefImg.numDimensions()];
        int countAll = 0;
        int countDifferent = 0;
        boolean firstDiffFound = false;

        while (ci1.hasNext())
        {
            ci1.fwd();
            ci1.localize(loc);
            ci2.setPosition(loc);

            final Object t1 = ci1.get();
            final Object t2 = ci2.get();

            countAll++;

            if (!t1.equals(t2))
            {
                countDifferent++;
                if (!firstDiffFound) {
                    firstDiffFound = true;

                    // Produce error log with coordinates and values of first
                    // not matching location
                    String errorMsg = "Images differ. First occurence at: [";
                    for (int i = 0; i < aRefImg.numDimensions(); ++i) {
                        errorMsg += loc[i];
                        if (i < aRefImg.numDimensions() - 1) {
                            errorMsg += ",";
                        }
                    }
                    errorMsg += "] Values: [" + t1 + "] vs. [" + t2 + "]";
                    logger.error(errorMsg);
                }
            }
        }

        if (firstDiffFound) {
            logger.error("Different pixels / all pixels: [" + countDifferent + "/" + countAll + "]");
            return false;
        }

        return true;
    }

    /**
     * Gets image basing on its name from IJ's WindowManager
     * @param aName Name of image opened/showed in plugin
     * @return ImagePlus or null if there is no such image
     */
    protected ImagePlus getImageByName(String aName) {
        final int[] ids = WindowManager.getIDList();
        if (ids != null) {
            for (final int id : ids) {
                final ImagePlus img = WindowManager.getImage(id);
                if (img.getTitle().equals(aName)) {
                    return img;
                }
            }
        }

        return null;
    }

    /**
     * Open image
     *
     * @param aFileName Fully qualified image name
     * @return ImagePlus
     */

    protected ImagePlus loadImagePlus(String aFileName)
    {
        return IJ.openImage(aFileName);
    }

    /**
     * Loads image in imglib2 format. Since some information about type is lost
     * when converting image via imglib2 adapters the chosen technique is to save
     * ImagePlus and open it again.
     * @param aImageName Name of image opened/showed in plugin
     * @return Img or null if there is no such image
     */
    protected Img<?> loadImageByName(String aImageName) {
        logger.debug("Loading image from IJ WindowManager: [" + aImageName + "]");
        final String outputFileName = SystemOperations.getTestTmpPath() + aImageName + ".tif";
        final ImagePlus imageByName = getImageByName(aImageName);
        if (imageByName != null) {
            logger.debug("Saving [" + aImageName + "] as [" + outputFileName + "]");
            IJ.saveAsTiff(imageByName, outputFileName);
            final Img<?> img = loadImage(outputFileName);

            return img;
        }
        return null;
    }

    /**
     * Loads image
     * @param aFileName Fully qualified image name
     * @return Img
     */
    protected Img<?> loadImage(String aFileName) {
        if (iImgOpener == null) {
            // Create ImgOpener with some default context, without it, it search for already existing one
            iImgOpener = new ImgOpener(new Context(SCIFIOService.class, AppService.class, StatusService.class ));
            // By default ImgOpener produces a lot of logs, this is one of the ways to switch it off.
            iImgOpener.log().setLevel(0);
        }

        try {
            logger.debug("Opening file: [" + aFileName + "]");
            final Img<?> img = iImgOpener.openImgs(aFileName).get(0);
            return img;
        } catch (final ImgIOException e) {
            e.printStackTrace();
            final String errorMsg = "Failed to load: [" + aFileName + "]";
            logger.error(errorMsg);
            fail(errorMsg);
        }

        return null;
    }

    /**
     * @return List of all images opened in ImageJ (not necessarily visible)
     */
    protected List<String> getAllImagesByName() {
        final List<String> result = new ArrayList<String>();
        final int[] ids = WindowManager.getIDList();
        if (ids != null) {
            for (final int id : ids) {
                logger.debug("WindowsManager picture id: " + id + " Name: [" + WindowManager.getImage(id).getTitle() + "]");
                final ImagePlus img = WindowManager.getImage(id);
                result.add(img.getTitle());
            }
        }

        return result;
    }

    protected void closeAllImageJimages() {
        ImagePlus img;
        while ((img = WindowManager.getCurrentImage()) != null) {
            logger.debug("Closing [" + img.getTitle() + "]");
            img.close();
        }
    }
    /**
     * Logs images available in IJ internal structures. Helpful during new TC writing.
     */
    protected void debugOutput() {
        logger.debug("getWindowCount(): " + WindowManager.getWindowCount());
        logger.debug("getBatchModeImageCount(): " + Interpreter.getBatchModeImageCount());
        getAllImagesByName();
    }
    
    protected static String readFile(String aFullPathFile) {
        try {
            final byte[] encoded = Files.readAllBytes(Paths.get(aFullPathFile));
            return new String(encoded, Charset.defaultCharset());
        } catch (final IOException e) {
            e.printStackTrace();
            fail("Reading [" + aFullPathFile + "] file failed.");
        }
        return null;
    }
}
