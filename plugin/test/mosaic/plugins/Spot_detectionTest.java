package mosaic.plugins;

import org.junit.Test;

import mosaic.test.framework.CommonBase;


public class Spot_detectionTest extends CommonBase {

    @Test
    public void testOneFrame()  {
        // Define test data
        final String tcDirName          = "FeaturePointDetection/singleFrame/";
        final String setupString        = "run";
        final String macroOptions       = "radius=2 cutoff=0.1 per/abs=0.6";
        final String[] inputFiles       = {"spots.tif"};
        final String[] expectedFiles    = {};
        final String[] referenceFiles   = {};
        final String[] expectedCsvFiles    = {"spots.tifdet.csv"};
        final String[] referenceCsvFiles   = {"spots.csv"};

        // Create tested plugIn
        final Spot_detection nt = new Spot_detection();

        // Test it
        testPlugin(nt, tcDirName,
                   inputFiles, 
                   expectedFiles, referenceFiles,
                   expectedCsvFiles, referenceCsvFiles,
                   setupString, macroOptions);
    }

}
