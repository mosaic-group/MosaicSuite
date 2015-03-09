package mosaic.plugins.test;

import mosaic.core.detection.Particle;
import mosaic.core.ipc.NoCSV;
import mosaic.core.utils.MosaicTest;
import mosaic.plugins.Naturalization;
import mosaic.plugins.ParticleTracker3DModular_;

import org.junit.Test;

public class Jtest 
{
	@Test
	public void segmentation() 
	{
		ParticleTracker3DModular_ pt = new ParticleTracker3DModular_();
		
		// test the particle tracker
		
		MosaicTest.<Particle>testPlugin(pt,"Particle Tracker",Particle.class);
	}
	
	@Test
	public void naturalization()
	{
		Naturalization nt = new Naturalization();
		
		// naturalization
		
		MosaicTest.<NoCSV>testPlugin(nt,"Naturalization",NoCSV.class);
	}
}
