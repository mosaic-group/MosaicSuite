package mosaic.plugins.test;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;
import org.scijava.util.FileUtils;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import mosaic.core.utils.MosaicTest;
import mosaic.plugins.utils.TimeMeasurement;


/**
 * This is the set of test
 *
 * @author Pietro Incardona
 */
public class Jtest implements PlugInFilter {

    protected static final Logger logger = Logger.getLogger(Jtest.class);

    /**
     * Run JTest filter
     */

    @Override
    public void run(ImageProcessor arg0) {

    }

    @Override
    public int setup(String arg0, ImagePlus arg1) {
        // Get the User home directory
        final String test = MosaicTest.getTestEnvironment();
        final File s_file = new File(test + File.separator + "succeful");
        FileUtils.deleteRecursively(s_file);

        final TimeMeasurement tm = new TimeMeasurement();

        // Test Squassh segmentation
        logger.info("========================== TestSuite: bregman.Jtest  ===================================");
        final mosaic.bregman.Jtest jtestBR = new mosaic.bregman.Jtest();
        jtestBR.segmentation();
        tm.logLapTimeSec("========================== bregman");

        // Test core utils
        logger.info("========================== TestSuite: core.cluster.Jtest ===================================");
        final mosaic.core.cluster.Jtest jtestMj = new mosaic.core.cluster.Jtest();
        jtestMj.mergetest();
        tm.logLapTimeSec("========================== core.cluster.Jtest");

        // Test Region competition segmentation
        logger.info("========================== TestSuite: region_competition.Jtest ===================================");
        final mosaic.region_competition.Jtest jtestRC = new mosaic.region_competition.Jtest();
        jtestRC.segmentation();
        tm.logLapTimeSec("========================== region_competition.Jtest");
        
        tm.logTimeSec("All tests SUCCESSFULLY completed");

        // Create a file that notify all test has been completed suceffuly
        try {
            final PrintWriter succeful = new PrintWriter(test + File.separator + "succeful");
            succeful.write(1);
            succeful.close();
        }
        catch (final FileNotFoundException e) {
            e.printStackTrace();
        }

        return DONE;
    }

}
