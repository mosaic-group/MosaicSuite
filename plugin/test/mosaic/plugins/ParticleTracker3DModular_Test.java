package mosaic.plugins;

import org.junit.Test;

import mosaic.test.framework.CommonBase;


public class ParticleTracker3DModular_Test extends CommonBase {

    @Test
    public void testThreeFrames()  {
        // Define test data
        final String tcDirName           = "ParticleTracker/3frames/";
        final String setupString         = "run";
        final String macroOptions        = "radius=3 cutoff=0 per/abs=0.6 link=2 displacement=12 dynamics=Brownian";
        final String inputFile           =  "threeFramesVirusMovie.tif";
        final String[] expectedImgFiles  = {};
        final String[] referenceImgFiles = {};
        final String[] expectedFiles     = {"report.xml", "Traj_threeFramesVirusMovie.tif.txt", "Traj_threeFramesVirusMovie.tif.csv"};
        final String[] referenceFiles    = {"threeFramesVirusMovieMssMsd.xml", "threeFramesVirusMovieReport.txt", "threeFramesVirusMovieTrajectories.csv"};

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
        final String macroOptions        = "object=1.001 dynamics_=1.002 optimizer=Hungarian radius=5 cutoff=1 per/abs=0.3 link=1 displacement=5 dynamics=straight";
        final String inputFile           =  "ArtificialTest.tif";
        final String[] expectedImgFiles  = {};
        final String[] referenceImgFiles = {};
        final String[] expectedFiles     = {"report.xml", "Traj_ArtificialTest.tif.txt", "Traj_ArtificialTest.tif.csv"};
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
}
