package mosaic.plugins;

import mosaic.plugins.utils.CurvatureFilterBase;
import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class VariationalCurvatureFilterTest extends CommonBase {

    @Test
    public void testGcSplitGrey8()  {
        // Define test data
        final String tcDirName          = "VCF/";
        final String setupString        = "run";
        final String macroOptions       = "filter=[GC (Gaussian Curvature)] method=Split number=10";
        final String[] inputFiles       = {"x8bit.png"};
        final String[] expectedFiles    = {"filtered_x8bit.png"};
        final String[] referenceFiles   = {"filteredGcSplit10_x8bit.tif"};

        // Create tested plugIn
        final CurvatureFilterBase nt = new VariationalCurvatureFilter();

        // Test it
        testPlugin(nt, tcDirName,
                inputFiles, expectedFiles, referenceFiles,
                setupString, macroOptions);

    }

    @Test
    public void testTvNoSplitRgb()  {
        // Define test data
        final String tcDirName          = "VCF/";
        final String setupString        = "run";
        final String macroOptions       = "filter=[TV (Total Variation)] method=[No Split] number=10";
        final String[] inputFiles       = {"x.png"};
        final String[] expectedFiles    = {"filtered_x.png"};
        final String[] referenceFiles   = {"filteredTvNoSplit10_x.tif"};

        // Create tested plugIn
        final CurvatureFilterBase nt = new VariationalCurvatureFilter();

        // Test it
        testPlugin(nt, tcDirName,
                inputFiles, expectedFiles, referenceFiles,
                setupString, macroOptions);

    }

    @Test
    public void testMcSplitGrey8()  {
        // Define test data
        final String tcDirName          = "VCF/";
        final String setupString        = "run";
        final String macroOptions       = "filter=[MC (Mean Curvature)] method=Split number=2";
        final String[] inputFiles       = {"x8bit.png"};
        final String[] expectedFiles    = {"filtered_x8bit.png"};
        final String[] referenceFiles   = {"filteredMcSplit2_x8bit.tif"};

        // Create tested plugIn
        final CurvatureFilterBase nt = new VariationalCurvatureFilter();

        // Test it
        testPlugin(nt, tcDirName,
                inputFiles, expectedFiles, referenceFiles,
                setupString, macroOptions);

    }

    @Test
    public void testBernsteinSplitGrey8()  {
        // Define test data
        final String tcDirName          = "VCF/";
        final String setupString        = "run";
        final String macroOptions       = "filter=[Bernstein] method=Split number=5";
        final String[] inputFiles       = {"x8bit.png"};
        final String[] expectedFiles    = {"filtered_x8bit.png"};
        final String[] referenceFiles   = {"filteredBernsteinSplit5_x8bit.tif"};

        // Create tested plugIn
        final CurvatureFilterBase nt = new VariationalCurvatureFilter();

        // Test it
        testPlugin(nt, tcDirName,
                inputFiles, expectedFiles, referenceFiles,
                setupString, macroOptions);

    }

}
