package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.io.File;

import mosaic.core.cluster.ClusterSession;
import mosaic.core.utils.DataCompression;
import mosaic.core.utils.Segmentation;
import mosaic.core.utils.DataCompression.Algorithm;


/**
 * @author Pietro Incardona
 * 
 * Small utility to merge jobs together
 * 
 */

public class NewClusterProfile implements PlugInFilter
{
	
	@Override
	public void run(ImageProcessor arg0) 
	{}
		
	@Override
	public int setup(String arg0, ImagePlus arg1) 
	{
		GenericDialog gd = new GenericDialog("New cluster profile");
		
		gd.addStringField("profile", "");
		gd.addStringField("address", "");
		gd.addStringField("run_dir", "");
		gd.addChoice("queues", null, null);
		gd.addChoice("compressor", null, null);

		gd.setVisible(true);
		
		return DONE;
	}					
}		