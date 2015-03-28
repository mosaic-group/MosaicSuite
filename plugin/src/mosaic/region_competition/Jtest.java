package mosaic.region_competition;

import mosaic.core.utils.MosaicTest;
import mosaic.core.utils.Segmentation;
import mosaic.plugins.Region_Competition;
import mosaic.region_competition.output.RCOutput;

import org.junit.Test;

public class Jtest 
{	
	@Test
	public void segmentation() 
	{
		Segmentation BG = new Region_Competition();
		MosaicTest.<RCOutput>testPlugin(BG,"Region_Competition",RCOutput.class);
	}
}
