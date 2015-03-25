package mosaic.test.framework;

import ij.IJ;

import java.io.File;
import java.io.IOException;

import mosaic.core.utils.ShellCommand;

/**
 * This class contains util's methods to operate on file system.
 */
public class SystemOperations {
    static final String SEPARATOR = File.separator;
    static final String TEST_TMP_DIR = "test";

    
    /**
     * Returns test data path. 
     * @return Absolute path to test data
     * 
     */
    static public String getTestDataPath() {
        final String path = System.getenv("MOSAIC_PLUGIN_TEST_DATA_PATH");

        if (path == null || path.equals("")) {
            // Throw and stop execution of test intentionally. It is easier to
            // debug if test will stop here.
            throw new RuntimeException("Environment variable MOSAIC_PLUGIN_TEST_DATA_PATH is not defined! It should point to Jtest_data in plugin source.");
        }

        return path;
    }
    
    /**
     * Returns temporary test path which should be used during tests execution.
     * @return Absolute path to temporary test data.
     */
    public static String getTestTmpPath() {
        return getTmpPath() + SEPARATOR + TEST_TMP_DIR + SEPARATOR;
    }
    
    /**
     * Returns prepared (empty) temporary test path which should be used 
     * during tests execution.
     * 
     * @return Absolute path to temporary test data.
     */
    public static String getCleanTestTmpPath() {
        removeTestTmpDir();
        createTestTmpDir();
        
        return getTestTmpPath();
    }
    
    /**
     * Removes test temporary directory. If any problem arise it
     * will throw and break an execution of test.
     */
    private static void removeTestTmpDir() {
        try {
            ShellCommand.exeCmdNoPrint("rm -rf " + getTestTmpPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Command: [rm -rf " + getTestTmpPath() + "] failed!");
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Command: [rm -rf " + getTestTmpPath() + "] failed!");
        }
    }
    
    /**
     * Creates test temporary directory. If any problem arise it
     * will throw and break an execution of test.
     */
    private static void createTestTmpDir() {
        try {
            ShellCommand.exeCmd("mkdir " + getTestTmpPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Command: [mkdir " + getTestTmpPath() + "] failed!");
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Command: [mkdir " + getTestTmpPath() + "] failed!");
        }
    }

    private static String getTmpPath() {
        return IJ.getDirectory("temp");
    }
}
