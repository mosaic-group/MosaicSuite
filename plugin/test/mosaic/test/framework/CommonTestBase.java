package mosaic.test.framework;

import static org.junit.Assert.fail;
import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;

import java.io.File;
import java.util.List;

import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.utils.ImgTest;
import mosaic.core.utils.MosaicTest;
import mosaic.core.utils.MosaicUtils;
import mosaic.plugins.PlugInFilterExt;

import org.junit.Ignore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class CommonTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CommonTestBase.class);
    
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
        List<ImgTest> imgT = MosaicTest.getTestData(testset);
        
        if (imgT == null || imgT.size() == 0) {
            logger.error("No test images found for testcase [" + testset + "])");
            fail("No Images to test");
            return;
        }
        
        for (ImgTest tmp : imgT)
        {
            MosaicTest.prepareTestEnvironment(tmp);
            
            // Create a plugin filter
            int rt = 0;
            if (tmp.img.length == 1) {
                String temp_img = tmp_dir + tmp.img[0].substring(tmp.img[0].lastIndexOf(File.separator)+1);
                ImagePlus img = MosaicUtils.openImg(temp_img);

                // TODO: to be decided what method (whether both?) should be used
                Interpreter.batchMode = true;
                tmp.options += "TEST";
                
                rt = BG.setup(tmp.options, img);
                
                logger.debug("windowcount: " + WindowManager.getWindowCount());
                logger.debug("Interpreter: " + Interpreter.getBatchModeImageCount());
//              
                int [] ids = WindowManager.getIDList();
                if (ids != null)
                for (int id : ids) {
                    logger.info("Filename: id=[" + id + "] name=[" + WindowManager.getImage(id).getTitle() + "]");
                    
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
}
