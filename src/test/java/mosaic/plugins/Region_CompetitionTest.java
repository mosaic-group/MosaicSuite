package mosaic.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.plugin.filter.PlugInFilterRunner;
import mosaic.test.framework.CommonBase;


public class Region_CompetitionTest extends CommonBase {

    @Test
    public void testDot()  {
        
        // Define test data
        final String tcDirName           = "Region_Competition/dot/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics show_normalized normalize=true";
        final String inputFile           = "dot.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/dot_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/dot_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/dot_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/dot_ObjectsData_c1.csv"};

        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testPsf()  {
        
        // Define test data
        final String tcDirName           = "Region_Competition/uc_psf/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics show_normalized";
        final String inputFile           = "uc_data.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/uc_data_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/uc_data_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/uc_data_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/uc_data_ObjectsData_c1.csv"};

        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        copyTestResources("psf_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        copyTestResources("psf_file_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        copyTestResources("uc_psf.tif", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testFusionCheck()  {
        
        // Define test data
        final String tcDirName           = "Region_Competition/fusionCheck/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics show_normalized";
        final String inputFile           = "1thing.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/1thing_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/1thing_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/1thing_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/1thing_ObjectsData_c1.csv"};

        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testSphere3D()  {
        
        // Define test data
        final String tcDirName           = "Region_Competition/sphere_3d/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics show_normalized";
        final String inputFile           = "sphere.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/sphere_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/sphere_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/sphere_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/sphere_ObjectsData_c1.csv"};

        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testTwoBars()  {
        
        // Define test data
        final String tcDirName           = "Region_Competition/twoBars/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics show_normalized";
        final String inputFile           = "twoBars.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/twoBars_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/twoBars_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/twoBars_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/twoBars_ObjectsData_c1.csv"};

        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testLabelImageFromFile()  {
        
        // Define test data
        final String tcDirName           = "Region_Competition/labelImgFromFile/";
        final String setupString         = "run";
        final String macroOptions        = "labelimage=label.tif show_and_save_statistics show_normalized";
        final String inputFile           = "object.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/object_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/object_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/object_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/object_ObjectsData_c1.csv"};

        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        copyTestResources("label.tif", getTestDataPath() + tcDirName, tmpPath);
        
        // A little hack - I have no found the other way to load second image for test purposes.
        Interpreter.batchMode = true;
        Interpreter.addBatchModeImage(loadImagePlus(tmpPath + "/label.tif"));

        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testDotCluster()  {
        
        // Define test data
        final String tcDirName           = "Region_Competition/dotCluster/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics show_normalized process username=" + System.getProperty("user.name");
        final String inputFile           = "dot.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/dot_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/dot_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/dot_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/dot_ObjectsData_c1.csv"};

        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   null, null, null, null);
        
        File dataDir = new File(getTestDataPath() + tcDirName);
        File testDir = new File(tmpPath);
        // compare output from plugin with reference images
        for (int i = 0; i < expectedImgFiles.length; ++i) {
            String refFile = findJobFile(referenceImgFiles[i], dataDir).getAbsoluteFile().toString();
            String testFile = findJobFile(expectedImgFiles[i], testDir).getAbsoluteFile().toString();
            testFile = "./" + testFile.substring(tmpPath.length(), testFile.length());
            compareImageFromIJ(refFile, testFile);
        }

        for (int i = 0; i < expectedFiles.length; ++i) {
            String refFile = findJobFile(referenceFiles[i], dataDir).getAbsoluteFile().toString();
            String testFile = findJobFile(expectedFiles[i], testDir).getAbsoluteFile().toString();
            compareCsvFiles(refFile, testFile);
        }
    }
    
    // TODO: Temporary stuff for running in DRS mode.
    // run("Region Competition", "inputimage=1thing.tif labelimage=[] keep_frames show_and_save_statistics segmentation=[Discrete Region Sampling]");
    @Test
//    @Ignore
    public void testTwoBarsDRS()  {
        // COPY/PASTE BOX:
        //    rect7x7init
        //    membrane_2D_init
        // 2sq_init
        // cshapeInit
        // cellsInit
        
        // Define test data
        final String tcDirName           = "Region_Competition/DRS/";
        final String setupString         = "run";
        final String macroOptions        = "labelimage=cellsInit.tif show_and_save_statistics segmentation=[Discrete Region Sampling] normalize=false";
        String inputFile           = null;
//        inputFile           = "5x5.tif";
//        inputFile           = "cross6x6.tif";
//        inputFile           = "2sq.tif";
//        inputFile = "membrane_2D_crop.tif";
//        inputFile = "rect7x7.tif";
//        inputFile = "cshape.tif";
        inputFile = "cells.tif";
        
        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", getTestDataPath() + tcDirName, "/tmp");
        
        copyTestResources("cellsInit.tif", getTestDataPath() + tcDirName, tmpPath);
        
        // A little hack - I have no found the other way to load second image for test purposes.
        Interpreter.batchMode = true;
        Interpreter.addBatchModeImage(loadImagePlus(tmpPath + "/cellsInit.tif"));
        
        
        tcPath = getTestDataPath() + tcDirName;
        String aInputFile = inputFile;
        copyTestResources(aInputFile, tcPath, tmpPath);
        Thread.currentThread().setName("Run$_" + plugin.getClass().getSimpleName());
        Macro.setOptions(Thread.currentThread(), macroOptions);
        final ImagePlus ip = loadImagePlus(tmpPath + aInputFile);
        WindowManager.setTempCurrentImage(ip);
        new PlugInFilterRunner(plugin, "pluginTest", setupString);
        
//        sleep(100000);
    }
    
    @Test
    @Ignore
    public void xxx() {
        ArrayList<Boolean> a = new ArrayList<>(Arrays.asList(new Boolean[10]));
//        Collections.fill(a, Boolean.FALSE);
        System.out.println(a);
    }
}
