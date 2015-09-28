package mosaic.core.cluster;


import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.junit.Test;

import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.ShellCommand;
import mosaic.plugins.MergeJobs;
import mosaic.test.framework.SystemOperations;


/**
 * It test the merging Jobs plugins functionality
 *
 * @see MergeJobs
 */
public class Jtest {
    private static final Logger logger = Logger.getLogger(Jtest.class);
    
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

    @Test
    public void mergetest() {
        final MergeJobs mj = new MergeJobs();

        final String dir = MosaicUtils.getTestDir();
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
            fail("Error: Merging jobs");
        }

        // TODO: It does not work. Actually compare returns false since two hash sets are *different*
        //       and because of that we do not have 'fail' message from below.
        if (compare(new File(dir_result), new File(dir_test))) {
            fail("Error: Merging jobs differs");
        }
    }
}
