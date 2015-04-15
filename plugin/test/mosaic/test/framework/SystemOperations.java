package mosaic.test.framework;

import ij.IJ;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/**
 * This class contains util's methods to operate on file system.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
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

        return path + SEPARATOR;
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
     * Copies file to directory
     * @param aSrcFile
     * @param aDestDir
     */
    public static void copyFileToDirectory(File aSrcFile, File aDestDir) {
        try {
            FileUtils.copyFileToDirectory(aSrcFile, aDestDir);
        } catch (IOException e) {
            e.printStackTrace();
            // intentionally break execution
            throw new RuntimeException("Cannot copy file [" + aSrcFile + "] to dir [" + aDestDir + "]");
        }
    }
    
    /**
     * Copies file to new file
     * @param aSrcFile
     * @param aDestFile
     */
    public static void copyFile(File aSrcFile, File aDestFile) {
        try {
            FileUtils.copyFile(aSrcFile, aDestFile);
        } catch (IOException e) {
            e.printStackTrace();
            // intentionally break execution
            throw new RuntimeException("Cannot copy file [" + aSrcFile + "] to [" + aDestFile + "]");
        }
    }
    /**
     * Removes test temporary directory. If any problem arise it
     * will throw and break an execution of test.
     */
    public static void removeTestTmpDir() {
        try {
            FileUtils.deleteDirectory(new File(getTestTmpPath()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Deleting directory: [" + getTestTmpPath() + "] failed!");
        }
    }
    
    /**
     * Creates test temporary directory. If any problem arise it
     * will throw and break an execution of test.
     */
    private static void createTestTmpDir() {
        try {
            FileUtils.forceMkdir(new File(getTestTmpPath()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Creating directory: [" + getTestTmpPath() + "] failed!");
        }

    }

    private static String getTmpPath() {
        return IJ.getDirectory("temp");
    }
    
}
