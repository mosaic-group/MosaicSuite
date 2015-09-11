package mosaic.plugins.test;


import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import mosaic.core.detection.Particle;
import mosaic.core.utils.MosaicTest;
import mosaic.plugins.ParticleTracker3DModular_;

import org.scijava.util.FileUtils;
import org.apache.log4j.Logger;

/**
 * 
 * This is the set of test 
 * 
 * @author Pietro Incardona
 *
 */

public class Jtest implements PlugInFilter
{
    protected static final Logger logger = Logger.getLogger(Jtest.class);

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
		logger.info("========================== TestSuite: CSV ===================================");
		mosaic.core.ipc.Jtest jtestIPC = new mosaic.core.ipc.Jtest();
		jtestIPC.csvtest();
		
		// Test Squassh segmentation
		logger.info("========================== TestSuite: bregman.Jtest  ===================================");
		mosaic.bregman.Jtest jtestBR = new mosaic.bregman.Jtest();
		jtestBR.segmentation();

		// Test core utils
		logger.info("========================== TestSuite: core.utils.Jtest ===================================");
		mosaic.core.utils.Jtest jtestTS = new mosaic.core.utils.Jtest();
		jtestTS.testtestsegmentation();
	
		// Test core utils
		logger.info("========================== TestSuite: core.cluster.Jtest ===================================");
		mosaic.core.cluster.Jtest jtestMj = new mosaic.core.cluster.Jtest();
		jtestMj.mergetest();
		
		// Test Region competition segmentation
		logger.info("========================== TestSuite: region_competition.Jtest ===================================");
		mosaic.region_competition.Jtest jtestRC = new mosaic.region_competition.Jtest();
		jtestRC.segmentation();
		
		// Tracker
		// TODO: Not working. Seems that test data are not complete (mandatory .cfg file is missing)
//		logger.info("========================== TestSuite: ParticleTracker3DModular_ ===================================");
//	    ParticleTracker3DModular_ pt = new ParticleTracker3DModular_();
//	    MosaicTest.<Particle>testPlugin(pt,"particle_tracker",Particle.class);
	    
	    
		logger.info("All tests SUCCESSFULLY completed");
		
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