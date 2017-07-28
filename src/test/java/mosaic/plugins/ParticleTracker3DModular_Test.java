package mosaic.plugins;

import org.junit.Assert;
import org.junit.Test;

import mosaic.core.particleLinking.ParticleLinkerGreedy;
import mosaic.test.framework.CommonBase;


public class ParticleTracker3DModular_Test extends CommonBase {
    @Test
    public void testOptimizeRelationMatrixFloatingPointProblem() {
        // TODO: This test should be moved to linker stuff during refactoring of particle tracker.
        
        
        // Special case of floating points:
        // 20.000162f + 356.00015f is equal to 20.000156f + 356.00015f
        // because of limited precision of float number.
        // Previously it cased problem of never ending loop since:
        // (aCostMatrix[i][j] + aCostMatrix[x][y]) - (aCostMatrix[i][y] - aCostMatrix[x][j]);
        // without parentheses was always negative with values given above.
        
        float[][] cost = {{20.000162f, 20.000156f, 400f},
                          {356.00015f, 356.00015f, 400f},
                          {400f, 400f, 0f}};
        
        boolean[][] g = {{false, true, false},
                         {true, false, false},
                         {false, false, true}};
        int[] gX = {1, 0, 2};
        int[] gY = {1, 0, 2};
        
        boolean[][] gExpected = {{false, true, false},
                                 {true, false, false},
                                 {false, false, true}};
        int[] gXExpected = {1, 0, 2};
        int[] gYExpected = {1, 0, 2};

        ParticleLinkerGreedy linker = new ParticleLinkerGreedy();
        linker.optimizeRelationMatrix(2, 2, 400f, cost, g, gX, gY);

        // After all arrays should be same as input
        Assert.assertArrayEquals(gXExpected, gX);
        Assert.assertArrayEquals(gYExpected, gY);
        compareArrays(gExpected, g);
    }
    
    @Test
    public void testOptimizeRelationMatrix() {
        // 100 + 150 is smaller than 100 + 200 so linking of two particles should be switched.
        float[][] cost = {{100, 200f, 400f},
                          {100f, 150f, 400f},
                          {400f, 400f, 0f}};
        
        boolean[][] g = {{false, true, false},
                         {true, false, false},
                         {false, false, true}};
        int[] gX = {1, 0, 2};
        int[] gY = {1, 0, 2};
        
        boolean[][] gExpected = {{true, false, false},
                                 {false, true, false},
                                 {false, false, true}};
        int[] gXExpected = {0, 1, 2};
        int[] gYExpected = {0, 1, 2};

        ParticleLinkerGreedy linker = new ParticleLinkerGreedy();
        linker.optimizeRelationMatrix(2, 2, 400f, cost, g, gX, gY);

        // After all arrays should be same as input
        Assert.assertArrayEquals(gXExpected, gX);
        Assert.assertArrayEquals(gYExpected, gY);
        compareArrays(gExpected, g);
    }
    
