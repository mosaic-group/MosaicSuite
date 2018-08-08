package mosaic.plugins;

import static org.junit.Assert.*;

import org.junit.Test;

import mosaic.test.framework.CommonBase;


public class BackgroundSubtractor2_Test extends CommonBase {

    @Test
    public void testFixedLength()  {
        // run("Background Subtractor", "length=10 show skipOnFailure");
        
        
        // Define test data
        final String tcDirName           = "BackgroundSubtractor/fixedLength/";
        final String setupString         = "";
        final String macroOptions        = "length=4";
        final String inputFile           = "test.tif";
        final String[] expectedImgFiles  = {"test.tif"};
        final String[] referenceImgFiles = {"testOutput.tif"};
        final String[] expectedFiles     = {};
        final String[] referenceFiles    = {};

        // Create tested plugIn
        final BackgroundSubtractor2_ plugin = new BackgroundSubtractor2_();
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles, 
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testFixedLengthBG()  {
        // Define test data
        final String tcDirName           = "BackgroundSubtractor/fixedLength/";
        final String setupString         = "";
        final String macroOptions        = "length=4 show";
        final String inputFile           = "test.tif";
        final String[] expectedImgFiles  = {"test.tif", "BG of test.tif"};
        final String[] referenceImgFiles = {"testOutput.tif", "testOutputBG.tif"};
        final String[] expectedFiles     = {};
        final String[] referenceFiles    = {};

        // Create tested plugIn
        final BackgroundSubtractor2_ plugin = new BackgroundSubtractor2_();
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles, 
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testAutoLength()  {
        // run("Background Subtractor", "length=10 show skipOnFailure");
        
        
        // Define test data
        final String tcDirName           = "BackgroundSubtractor/autoLength/";
        final String setupString         = "";
        final String macroOptions        = "length=-1";
        final String inputFile           = "test.tif";
        final String[] expectedImgFiles  = {"test.tif"};
        final String[] referenceImgFiles = {"testOutput.tif"};
        final String[] expectedFiles     = {};
        final String[] referenceFiles    = {};

        // Create tested plugIn
        final BackgroundSubtractor2_ plugin = new BackgroundSubtractor2_();
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles, 
                   expectedFiles, referenceFiles);
    }
}
