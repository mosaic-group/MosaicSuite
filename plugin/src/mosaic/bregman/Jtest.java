package mosaic.bregman;

import mosaic.bregman.output.Region3DColocRScript;
import mosaic.bregman.output.Region3DRScript;
import mosaic.core.utils.MosaicTest;
import mosaic.plugins.BregmanGLM_Batch;

import org.apache.log4j.Logger;
import org.junit.Test;

public class Jtest 
{
    protected static final Logger logger = Logger.getLogger(Jtest.class);
    
	@Test
	public void segmentation() 
	{
		BregmanGLM_Batch BG = new BregmanGLM_Batch();
		
		BG.bypass_GUI();
		
		// test the cluster
		logger.info("----------------------- TestCase: Squassh_cluster -----------------------");
		BG.setUseCluster(true);
		MosaicTest.<Region3DRScript>testPlugin(BG,"Squassh_cluster",Region3DRScript.class);
		
		logger.info("----------------------- TestCase: Squassh_testa -----------------------");
		BG.setUseCluster(false);
		MosaicTest.<Region3DColocRScript>testPlugin(BG,"Squassh_testa",Region3DColocRScript.class);
		
		logger.info("----------------------- TestCase: Squassh -----------------------");
		BG.setUseCluster(false);
		MosaicTest.<Region3DRScript>testPlugin(BG,"Squassh",Region3DRScript.class);
	}
}
