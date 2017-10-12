package mosaic.test.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import mosaic.utils.SysOps;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;


/**
 * Common stuff for MOSAIC suit testing
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
/**
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 *
 */
@Ignore
public class CommonBase extends Info {

    private static final Logger logger = Logger.getLogger(CommonBase.class);
    
    // TODO: Commented out until "protected Img<?> loadImage(String aFileName)" is fixed
//    private static ImgOpener iImgOpener = null;
    
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
        tmpPath = getCleanTestTmpPath();
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
        tcPath = getTestDataPath() + aTcDirName;
        
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
        printInformationAboutOpenWindowsInIj();
        
        // compare output from plugin with reference images
        if (aExpectedImgFiles != null && aReferenceImgFiles != null) {
            for (int i = 0; i < aExpectedImgFiles.length; ++i) {
                String refFile = tcPath + aReferenceImgFiles[i];
                String testFile = aExpectedImgFiles[i];
                compareImageFromIJ(refFile, testFile);
            }
        }
        if (aExpectedFiles != null && aReferenceFiles != null) {
            for (int i = 0; i < aExpectedFiles.length; ++i) {
                String refFile = tcPath + aReferenceFiles[i];
                String testFile = tmpPath + aExpectedFiles[i];
                String extension = FilenameUtils.getExtension(refFile);
                if (extension.equals("csv")) { 
                    compareCsvFiles(refFile, testFile);
                }
                else {
                    compareTextFiles(refFile, testFile);
                }
            }
        }
    }

    /**
     * Compare two images, tested one will be read from IJ by its name (it is usually image generated by plugin)
     * @param aReferenceFileName - absolute path to reference file
     * @param aGeneratedImageWindowName - name of window containing tested image
     */
    protected void compareImageFromIJ(String aReferenceFileName, String aGeneratedImageWindowName) {
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
        logger.debug("Files match!");
    }

    /**
     * Compare two text files.
     * @param refFile - absolute path to reference file
     * @param testFile - absolute path to tested file
     */
    protected void compareTextFiles(String refFile, String testFile) {
        logger.debug("Comparing output of two text files:");
        logger.debug("    ref: [" + refFile + "]");
        logger.debug("    test:[" + testFile + "]");
        logger.debug(" cp " + testFile + " " + refFile);
        String expected = readFile(refFile);
        String result = readFile(testFile);
        assertEquals("Files differ!", expected, result);
        logger.debug("Files match!");
    }

    protected void compareCsvFiles(String refFile, String testFile) {
        logger.debug("Comparing output of two CSV files:");
        logger.debug("    ref: [" + refFile + "]");
        logger.debug("    test:[" + testFile +"]");
        logger.debug(" cp " + testFile + " " + refFile);
        try {
            List<String> ref = readLines(refFile);
            List<String> test = readLines(testFile);
            if (ref.size() != test.size()) {
                fail("Size of compared CVS files are different!");
            }
            for (int i = 0; i < ref.size(); ++i) {
                if (ref.get(i).startsWith("%background:")) continue;
                assertEquals(ref.get(i), test.get(i));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Exception!");
        }
        logger.debug("Files match!");
    }
    
    protected List<String> readLines(String aFileName) throws IOException {
        ArrayList<String> lines = new ArrayList<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(aFileName))) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
               lines.add(line);
            }
        }
        return lines;
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
                SysOps.copyDirectoryToDirectory(in, out);
            }
            else {
                SysOps.copyFileToDirectory(in, out);
            }
        }
    }

    /**
     * Compare two imglib2 images
     * @param aRefImg Image1
     * @param aTestedImg Image2
     * @return true if they match, false otherwise
     */
    protected boolean compareImages(IterableInterval<?> aRefImg, RandomAccessible<?> aTestedImg) {
        final Cursor<?> ci1 = aRefImg.localizingCursor();
        final RandomAccess<?> ci2 = aTestedImg.randomAccess();

        int countAll = 0;
        int countDifferent = 0;
        boolean firstDiffFound = false;
        while (ci1.hasNext())
        {
            ci1.fwd();
            ci2.setPosition(ci1);
            final Object t1 = ci1.get();
            final Object t2 = ci2.get();

            countAll++;

            // Currently compares strings..., it seems that t1.equals(t2) fails for types extended from AbstractNativeType in 
            // imglib2 since it does not override equals method and compare just references.
            if (!t2.toString().equals(t1.toString()))
            {
                countDifferent++;
                if (!firstDiffFound) {
                    firstDiffFound = true;
                    // Produce error log with coordinates and values of first
                    // not matching location
                    logger.debug(t1.getClass() + " vs " + t2.getClass());
                    String errorMsg = "Images differ. First occurence at: [";
                    for (int i = 0; i < aRefImg.numDimensions(); ++i) {
                        errorMsg += ci1.getIntPosition(i);
                        if (i < aRefImg.numDimensions() - 1) {
                            errorMsg += ",";
                        }
                    }
                    errorMsg += "] Values (orig vs. test): [" + t1 + "] vs. [" + t2 + "]";
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
        final String outputFileName = getTestTmpPath() + aImageName + ".savedForTest.tif";
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
        logger.debug("Opening file: [" + aFileName + "]");
        ImagePlus ip = loadImagePlus(aFileName);
        if (ip == null) {
            logger.error("Failed to load: [" + aFileName + "]");
            fail();
        }
        return ImagePlusAdapter.wrap(ip);
        
        // Code below is intentionally commented out. It seems that ImgOpener works well on regular files (tiff.. ) but 
        // when it comes to zip'ed tif files (*.zip) it is extremally slow. For image 1000x1000 it takes >> 10s to open which is not a case 
        // when the same file is unzipped.
        // TODO: It should be checked with new versions of imglib how it behaves and reverted.
        //       Until that time legacy opener with imglib2 wrapper is good enough.
        
//        if (iImgOpener == null) {
//            // Create ImgOpener with some default context, without it, it search for already existing one
//            iImgOpener = new ImgOpener(new Context(SCIFIOService.class, AppService.class, StatusService.class ));
//            // By default ImgOpener produces a lot of logs, this is one of the ways to switch it off.
//            iImgOpener.log().setLevel(0);
//        }
//
//        try {
//            logger.debug("Opening file: [" + aFileName + "]");
//            final Img<?> img = iImgOpener.openImgs(aFileName).get(0);
//            return img;
//        } catch (final ImgIOException e) {
//            e.printStackTrace();
//            final String errorMsg = "Failed to load: [" + aFileName + "]";
//            logger.error(errorMsg);
//            fail(errorMsg);
//        }
//
//        return null;
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
    protected void printInformationAboutOpenWindowsInIj() {
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
    
    /**
     * Returns test data path.
     * @return Absolute path to test data
     *
     */
    static public String getTestDataPath() {
        final String dataPathEnvVar = "MOSAIC_PLUGIN_TEST_DATA_PATH";
        final String path = System.getenv(dataPathEnvVar);

        if (path == null || path.equals("")) {
            // Throw and stop execution of test intentionally. It is easier to
            // debug if test will stop here.
            throw new RuntimeException("Environment variable " + dataPathEnvVar + " is not defined! It should point to Jtest_data in plugin source.");
        }

        return path + SysOps.SEPARATOR;
    }
    
    /**
     * Returns temporary test path which should be used during tests execution.
     * @return Absolute path to temporary test data.
     */
    public static String getTestTmpPath() {
        final String TEST_TMP_DIR = "test";
        return SysOps.getTmpPath() + TEST_TMP_DIR + SysOps.SEPARATOR;
    }

    /**
     * Returns prepared (empty) temporary test path which should be used
     * during tests execution.
     *
     * @return Absolute path to temporary test data.
     */
    public static String getCleanTestTmpPath() {
        removeTestTmpDir();
        createTestTmpDir();

        return getTestTmpPath();
    }
    
    /**
     * Removes test temporary directory. If any problem arise it
     * will throw and break an execution of test.
     */
    public static void removeTestTmpDir() {
        SysOps.removeDir(getTestTmpPath());

    }

    /**
     * Creates test temporary directory. If any problem arise it
     * will throw and break an execution of test.
     */
    private static void createTestTmpDir() {
        SysOps.createDir(getTestTmpPath());
    }
    
    /**
     * Find for a job file "aName" in a parent directory "aDir". It searches through all files in subdirectories starting from "Job" name.
     * @return found file or null otherwise
     */
    protected File findJobFile(String aName, File aDir) {
        File[] fileList = aDir.listFiles();
        for (File f : fileList) {
            if (f.isDirectory() && f.getName().substring(0, 3).equals("Job")) { 
                Collection<File> lf = FileUtils.listFiles(f, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
                for (File c : lf) {
                    if (c.getAbsolutePath().endsWith(aName)) return c;
                }
            }
        }
        return null;
    }
    
//    static protected void compareArrays(Object[] expected, Object[] result) {
//        assertTrue(Arrays.deepEquals(expected, result));
//    }
    
    /**
     * Compares 2D boolean arrays with nice output of diff element (if such is found). When arrays diff execution of
     * test is stopped (asserEquals is used)
     */
    static protected void compareArrays(boolean[][] expected, boolean[][] result) {
        for (int i = 0; i < expected.length; ++i) {
            for (int j = 0; j < expected[0].length; ++j) {
                assertEquals("Elements at [" + i + "][" + j + "] should be same", expected[i][j], result[i][j]);
            }
        }
    }
    
    /**
     * Compares 2D float arrays with nice output of diff element (if such is found). When arrays diff execution of
     * test is stopped (asserEquals is used)
     */
    static protected void compareArrays(float[][] expected, float[][] result) {
        for (int i = 0; i < expected.length; ++i) {
            for (int j = 0; j < expected[0].length; ++j) {
                assertEquals("Elements at [" + i + "][" + j + "] should be same", expected[i][j], result[i][j], 0.0f);
            }
        }
    }
    
    /**
     * Compares 3D float arrays with nice output of diff element (if such is found). When arrays diff execution of
     * test is stopped (asserEquals is used)
     */
    static protected void compareArrays(float[][][] expected, float[][][] result, float epsilon) {
        for (int z = 0; z < expected.length; ++z) {
            for (int i = 0; i < expected[0].length; ++i) {
                for (int j = 0; j < expected[0][0].length; ++j) {
                    assertEquals("Elements at [" + z + "][" + i + "][" + j + "] should be same", expected[z][i][j], result[z][i][j], epsilon);
                }
            }
        }
    }

    /**
     * Compares 2D double arrays with nice output of diff element (if such is found). When arrays diff execution of
     * test is stopped (asserEquals is used)
     */
    static protected void compareArrays(double[][] expected, double[][] result) {
        for (int i = 0; i < expected.length; ++i) {
            for (int j = 0; j < expected[0].length; ++j) {
                assertEquals("Elements at [" + i + "][" + j + "] should be same", expected[i][j], result[i][j], 0.0f);
            }
        }
    }
    
    /**
     * Compares 3D double arrays with nice output of diff element (if such is found). When arrays diff execution of
     * test is stopped (asserEquals is used)
     */
    static protected void compareArrays(double[][][] expected, double[][][] result) {
        for (int z = 0; z < expected.length; ++z) {
            for (int i = 0; i < expected[0].length; ++i) {
                for (int j = 0; j < expected[0][0].length; ++j) {
                    assertEquals("Elements at [" + z + "][" + i + "][" + j + "] should be same", expected[z][i][j], result[z][i][j], 0.0f);
                }
            }
        }
    }
    
    static protected void printArray(double[][][] aExpected, int aPrecision) {
        for (int z = 0; z < aExpected.length; ++z) {
            for (int i = 0; i < aExpected[0].length; ++i) {
                for (int j = 0; j < aExpected[0][0].length; ++j) {
                    System.out.print("\t" + IJ.d2s(aExpected[z][i][j], aPrecision));
                }
                System.out.println();
            }
            System.out.println();
        }
    }

    static protected void printArray(double[][] aArray, int aPrecision) {
        for (int i = 0; i < aArray.length; ++i) {
            for (int j = 0; j < aArray[0].length; ++j) {
                System.out.print("\t" + IJ.d2s(aArray[i][j], aPrecision));
            }
            System.out.println();
        }
        System.out.println();
    }

    static protected void sleep(int aMilliseconds) {
        try {
            Thread.sleep(aMilliseconds);
        }
        catch (InterruptedException e) {
            // Nothing to do
        }
    }
    
}
