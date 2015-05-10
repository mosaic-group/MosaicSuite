package mosaic.core.particleLinking;

import ij.IJ;

import java.util.Vector;

import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;

public class ParticleLinkerHun implements ParticleLinker
{		
	/**
	 * Second phase of the algorithm - 
	 * <br>Identifies points corresponding to the 
	 * same physical particle in subsequent frames and links the positions into trajectories
	 * <br>The length of the particles next array will be reset here according to the current linkrange
	 * <br>Hungarian Pietro Incardona
	 */	

	private float cost_link(Particle p1, Particle p2, linkerOptions l, int n, double max_cost)
	{
		double cost = 0.0;
		float distance_sq = (p1.x - p2.x)*(p1.x - p2.x) + 
			(p1.y - p2.y)*(p1.y - p2.y) + 
			(p1.z - p2.z)*(p1.z - p2.z);
	
		cost = (float) (distance_sq*l.l_s +
				l.l_f*Math.cbrt(((p1.m0 - p2.m0)*(p1.m0 - p2.m0)) + 
				(p1.m2 - p2.m2)*(p1.m2 - p2.m2)));
	
		if (l.force == true)
		{
			if (p1.distance >= 0.0)
			{
				float lx = (p2.x - p1.x) / (n+1) - p1.lx;
				float ly = (p2.y - p1.y) / (n+1) - p1.ly;
				float lz = (p2.z - p1.z) / (n+1)- p1.lz;
			
				float f_magn_sq = lx * lx + ly * ly + lz * lz;
				cost += l.l_d * f_magn_sq;
			}
			else
			{
				// This is a fresh particle we have no idea where is going
				
				cost += max_cost / 3.0;
			}
		}
		else if (l.straight_line == true  && p1.distance >= 0.0)
		{
			// Calculate the module
		
			float l1_m = p1.linkModule();
		
			float lx1 = p1.lx/l1_m;
			float ly1 = p1.ly/l1_m;
			float lz1 = p1.lz/l1_m;
		
			float lx2 = (p2.x - p1.x + p1.lxa);
			float ly2 = (p2.y - p1.y + p1.lya);
			float lz2 = (p2.z - p1.z + p1.lza);
		
			float l2_m = (float) Math.sqrt(lx2 * lx2 + ly2 * ly2 + lz2 * lz2);
		
			if (l2_m >= l.r_sq)
			{
				lx2 /= l2_m;
				ly2 /= l2_m;
				lz2 /= l2_m;
			
				float cos_phi = lx1 * lx2 + ly1 * ly2 + lz1 * lz2;
		
				cost += (cos_phi - 1)*(cos_phi - 1)*l.displacement*l.displacement;
			}
		}
		return (float) cost;
	}
	
