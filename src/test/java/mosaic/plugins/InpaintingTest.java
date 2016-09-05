package mosaic.plugins;

import org.junit.Test;

import mosaic.test.framework.CommonBase;

public class InpaintingTest extends CommonBase  {

    @Test
    public void testTvNoSplitGrey8()  {
        // Define test data
        final String tcDirName          = "VCF/";
        final String setupString        = "run";
        final String tcPath = getTestDataPath() + tcDirName;
        final String maskFile = tcPath + "inpaint_mask.png";
        final String macroOptions       = "(inpainting)=" + maskFile + " filter=[TV (Total Variation)] number=20";
        final String inputFile          = "inpaint.png";
        final String[] expectedFiles    = {"inpainting_inpaint.png"};
        final String[] referenceFiles   = {"inpaintTvNoSplit20.tif"};

        // Create tested plugin
        final Inpainting nt = new Inpainting();

        // Test it
        testPlugin(nt, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedFiles, referenceFiles);
    }

}
