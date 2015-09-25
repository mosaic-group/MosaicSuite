package mosaic.bregman;


import mosaic.bregman.output.Region3DColocRScript;
import mosaic.bregman.output.Region3DRScript;
import mosaic.core.utils.MosaicTest;
import mosaic.plugins.BregmanGLM_Batch;
import mosaic.plugins.utils.TimeMeasurement;

import org.apache.log4j.Logger;
import org.junit.Test;


public class Jtest {

    protected static final Logger logger = Logger.getLogger(Jtest.class);

    @Test
    public void segmentation() {
        final BregmanGLM_Batch BG = new BregmanGLM_Batch();

        BG.bypass_GUI();
        final TimeMeasurement tm = new TimeMeasurement();

        // test the cluster
        logger.info("----------------------- TestCase: Squassh_cluster -----------------------");
        BG.setUseCluster(true);
        MosaicTest.<Region3DRScript> testPlugin(BG, "Squassh_cluster", Region3DRScript.class);
        tm.logLapTimeSec("----------------------- Squassh_cluster");

        logger.info("----------------------- TestCase: Squassh_testa -----------------------");
        BG.setUseCluster(false);
        MosaicTest.<Region3DColocRScript> testPlugin(BG, "Squassh_testa", Region3DColocRScript.class);
        tm.logLapTimeSec("----------------------- Squassh_testa");

        logger.info("----------------------- TestCase: Squassh -----------------------");
        BG.setUseCluster(false);
        MosaicTest.<Region3DRScript> testPlugin(BG, "Squassh", Region3DRScript.class);
        tm.logLapTimeSec("----------------------- Squassh");
    }
}
