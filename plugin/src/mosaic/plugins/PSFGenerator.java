package mosaic.plugins;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import mosaic.core.psf.GeneratePSF;

class PSFGenerator implements PlugInFilter
{

	@Override
	public void run(ImageProcessor arg0) 
	{
		GeneratePSF gpsf = new GeneratePSF();
		
	}

	@Override
	public int setup(String arg0, ImagePlus arg1) {
		// TODO Auto-generated method stub
		return 0;
	}

}
