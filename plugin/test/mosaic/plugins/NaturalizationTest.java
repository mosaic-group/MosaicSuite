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
import mosaic.test.framework.CommonTestBase;
import net.imglib2.img.Img;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NaturalizationTest extends CommonTestBase {
    private static final Logger logger = LoggerFactory.getLogger(NaturalizationTest.class);
  
    @Test
    public void test() {
        Naturalization nt = new Naturalization();
        String[] inputFiles = {"asf", "fffff"};
        CommonTestBase.<NoCSV>testPlugin(nt,"Naturalization",NoCSV.class);
        
        List<ImgTest> it = MosaicTest.getTestData("Naturalization");

        ImgTest tmp = it.get(0);

        for (String rs : tmp.result_imgs) {
            Img<?> image = null;
            Img<?> image_rs = null;
            
            try {
                image = loadImage(rs); 
                image_rs = loadImageByName("x.png_naturalized");
            }
            catch (java.lang.UnsupportedOperationException e)   {
                e.printStackTrace();
                fail("Error: Image " + rs + " does not match the result");
            }

            if (MosaicUtils.compare(image, image_rs) == false) {
                fail("Error: Image " + rs + " does not match the result");
            }
        }
        
    }

}
