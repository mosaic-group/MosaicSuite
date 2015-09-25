package mosaic.plugins;

import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class GaussianCurvature3DTest extends CommonBase {

    @Test
    public void testGc3D()  {
        // Define test data
        final String tcDirName          = "VCF/";
        final String setupString        = "run";
        final String macroOptions       = "number_of_iterations=2";
        final String[] inputFiles       = {"sphereSmall.tif"};
        final String[] expectedFiles    = {"filtered_sphereSmall.tif"};
        final String[] referenceFiles   = {"filteredGc2_sphereSmall.tif"};

        // Create tested plugIn
        final GaussianCurvature3D nt = new GaussianCurvature3D();

        // Test it
        testPlugin(nt, tcDirName,
                inputFiles, expectedFiles, referenceFiles,
                setupString, macroOptions);

    }
}