	public boolean linkParticles(MyFrame[] frames, int frames_number, linkerOptions l) 
	{
		int m, i, j, nop, nop_next, n;
		int curr_linkrange;
		
		if (l.linkrange > 1)
		{
			IJ.error("Error Hungarian linker for now does not support link range > 1");
			return false;
		}
		
		/** okv is a helper vector in the initialization phase. It keeps track of the empty columns
		 * in g. */
		boolean[] okv;
		float max_cost;
		/** The cost matrix - TODO: it is quite sparse and one should take advantage of that. */
		Vector<Particle> p1, p2;

		// set the length of the particles next array according to the linkrange
		// it is done now since link range can be modified after first run
		for (int fr = 0; fr<frames.length; fr++) {
			for (int pr = 0; pr<frames[fr].getParticles().size(); pr++) {
				frames[fr].getParticles().elementAt(pr).next = new int[l.linkrange];
			}
		}
		curr_linkrange = l.linkrange;

		/* If the linkrange is too big, set it the right value */
		if(frames_number < (curr_linkrange + 1))
			curr_linkrange = frames_number - 1;

//		max_cost = displacement * displacement;

		for(m = 0; m < frames_number - curr_linkrange; m++) 
		{
			IJ.showStatus("Linking Frame " + (m+1));
			nop = frames[m].getParticles().size();
			for(i = 0; i < nop; i++) 
			{
				frames[m].getParticles().elementAt(i).special = false;
				for(n = 0; n < l.linkrange; n++)
					frames[m].getParticles().elementAt(i).next[n] = -1;
			}

			for(n = 0; n < curr_linkrange; n++) 
			{
				max_cost = (n + 1) * l.displacement * (n + 1) * l.displacement;

				nop_next = frames[m + (n + 1)].getParticles().size();

				okv = new boolean[nop_next+1];
				for (i = 0; i< okv.length; i++) okv[i] = true;

				/* Set g to zero - not necessary */
				//for (i = 0; i< g.length; i++) g[i] = false;

				p1 = frames[m].getParticles();
				p2 = frames[m + (n + 1)].getParticles();
				//    			p1 = frames[m].particles;
				//    			p2 = frames[m + (n + 1)].particles
				
				
				// Create a bipartite graph
					
				BipartiteMatcher bpm = new BipartiteMatcher(nop + nop_next);
				
				//
				
				/* Fill in the costs */
				for(i = 0; i < nop + nop_next; i++) 
				{
					for(j = 0; j < nop + nop_next; j++) 
					{
						if (i < nop && j < nop_next)
						{
							double cost = cost_link(p1.elementAt(i),p2.elementAt(j),l,n,max_cost);
							bpm.setWeight(i, j, max_cost-cost);
						}
						else
						{
							bpm.setWeight(i, j, 0.0);
						}
					}
				}
				
				int[] mac = bpm.getMatching();
				
				/* After optimization, the particles needs to be linked */
				for(i = 0; i < nop + nop_next; i++) 
				{
					// Adjust mac[i]
					
					if (i >= nop || mac[i] >= nop_next)
					{
						continue;
					}
					
					p1 = frames[m].getParticles();
					p2 = frames[m + 1].getParticles();
					
					if (cost_link(p1.elementAt(i),p2.elementAt(mac[i]),l,0,max_cost) >= max_cost)
					{
						continue;
					}
					
					// link particle
					
					p1.elementAt(i).next[n] = mac[i];

					// if linked to a dummy particle does not calculate
					
					if (mac[i] >= nop_next)
						continue;
					
					// Calculate the square distance and store the normalized linking vector
						
					if (l.force == true)
					{						
						p2.elementAt(mac[i]).lx = (p2.elementAt(mac[i]).x - p1.elementAt(i).x) / (n+1);
						p2.elementAt(mac[i]).ly = (p2.elementAt(mac[i]).y - p1.elementAt(i).y) / (n+1);
						p2.elementAt(mac[i]).lz = (p2.elementAt(mac[i]).z - p1.elementAt(i).z) / (n+1);
								
						// We do not use distance is just to indicate that the particle has a link vector
								
						p2.elementAt(mac[i]).distance = 1.0f;
					}
					else if (l.straight_line == true)
					{
						float distance_sq = (float) Math.sqrt((p1.elementAt(i).x - p2.elementAt(mac[i]).x)*(p1.elementAt(i).x - p2.elementAt(mac[i]).x) + 
									(p1.elementAt(i).y - p2.elementAt(mac[i]).y)*(p1.elementAt(i).y - p2.elementAt(mac[i]).y) + 
									(p1.elementAt(i).z - p2.elementAt(mac[i]).z)*(p1.elementAt(i).z - p2.elementAt(mac[i]).z));
								
						if (distance_sq >= l.r_sq)
						{	
							p2.elementAt(mac[i]).lx = (p2.elementAt(mac[i]).x - p1.elementAt(i).x) + p1.elementAt(i).lxa;
							p2.elementAt(mac[i]).ly = (p2.elementAt(mac[i]).y - p1.elementAt(i).y) + p1.elementAt(i).lya;
							p2.elementAt(mac[i]).lz = (p2.elementAt(mac[i]).z - p1.elementAt(i).z) + p1.elementAt(i).lza;
						}
						else
						{
							// Propagate the previous link vector
								
							p2.elementAt(mac[i]).lx = p1.elementAt(i).lx;
							p2.elementAt(mac[i]).ly = p1.elementAt(i).ly;
							p2.elementAt(mac[i]).lz = p1.elementAt(i).lz;
									
							p2.elementAt(mac[i]).lxa += (p2.elementAt(mac[i]).x - p1.elementAt(i).x) + p1.elementAt(i).lxa;
							p2.elementAt(mac[i]).lya += (p2.elementAt(mac[i]).y - p1.elementAt(i).y) + p1.elementAt(i).lya;
							p2.elementAt(mac[i]).lza += (p2.elementAt(mac[i]).z - p1.elementAt(i).z) + p1.elementAt(i).lza;
								
							if (p2.elementAt(mac[i]).linkModuleASq() >= l.r_sq)
							{
								p2.elementAt(mac[i]).lx = p2.elementAt(mac[i]).lxa;
								p2.elementAt(mac[i]).ly = p2.elementAt(mac[i]).lya;
								p2.elementAt(mac[i]).lz = p2.elementAt(mac[i]).lza;
										
								p2.elementAt(mac[i]).lxa = 0;
								p2.elementAt(mac[i]).lya = 0;
								p2.elementAt(mac[i]).lza = 0;
							}
						
							p2.elementAt(mac[i]).distance = (float) Math.sqrt(distance_sq);
						}
					}
				}
			}

			if(m == (frames_number - curr_linkrange - 1) && curr_linkrange > 1)
				curr_linkrange--;
		}

		/* At the last frame all trajectories end */
		for(i = 0; i < frames[frames_number - 1].getParticles().size(); i++) 
		{
			frames[frames_number - 1].getParticles().elementAt(i).special = false;
			for(n = 0; n < l.linkrange; n++)
				frames[frames_number - 1].getParticles().elementAt(i).next[n] = -1;
		}
		
		return true;
	}
}
