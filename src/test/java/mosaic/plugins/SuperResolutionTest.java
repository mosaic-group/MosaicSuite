package mosaic.plugins;

import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class SuperResolutionTest extends CommonBase {

    @Test
    public void testTvSplitGrey8()  {
        // Define test data
        final String tcDirName          = "VCF/";
        final String setupString        = "run";
        final String macroOptions       = "filter=[TV (Total Variation)] method=Split number=20";
        final String inputFile          = "lenaSmall.tif";
        final String[] expectedFiles    = {"resized_lenaSmall.tif"};
        final String[] referenceFiles   = {"resizedLenaSmall.tif"};

        // Create tested plugIn
        final SuperResolution nt = new SuperResolution();

        // Test it
        testPlugin(nt, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedFiles, referenceFiles);
    }

}
