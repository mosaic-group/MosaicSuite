package mosaic.test.framework;

import static org.junit.Assert.fail;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import io.scif.SCIFIOService;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.utils.ImgTest;
import mosaic.core.utils.MosaicTest;
import mosaic.core.utils.MosaicUtils;
import mosaic.plugins.PlugInFilterExt;
import net.imglib2.img.Img;


//import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.scijava.Context;
import org.scijava.app.AppService;
import org.scijava.app.StatusService;



@Ignore
public class CommonBase {

    //protected static final Logger logger = Logger.getLogger(CommonBase.class);
    private static ImgOpener iImgOpener = null;
    
    /**
     * Test the plugin filter
     * 
     * @param BG plugins filter filter
     * @param testset String that indicate the test to use (all the test are in Jtest_data folder)
     * @param Class<T> Class for reading csv files used for InterPlugInCSV class
     */
    protected static <T extends ICSVGeneral> void testPlugin(PlugInFilterExt BG, String testset,Class<T> cls)
    {
        String tmp_dir = SystemOperations.getTestTmpPath();
        
        // Get all test images
        List<ImgTest> imgT =  new ArrayList<ImgTest>();
        imgT.addAll(Arrays.asList(MosaicUtils.getTestImages(testset)));
        
        if (imgT == null || imgT.size() == 0) {
//            logger.error("No test images found for testcase [" + testset + "])");
            fail("No Images to test");
            return;
        }
        
        for (ImgTest tmp : imgT)
        {
            //MosaicTest.prepareTestEnvironment(tmp);
            
            // Create a plugin filter
            int rt = 0;
            if (tmp.img.length == 1) {
                String temp_img = tmp_dir + tmp.img[0].substring(tmp.img[0].lastIndexOf(File.separator)+1);
                ImagePlus img = MosaicUtils.openImg(temp_img);

                // Make it running in batch mode.
                Interpreter.batchMode = true;
                
                rt = BG.setup(tmp.options, img);
                
//                logger.debug("windowcount: " + WindowManager.getWindowCount());
//                logger.debug("Interpreter: " + Interpreter.getBatchModeImageCount());
//              
                int [] ids = WindowManager.getIDList();
                if (ids != null)
                for (int id : ids) {
//                    logger.info("Filename: id=[" + id + "] name=[" + WindowManager.getImage(id).getTitle() + "]");
                    
                }
            }
            else {
                rt = BG.setup(tmp.options,null);
            }
            
            if (rt != tmp.setup_return) {
                fail("Setup error expecting: " + tmp.setup_return + " getting: " + rt);
            }
            
            // run the filter
            BG.run(null);

        }
    }
    
    /**
     * @return List of all images opened in ImageJ (not necessarily visible)
     */
    protected List<String> getAllImagesByName() {
        List<String> result = new ArrayList<String>();
        int[] ids = WindowManager.getIDList();
        if (ids != null) {
            for (int id : ids) {
                ImagePlus img = WindowManager.getImage(id);
                result.add(img.getTitle());
            }
        }
        
        return result;
    }
    
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
    
    protected Img<?> loadImageByName(String aImageName) {
        String outputFileName = SystemOperations.getTestTmpPath() + aImageName + ".tif";
        IJ.saveAsTiff(getImageByName(aImageName), outputFileName);
        Img<?> img = loadImage(outputFileName);
        return img;
    }
    
    protected Img<?> loadImage(String aFileName) {
        if (iImgOpener == null) {
            // Create ImgOpener with some default context, without it, it search for already existing one
            iImgOpener = new ImgOpener(new Context(SCIFIOService.class, AppService.class, StatusService.class ));
            // By default ImgOpener produces a lot of logs, this is one of the ways to switch it off. 
            iImgOpener.log().setLevel(0);
        }

        try {
            Img<?> img = (Img<?>) iImgOpener.openImgs(aFileName).get(0);
            return img;
        } catch (ImgIOException e) {
            e.printStackTrace();
            fail("Failed to load: [" + aFileName + "]");
        }
        
        return null;
    }
}
