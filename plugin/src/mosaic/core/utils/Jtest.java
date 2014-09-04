package mosaic.core.utils;

import static org.junit.Assert.*;

import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import mosaic.bregman.output.Region3DRScript;
import mosaic.plugins.BregmanGLM_Batch;

import org.junit.Test;

/**
 * 
 * It test the testsegmentation procedure of mosaic utils
 * 
 */

public class Jtest 
{
	class segStub implements Segmentation
	{
		int cnt = 0;
		
		@Override
		public void run(ImageProcessor arg0) 
		{
			String Basefile = testImg[cnt].img[0].substring(0,testImg[cnt].img[0].lastIndexOf(File.separator)) + File.separator + "test";
			ShellCommand.copy(new File(Basefile), new File(to), null);
			cnt++;
		}

		@Override
		public int setup(String arg0, ImagePlus arg1) 
		{
			return 4096;
		}

		@Override
		public String[] getMask(ImagePlus imp) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String[] getRegionList(ImagePlus imp) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void closeAll() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	ImgTest[] testImg;
	String to;
	
	@Test
	public void testtestsegmentation() 
	{
		segStub BG = new segStub();
		
		// test the cluster
		
		testImg = MosaicUtils.getTestImages("job_compare_test");
		to = IJ.getDirectory("temp") + File.separator + "test";
		MosaicUtils.<Region3DRScript>testPlugin(BG,"job_compare_test",Region3DRScript.class);
	}
	
	@Test
	public void coreUtilGeneral()
	{
		String test = MosaicUtils.removeExtension("tmp_1.tif");
		if (!test.equals("tmp_1"))
		{
			fail("Remove extension: expected tmp_1 got " + test);
		}
	}
}