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

}
