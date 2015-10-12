package mosaic.plugins;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.scijava.util.FileUtils;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import mosaic.bregman.output.Region3DColocRScript;
import mosaic.bregman.output.Region3DRScript;
import mosaic.core.utils.MosaicTest;
import mosaic.core.utils.Segmentation;
import mosaic.core.utils.ShellCommand;
import mosaic.plugins.utils.TimeMeasurement;
import mosaic.region_competition.output.RCOutput;
import mosaic.test.framework.SystemOperations;


/**
 * This is the set of test
 *
 * @author Pietro Incardona
 */
public class Jtest implements PlugInFilter {

    private static final Logger logger = Logger.getLogger(Jtest.class);

    /**
     * Run JTest filter
     */
    @Override
    public void run(ImageProcessor arg0) {}

    @Override
    public int setup(String arg0, ImagePlus arg1) {
        // Get the User home directory
        final String test = SystemOperations.getTestTmpPath();
        final File s_file = new File(test + File.separator + "succeful");
        FileUtils.deleteRecursively(s_file);

        final TimeMeasurement tm = new TimeMeasurement();
        
        // -----------------------------------------------------------------------------------
        // Test Squassh segmentation
//        logger.info("========================== TestSuite: bregman.Jtest  ===================================");
//        final BregmanGLM_Batch BG = new BregmanGLM_Batch();
//        BG.bypass_GUI();
//        final TimeMeasurement tm1 = new TimeMeasurement();
//        // test the cluster
//        Jtest.logger.info("----------------------- TestCase: Squassh_cluster -----------------------");
//        BG.setUseCluster(true);
//        MosaicTest.<Region3DRScript> testPlugin(BG, "Squassh_cluster", Region3DRScript.class);
//        tm1.logLapTimeSec("----------------------- Squassh_cluster");
//        
//        Jtest.logger.info("----------------------- TestCase: Squassh_testa -----------------------");
//        BG.setUseCluster(false);
//        MosaicTest.<Region3DColocRScript> testPlugin(BG, "Squassh_testa", Region3DColocRScript.class);
//        tm1.logLapTimeSec("----------------------- Squassh_testa");
//        
//        Jtest.logger.info("----------------------- TestCase: Squassh -----------------------");
//        BG.setUseCluster(false);
//        MosaicTest.<Region3DRScript> testPlugin(BG, "Squassh", Region3DRScript.class);
//        tm1.logLapTimeSec("----------------------- Squassh");
//        tm.logLapTimeSec("========================== bregman");
//
//        // -----------------------------------------------------------------------------------
//        // Test core utils
//        logger.info("========================== TestSuite: core.cluster.Jtest ===================================");
//        mergetest();
//        tm.logLapTimeSec("========================== core.cluster.Jtest");

        // -----------------------------------------------------------------------------------
        // Test Region competition segmentation
        logger.info("========================== TestSuite: region_competition.Jtest ===================================");
        Jtest.logger.info("----------------------- TestCase: Region_Competition -----------------------");
        final Segmentation BG1 = new Region_Competition();
        MosaicTest.<RCOutput> testPlugin(BG1, "Region_Competition", RCOutput.class);
        tm.logLapTimeSec("========================== region_competition.Jtest");
        
        tm.logTimeSec("All tests SUCCESSFULLY completed");

        // -----------------------------------------------------------------------------------
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
    
    /**
     * Recursively take all the tree structure of a directory
     *
     * @param set
     * @param dir
     */
    private static void populate(HashSet<File> set, File dir) {
        set.add(dir);
        logger.info("A: " + dir);
        if (dir.isDirectory()) {
            logger.info("D: " + dir);
            for (final File t : dir.listFiles()) {
                populate(set, t);
            }
        }
    }

    /**
     * Compare if two directories are the same as dir and file structure
     *
     * @param a1 dir1
     * @param a2 dir3
     * @return true if they match, false otherwise
     */
    private static boolean compare(File a1, File a2) {
        final HashSet<File> seta1 = new HashSet<File>();
        populate(seta1, a1);
        final HashSet<File> seta2 = new HashSet<File>();
        populate(seta2, a2);

        // Check if the two HashSet match
        return seta1.containsAll(seta2);
    }

    private void mergetest() {
        final MergeJobs mj = new MergeJobs();

        final String dir = MosaicTest.getTestDir();
        final String dirOutTest = SystemOperations.getCleanTestTmpPath();
        final String dir_test = dirOutTest + File.separator + "merge_jobs" + File.separator + "Test";
        final String dir_sample = dir + File.separator + "merge_jobs" + File.separator + "Sample";

        // Remove test dir, create test dir and copy sample dir
        try {
            logger.info("Remove dir: [" + dir_test + "]");
            ShellCommand.exeCmd("rm -rf " + dir_test);
            logger.info("Create dir: [" + dir_test + "]");
            ShellCommand.exeCmd("mkdir -p " + dir_test);
            logger.info("Copy from dir: [" + dir_sample + "] to [" + dir_test + "]");
            ShellCommand.copy(new File(dir_sample), new File(dir_test), null);
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        catch (final InterruptedException e) {
            e.printStackTrace();
        }

        mj.setDir(dir_test);
        mj.setup("", null);

        // Check the result
        final String dir_result = dir + File.separator + "merge_jobs" + File.separator + "Result";

        final File result[] = new File(dir_result).listFiles();
        final File test[] = new File(dir_test).listFiles();
        logger.info("Result dir [" + dir_result + "] has " + result.length + " files");
        logger.info("Test dir [" + dir_test + "] has " + test.length + " files");
        
        if (result.length != test.length || result.length == 0 || test.length == 0) {
            throw new RuntimeException("Error: Merging jobs");
        }

        // TODO: It does not work. Actually compare returns false since two hash sets are *different*
        //       and because of that we do not have 'fail' message from below.
        if (compare(new File(dir_result), new File(dir_test))) {
            throw new RuntimeException("Error: Merging jobs differs");
        }
    }
}
