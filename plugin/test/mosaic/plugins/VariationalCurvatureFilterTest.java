package mosaic.plugins;

import static org.junit.Assert.*;
import ij.plugin.filter.PlugInFilter;
import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class VariationalCurvatureFilterTest extends CommonBase {

    @Test
    public void testGrey8()  {
        // Define test data
        String tcDirName          = "VCF/";
        String setupString        = "run";
        int expectedSetupRetValue = PlugInFilter.DOES_ALL | PlugInFilter.DOES_STACKS | PlugInFilter.CONVERT_TO_FLOAT | PlugInFilter.FINAL_PROCESSING | PlugInFilter.PARALLELIZE_STACKS;;
        String[] inputFiles       = {"x8bit.png"};
        String[] expectedFiles    = {"filtered_x8bit.png"};
        String[] referenceFiles   = {"filteredGc_x8bit.tif"};
           
        // Create tested plugin
        VariationalCurvatureFilter nt = new VariationalCurvatureFilter();
    
        // Test it
        testPlugin2(nt, tcDirName, 
                   inputFiles, expectedFiles, referenceFiles, 
                   setupString, expectedSetupRetValue);    
    }

}
