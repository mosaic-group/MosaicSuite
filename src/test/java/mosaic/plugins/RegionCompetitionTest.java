package mosaic.plugins;

import java.io.File;

import org.junit.Test;

import ij.macro.Interpreter;
import mosaic.test.framework.CommonBase;


public class RegionCompetitionTest extends CommonBase {

    @Test
    public void testDot()  {
        
        // Define test data
        final String tcDirName           = "RegionCompetition/dot/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics normalize_input_image normalize=true";
        final String inputFile           = "dot.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/dot_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/dot_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/dot_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/dot_ObjectsData_c1.csv"};

        // Create tested plugIn
        final RegionCompetition plugin = new RegionCompetition();
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
        final String tcDirName           = "RegionCompetition/uc_psf/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics normalize_input_image";
        final String inputFile           = "uc_data.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/uc_data_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/uc_data_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/uc_data_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/uc_data_ObjectsData_c1.csv"};

        // Create tested plugIn
        final RegionCompetition plugin = new RegionCompetition();
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
        final String tcDirName           = "RegionCompetition/fusionCheck/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics normalize_input_image";
        final String inputFile           = "1thing.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/1thing_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/1thing_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/1thing_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/1thing_ObjectsData_c1.csv"};

        // Create tested plugIn
        final RegionCompetition plugin = new RegionCompetition();
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
        final String tcDirName           = "RegionCompetition/sphere_3d/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics normalize_input_image";
        final String inputFile           = "sphere.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/sphere_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/sphere_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/sphere_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/sphere_ObjectsData_c1.csv"};

        // Create tested plugIn
        final RegionCompetition plugin = new RegionCompetition();
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
        final String tcDirName           = "RegionCompetition/twoBars/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics normalize_input_image";
        final String inputFile           = "twoBars.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/twoBars_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/twoBars_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/twoBars_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/twoBars_ObjectsData_c1.csv"};

        // Create tested plugIn
        final RegionCompetition plugin = new RegionCompetition();
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
        final String tcDirName           = "RegionCompetition/labelImgFromFile/";
        final String setupString         = "run";
        final String macroOptions        = "labelimage=label.tif show_and_save_statistics normalize_input_image";
        final String inputFile           = "object.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/object_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/object_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/object_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/object_ObjectsData_c1.csv"};

        // Create tested plugIn
        final RegionCompetition plugin = new RegionCompetition();
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
        final String tcDirName           = "RegionCompetition/dotCluster/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics normalize_input_image process username=" + System.getProperty("user.name");
        final String inputFile           = "dot.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/dot_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/dot_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/dot_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/dot_ObjectsData_c1.csv"};

        // Create tested plugIn
        final RegionCompetition plugin = new RegionCompetition();
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
}
