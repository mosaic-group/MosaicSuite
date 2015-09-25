package mosaic.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import mosaic.test.framework.CommonBase;

/**
 * {@link Naturalization} plugin tests.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class NaturalizationTest extends CommonBase {

    @Test
    public void testColorRgb() {
        // Define test data
        String tcDirName          = "Naturalization/flower/";
        String setupString        = "run";
        String[] inputFiles       = {"x.png"};
        String[] expectedFiles    = {"naturalized_x.png"};
        String[] referenceFiles   = {"x_nat.tif"};

        // Create tested plugin
        Naturalization nt = new Naturalization();

        // Test it
        testPlugin(nt, tcDirName,
                inputFiles, expectedFiles, referenceFiles,
                setupString);
    }

    @Test
    public void testGrey8() {
        // Define test data
        String tcDirName          = "Naturalization/flower/";
        String setupString        = "run";
        String[] inputFiles       = {"x8bit.png"};
        String[] expectedFiles    = {"naturalized_x8bit.png"};
        String[] referenceFiles   = {"x8bit_nat.tif"};

        // Create tested plugin
        Naturalization nt = new Naturalization();

        // Test it
        testPlugin(nt, tcDirName,
                inputFiles, expectedFiles, referenceFiles,
                setupString);
    }

    @Test
    public void testPSNR() {
        // Create tested plugin
        Naturalization nt = new Naturalization();

        // Check values
        logger.debug("Testting PSNR for different ranges of input values");

        assertTrue("x >=  0 && x <= 0.934", nt.calculate_PSNR(0).startsWith("3.6"));
        assertTrue("x >=  0 && x <= 0.934", nt.calculate_PSNR(0.9).startsWith("40.56"));

        assertEquals("x > 0.934 && x < 1.07", "> 40", nt.calculate_PSNR(1.00));

        assertTrue("x >= 1.07 && x < 1.9", nt.calculate_PSNR(1.08).startsWith("40.28"));
        assertTrue("x >= 1.07 && x < 1.9", nt.calculate_PSNR(1.8).startsWith("31.96"));

        assertTrue("x >= 1.9", nt.calculate_PSNR(1.95).startsWith("29.77"));
        assertTrue("x >= 1.9", nt.calculate_PSNR(10.0).startsWith("44444."));
    }

}
