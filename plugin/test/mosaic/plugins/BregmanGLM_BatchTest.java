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
    public void testTest2dFull()  {
        
        // Define test data
        final String tcDirName           = "Squassh/Test2dFull/";
        final String setupString         = "run";
        final String macroOptions        = "";
        final String inputFile           = "test2d.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/test2d_outline_overlay_c1.zip", "__intensities_c1.zip/test2d_intensities_c1.zip",
                                            "__mask_c1.zip/test2d_mask_c1.zip", "__seg_c1.zip/test2d_seg_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/test2d_outline_overlay_c1.zip", "__intensities_c1.zip/test2d_intensities_c1.zip",
                                            "__mask_c1.zip/test2d_mask_c1.zip", "__seg_c1.zip/test2d_seg_c1.zip"};
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
    
    @Test
    public void testVideo3D()  {
        
        // Define test data
        final String tcDirName           = "Squassh/Video3D/";
        final String setupString         = "run";
        final String macroOptions        = "";
        final String inputFile           = "dropletVideoSmall.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/dropletVideoSmall_outline_overlay_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/dropletVideoSmall_outline_overlay_c1.zip"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/dropletVideoSmall_ObjectsData_c1.csv", "__ImagesData.csv/dropletVideoSmall_ImagesData.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/dropletVideoSmall_ObjectsData_c1.csv", "__ImagesData.csv/dropletVideoSmall_ImagesData.csv"};

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
    public void testScriptR()  {
        
        // Define test data
        final String tcDirName           = "Squassh/ScriptR/";
        final String setupString         = "run";
        final String macroOptions        = "";
        final String inputFile           = "1 Ctrl 1.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/1 Ctrl 1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/1 Ctrl 2_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/1 Ctrl 3_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/2 OA1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/2 OA2_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/2 OA4_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/3 Ctrl1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/3 Ctrl2_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/3 Ctrl3_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/4 OA1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/4 OA3_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/4 OA4_outline_overlay_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/1 Ctrl 1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/1 Ctrl 2_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/1 Ctrl 3_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/2 OA1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/2 OA2_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/2 OA4_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/3 Ctrl1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/3 Ctrl2_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/3 Ctrl3_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/4 OA1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/4 OA3_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/4 OA4_outline_overlay_c1.zip"};
        final String[] expectedFiles     = {"stitch_ImagesData.csv"};
        final String[] referenceFiles    = {"stitch_ImagesData.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.dat", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        copyTestResources("1 Ctrl 2.tif", SystemOperations.getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("1 Ctrl 3.tif", SystemOperations.getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("2 OA1.tif", SystemOperations.getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("2 OA2.tif", SystemOperations.getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("2 OA4.tif", SystemOperations.getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("3 Ctrl1.tif", SystemOperations.getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("3 Ctrl2.tif", SystemOperations.getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("3 Ctrl3.tif", SystemOperations.getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("4 OA1.tif", SystemOperations.getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("4 OA3.tif", SystemOperations.getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("4 OA4.tif", SystemOperations.getTestDataPath() + tcDirName, tmpPath);
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
}