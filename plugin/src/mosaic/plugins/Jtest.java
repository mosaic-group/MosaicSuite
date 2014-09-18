package mosaic.plugins;


import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;


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
		// Test particle tracker
		
		mosaic.core.ipc.Jtest jtestIPC = new mosaic.core.ipc.Jtest();
		
		jtestIPC.csvtest();
		
		mosaic.core.utils.Jtest jtestTS = new mosaic.core.utils.Jtest();
		
		jtestTS.testtestsegmentation();
	
		mosaic.core.cluster.Jtest jtestMj = new mosaic.core.cluster.Jtest();
		
		jtestMj.mergetest();
		
		mosaic.bregman.Jtest jtestBR = new mosaic.bregman.Jtest();
		
		jtestBR.segmentation();
		
		mosaic.region_competition.Jtest jtestRC = new mosaic.region_competition.Jtest();
		
		jtestRC.segmentation();
		
		IJ.showMessage("All test SUCCEFULLY completed");
		
		return DONE;
	}
	
}