package mosaic.core.particleLinking;

import ij.IJ;

import java.util.Vector;

import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;

public class ParticleLinker_MarkKittisopikul{
	/**
	 * Second phase of the algorithm - 
	 * <br>Identifies points corresponding to the 
	 * same physical particle in subsequent frames and links the positions into trajectories
	 * <br>The length of the particles next array will be reset here according to the current linkrange
	 * 
	 * @author Mark Kittisopikul, UT Southwestern
	 */
	public void linkParticles(MyFrame[] frames, int frames_number, int linkrange, double displacement) {

		int m, i, j, l, nop, nop_next, n;
		int prev, x = 0, y = 0, curr_linkrange;
		int prev_i, prev_j;
		int[] xv,yv;
		boolean[][] g;
		boolean[][] inrange;
		boolean[] okv;
		double min, max_cost;
		double[][] cost;
		int[][] jm;
		Vector<Particle> p1, p2;
		Particle p1i, p2j;

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

		max_cost = displacement * displacement;

		for(m = 0; m < frames_number - curr_linkrange; m++) {
			IJ.showStatus("Linking Frame " + (m+1));
			nop = frames[m].getParticles().size();
			for(i = 0; i < nop; i++) {
				frames[m].getParticles().elementAt(i).special = false;
				for(n = 0; n < linkrange; n++)
					frames[m].getParticles().elementAt(i).next[n] = -1;
			}
			for(n = 0; n < curr_linkrange; n++) {
				IJ.showStatus("Linking Frame " + (m+1) + ": initialized " + nop + "Particles");
				IJ.showProgress(n,curr_linkrange);
				max_cost = (double)(n + 1) * displacement * (double)(n + 1) * displacement;

				nop_next = frames[m + (n + 1)].getParticles().size();
//				System.out.println("nop: " + nop);
//				System.out.println("nop_next: " + nop_next);

				/* Set up the cost matrix */
				//cost = new double[nop + 1][nop_next + 1];
				cost = new double[nop +1][];
				jm = new int[nop+1][];

				/* Set up the relation matrix */
			//	g = new int[(nop + 1) * (nop_next + 1)];
				g = new boolean[nop+1][nop_next+1];
				inrange = new boolean[nop+1][nop_next+1];
				okv = new boolean[nop_next+1];
				xv = new int[nop_next+1];
				yv = new int[nop+1];
			
				/* Set g to zero */
				// don't need to do this according to language spec
				//for (i = 0; i< g.length; i++) for (j = 0; j < g[i].length; j++) g[i][j] = false;
				//for (i = 0; i< inrange.length; i++) for (j = 0; j < inrange[i].length; j++) inrange[i][j] = false;
				for (i = 0; i< okv.length; i++) okv[i] = true;

				p1 = frames[m].getParticles();
				p2 = frames[m + (n + 1)].getParticles();

				/* Fill in the costs */
				IJ.showStatus("Linking Frame " + (m+1) + ": Calculating cost");
				double dx,dy,dz,dm0,dm2;
				double currentCost;
				double[] currentCosts = new double[nop_next];
				int[] jv = new int[nop_next];
				for(i = 0; i < nop; i++) {
					IJ.showProgress(i,nop);
					p1i = p1.elementAt(i);
					l = 0;
					for(j = 0; j < nop_next; j++) {
						p2j = p2.elementAt(j);
						dx = p1i.x - p2j.x;
						dy = p1i.y - p2j.y;
						dz = p1i.z - p2j.z;
						dm0 = p1i.m0 - p2j.m0;
						dm2 = p1i.m2 - p2j.m2;
						currentCost = dx*dx + dy*dy + dz*dz + dm0*dm0 + dm2*dm2;
						if(currentCost <= max_cost) {
							inrange[i][j] = true;
							currentCosts[l] = currentCost;
							jv[l] = j;
							l++;
						}
					}
					inrange[i][nop_next] = true;
					currentCosts[l] = max_cost;
					jv[l] = nop_next;
					l++;
					cost[i] = new double[l];
					System.arraycopy(currentCosts,0,cost[i],0,l);
					jm[i] = new int[l];
					System.arraycopy(jv,0,jm[i],0,l);
				}

	//			for(i = 0; i < nop + 1; i++) {
		//			cost[i][nop_next] = max_cost;
	//				inrange[i][nop_next] = true;
	//			}
	
				// the following is a waste of memory O(N)
				cost[nop] = new double[nop_next+1]; 
				jm[nop] = new int[nop_next+1];
				for(j = 0; j < nop_next + 1; j++) {
					jm[nop][j] = j;
					cost[nop][j] = max_cost;
					inrange[nop][j] = true;
				}
				cost[nop][nop_next] = 0.0;
				//inrange[nop][nop_next] = false;

				/// DEBUG loop: check if jm entries and the inrange matrix are consistent.
				for(i =0; i < nop; i++) {
					for(l=0; l < jm[i].length; l++) {
						j = jm[i][l];
						if(inrange[i][j]) {
						} else {
							System.out.println("Checking out of range: " + i + " , " + j);
						}
					}
				}
				/// END DEBUG loop

				/* Initialize the relation matrix */
				for(i = 0; i < nop; i++) { // Loop over the x-axis
					IJ.showStatus("Linking Frame " + (m+1) + ": Initializing Relation matrix");
					IJ.showProgress(i,nop);
					min = max_cost;
					prev = -1;
					for(l = 0; l < jm[i].length-1; l++) { // Loop over the y-axis without the dummy
						j = jm[i][l];

						/* Let's see if we can use this coordinate */						
						if(okv[j] && min > cost[i][l]) {
							min = cost[i][l];
							if(prev >= 0) {
								okv[prev] = true;
								g[i][prev] = false;
							}

							okv[j] = false;
							g[i][j] = true;
							
							prev = j;
						}
					}

					/* Check if we have a dummy particle */
					if(min == max_cost) {
						if(prev >= 0) {
							okv[prev] = true;
							g[i][prev] = false;
						}
						g[i][nop_next] = true;
						okv[nop_next] = false;
					}
				}

				/* Look for columns that are zero */
				for(j = 0; j < nop_next; j++) {
					if(okv[j])
						g[nop][j] = true;
				}
				IJ.showStatus("Linking Frame " + (m+1) + ": Relation matrix initialized");

				/* The relation matrix is initilized */

				/* Build xv and yv, a speedup for g */
				for(i = 0; i < nop +1; i++) {
				//	for(j = 0; j < nop_next+1; j++) {
					for(l =0; l < jm[i].length; l++) {
						j = jm[i][l];
						if(g[i][j]) {
							if(!inrange[i][j])
								System.out.println("Out of range xy init" + i + "," + j);
							xv[j] = i;
							yv[i] = j;
						}
					}
					/*if(g[i][nop_next]) {
						xv[nop_next] = i;
						yv[i] = nop_next;
					}*/
				}
				xv[nop_next] = nop;
				yv[nop] = nop_next;
			
				/* Build the matrix t (similar to in_range) */
				// the following loop consumes more memory but saves processor later
				int[][] t= new int[nop+1][nop_next+1];
				for(i=0; i < nop +1; i++) {
					l = 0;
					for(j=0; j < nop_next+1; j++) {
		//			for(l=0; l < jm[i].length; l++) {
		//				j = jm[i][l];
						if(inrange[i][j]) {			
							t[i][j] = l;
							l++;
						} else if(i == nop && j == nop_next) {
							t[i][j] = l;
							l++;
						} else {
							// we should not consider particles that are out of range
							// since linking to a dummy particle will always be more favorable
							t[i][j] = -1;
						}
					}
				}

				/*
				///DEBUG
				try {
					int fi = 0;
					java.io.File file = new java.io.File("/home/janickc/new_g_init_frame_"+fi+".txt");
					while(true){
						if(!file.exists())
							break;
						fi++;
						file = new java.io.File("/home/janickc/new_g_init_frame_"+fi+".txt");
					}	
					java.io.Writer output = new java.io.BufferedWriter(new java.io.FileWriter(file));

				
					for(i = 0; i < nop +1; i++) { // Loop over the x-axis
						for(j = 0; j < nop_next+1; j++) {
							int d = 0;
							if(g[i][j])
								d = 1;	
							output.write("" + d + " ");
						}
						output.write("\n");
					}
					output.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				///ENDDEBUG
				*/
				
				
				/* Precalculate the reduced cost matrix zm and the current minima within zm: zmini */
				double[][] zm = new double[nop+1][];
				double[] zmini = new double[nop+1];
				int[] zmini_j = new int[nop+1];
				for(i =0; i < nop +1; i++) {
					zm[i] = new double[cost[i].length];
					zmini[i] = 0;
		//			for(j =0; j < nop_next +1; j++) {
					for(l=0; l < jm[i].length; l++) {
						j = jm[i][l];
						if(!g[i][j]) {
							if(!inrange[i][j])
								System.out.println("Out of range ij optimization" + i + "," + j);
							/* Calculate the reduced cost */

							// ij is a proposed paring
							// xj and iy are the current pairings
							// xy is the other proposed pairing
							x = xv[j];
							y = yv[i];
							if(!inrange[x][y] && x != nop && y != nop_next)
								System.out.println("Out of range xy optimization" + x + "," + y);
							if(!inrange[i][y] && i != nop && y != nop_next)
								System.out.println("Out of range xy optimization" + i + "," + y);
							if(!inrange[x][j] && x != nop && j != nop_next)
								System.out.println("Out of range xy optimization" + x + "," + j);
							// Note that l = t[i][j];
							// System.out.println(i + "," + j + "," + x +"," +y);
							// System.out.println(t[i][j] + "," + t[x][y] + "," + t[i][y] +"," +t[x][j]);
							/*zm[i][l] = cost[i][t[i][j]] +
								cost[x][t[x][y]] -
								cost[i][t[i][y]] -
								cost[x][t[x][j]];*/
							zm[i][l] = getReducedCost(i,j,cost,t,xv,yv,inrange);
							if(zm[i][l] > -1.0e-10) {
								zm[i][l] = 0;
							}
							if(zm[i][l] < zmini[i] && inrange[xv[j]][yv[i]]) {
								zmini[i] = zm[i][l];
								zmini_j[i] = j;
							}
						}
					}
				}		
				System.out.println("zmini_j[nop]: " + zmini_j[nop]);
		
				min = -1.0;
				double firstMin = 0;
//				double checkMin = 0;
//				int check_i =0;
//				int check_j = 0;
//				int pp_i = 0;
//				int pp_j = 0;
//				int pp_x = 0;
//				int pp_y = 0;
				prev_i = 0;
				prev_j = 0;
				
				/* Now the relation matrix needs to be optimized */
				while(min < 0.0) {
					IJ.showStatus("Linking Frame " + (m+1) + ": Optimizing.");
					//System.out.println("While loop");
					min = 0.0;
//					pp_i = prev_i;
//					pp_j = prev_j;
					prev_i = 0;
					prev_j = 0;
					for(i = 0; i < nop + 1; i++) {
						x = xv[zmini_j[i]];
						y = yv[i];
						if(!inrange[x][y] && zmini[i] < 0) {
							//System.out.println("Correcting xy min " + x);
							zmini[x] = 0;
							zmini_j[x] = 0;
							for(l=0; l < jm[x].length; l++) {
								j = jm[x][l];
								if(!g[x][j] && inrange[xv[j]][yv[x]] && zm[x][l] < zmini[x]) {
								       	zmini[x] = zm[x][l];
									zmini_j[x] = j;
								}
							}
						}
						if(zmini[i] < min && inrange[xv[zmini_j[i]]][yv[i]]) {
							min = zmini[i];
							prev_i = i;
							prev_j = zmini_j[i];
						}
					}
					/*
				    //begin old-style checking loop
					checkMin = 0;
					check_i = 0;
					check_j = 0;
					for(i = 0; i < nop +1; i++) {
						for(l=0; l < jm[i].length; l++) {
							j = jm[i][l];
							x = xv[j];
							y = yv[i];
							if(!g[i][j] && inrange[i][j] && inrange[x][y]) {
								z = zm[i][l];
								z =  cost[i][t[i][j]];
								z += cost[x][t[x][y]];
								z -= cost[i][t[i][y]];
								z -= cost[x][t[x][j]];
								if(z > -1.0e-10) {
									z = 0;
								}
								double z2 = getReducedCost(i,j,cost,t,xv,yv,inrange);
								if(z != z2) {
									System.out.println("getReducedCost: error!");
									System.out.println("z: " + z);
									System.out.println("z2: " + z2);
								}
								if(z != zm[i][l]) {
									System.out.println("Different reduced cost for (i,j): " + i + "," + j);
									System.out.println("z: " + z);
									System.out.println("zm[i][l]: " + zm[i][l]);
									System.out.println("pp_i: " + pp_i);
									System.out.println("pp_j: " + pp_j);
									System.out.println("pp_x: " + xv[pp_j]);
									System.out.println("pp_y: " + yv[pp_i]);
									zm[i][l] = z;
								}
								if(z < checkMin) {
									checkMin = z;
									check_i = i;
									check_j = j;
								}
							}
						}
					}
					if(checkMin != min) {
						System.out.println("Warning checkMin does not equal min!!!");
						System.out.println("Min: " + min);
						System.out.println("Checkmin: " + checkMin);
						System.out.println("prev_i,j: " + prev_i + ", " + prev_j);
						System.out.println("check_i,j: " + check_i + ", " + check_j);
						System.out.println("zmini[check_i]: " + zmini[check_i]);
						System.out.println("zmini_j[check_i]: " + zmini_j[check_i]);
						System.out.println("zm[prev_i][prev_j]: " + zm[prev_i][t[prev_i][prev_j]]);
						min = checkMin;
						prev_i = check_i;
						prev_j = check_j;
					}
					// end old style checking */

					if(min < 0.0) {
						i = prev_i;
						j = prev_j;
						x = xv[j];
						y = yv[i];
						//System.out.println("(i,j),(x,y): (" + i + "," + j + "),(" + x + "," + y + ")");
						if(!inrange[i][j])
							System.out.println("Out of range ij: " + i + "," + j);
						if(!inrange[x][y]) {
							System.out.println("Out of range xy: " + x + "," + y);
							System.out.println("ij: " + i + "," + j);
							System.out.println("min: " + min);
						/*	System.out.println("t: " + t[x][y]);
							System.out.println("zm ij: " + zm[i][t[i][j]]);
							System.out.println("zm xy: " + zm[i][t[i][j]]);*/
							if(x != nop)
								System.out.println("x: " + p1.elementAt(x).toString());
							if(y != nop_next)
								System.out.println("y: " + p2.elementAt(y).toString());
							if(i != nop)
								System.out.println("i: " + p1.elementAt(i).toString());
							if(j != nop_next)
								System.out.println("j: " + p2.elementAt(j).toString());
						}
						// make pair ij
						g[i][j] = true;
						xv[j] = i;
						yv[i] = j;
						// make pair xy
						g[x][y] = true;
						xv[y] = x;
						yv[x] = y;
						//unset pairs iy and xj
						g[i][y] = false;
						g[x][j] = false;
						// ensure the dummies still map to each other
						xv[nop_next] = nop;
						yv[nop] = nop_next;
						//make changes to the reduced cost
						//the reverse reduced cost is the negative
						// of the current reduced cost
						//zm[i][t[i][y]] = -zm[i][t[i][j]];
						//zm[x][t[x][j]] = -zm[i][t[i][j]];
						zm[i][t[i][y]] = getReducedCost(i,y,cost,t,xv,yv,inrange);
						zm[x][t[x][j]] = getReducedCost(x,j,cost,t,xv,yv,inrange);
						
						/*if(zm[i][t[i][y]] != getReducedCost(i,y,cost,t,xv,yv,inrange)) {
							System.out.println("iy reduced cost swap fail (" +i + "," + y +")");
							System.out.println(zm[i][t[i][y]]);
							System.out.println(getReducedCost(i,y,cost,t,xv,yv,inrange));
						}
						if(zm[x][t[x][j]] != getReducedCost(x,j,cost,t,xv,yv,inrange)) {
							System.out.println("xj reduced cost swap fail (" +x + "," + j +")");
							System.out.println(zm[x][t[x][j]]);
							System.out.println(getReducedCost(x,j,cost,t,xv,yv,inrange));
						}*/
						// the connected pairs have no reduced cost
						//zm[i][t[i][j]] = 0;
						//zm[x][t[x][y]] = 0;
						zm[i][t[i][j]] = getReducedCost(i,j,cost,t,xv,yv,inrange);
						zm[x][t[x][y]] = getReducedCost(x,y,cost,t,xv,yv,inrange);
						
						if(getReducedCost(i,j,cost,t,xv,yv,inrange) != 0) {
							System.out.println("ij reduced cost swap fail (" +i + "," + j +")");
						}
						if(getReducedCost(x,y,cost,t,xv,yv,inrange) != 0) {
							System.out.println("xy reduced cost swap fail (" +x + "," + y +")");
						}
						zmini[i] = 0;
				//		for(j=0; j < nop_next+1; j++) {
				//			if(!g[i][j] && inrange[i][j]) {
						for(l=0; l < jm[i].length; l++) {
							j = jm[i][l];
							zm[i][l] = getReducedCost(i,j,cost,t,xv,yv,inrange);
							if(!g[i][j] && inrange[xv[j]][yv[i]] && zm[i][l] < zmini[i]) {
								zmini[i] = zm[i][l];
								zmini_j[i] = j;
							}
						}
						/*if(!g[i][nop_next]) {
							zmini[i] = Math.min(zmini[i],zmin[i][t[i][nop_next]];
						}*/
						zmini[x] = 0;
				//		for(j=0; j < nop_next+1; j++) {
				//			if(!g[x][j] && inrange[x][j]) {
						for(l=0; l < jm[x].length; l++) {
							j = jm[x][l];
							zm[x][l] = getReducedCost(x,j,cost,t,xv,yv,inrange);
							if(!g[x][j] && inrange[xv[j]][yv[x]] && zm[x][l] < zmini[x]) {
								zmini[x] = zm[x][l];
								zmini_j[x] = j;
							}
						}
						/*if(!g[x][nop_next]) {
							zmini[x] = Math.min(zmini[x],zmin[x][t[i][nop_next]];
						}*/
						//scan j and y for changes
						j = prev_j;
						//y = yv[prev_i];
						for(i=0; i < nop+1; i++) {
							if(inrange[i][j] && inrange[xv[j]][yv[i]]) {
								zm[i][t[i][j]] = getReducedCost(i,j,cost,t,xv,yv,inrange);
								if(zmini_j[i] == j) {
									zmini_j[i] = findReducedCostMin(i,jm,zm,xv,yv,inrange,g);
									zmini[i] = zm[i][t[i][zmini_j[i]]];
								}
								else if(!g[i][j] &&  zm[i][t[i][j]] < zmini[i]) {
									//System.out.println("Test 1 " + i + ", " + j);
									zmini[i] = zm[i][t[i][j]];
									zmini_j[i] = j;
								}
							}
							if(inrange[i][y] && inrange[xv[y]][yv[i]]) {
								zm[i][t[i][y]] = getReducedCost(i,y,cost,t,xv,yv,inrange);
								if(zmini_j[i] == y) {
									zmini_j[i] = findReducedCostMin(i,jm,zm,xv,yv,inrange,g);
									zmini[i] = zm[i][t[i][zmini_j[i]]];
								}
								else if(!g[i][y] && zm[i][t[i][y]] < zmini[i]) {
									//System.out.println("Test 2 " + i + ", " + y);
									zmini[i] = zm[i][t[i][y]];
									zmini_j[i] = y;
								
								}
							}
						}
						if(firstMin == 0)
							firstMin = min;

						IJ.showProgress(1-min/firstMin);
					}
				}
				IJ.showProgress(1.0);

				IJ.showStatus("Linking Frame " + m + ": Optimized. Linking");
				/* After optimization, the particles needs to be linked */
				for(i = 0; i < nop; i++) {
					for(j = 0; j < nop_next; j++) {
						if(g[i][j])
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
	protected double getReducedCost(int i, int j, double[][] cost, int[][] t, int[] xv, int[] yv, boolean[][] inrange) {
		double z;
		int x, y;
		//42 is the answer to the ultimate question of life,
		// the universe and everything.
		// Also an arbitrary non-negative number.
		z = 42;
		x = xv[j];
		y = yv[i];
		if(inrange[i][j] && inrange[x][y]) {
			z = cost[i][t[i][j]] +
				cost[x][t[x][y]] -
				cost[i][t[i][y]] -
				cost[x][t[x][j]];
				if(z > -1.0e-10) {
					z = 0;
				}
		} else {
			System.out.println("Warning: getReducedCost: Out of range call");
			if(!inrange[i][j])
				System.out.println("(i,j): " + i + "," + j);
			if(!inrange[x][y])
				System.out.println("(x,y): " + x + "," + y);
		}
		return z;
	}
	protected int findReducedCostMin(int i,int[][] jm,double[][] zm,int[] xv, int[] yv,boolean[][] inrange, boolean[][] g) {
		int l, j;
		double min = 1;
		int min_j = jm[i][jm[i].length-1];
		j = -1;
		for(l=0; l < jm[i].length; l++) {
			j = jm[i][l];
			if(!g[i][j] && inrange[i][j] && inrange[xv[j]][yv[i]] && zm[i][l] < min) {
				min = zm[i][l];
				min_j = j;
				//System.out.println(zm[i][l]);
			}
		}
		//System.out.println("findReducedCostMin: (" + i +"," +min_j +")" + inrange[i][j]);
		return min_j;
	}
}
