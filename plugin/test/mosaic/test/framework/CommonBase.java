package mosaic.test.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import io.scif.SCIFIOService;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.scijava.Context;
import org.scijava.app.AppService;
import org.scijava.app.StatusService;


/**
 * Common stuff for MOSAIC suit testing
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
@Ignore
public class CommonBase extends Info {

    protected static final Logger logger = Logger.getLogger(CommonBase.class);
    private static ImgOpener iImgOpener = null;
    
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
    }
    
    @After
    public void tearDownTc() {
        //
        logger.info("----- TestCase[END]:   " + iTestCaseName);
        logger.info("-----------------------");
        logger.info("");
    }
    
    /**
     * Tests plugin (one image input, one image output)
     * TODO: this method should be refactored to handle many-to-many cases
     *       (for example 1 img input -> 3 img output or opposite)
     * @param aTestedPlugin Tested plugin
     * @param aTcDirName Directory name with input files
     * @param aInputFiles Input files used to test plugin
     * @param aExpectedFiles Expected output files (WindowManager)
     * @param aReferenceFiles Reference files used to compare values
     * @param aSetupString Setup string passed to plugin
     * @param aExpectedSetupRetValue Expected setup(..) method output from plugin
     */
    protected void testPlugin(PlugInFilter aTestedPlugin, 
                              final String aTcDirName,
                              final String[] aInputFiles,
                              final String[] aExpectedFiles,
                              final String[] aReferenceFiles,
                              final String aSetupString,
                              final int aExpectedSetupRetValue) {
        // TODO: extend test base functionality
        if (aInputFiles.length != 1 || aExpectedFiles.length != 1 || aReferenceFiles.length != 1) {
            logger.error("Wrong lenght of input file's array");
            fail("Wrong test data");
        }
        
        // Prepare needed paths
        final String tmpPath = SystemOperations.getCleanTestTmpPath();
        final String tcPath = SystemOperations.getTestDataPath() + aTcDirName;
        
        logger.debug("Preparing test directory");
        prepareTestDirectory(aInputFiles, tcPath, tmpPath);
        
        // Make it running in batch mode (no GUI)
        Interpreter.batchMode = true;
        
        // Test plugin
        for (String file : aInputFiles) {
            logger.debug("Loading image for testing: [" + tmpPath + file + "]");
            ImagePlus ip = loadImagePlus(tmpPath + file);
            logger.debug("Testing plugin");
            assertEquals("Setup result.", aExpectedSetupRetValue, aTestedPlugin.setup(aSetupString, ip));
            aTestedPlugin.run(null);
            
            // compare output from plugin with reference images
            logger.debug("Comparing output of two images:");
            logger.debug("    ref: [" + tcPath + aReferenceFiles[0] + "]");
            logger.debug("    test:[" + aExpectedFiles[0] +"]");
            Img<?> referenceImg = loadImage(tcPath + aReferenceFiles[0]);
            Img<?> processedImg = loadImageByName(aExpectedFiles[0]);
            
            assertTrue("Reference vs. processed file.", compare(referenceImg, processedImg));
        }
    }
    
    /**
     * Tests plugin (one image input, one image output)
     * TODO: this method should be refactored to handle many-to-many cases
     *       (for example 1 img input -> 3 img output or opposite)
     * @param aTestedPlugin Tested plugin
     * @param aTcDirName Directory name with input files
     * @param aInputFiles Input files used to test plugin
     * @param aExpectedFiles Expected output files (WindowManager)
     * @param aReferenceFiles Reference files used to compare values
     * @param aSetupString Setup string passed to plugin
     * @param aExpectedSetupRetValue Expected setup(..) method output from plugin
     */
    protected void testPlugin2(PlugInFilter aTestedPlugin, 
                              final String aTcDirName,
                              final String[] aInputFiles,
                              final String[] aExpectedFiles,
                              final String[] aReferenceFiles,
                              final String aSetupString,
                              final int aExpectedSetupRetValue) {
        // TODO: extend test base functionality
        if (aInputFiles.length != 1 || aExpectedFiles.length != 1 || aReferenceFiles.length != 1) {
            logger.error("Wrong lenght of input file's array");
            fail("Wrong test data");
        }
        
        // Prepare needed paths
        final String tmpPath = SystemOperations.getCleanTestTmpPath();
        final String tcPath = SystemOperations.getTestDataPath() + aTcDirName;
        
        logger.debug("Preparing test directory");
        prepareTestDirectory(aInputFiles, tcPath, tmpPath);
        
        // Make it running in batch mode (no GUI)
        Interpreter.batchMode = true;
        
        // Test plugin
        for (String file : aInputFiles) {
            logger.debug("Loading image for testing: [" + tmpPath + file + "]");
            ImagePlus ip = loadImagePlus(tmpPath + file);
            logger.debug("Testing plugin");
            assertEquals("Setup result.", aExpectedSetupRetValue, aTestedPlugin.setup(aSetupString, ip));
            
            FloatProcessor fp = new FloatProcessor(ip.getWidth(),ip.getHeight());
            ImageProcessor iproc = ip.getProcessor();
            fp = iproc.toFloat(0, fp);
            aTestedPlugin.run(fp);
            iproc.setPixels(0, fp);
            assertEquals("Final setup result.", aExpectedSetupRetValue, aTestedPlugin.setup("final", ip));
            
            // compare output from plugin with reference images
            logger.debug("Comparing output of two images:");
            logger.debug("    ref: [" + tcPath + aReferenceFiles[0] + "]");
            logger.debug("    test:[" + aExpectedFiles[0] +"]");
            Img<?> referenceImg = loadImage(tcPath + aReferenceFiles[0]);
            Img<?> processedImg = loadImageByName(aExpectedFiles[0]);
            
            assertTrue("Reference vs. processed file.", compare(referenceImg, processedImg));
        }
    }
    /**
     * Copies aInputFiles from test case data directory to temporary path
     * @param aInputFiles array of input files
     * @param aTcPath source test case path
     * @param aTmpPath destination temporary path
     */
    protected void prepareTestDirectory(final String[] aInputFiles, final String aTcPath, final String aTmpPath) {
        
        // Copy input file to temporary directory
        for (String file : aInputFiles) {
            File in = new File(aTcPath + file);
            File out = new File(aTmpPath);
            SystemOperations.copyFileToDirectory(in, out);
        }
    }
    
    /**
     * Compare two imglib2 images
     * @param aRefImg Image1
     * @param aTestedImg Image2
     * @return true if they match, false otherwise
     */
    protected boolean compare(Img<?> aRefImg, Img<?> aTestedImg)
    {

        Cursor<?> ci1 = aRefImg.cursor();
        RandomAccess<?> ci2 = aTestedImg.randomAccess();
        
        int loc[] = new int[aRefImg.numDimensions()];
        
        while (ci1.hasNext())
        {
            ci1.fwd();
            ci1.localize(loc);
            ci2.setPosition(loc);
            
            Object t1 = ci1.get();
            Object t2 = ci2.get();
            
            if (!t1.equals(t2))
            {
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
                
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets image basing on its name from IJ's WindowManager
     * @param aName Name of image opened/showed in plugin
     * @return ImagePlus or null if there is no such image
     */
    protected ImagePlus getImageByName(String aName) {
        int[] ids = WindowManager.getIDList();
        if (ids != null) {
            for (int id : ids) {
                ImagePlus img = WindowManager.getImage(id);
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
        String outputFileName = SystemOperations.getTestTmpPath() + aImageName + ".tif";
        ImagePlus imageByName = getImageByName(aImageName);
        if (imageByName != null) {
            logger.debug("Saving [" + aImageName + "] as [" + outputFileName + "]");
            IJ.saveAsTiff(imageByName, outputFileName);
            Img<?> img = loadImage(outputFileName);

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
            Img<?> img = (Img<?>) iImgOpener.openImgs(aFileName).get(0);
            return img;
        } catch (ImgIOException e) {
            e.printStackTrace();
            String errorMsg = "Failed to load: [" + aFileName + "]";
            logger.error(errorMsg);
            fail(errorMsg);
        }
        
        return null;
    }
    
    /**
     * @return List of all images opened in ImageJ (not necessarily visible)
     */
    protected List<String> getAllImagesByName() {
        List<String> result = new ArrayList<String>();
        int[] ids = WindowManager.getIDList();
        if (ids != null) {
            for (int id : ids) {
                logger.debug("WindowsManager picture id: " + id + " Name: [" + WindowManager.getImage(id).getTitle() + "]");
                ImagePlus img = WindowManager.getImage(id);
                result.add(img.getTitle());
            }
        }
        
        return result;
    }
    
    /**
     * Logs images available in IJ internal structures. Helpful during new TC writing.
     */
    protected void debugOutput() {    
      logger.debug("getWindowCount(): " + WindowManager.getWindowCount());
      logger.debug("getBatchModeImageCount(): " + Interpreter.getBatchModeImageCount());
      getAllImagesByName();
    }
}
