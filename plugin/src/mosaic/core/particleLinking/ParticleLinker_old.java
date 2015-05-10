package mosaic.core.particleLinking;

import java.io.IOException;
import java.util.Vector;

import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.core.utils.MosaicUtils;

public class ParticleLinker_old {
	/**
	 * Second phase of the algorithm - 
	 * <br>Identifies points corresponding to the 
	 * same physical particle in subsequent frames and links the positions into trajectories
	 * <br>The length of the particles next array will be reset here according to the current linkrange
	 * <br>Adapted from Ingo Oppermann implementation
	 */
	public void linkParticles(MyFrame[] frames, int frames_number, int linkrange, double displacement) {

		int m, i, j, k, nop, nop_next, n;
		int ok, prev, prev_s, x = 0, y = 0, curr_linkrange;
		int[] g;
		double min, z, max_cost;
		double[] cost;
		Vector<Particle> p1, p2;

		// set the length of the particles next array according to the linkrange
		// it is done now since link range can be modified after first run
		for (int fr = 0; fr<frames.length; fr++) {
			for (int pr = 0; pr<frames[fr].getParticles().size(); pr++) {
				frames[fr].getParticles().elementAt(pr).next = new int[linkrange];
			}
		}
		curr_linkrange = linkrange;

		/* If the linkrange is too big, set it the right value */
		if(frames_number < (curr_linkrange + 1))
			curr_linkrange = frames_number - 1;

//		max_cost = displacement * displacement;

		for(m = 0; m < frames_number - curr_linkrange; m++) {
			nop = frames[m].getParticles().size();
			for(i = 0; i < nop; i++) {
				frames[m].getParticles().elementAt(i).special = false;
				for(n = 0; n < linkrange; n++)
					frames[m].getParticles().elementAt(i).next[n] = -1;
			}

			for(n = 0; n < curr_linkrange; n++) {
				max_cost = (n + 1) * displacement * (n + 1) * displacement;

				nop_next = frames[m + (n + 1)].getParticles().size();

				/* Set up the cost matrix */
				cost = new double[(nop + 1) * (nop_next + 1)];

				/* Set up the relation matrix */
				g = new int[(nop + 1) * (nop_next + 1)];

				/* Set g to zero */
				for (i = 0; i< g.length; i++) g[i] = 0;

				p1 = frames[m].getParticles();
				p2 = frames[m + (n + 1)].getParticles();
				//    			p1 = frames[m].particles;
				//    			p2 = frames[m + (n + 1)].particles;


				/* Fill in the costs */
				for(i = 0; i < nop; i++) {
					for(j = 0; j < nop_next; j++) {
						cost[MosaicUtils.coord(i, j, nop_next + 1)] = 
							(p1.elementAt(i).x - p2.elementAt(j).x)*(p1.elementAt(i).x - p2.elementAt(j).x) + 
							(p1.elementAt(i).y - p2.elementAt(j).y)*(p1.elementAt(i).y - p2.elementAt(j).y) + 
							(p1.elementAt(i).z - p2.elementAt(j).z)*(p1.elementAt(i).z - p2.elementAt(j).z) + 
							(p1.elementAt(i).m0 - p2.elementAt(j).m0)*(p1.elementAt(i).m0 - p2.elementAt(j).m0) + 
							(p1.elementAt(i).m2 - p2.elementAt(j).m2)*(p1.elementAt(i).m2 - p2.elementAt(j).m2);
					}
				}

				for(i = 0; i < nop + 1; i++)
					cost[MosaicUtils.coord(i, nop_next, nop_next + 1)] = max_cost;
				for(j = 0; j < nop_next + 1; j++)
					cost[MosaicUtils.coord(nop, j, nop_next + 1)] = max_cost;
				cost[MosaicUtils.coord(nop, nop_next, nop_next + 1)] = 0.0;

				/* Initialize the relation matrix */
				for(i = 0; i < nop; i++) { // Loop over the x-axis
					min = max_cost;
					prev = 0;
					for(j = 0; j < nop_next; j++) { // Loop over the y-axis
						/* Let's see if we can use this coordinate */
						ok = 1;
						for(k = 0; k < nop + 1; k++) {
							if(g[MosaicUtils.coord(k, j, nop_next + 1)] == 1) {
								ok = 0;
								break;
							}
						}
						if(ok == 0) // No, we can't. Try the next column
							continue;

						/* This coordinate is OK */
						if(cost[MosaicUtils.coord(i, j, nop_next + 1)] < min) {
							min = cost[MosaicUtils.coord(i, j, nop_next + 1)];
							g[MosaicUtils.coord(i, prev, nop_next + 1)] = 0;
							prev = j;
							g[MosaicUtils.coord(i, prev, nop_next + 1)] = 1;
						}
					}

					/* Check if we have a dummy particle */
					if(min == max_cost) {
						g[MosaicUtils.coord(i, prev, nop_next + 1)] = 0;
						g[MosaicUtils.coord(i, nop_next, nop_next + 1)] = 1;
					}
				}

				/* Look for columns that are zero */
				for(j = 0; j < nop_next; j++) {
					ok = 1;
					for(i = 0; i < nop + 1; i++) {
						if(g[MosaicUtils.coord(i, j, nop_next + 1)] == 1)
							ok = 0;
					}

					if(ok == 1)
						g[MosaicUtils.coord(nop, j, nop_next + 1)] = 1;
				}

				/* The relation matrix is initilized */

				///DEBUG
				try {
					int fi = 0;
					java.io.File file = new java.io.File("/home/janickc/old_g_init_frame_"+fi+".txt");
					while(true){
						if(!file.exists())
							break;
						fi++;
						file = new java.io.File("/home/janickc/old_g_init_frame_"+fi+".txt");
					}	
					java.io.Writer output = new java.io.BufferedWriter(new java.io.FileWriter(file));

				
					for(i = 0; i < nop +1; i++) { // Loop over the x-axis
						for(j = 0; j < nop_next+1; j++) {
	
							output.write(""+g[MosaicUtils.coord(i, j, nop_next + 1)] + " ");
						}
						output.write("\n");
					}
					output.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				///ENDDEBUG
				
				
				/* Now the relation matrix needs to be optimized */
				min = -1.0;
				while(min < 0.0) {
					min = 0.0;
					prev = 0;
					prev_s = 0;
					for(i = 0; i < nop + 1; i++) {
						for(j = 0; j < nop_next + 1; j++) {
							if(i == nop && j == nop_next)
								continue;

							if(g[MosaicUtils.coord(i, j, nop_next + 1)] == 0 && 
									cost[MosaicUtils.coord(i, j, nop_next + 1)] <= max_cost) {
								/* Calculate the reduced cost */

								// Look along the x-axis, including
								// the dummy particles
								for(k = 0; k < nop + 1; k++) {
									if(g[MosaicUtils.coord(k, j, nop_next + 1)] == 1) {
										x = k;
										break;
									}
								}

								// Look along the y-axis, including
								// the dummy particles
								for(k = 0; k < nop_next + 1; k++) {
									if(g[MosaicUtils.coord(i, k, nop_next + 1)] == 1) {
										y = k;
										break;
									}
								}

								/* z is the reduced cost */
								if(j == nop_next)
									x = nop;
								if(i == nop)
									y = nop_next;

								z = cost[MosaicUtils.coord(i, j, nop_next + 1)] + 
								cost[MosaicUtils.coord(x, y, nop_next + 1)] - 
								cost[MosaicUtils.coord(i, y, nop_next + 1)] - 
								cost[MosaicUtils.coord(x, j, nop_next + 1)];
								if(z > -1.0e-10)
									z = 0.0;
								if(z < min) {
									min = z;
									prev = MosaicUtils.coord(i, j, nop_next + 1);
									prev_s = MosaicUtils.coord(x, y, nop_next + 1);
								}
							}
						}
					}

					if(min < 0.0) {
						g[prev] = 1;
						g[prev_s] = 1;
						g[MosaicUtils.coord(prev / (nop_next + 1), prev_s % (nop_next + 1), nop_next + 1)] = 0;
						g[MosaicUtils.coord(prev_s / (nop_next + 1), prev % (nop_next + 1), nop_next + 1)] = 0;
					}
				}

				/* After optimization, the particles needs to be linked */
				for(i = 0; i < nop; i++) {
					for(j = 0; j < nop_next; j++) {
						if(g[MosaicUtils.coord(i, j, nop_next + 1)] == 1)
							p1.elementAt(i).next[n] = j;
					}
				}
			}

			if(m == (frames_number - curr_linkrange - 1) && curr_linkrange > 1)
				curr_linkrange--;
		}

		/* At the last frame all trajectories end */
		for(i = 0; i < frames[frames_number - 1].getParticles().size(); i++) {
			frames[frames_number - 1].getParticles().elementAt(i).special = false;
			for(n = 0; n < linkrange; n++)
				frames[frames_number - 1].getParticles().elementAt(i).next[n] = -1;
		}
	}	
}