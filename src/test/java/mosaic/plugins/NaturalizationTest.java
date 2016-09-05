package mosaic.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.Test;

import mosaic.test.framework.CommonBase;

/**
 * {@link Naturalization} plugin tests.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class NaturalizationTest extends CommonBase {
    private static final Logger logger = Logger.getLogger(NaturalizationTest.class);
    
    @Test
    public void testColorRgb() {
        // Define test data
        final String tcDirName          = "Naturalization/flower/";
        final String setupString        = "run";
        final String inputFile          = "x.png";
        final String[] expectedFiles    = {"naturalized_x.png"};
        final String[] referenceFiles   = {"x_nat.tif"};

        // Create tested plugin
        final Naturalization nt = new Naturalization();

        // Test it
        testPlugin(nt, tcDirName, 
                   null, 
                   setupString, inputFile, 
                   expectedFiles, referenceFiles);
    }

    @Test
    public void testGrey8() {
        // Define test data
        final String tcDirName          = "Naturalization/flower/";
        final String setupString        = "run";
        final String inputFile          = "x8bit.png";
        final String[] expectedFiles    = {"naturalized_x8bit.png"};
        final String[] referenceFiles   = {"x8bit_nat.tif"};

        // Create tested plugin
        final Naturalization nt = new Naturalization();

        // Test it
        testPlugin(nt, tcDirName, 
                   null, 
                   setupString, inputFile, 
                   expectedFiles, referenceFiles);
    }

    @Test
    public void testPSNR() {
        // Create tested plugin
        final Naturalization nt = new Naturalization();

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
