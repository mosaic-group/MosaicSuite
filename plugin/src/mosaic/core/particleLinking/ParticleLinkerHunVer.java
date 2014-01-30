package mosaic.core.particleLinking;

import ij.IJ;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.particleLinking.linkerOptions;

public class ParticleLinkerHunVer {
		
	/**
	 * Second phase of the algorithm - 
	 * <br>Identifies points corresponding to the 
	 * same physical particle in subsequent frames and links the positions into trajectories
	 * <br>The length of the particles next array will be reset here according to the current linkrange
	 * <br>Hungarian Pietro Incardona
	 */	
	
/*	class VerletList
	{
		Vector<Integer> p;
	}
	
	
	class RelationMatrix
	{
		int cursor = 0;
		int col;
		int row;
		
		boolean g[];

		RelationMatrix(int i, int j)
		{
			g = new boolean[i * j];
			col = j;
			row = i;
		}

		int getRow()
		{
			return row;
		}
		
		int getCol()
		{
			return col;
		}
		
		boolean hasNext()
		{
			if (cursor < g.length)
				return true;
			else
				return false;
		}
		
		boolean next()
		{
			return g[cursor];
		}
		
		boolean get(int i, int j)
		{
			return g[i*col + j];
		}
	}*/
	
/*	class CostMatrix
	{
		int cursor = 0;
		int col;
		int row;
		
		float c[];

		CostMatrix(int i, int j)
		{
			c = new float[i * j];
			row = i;
			col = j;
		}

		int getRow()
		{
			return row;
		}
		
		int getCol()
		{
			return col;
		}
		
		boolean hasNext()
		{
			if (cursor < c.length)
				return true;
			else
				return false;
		}
		
		float next()
		{
			return c[cursor];
		}
		
		float get(int i, int j)
		{
			return c[i*col + j];
		}
	}*/
	

/*	public class JohnsonTrotter 
	{
		int[] p;
		int[] pi;
		int[] dir;
		
		public void permInit(int N)
		{
			p   = new int[N];     // permutation
			pi  = new int[N];     // inverse permutation
			dir = new int[N];     // direction = +1 or -1
			for (int i = 0; i < N; i++) 
			{
				dir[i] = -1;
				p[i]  = i;
				pi[i] = i;
			}
		}

		public void perm(int n)
		{ 
			// base case - print out permutation
			if (n >= p.length) 
			{
				for (int i = 0; i < p.length; i++)
					System.out.print(p[i]);
				return;
			}

			perm(n+1);
			for (int i = 0; i <= n-1; i++) 
			{
				// swap 
				System.out.printf("   (%d %d)\n", pi[n], pi[n] + dir[n]);
				int z = p[pi[n] + dir[n]];
				p[pi[n]] = z;
				p[pi[n] + dir[n]] = n;
				pi[z] = pi[n];
				pi[n] = pi[n] + dir[n];  

				perm(n+1); 
			}
			dir[n] = -dir[n];
		}
		
		
		int [] getP()
		{
			return p;
		}
	}*/

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
	
	public void linkParticles(MyFrame[] frames, int frames_number, linkerOptions l) 
	{
		int m, i, j, nop, nop_next, n;
		int curr_linkrange;
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
				max_cost = (float)(n + 1) * l.displacement * (float)(n + 1) * l.displacement;

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
	}
	
/*	class link
	{
		int i;
		int j;
	};
	
	private double calculateCost(link l[], CostMatrix costSub)
	{
		double cost = 0.0;
		
		for (int i = 0 ; i < l.length ; i++)
		{
			cost += costSub.get(l[i].i,l[i].j);
		}
		
		return cost;
	}
	
	private link[] createLink(RelationMatrix subR)
	{
		link [] l = new link[subR.getRow()];
		for (int i = 0 ; i < subR.getRow() ; i++)
		{
			for (int j = 0 ; j < subR.getCol() ; j++)
			{
				if (subR.get(i, j) == true)
				{
					l[i].i = i;
					l[i].j = j;
				}
			}
		}
		
		return l;
	}
	
	private int[] createComb(RelationMatrix subR)
	{
		int i = 0;
		int j = 0;
		
		int [] comb = new int [subR.getRow()];
		
		for (i = 0 ; i < subR.getRow()  ; i++)
		{
			for (j = 0 ; j < subR.getCol() ; j++)
			{
				if (subR.get(i, j) == true)
				{
					break;
				}
			}
			
			comb[i] = j;
		}
		
		return comb;
	}*/
	
	/**
	 * 
	 * It find all the permutation
	 * 
	 * @param comb
	 * @param end
	 * @return
	 */
	
/*	private boolean Increment(int [] comb, int [] end)
	{
		comb[0]++;
		
		for (int i = 0 ; i < comb.length ; i++)
		{
			if (comb[i] >= comb.length)
			{
				comb[i] = 0;
				
				if (i + 1 < comb.length)
					comb[i]++;
				else
				{
					for (int j = 0 ; j < comb.length ; j++)
						comb[j] = 0;
				}
			}
		}
			
		return false;
	}*/
	
	/**
	 * 
	 * Calculate the cost of the permutation p
	 * 
	 * @param p
	 * @param cSub
	 * @return
	 */
	
/*	private float calculateCost(int p[], CostMatrix cSub)
	{
		float cost = 0.0f;
		
		for (int i = 0 ; i < p.length ; i++)
		{
			cost += cSub.get(i, p[i]);
		}
		
		return cost;
	}
	
	class BestPerm extends JohnsonTrotter
	{
		double best_min;
		int best_p[];
		
		void Permutation()
		{
			double cost = 0.0/*calculateCost(p);
			
			if (cost < best_min)
			{
				best_min = cost;
				best_p = p.clone();
			}
		}
	}*/
	
/*	private boolean optimizeSublist(Particle p1_sub[], Particle p2_sub[], RelationMatrix subR, CostMatrix costSub)
	{
		int comb[] = createComb(subR);
		int[] end = comb.clone();
		
		link l[] = createLink(subR);
		double cost_prev = calculateCost(l,costSub);*/
		
		
		
/*<<<<<<< HEAD
		BestPerm js = new BestPerm();
=======
		JhonsonTrotter js = new JhonsonTrotter() {
			void Permutation()
			{
				cost = calculateCost(p);
			}
		}
		
		do
		{
			
			float cost_post = calculateNewCost();*/
			
			/* Better situation */
			
/*			if (calculate_post < calculate_prev)
			{
				updateRelation();
			}
			
		} while (Increment(comb,end));
>>>>>>> Oxidation_analysis*/
		
/*		return true;
	}*/
}
