package mosaic.plugins;

import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class VariationalCurvatureFilterTest extends CommonBase {

    @Test
    public void testGrey8()  {
        // Define test data
        String tcDirName          = "VCF/";
        String setupString        = "run";
        String[] inputFiles       = {"x8bit.png"};
        String[] expectedFiles    = {"filtered_x8bit.png"};
        String[] referenceFiles   = {"filteredGcSplit10_x8bit.tif"};
        
        // Create tested plugin
        VariationalCurvatureFilter nt = new VariationalCurvatureFilter();
       
        // Test it
        testPlugin2(nt, tcDirName, 
                   inputFiles, expectedFiles, referenceFiles, 
                   setupString, "filter=GC method=Split number=10");

    }

    @Test
    public void testRgb()  {
        // Define test data
        String tcDirName          = "VCF/";
        String setupString        = "run";
        String[] inputFiles       = {"x.png"};
        String[] expectedFiles    = {"filtered_x.png"};
        String[] referenceFiles   = {"filteredTvNoSplit10_x.tif"};
        
        // Create tested plugin
        VariationalCurvatureFilter nt = new VariationalCurvatureFilter();
       
        // Test it
        testPlugin2(nt, tcDirName, 
                   inputFiles, expectedFiles, referenceFiles, 
                   setupString, "filter=TV method=[No Split] number=10");

    }
}
