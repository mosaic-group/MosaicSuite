package mosaic.plugins;

import static org.junit.Assert.*;
import mosaic.test.framework.CommonBase;
import mosaic.test.framework.SystemOperations;

import org.junit.Test;

public class InpaintingTest extends CommonBase  {

    @Test
    public void testTvNoSplitGrey8()  {
        // Define test data
        String tcDirName          = "VCF/";
        String setupString        = "run";
        final String tcPath = SystemOperations.getTestDataPath() + tcDirName;
        final String maskFile=tcPath + "inpaint_mask.png"; 
        String macroOptions       = "(inpainting)=" + maskFile + " filter=TV number=20";
        String[] inputFiles       = {"inpaint.png"};
        String[] expectedFiles    = {"inpainting_inpaint.png"};
        String[] referenceFiles   = {"inpaintTvNoSplit20.tif"};
        
        // Create tested plugin
        Inpainting nt = new Inpainting();
       
        // Test it
        testPlugin(nt, tcDirName, 
                    inputFiles, expectedFiles, referenceFiles, 
                    setupString, macroOptions);

    }

}
