package mosaic.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import ij.IJ;

/**
 * This class contains util's methods to operate on file system.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class SysOps {
    public static final String SEPARATOR = File.separator;

    /**
     * Copies file to directory
     * @param aSrcFile
     * @param aDestDir
     */
    public static void copyFileToDirectory(String aSrcFile, String aDestDir) {
        copyFileToDirectory(new File(aSrcFile), new File(aDestDir));
    }
    
    /**
     * Copies file to directory
     * @param aSrcFile
     * @param aDestDir
     */
    public static void copyFileToDirectory(File aSrcFile, File aDestDir) {
        try {
            FileUtils.copyFileToDirectory(aSrcFile, aDestDir);
        } catch (final IOException e) {
            e.printStackTrace();
            // intentionally break execution
            throw new RuntimeException("Cannot copy file [" + aSrcFile + "] to dir [" + aDestDir + "] [" + e.getMessage() + "]");
        }
    }

    /**
     * Copies directory to directory
     * @param aSrcDir
     * @param aDestDir
     */
    public static void copyDirectoryToDirectory(File aSrcDir, File aDestDir) {
        try {
            FileUtils.copyDirectoryToDirectory(aSrcDir, aDestDir);
        } catch (final IOException e) {
            e.printStackTrace();
            // intentionally break execution
            throw new RuntimeException("Cannot copy directory [" + aSrcDir + "] to dir [" + aDestDir + "] [" + e.getMessage() + "]");
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
        } catch (final IOException e) {
            e.printStackTrace();
            // intentionally break execution
            throw new RuntimeException("Cannot copy file [" + aSrcFile + "] to [" + aDestFile + "]");
        }
    }

    /**
     * Creates with specified absolute path.
     * @param aDirName - absolute path dir name
     */
    public static void createDir(String aDirName) {
        createDir(new File(aDirName));
    }
    
    /**
     * Creates with specified absolute path.
     * @param aDirName - absolute path dir name
     */
    public static void createDir(File aDir) {
        try {
            FileUtils.forceMkdir(aDir);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Creating directory: [" + aDir.getAbsolutePath() + "] failed! [" + e.getMessage() + "]");
        }
    }

    /**
     * Moves file to file.
     * @param aSrcFile - absolute path to source file
     * @param aDestFile - absolute path to destination file
     */
    public static void moveFile(String aSrcFile, String aDestFile) {
        final File src = new File(aSrcFile);
        final File dest = new File(aDestFile);
        moveFile(src, dest, false);
    }

    /**
     * Moves file to file.
     * @param aSrcFile - absolute path to source file
     * @param aDestFile - absolute path to destination file
     * @param aQuiteModeActive - if set to true, problems with execution will not be visible
     */
    public static void moveFile(String aSrcFile, String aDestFile, boolean aQuiteModeActive) {
        final File src = new File(aSrcFile);
        final File dest = new File(aDestFile);
        moveFile(src, dest, aQuiteModeActive);
    }

    /**
     * Moves file to directory.
     * @param aSrcFile - source file
     * @param aDestDir - destination dir
     * @param aQuiteModeActive - if set to true, problems iwth execution will not be visible
     * @param aCreateDestDir - should dest dir be crated
     */
    public static void moveFileToDir(File aSrcFile, File aDestDir, boolean aQuiteModeActive, boolean aCreateDestDir) {
        try {
            if (!aSrcFile.exists() && aQuiteModeActive) {
                // Return quietly - just to comply to current behavior.
                return;
            }
            // Remove destination file if exist before moving (if not it generate exception).
            String movedFileAbsolutePath = aDestDir.getAbsolutePath() + File.separator + aSrcFile.getName();
            File movedFile = new File(movedFileAbsolutePath);
            if (movedFile.exists()) {
                FileUtils.deleteQuietly(movedFile);
            }
            FileUtils.moveFileToDirectory(aSrcFile, aDestDir, aCreateDestDir);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot move file [" + aSrcFile + "] to [" + aDestDir + "] [" + e.getMessage() + "]");
        }
    }
    
    /**
     * Moves file to file.
     * @param aSrcFile - source file
     * @param aDestFile - destination file
     * @param aQuiteModeActive - if set to true, problems iwth execution will not be visible
     */
    public static void moveFile(File aSrcFile, File aDestFile, boolean aQuiteModeActive) {
        try {
            if (aDestFile.exists()) {
                FileUtils.deleteQuietly(aDestFile);
            }
            if (!aSrcFile.exists() && aQuiteModeActive) {
                // Return quietly - just to comply to current behavior.
                return;
            }
            FileUtils.moveFile(aSrcFile, aDestFile);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot move file [" + aSrcFile + "] to [" + aDestFile + "] [" + e.getMessage() + "]");
        }
    }

    /**
     * Removes a directory.
     * @param aDirName - absolute path to directory to be deleted
     */
    public static void removeDir(String aDirName) {
        removeDir(new File(aDirName));
    }

    /**
     * Removes a directory
     * @param aDir - directory to be deleted
     */
    public static void removeDir(File aDir) {
        try {
            FileUtils.deleteDirectory(aDir);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Deleting directory: [" + aDir.getAbsolutePath() + "] failed! [" + e.getMessage() + "]");
        }
    }

    /**
     * Provides temporary path in system
     * @return path to temp
     */
    public static String getTmpPath() {
        // Simply use IJ functionality
        return IJ.getDirectory("temp");
    }

    /**
     * Remove the file extension
     * @param aFileName String from where to remove the extension
     * @return the String
     */
    public static String removeExtension(String aFileName) {
        return FilenameUtils.removeExtension(aFileName);
    }
    
    /**
     * @return return directory path taken from given path to file
     */
    public static String getPathToFile(String aFileName) {
        return FilenameUtils.getFullPath(aFileName);
    }
    
    /**
     * @return returns an extension from a provided filename
     */
    public static String getExtension(String aFileName) {
        return FilenameUtils.getExtension(aFileName);
    }
    
    /**
     * @return system username (usually login name of a user) or null if not possible to detect.
     */
    public static String getSystemUsername() {
        String username = System.getenv("USER");
        if (username == null || username.equals("")) return null;
        return username;
    }
    
    /**
     * @return returns nicer path with removed redundant separators ("/tmp///x.tif" -> "/tmp/x.tif") 
     */
    public static String removeRedundantSeparators(String aFileName) {
        return aFileName.replaceAll("/+", "/");
    }
    
    
}
