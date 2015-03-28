package mosaic.plugins;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mosaic.core.ipc.NoCSV;
import mosaic.core.utils.ImgTest;
import mosaic.core.utils.MosaicTest;
import mosaic.core.utils.MosaicUtils;
import mosaic.test.framework.CommonBase;
import mosaic.test.framework.SystemOperations;
import net.imglib2.img.Img;

import org.junit.Test;


public class NaturalizationTest extends CommonBase {
    //private static final Logger logger = LoggerFactory.getLogger(NaturalizationTest.class);
  
    @Test
    public void test() {
//        // Define test data
//        String tcDir = "Naturalization/flower";
//        String testDir = SystemOperations.getTestDataPath();
//        String[] inputFiles = {"x.png"};
//        String[] referenceFiles = {"x_nat.tif"};
//        String[] expectedFiles = {"x.png_naturalized"};
//        
//        // Prepare test data
//        
//        
//        
//        // Create plugin
//        Naturalization nt = new Naturalization();
//        
//        
//        CommonTestBase.<NoCSV>testPlugin(nt,"Naturalization",NoCSV.class);
//        
//        List<ImgTest> it = MosaicTest.getTestData("Naturalization");
//
//        ImgTest tmp = it.get(0);
//
//        for (String rs : tmp.result_imgs) {
//            Img<?> image = null;
//            Img<?> image_rs = null;
//            
//            try {
//                image = loadImage(rs); 
//                image_rs = loadImageByName("x.png_naturalized");
//            }
//            catch (java.lang.UnsupportedOperationException e)   {
//                e.printStackTrace();
//                fail("Error: Image " + rs + " does not match the result");
//            }
//
//            if (MosaicUtils.compare(image, image_rs) == false) {
//                fail("Error: Image " + rs + " does not match the result");
//            }
//        }
        
    }
}
