package mosaic.bregman;

import mosaic.bregman.output.Region3DColocRScript;
import mosaic.bregman.output.Region3DRScript;
import mosaic.core.utils.MosaicTest;
import mosaic.plugins.BregmanGLM_Batch;

import org.junit.Test;

public class Jtest 
{
	//@Test
	public void segmentation() 
	{
		BregmanGLM_Batch BG = new BregmanGLM_Batch();
		
		BG.setUseCluster(false);
		MosaicTest.<Region3DColocRScript>testPlugin(BG,"Squassh_testa",Region3DColocRScript.class);
		
		BG.setUseCluster(false);
		MosaicTest.<Region3DRScript>testPlugin(BG,"Squassh",Region3DRScript.class);
		
		// test the cluster
		
		BG.setUseCluster(true);
		MosaicTest.<Region3DRScript>testPlugin(BG,"Squassh_cluster",Region3DRScript.class);
		
		// Test batch system
		
		
	}
}
