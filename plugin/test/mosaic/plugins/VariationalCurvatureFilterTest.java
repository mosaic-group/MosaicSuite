package mosaic.plugins;

import mosaic.plugins.utils.CurvatureFilterBase;
import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class VariationalCurvatureFilterTest extends CommonBase {

    @Test
    public void testGcSplitGrey8()  {
        // Define test data
        String tcDirName          = "VCF/";
        String setupString        = "run";
        String macroOptions       = "filter=GC method=Split number=10";
        String[] inputFiles       = {"x8bit.png"};
        String[] expectedFiles    = {"filtered_x8bit.png"};
        String[] referenceFiles   = {"filteredGcSplit10_x8bit.tif"};
        
        // Create tested plugIn
        CurvatureFilterBase nt = new VariationalCurvatureFilter();
       
        // Test it
        testPlugin(nt, tcDirName, 
                   inputFiles, expectedFiles, referenceFiles, 
                   setupString, macroOptions);

    }

    @Test
    public void testTvNoSplitRgb()  {
        // Define test data
        String tcDirName          = "VCF/";
        String setupString        = "run";
        String macroOptions       = "filter=TV method=[No Split] number=10";
        String[] inputFiles       = {"x.png"};
        String[] expectedFiles    = {"filtered_x.png"};
        String[] referenceFiles   = {"filteredTvNoSplit10_x.tif"};
        
        // Create tested plugIn
        CurvatureFilterBase nt = new VariationalCurvatureFilter();
       
        // Test it
        testPlugin(nt, tcDirName, 
                   inputFiles, expectedFiles, referenceFiles, 
                   setupString, macroOptions);

    }
    
    @Test
    public void testMcSplitGrey8()  {
        // Define test data
        String tcDirName          = "VCF/";
        String setupString        = "run";
        String macroOptions       = "filter=MC method=Split number=2";
        String[] inputFiles       = {"x8bit.png"};
        String[] expectedFiles    = {"filtered_x8bit.png"};
        String[] referenceFiles   = {"filteredMcSplit2_x8bit.tif"};
        
        // Create tested plugIn
        CurvatureFilterBase nt = new VariationalCurvatureFilter();
       
        // Test it
        testPlugin(nt, tcDirName, 
                   inputFiles, expectedFiles, referenceFiles, 
                   setupString, macroOptions);

    }
}
