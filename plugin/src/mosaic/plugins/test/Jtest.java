package mosaic.plugins.test;


import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import mosaic.core.utils.MosaicTest;

import org.scijava.util.FileUtils;


/**
 * 
 * This is the set of test 
 * 
 * @author Pietro Incardona
 *
 */

public class Jtest implements PlugInFilter
{

	/**
	 * 
	 * Run JTest filter
	 * 
	 */
	
	@Override
	public void run(ImageProcessor arg0) 
	{
				
	}
	
	@Override
	public int setup(String arg0, ImagePlus arg1) 
	{
		// Get the User home directory
		String test = MosaicTest.getTestEnvironment();
		File s_file = new File(test + File.separator + "succeful");
		FileUtils.deleteRecursively(s_file);
		
		// Test CSV system
		
		mosaic.core.ipc.Jtest jtestIPC = new mosaic.core.ipc.Jtest();
		jtestIPC.csvtest();
		
		// Test Squassh segmentation
		
		mosaic.bregman.Jtest jtestBR = new mosaic.bregman.Jtest();
		jtestBR.segmentation();

		// Test core utils
		
		mosaic.core.utils.Jtest jtestTS = new mosaic.core.utils.Jtest();
		jtestTS.testtestsegmentation();
	
		// Test core utils
		
		mosaic.core.cluster.Jtest jtestMj = new mosaic.core.cluster.Jtest();
		jtestMj.mergetest();
		
		// Test Region competition segmentation
		
		mosaic.region_competition.Jtest jtestRC = new mosaic.region_competition.Jtest();
		jtestRC.segmentation();
		
		// Tracker
//	    ParticleTracker3DModular_ pt = new ParticleTracker3DModular_();
//	    MosaicTest.<Particle>testPlugin(pt,"Particle Tracker",Particle.class);
	        
		IJ.showMessage("All test SUCCEFULLY completed");
		// Create a file that notify all test has been completed suceffuly
		
		try 
		{
			PrintWriter succeful = new PrintWriter(test + File.separator + "succeful");
			succeful.write(1);
			succeful.close();
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		
		return DONE;
	}
	
}