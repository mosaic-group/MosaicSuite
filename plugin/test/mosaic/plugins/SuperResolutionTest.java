package mosaic.plugins;

import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class SuperResolutionTest extends CommonBase {

    @Test
    public void testTvSplitGrey8()  {
        // Define test data
        String tcDirName          = "VCF/";
        String setupString        = "run";
        String macroOptions       = "filter=TV method=Split number=20";
        String[] inputFiles       = {"lenaSmall.tif"};
        String[] expectedFiles    = {"resized_lenaSmall.tif"};
        String[] referenceFiles   = {"resizedLenaSmall.tif"};
        
        // Create tested plugIn
        SuperResolution nt = new SuperResolution();
       
        // Test it
        testPlugin(nt, tcDirName, 
                   inputFiles, expectedFiles, referenceFiles, 
                   setupString, macroOptions);

    }

}
