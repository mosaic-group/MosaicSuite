package mosaic.plugins.test;

import mosaic.core.detection.Particle;
import mosaic.core.utils.MosaicTest;
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
}
