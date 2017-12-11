package mosaic.plugins;

import org.junit.Test;

import mosaic.test.framework.CommonBase;


public class DiscreteRegionSamplingTest extends CommonBase {

    /**
     * "useBiasedProposal": false,
     * "usePairProposal": false,
     * "allowFusion": false,
     * "allowFission": true,
     * "allowHandles": true,
     */
    @Test
    public void testDrs1()  {
        
        // Define test data
        final String tcDirName           = "DiscreteRegionSampling/drs1/";
        final String setupString         = "DRS";
        final String macroOptions        = "";
        final String inputFile           = "squareWithHole2100pts.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/squareWithHole2100pts_seg_c1.tif", "__prob_c1.tif/squareWithHole2100pts_prob_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/squareWithHole2100pts_seg_c1.tif", "__prob_c1.tif/squareWithHole2100pts_prob_c1.tif"};
        final String[] expectedFiles     = {};
        final String[] referenceFiles    = {};

        // Create tested plugIn
        final DiscreteRegionSampling plugin = new DiscreteRegionSampling();
        copyTestResources("drs_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles, 
                   expectedFiles, referenceFiles);
    }
    
    /**
     * "useBiasedProposal": false,
     * "usePairProposal": true,
     * "allowFusion": false,
     * "allowFission": false,
     * "allowHandles": true,
     */
    @Test
    public void testDrs2()  {
        
        // Define test data
        final String tcDirName           = "DiscreteRegionSampling/drs2/";
        final String setupString         = "DRS";
        final String macroOptions        = "";
        final String inputFile           = "squareWithHole2100pts.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/squareWithHole2100pts_seg_c1.tif", "__prob_c1.tif/squareWithHole2100pts_prob_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/squareWithHole2100pts_seg_c1.tif", "__prob_c1.tif/squareWithHole2100pts_prob_c1.tif"};
        final String[] expectedFiles     = {};
        final String[] referenceFiles    = {};

        // Create tested plugIn
        final DiscreteRegionSampling plugin = new DiscreteRegionSampling();
        copyTestResources("drs_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    /**
     * "useBiasedProposal": false,
     * "usePairProposal": false,
     * "allowFusion": true,
     * "allowFission": true,
     * "allowHandles": true,
     */
    @Test
    public void testDrs3()  {
        
        // Define test data
        final String tcDirName           = "DiscreteRegionSampling/drs3/";
        final String setupString         = "DRS";
        final String macroOptions        = "";
        final String inputFile           = "squareWithHole2100pts.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/squareWithHole2100pts_seg_c1.tif", "__prob_c1.tif/squareWithHole2100pts_prob_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/squareWithHole2100pts_seg_c1.tif", "__prob_c1.tif/squareWithHole2100pts_prob_c1.tif"};
        final String[] expectedFiles     = {};
        final String[] referenceFiles    = {};

        // Create tested plugIn
        final DiscreteRegionSampling plugin = new DiscreteRegionSampling();
        copyTestResources("drs_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    /**
     * "useBiasedProposal": false,
     * "usePairProposal": false,
     * "allowFusion": true,
     * "allowFission": true,
     * "allowHandles": false,
     */
    @Test
    public void testDrs4()  {
        
        // Define test data
        final String tcDirName           = "DiscreteRegionSampling/drs4/";
        final String setupString         = "DRS";
        final String macroOptions        = "";
        final String inputFile           = "squareWithHole2100pts.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/squareWithHole2100pts_seg_c1.tif", "__prob_c1.tif/squareWithHole2100pts_prob_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/squareWithHole2100pts_seg_c1.tif", "__prob_c1.tif/squareWithHole2100pts_prob_c1.tif"};
        final String[] expectedFiles     = {};
        final String[] referenceFiles    = {};

        // Create tested plugIn
        final DiscreteRegionSampling plugin = new DiscreteRegionSampling();
        copyTestResources("drs_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    /**
     * "useBiasedProposal": true,
     * "usePairProposal": false,
     * "allowFusion": true,
     * "allowFission": true,
     * "allowHandles": true,
     */
    @Test
    public void testDrs5()  {
        
        // Define test data
        final String tcDirName           = "DiscreteRegionSampling/drs5/";
        final String setupString         = "DRS";
        final String macroOptions        = "";
        final String inputFile           = "squareWithHole2100pts.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/squareWithHole2100pts_seg_c1.tif", "__prob_c1.tif/squareWithHole2100pts_prob_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/squareWithHole2100pts_seg_c1.tif", "__prob_c1.tif/squareWithHole2100pts_prob_c1.tif"};
        final String[] expectedFiles     = {};
        final String[] referenceFiles    = {};

        // Create tested plugIn
        final DiscreteRegionSampling plugin = new DiscreteRegionSampling();
        copyTestResources("drs_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    /**
     * "useBiasedProposal": false,
     * "usePairProposal": true,
     * "allowFusion": true,
     * "allowFission": true,
     * "allowHandles": true,
     */
    @Test
    public void testDrs6()  {
        
        // Define test data
        final String tcDirName           = "DiscreteRegionSampling/drs6/";
        final String setupString         = "DRS";
        final String macroOptions        = "";
        final String inputFile           = "squareWithHole2100pts.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/squareWithHole2100pts_seg_c1.tif", "__prob_c1.tif/squareWithHole2100pts_prob_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/squareWithHole2100pts_seg_c1.tif", "__prob_c1.tif/squareWithHole2100pts_prob_c1.tif"};
        final String[] expectedFiles     = {};
        final String[] referenceFiles    = {};

        // Create tested plugIn
        final DiscreteRegionSampling plugin = new DiscreteRegionSampling();
        copyTestResources("drs_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    /**
     * "useBiasedProposal": true,
     * "usePairProposal": true,
     * "allowFusion": true,
     * "allowFission": true,
     * "allowHandles": true,
     */
    @Test
    public void testDrs7()  {
        
        // Define test data
        final String tcDirName           = "DiscreteRegionSampling/drs7/";
        final String setupString         = "DRS";
        final String macroOptions        = "normalize=false";
        final String inputFile           = "sphere-1.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/sphere-1_seg_c1.tif", "__prob_c1.tif/sphere-1_prob_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/sphere-1_seg_c1.tif", "__prob_c1.tif/sphere-1_prob_c1.tif"};        
        final String[] expectedFiles     = {};
        final String[] referenceFiles    = {};
        
        // Create tested plugIn
        final DiscreteRegionSampling plugin = new DiscreteRegionSampling();
        copyTestResources("drs_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
}
