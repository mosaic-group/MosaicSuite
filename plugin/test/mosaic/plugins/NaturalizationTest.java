package mosaic.plugins;

import static org.junit.Assert.*;
import ij.WindowManager;
import ij.macro.Interpreter;
import mosaic.core.ipc.NoCSV;
import mosaic.core.utils.MosaicTest;
import mosaic.test.framework.CommonTestBase;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NaturalizationTest extends CommonTestBase {
    private static final Logger logger = LoggerFactory.getLogger(NaturalizationTest.class);
  
    @Test
    public void test() {
        Naturalization nt = new Naturalization();
        CommonTestBase.<NoCSV>testPlugin(nt,"Naturalization",NoCSV.class);
        System.out.println("===== after ===");
        logger.debug("windowcount: " + WindowManager.getWindowCount());
        logger.debug("Interpreter: " + Interpreter.getBatchModeImageCount());
//      
        int [] ids = WindowManager.getIDList();
        if (ids != null)
        for (int id : ids) {
            logger.info("Filename: id=[" + id + "] name=[" + WindowManager.getImage(id).getTitle() + "]");
            
        }
    }

}
