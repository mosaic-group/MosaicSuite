package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import io.scif.img.ImgOpener;

import java.io.File;

import mosaic.core.cluster.ClusterSession;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Segmentation;


/**
 * @author Pietro Incardona
 * 
 * Class filter used as a callback for debugging
 * 
 */

public class Debug implements PlugInFilter
{	
	@Override
	public void run(ImageProcessor arg0) 
	{
		
	}
		
	@Override
	public int setup(String arg0, ImagePlus arg1)
	{
		ImgOpener io = new ImgOpener();
		
		IJ.showMessage(IJ.getDirectory("temp"));
		
		mosaic.core.utils.Jtest ms = new mosaic.core.utils.Jtest();
		
		ms.coreUtilGeneral();
		
		return DONE;
	}
}
