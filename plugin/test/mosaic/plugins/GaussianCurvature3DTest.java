package mosaic.plugins;

import static org.junit.Assert.*;
import mosaic.plugins.utils.CurvatureFilterBase;
import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class GaussianCurvature3DTest extends CommonBase {

    @Test
    public void testGc3D()  {
        // Define test data
        String tcDirName          = "VCF/";
        String setupString        = "run";
        String macroOptions       = "number_of_iterations=2";
        String[] inputFiles       = {"sphereSmall.tif"};
        String[] expectedFiles    = {"filtered_sphereSmall.tif"};
        String[] referenceFiles   = {"filteredGc2_sphereSmall.tif"};
        
        // Create tested plugIn
        GaussianCurvature3D nt = new GaussianCurvature3D();
       
        // Test it
        testPlugin(nt, tcDirName, 
                   inputFiles, expectedFiles, referenceFiles, 
                   setupString, macroOptions);

    }
}
