package mosaic.plugins;

import org.junit.Test;

import mosaic.test.framework.CommonBase;


public class Spot_detectionTest extends CommonBase {

    @Test
    public void testOneFrame()  {
        // Define test data
        final String tcDirName          = "FeaturePointDetection/singleFrame/";
        final String setupString        = "run";
        final String macroOptions       = "radius=2 cutoff=0.0333 per/abs=0.6";
        final String inputFile          = "spots.tif";
        final String[] expectedFiles    = {};
        final String[] referenceFiles   = {};
        final String[] expectedCsvFiles    = {"spots.tifdet.csv"};
        final String[] referenceCsvFiles   = {"spots.csv"};

        // Create tested plugIn
        final Spot_detection nt = new Spot_detection();

        // Test it
        testPlugin(nt, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedFiles, referenceFiles,
                   expectedCsvFiles, referenceCsvFiles);
    }
    
    @Test
    public void testThreeFrames()  {
        // Define test data
        final String tcDirName          = "FeaturePointDetection/threeFrames/";
        final String setupString        = "run";
        final String macroOptions       = "radius=3 cutoff=0.0 per/abs=0.4";
        final String inputFile          = "threeFrames.tif";
        final String[] expectedFiles    = {};
        final String[] referenceFiles   = {};
        final String[] expectedCsvFiles    = {"threeFrames.tifdet.csv"};
        final String[] referenceCsvFiles   = {"threeFrames.csv"};

        // Create tested plugIn
        final Spot_detection nt = new Spot_detection();

        // Test it
        testPlugin(nt, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedFiles, referenceFiles,
                   expectedCsvFiles, referenceCsvFiles);
    }

    @Test
    public void test3D()  {
        // Define test data
        final String tcDirName          = "FeaturePointDetection/3d/";
        final String setupString        = "run";
        final String macroOptions       = "radius=3 cutoff=0.0 per/abs=0.1";
        final String inputFile          = "3Dframe.tif";
        final String[] expectedFiles    = {};
        final String[] referenceFiles   = {};
        final String[] expectedCsvFiles    = {"3Dframe.tifdet.csv"};
        final String[] referenceCsvFiles   = {"3Dframe.csv"};

        // Create tested plugIn
        final Spot_detection nt = new Spot_detection();

        // Test it
        testPlugin(nt, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedFiles, referenceFiles,
                   expectedCsvFiles, referenceCsvFiles);
    }
}