    @Test
    public void test2StraightTracks()  {
        // Define test data
        final String tcDirName           = "ParticleTracker/StraightTracks/";
        final String setupString         = "run";
        final String macroOptions        = "object=1.000 dynamics_=2 optimizer=Greedy radius=1 cutoff=0 per/abs=1 absolute link=1 displacement=13 dynamics=[Constant velocity]";
        final String inputFile           =  "linkStraight.tif";
        final String[] expectedImgFiles  = {};
        final String[] referenceImgFiles = {};
        final String[] expectedFiles     = {"Traj_linkStraight.tif.xml", "Traj_linkStraight.tif.txt", "Traj_linkStraight.tif.csv"};
        final String[] referenceFiles    = {"linkStraight.xml", "linkStraight.txt", "linkStraight.csv"};

        // Create tested plugIn
        final ParticleTracker3DModular_ plugin = new ParticleTracker3DModular_();

        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testThreeFrames()  {
        // Define test data
        final String tcDirName           = "ParticleTracker/3frames/";
        final String setupString         = "run";
        final String macroOptions        = "radius=3 cutoff=0 saveMss per/abs=0.6 link=2 displacement=12 dynamics=Brownian";
        final String inputFile           =  "threeFramesVirusMovie.tif";
        final String[] expectedImgFiles  = {};
        final String[] referenceImgFiles = {};
        final String[] expectedFiles     = {"Traj_threeFramesVirusMovie.tif.xml", "Traj_threeFramesVirusMovie.tif.csv"};
        final String[] referenceFiles    = {"threeFramesVirusMovieMssMsd.xml", "threeFramesVirusMovieTrajectories.csv"};
        // Create tested plugIn
        final ParticleTracker3DModular_ plugin = new ParticleTracker3DModular_();

        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }

    @Test
    public void testSixFrames()  {
        // Define test data
        final String tcDirName           = "ParticleTracker/ArtificialImgTest/";
        final String setupString         = "run";
        final String macroOptions        = "object=1.001 dynamics_=1.002 optimizer=Hungarian radius=5 cutoff=0.1 per/abs=0.3 link=1 displacement=5 dynamics=Brownian saveMss";
        final String inputFile           =  "ArtificialTest.tif";
        final String[] expectedImgFiles  = {};
        final String[] referenceImgFiles = {};
        final String[] expectedFiles     = {"Traj_ArtificialTest.tif.xml", "Traj_ArtificialTest.tif.txt", "Traj_ArtificialTest.tif.csv", "TrajMss_ArtificialTest.tif.csv"};
        final String[] referenceFiles    = {"ArtificialTestMssMsd.xml", "ArtificialTestReport.txt", "ArtificialTestTrajectories.csv", "TrajMss_ArtificialTest.tif.csv"};

        // Create tested plugIn
        final ParticleTracker3DModular_ plugin = new ParticleTracker3DModular_();

        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testSixFramesDynamicsConstantVelocity()  {
        // Define test data
        final String tcDirName           = "ParticleTracker/ArtificialImgTest/";
        final String setupString         = "run";
        final String macroOptions        = "object=1.001 dynamics_=1.002 optimizer=Hungarian radius=5 cutoff=0.1 per/abs=0.3 link=1 displacement=5 dynamics=[Constant velocity]";
        final String inputFile           =  "ArtificialTest.tif";
        final String[] expectedImgFiles  = {};
        final String[] referenceImgFiles = {};
        final String[] expectedFiles     = {"Traj_ArtificialTest.tif.xml", "Traj_ArtificialTest.tif.txt", "Traj_ArtificialTest.tif.csv"};
        final String[] referenceFiles    = {"ArtificialTestMssMsd.xml", "ArtificialTestReport.txt", "ArtificialTestTrajectories.csv"};

        // Create tested plugIn
        final ParticleTracker3DModular_ plugin = new ParticleTracker3DModular_();

        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }   
    
    @Test
    public void testSixFramesDynamicsStraight()  {
        // Define test data
        final String tcDirName           = "ParticleTracker/ArtificialImgTest/";
        final String setupString         = "run";
        final String macroOptions        = "object=1.001 dynamics_=1.002 optimizer=Hungarian radius=5 cutoff=0.1 per/abs=0.3 link=1 displacement=5 dynamics=[Straight lines]";
        final String inputFile           =  "ArtificialTest.tif";
        final String[] expectedImgFiles  = {};
        final String[] referenceImgFiles = {};
        final String[] expectedFiles     = {"Traj_ArtificialTest.tif.xml", "Traj_ArtificialTest.tif.txt", "Traj_ArtificialTest.tif.csv"};
        final String[] referenceFiles    = {"ArtificialTestMssMsd.xml", "ArtificialTestReport.txt", "ArtificialTestTrajectories.csv"};

        // Create tested plugIn
        final ParticleTracker3DModular_ plugin = new ParticleTracker3DModular_();

        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testLoadFromCsv()  {
        // TODO: input image should not be needed when reading from csv. Unfortunatelly current implementation of 
        //       tracker need it for path to save output files.
        
        // Define test data
        final String tcDirName           = "ParticleTracker/LoadFromCsv/";
        final String setupString         = "run";
        final String macroOptions        = "csv test=" + tmpPath + "test.csv link=2 displacement=10 size=0 intensity=-1 dynamics=[Straight lines]";
        final String inputFile           = null;
        final String[] expectedImgFiles  = {};
        final String[] referenceImgFiles = {};
        final String[] expectedFiles     = {"Traj_text_files.csv", "Traj_text_files.txt"};
        final String[] referenceFiles    = {"resultTrajectories.csv", "resultReport.txt"};

        // Create tested plugIn
        final ParticleTracker3DModular_ plugin = new ParticleTracker3DModular_();
        copyTestResources("test.csv", getTestDataPath() + tcDirName, tmpPath);
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testLoadFromTextFiles()  {
        
        // Define test data
        final String tcDirName           = "ParticleTracker/LoadFromText/";
        final String setupString         = "run";
        final String macroOptions        = "multiple test=" + tmpPath + "/textFrames/frame_0 link=2 displacement=10 dynamics=[Constant velocity]";
        final String inputFile           = null;
        final String[] expectedImgFiles  = {};
        final String[] referenceImgFiles = {};
        final String[] expectedFiles     = {"textFrames/Traj_text_files.csv", "textFrames/Traj_text_files.txt"};
        final String[] referenceFiles    = {"resultTrajectories.csv", "resultReport.txt"};

        // Create tested plugIn
        final ParticleTracker3DModular_ plugin = new ParticleTracker3DModular_();
        copyTestResources("textFrames", getTestDataPath() + tcDirName, tmpPath);
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testOutputFromSquassh()  {
        
        // Define test data
        final String tcDirName           = "ParticleTracker/TrackSquasshOutput/";
        final String setupString         = "run";
        final String macroOptions        = "link=3 displacement=10 dynamics=Brownian size=0 intensity=-1";
        final String inputFile           = "sequence.tif";
        final String[] expectedImgFiles  = {};
        final String[] referenceImgFiles = {};
        final String[] expectedFiles     = {"__ObjectData.csv/Traj_sequence.tif.csv", "__ObjectData.csv/Traj_sequence.tif.txt"};
        final String[] referenceFiles    = {"Traj_sequence.tif.csv", "Traj_sequence.tif.txt"};

        // Create tested plugIn
        final ParticleTracker3DModular_ plugin = new ParticleTracker3DModular_();
        copyTestResources("__ObjectData.csv", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("stitch__ObjectData.csv", getTestDataPath() + tcDirName, tmpPath);
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testTracking3D()  {
        // Define test data
        final String tcDirName           = "ParticleTracker/Tracking3D/";
        final String setupString         = "run";
        final String macroOptions        = "radius=6 cutoff=0.01 per/abs=0.5 link=1 displacement=100 dynamics=Brownian";
        final String inputFile           = "Regions_size_5_5_100_100.tif";
        final String[] expectedImgFiles  = {};
        final String[] referenceImgFiles = {};
        final String[] expectedFiles     = {"Traj_Regions_size_5_5_100_100.tif.xml", "Traj_Regions_size_5_5_100_100.tif.txt", "Traj_Regions_size_5_5_100_100.tif.csv"};
        final String[] referenceFiles    = {"report.xml", "Traj_Regions_size_5_5_100_100.tif.txt", "Traj_Regions_size_5_5_100_100.tif.csv"};

        // Create tested plugIn
        final ParticleTracker3DModular_ plugin = new ParticleTracker3DModular_();

        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
}
