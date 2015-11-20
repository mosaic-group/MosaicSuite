package mosaic.plugins;

import org.junit.Test;

import mosaic.test.framework.CommonBase;
import mosaic.test.framework.SystemOperations;

public class BregmanGLM_BatchTest extends CommonBase{
    
    @Test
    public void testTest2D()  {
        
        // Define test data
        final String tcDirName           = "Squassh/Test2D/";
        final String setupString         = "run";
        final String macroOptions        = "";
        final String inputFile           = "test2d.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/test2d_outline_overlay_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/test2d_outline_overlay_c1.zip"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/test2d_ObjectsData_c1.csv", "__ImagesData.csv/test2d_ImagesData.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/test2d_ObjectsData_c1.csv", "__ImagesData.csv/test2d_ImagesData.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.dat", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }

    @Test
    public void testSphereSmall3D()  {
        
        // Define test data
        final String tcDirName           = "Squassh/Sphere3D/";
        final String setupString         = "run";
        final String macroOptions        = "";
        final String inputFile           = "sphereSmall.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/sphereSmall_outline_overlay_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/sphereSmall_outline_overlay_c1.zip"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/sphereSmall_ObjectsData_c1.csv", "__ImagesData.csv/sphereSmall_ImagesData.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/sphereSmall_ObjectsData_c1.csv", "__ImagesData.csv/sphereSmall_ImagesData.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.dat", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
}
