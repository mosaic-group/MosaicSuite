package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.core.ipc.InterPluginCSV;
import mosaic.core.particleLinking.ParticleLinker;
import mosaic.core.particleLinking.ParticleLinkerHun;
import mosaic.core.particleLinking.linkerOptions;

public class GT_Calibration implements PlugInFilter
{
	@Override
	public void run(ImageProcessor arg0) 
	{
		
	}

	private List<MyFrame[]> splitFrameIntoSetPairFrames(Vector<Particle> p1, Vector<Particle> p2)
	{
		MyFrame f1[] = MyFrame.createFrames(p1, 1);
		MyFrame f2[] = MyFrame.createFrames(p2, 1);
		
		if (f1.length != f2.length)
		{
			IJ.error("Error, you cannot compare vector particles with different size");
		}
		
		List<MyFrame[]> sfrm = new ArrayList<MyFrame[]>();
		
		for (int i = 0 ; i < f1.length ; i++)
		{
			MyFrame tmp[] = new MyFrame[2];
			
			tmp[0] = f1[i];
			tmp[1] = f2[i];
			
			sfrm.add(tmp);
		}
		
		return sfrm;
	}
	
	@Override
	public int setup(String arg0, ImagePlus arg1) 
	{
		// Ask the two csv files
		
		OpenDialog op1 = new OpenDialog("Choose first file","Particle set 1");
		OpenDialog op2 = new OpenDialog("Choose second file","Particle set 2");
		
		String ps1 = op1.getDirectory() + File.pathSeparator + op1.getFileName();
		String ps2 = op2.getDirectory() + File.pathSeparator + op2.getFileName();
		
		// Read particle set 1
		
		InterPluginCSV<Particle> csv1 = new InterPluginCSV<Particle>(Particle.class);
		Vector<Particle> vtp1 = csv1.Read(ps1);
		
		// Read particle set 2
		
		InterPluginCSV<Particle> csv2 = new InterPluginCSV<Particle>(Particle.class);
		Vector<Particle> vtp2 = csv2.Read(ps2);
		
		// Link them
		
		ParticleLinker pl = new ParticleLinkerHun();
		
		// split in a set of coupled frames
		
		List<MyFrame[]> sfrm = splitFrameIntoSetPairFrames(vtp1,vtp2);
		
		linkerOptions lo = new linkerOptions();
		
		// we set linker option
		
		lo.displacement = 10;
		lo.force = false;
		lo.l_d = 1000.0f;
		lo.l_f = 0.0f;
		lo.l_s = 0.0f;
		lo.linkrange = 1;
		lo.straight_line = false;
		lo.r_sq = 0.0f;
		
		for (MyFrame[] frm : sfrm)
		{
			pl.linkParticles(frm, 2, lo);
		}
		
		// Get all non-zero prop
		
		// Ask about deltas (or binnings)
		
		// do statistics
		
		// Create a Csv files
		
		// do statistics of the position
		
		double mean_pos = 0.0;
		double var_pos = 0.0;
		
		for (int i = 0 ; i < sfrm.size() ; i++)
		{
			Vector<Particle> p1 = sfrm.get(i)[0].getParticles();
			Vector<Particle> p2 = sfrm.get(i)[1].getParticles();
			
			for (int j = 0 ; j < p1.size() ; j++)
			{
				
			}
			
			for (int j = 0 ; j < p2.size() ; j++)
			{
				
			}
		}
		
		
		
/*		for (int i = 0 ; i < sfrm.get(0)[0].getParticles().size() ; i++)
		{
			sfrm.get(0)[0].getParticles().
		}*/
		
		
		
		
		// get min and max of all properties
		
		// Ask for N bin for each properties
		
		// Plot
		
		return 0;
	}
	
	
}