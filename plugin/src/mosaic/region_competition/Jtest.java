package mosaic.region_competition;


import mosaic.core.utils.MosaicTest;
import mosaic.core.utils.Segmentation;
import mosaic.plugins.Region_Competition;
import mosaic.region_competition.output.RCOutput;

import org.apache.log4j.Logger;
import org.junit.Test;


public class Jtest {

    protected static final Logger logger = Logger.getLogger(Jtest.class);

    @Test
    public void segmentation() {
        logger.info("----------------------- TestCase: Region_Competition -----------------------");
        Segmentation BG = new Region_Competition();
        MosaicTest.<RCOutput> testPlugin(BG, "Region_Competition", RCOutput.class);
    }
}
