package mosaic.plugins;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import ij.macro.Interpreter;
import mosaic.test.framework.CommonBase;

public class BregmanGLM_BatchTest extends CommonBase {
    
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
        copyTestResources("spb_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }

    @Test
    public void testTestPsfCircleLowIntensity()  {
        
        // Define test data
        final String tcDirName           = "Squassh/psfCircleLowIntensity/";
        final String setupString         = "run";
        final String macroOptions        = "";
        final String inputFile           = "psf.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/psf_outline_overlay_c1.zip", "__intensities_c1.zip/psf_intensities_c1.zip",
                                            "__mask_c1.zip/psf_mask_c1.zip", "__seg_c1.zip/psf_seg_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/psf_outline_overlay_c1.zip", "__intensities_c1.zip/psf_intensities_c1.zip",
                                            "__mask_c1.zip/psf_mask_c1.zip", "__seg_c1.zip/psf_seg_c1.zip"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/psf_ObjectsData_c1.csv", "__ImagesData.csv/psf_ImagesData.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/psf_ObjectsData_c1.csv", "__ImagesData.csv/psf_ImagesData.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    // Temporarily turned off. Some tolerance on output should be introduced. It happens that estimateIntensityClustering() in AnalysePatch gives slightly
    // different results (Random based algorithm and there is no way to provide random seed).
    @Test
//    @org.junit.Ignore
    public void testTestPsfCircleMediumIntensity()  {
        
        // Define test data
        final String tcDirName           = "Squassh/psfCircleMediumIntensity/";
        final String setupString         = "run";
        final String macroOptions        = "";
        final String inputFile           = "psf.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/psf_outline_overlay_c1.zip", "__intensities_c1.zip/psf_intensities_c1.zip",
                                            "__mask_c1.zip/psf_mask_c1.zip", "__seg_c1.zip/psf_seg_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/psf_outline_overlay_c1.zip", "__intensities_c1.zip/psf_intensities_c1.zip",
                                            "__mask_c1.zip/psf_mask_c1.zip", "__seg_c1.zip/psf_seg_c1.zip"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/psf_ObjectsData_c1.csv", "__ImagesData.csv/psf_ImagesData.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/psf_ObjectsData_c1.csv", "__ImagesData.csv/psf_ImagesData.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
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
        copyTestResources("spb_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testTest2ChannelsWithMaskFull()  {
        
        // Define test data
        final String tcDirName           = "Squassh/2channelsWithMasks/";
        final String setupString         = "run";
        final String macroOptions        = "";
        final String inputFile           = "moImg.tif";
        final String[] expectedImgFiles  = {"__coloc.zip/moImg_coloc.zip", 
                                            "__intensities_c1.zip/moImg_intensities_c1.zip", "__intensities_c2.zip/moImg_intensities_c2.zip", 
                                            "__mask_c1.zip/moImg_mask_c1.zip", "__mask_c2.zip/moImg_mask_c2.zip", 
                                            "__outline_overlay_c1.zip/moImg_outline_overlay_c1.zip", "__outline_overlay_c2.zip/moImg_outline_overlay_c2.zip",
                                            "__seg_c1.zip/moImg_seg_c1.zip", "__seg_c2.zip/moImg_seg_c2.zip"};
        final String[] referenceImgFiles = {"__coloc.zip/moImg_coloc.zip", 
                                            "__intensities_c1.zip/moImg_intensities_c1.zip", "__intensities_c2.zip/moImg_intensities_c2.zip", 
                                            "__mask_c1.zip/moImg_mask_c1.zip", "__mask_c2.zip/moImg_mask_c2.zip", 
                                            "__outline_overlay_c1.zip/moImg_outline_overlay_c1.zip", "__outline_overlay_c2.zip/moImg_outline_overlay_c2.zip",
                                            "__seg_c1.zip/moImg_seg_c1.zip", "__seg_c2.zip/moImg_seg_c2.zip"};
        final String[] expectedFiles     = {"__ImagesData.csv/moImg_ImagesData.csv", "__ObjectsData_c1.csv/moImg_ObjectsData_c1.csv", "__ObjectsData_c2.csv/moImg_ObjectsData_c2.csv"};
        final String[] referenceFiles    = {"__ImagesData.csv/moImg_ImagesData.csv", "__ObjectsData_c1.csv/moImg_ObjectsData_c1.csv", "__ObjectsData_c2.csv/moImg_ObjectsData_c2.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
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
        copyTestResources("spb_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
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
        copyTestResources("spb_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
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
        final String[] expectedFiles     = {"stitch_ImagesData.csv", "R_analysis.R"};
        final String[] referenceFiles    = {"stitch_ImagesData.csv", "R_analysis.R"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        copyTestResources("1 Ctrl 2.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("1 Ctrl 3.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("2 OA1.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("2 OA2.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("2 OA4.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("3 Ctrl1.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("3 Ctrl2.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("3 Ctrl3.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("4 OA1.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("4 OA3.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("4 OA4.tif", getTestDataPath() + tcDirName, tmpPath);
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testColocalization()  {
        
        // Define test data
        final String tcDirName           = "Squassh/Colocalization/";
        final String setupString         = "run";
        final String macroOptions        = "";
        final String inputFile           = "coloc_test.tif";
        final String[] expectedImgFiles  = {"coloc_test_intensities_c1.tif",
                                            "coloc_test_intensities_c2.tif",
                                            "coloc_test_mask_c1.tif",
                                            "coloc_test_mask_c2.tif",
                                            "coloc_test_outline_overlay_c1.tif",
                                            "coloc_test_outline_overlay_c2.tif",
                                            "coloc_test_seg_c1.tif",
                                            "coloc_test_seg_c2.tif"};
        final String[] referenceImgFiles = {"__intensities_c1.zip/coloc_test_intensities_c1.zip",
                                            "__intensities_c2.zip/coloc_test_intensities_c2.zip",
                                            "__mask_c1.zip/coloc_test_mask_c1.zip",
                                            "__mask_c2.zip/coloc_test_mask_c2.zip",
                                            "__outline_overlay_c1.zip/coloc_test_outline_overlay_c1.zip",
                                            "__outline_overlay_c2.zip/coloc_test_outline_overlay_c2.zip",
                                            "__seg_c1.zip/coloc_test_seg_c1.zip",
                                            "__seg_c2.zip/coloc_test_seg_c2.zip"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/coloc_test_ObjectsData_c1.csv", "__ObjectsData_c2.csv/coloc_test_ObjectsData_c2.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/coloc_test_ObjectsData_c1.csv", "__ObjectsData_c2.csv/coloc_test_ObjectsData_c2.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    @org.junit.Ignore
    public void testCluster()  {
        
        // Define test data
        final String tcDirName           = "Squassh/cluster/";
        final String setupString         = "run";
        final String macroOptions        = "username=" + System.getProperty("user.name");
        final String inputFile           = null;
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/droplet_1_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_2_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_3_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_4_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_5_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_6_outline_overlay_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/droplet_1_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_2_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_3_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_4_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_5_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_6_outline_overlay_c1.zip"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/droplet_1_ObjectsData_c1.csv",
                                            "__ObjectsData_c1.csv/droplet_2_ObjectsData_c1.csv",
                                            "__ObjectsData_c1.csv/droplet_3_ObjectsData_c1.csv",
                                            "__ObjectsData_c1.csv/droplet_4_ObjectsData_c1.csv",
                                            "__ObjectsData_c1.csv/droplet_5_ObjectsData_c1.csv",
                                            "__ObjectsData_c1.csv/droplet_6_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/droplet_1_ObjectsData_c1.csv",
                                            "__ObjectsData_c1.csv/droplet_2_ObjectsData_c1.csv",
                                            "__ObjectsData_c1.csv/droplet_3_ObjectsData_c1.csv",
                                            "__ObjectsData_c1.csv/droplet_4_ObjectsData_c1.csv",
                                            "__ObjectsData_c1.csv/droplet_5_ObjectsData_c1.csv",
                                            "__ObjectsData_c1.csv/droplet_6_ObjectsData_c1.csv"};
        
        // Create tested plugIn
        Interpreter.batchMode = true;
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        BregmanGLM_Batch.test_mode = true;
        BregmanGLM_Batch.test_path = getTestTmpPath();
        plugin.bypass_GUI();
        plugin.setUseCluster(true);
        copyTestResources("spb_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        copyTestResources("droplet_1.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("droplet_2.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("droplet_3.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("droplet_4.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("droplet_5.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("droplet_6.tif", getTestDataPath() + tcDirName, tmpPath);
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   null, null, null, null);
                   
        File dataDir = new File(getTestDataPath() + tcDirName);
        File testDir = new File(tmpPath);
        
        // compare output from plugin with reference images
        for (int i = 0; i < expectedImgFiles.length; ++i) {
            final File refJobFile = findJobFile(referenceImgFiles[i], dataDir);
            assertTrue("Reference file [" + dataDir + referenceImgFiles[i] + "] not found!", refJobFile != null);
            String refFile = refJobFile.getAbsoluteFile().toString();
            final File testJobFile = findJobFile(expectedImgFiles[i], testDir);
            assertTrue("Test file [" + testDir + expectedImgFiles[i] + "] not found!", testJobFile != null);
            String testFile = testJobFile.getAbsoluteFile().toString();
            testFile = "./" + testFile.substring(tmpPath.length(), testFile.length());
            compareImageFromIJ(refFile, testFile);
        }

        for (int i = 0; i < expectedFiles.length; ++i) {
            String refFile = findJobFile(referenceFiles[i], dataDir).getAbsoluteFile().toString();
            String testFile = findJobFile(expectedFiles[i], testDir).getAbsoluteFile().toString();
            compareCsvFiles(refFile, testFile);
        }
    }
}
