package mosaic.bregman;

import static org.junit.Assert.*;
import ij.IJ;
import ij.ImagePlus;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import mosaic.bregman.output.Region3DRScript;
import mosaic.core.ipc.InterPluginCSV;
import mosaic.core.utils.ImgTest;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Segmentation;
import mosaic.core.utils.ShellCommand;
import mosaic.plugins.BregmanGLM_Batch;
import mosaic.plugins.Region_Competition;
import mosaic.region_competition.output.RCOutput;
import net.imglib2.img.Img;

import org.junit.Test;

public class Jtest 
{
	@Test
	public void segmentation() 
	{
		BregmanGLM_Batch BG = new BregmanGLM_Batch();
		
		// test the cluster
		
/*		BG.setUseCluster(false);
		MosaicUtils.<Region3DRScript>testSegmentation(BG,"Squassh_testa",Region3DRScript.class);
		
		BG.setUseCluster(false);
		MosaicUtils.<Region3DRScript>testSegmentation(BG,"Squassh",Region3DRScript.class);*/
		
		BG.setUseCluster(true);
		MosaicUtils.<Region3DRScript>testSegmentation(BG,"Squassh_cluster",Region3DRScript.class);
	}
}
