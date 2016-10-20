package mosaic.core.particleLinking;


import ij.IJ;

import java.util.Vector;

import org.apache.log4j.Logger;

import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;


public class ParticleLinkerBestOnePerm implements ParticleLinker {
    private static final Logger logger = Logger.getLogger(ParticleLinkerBestOnePerm.class);

    /**
     * Second phase of the algorithm - Identifies points corresponding to the
     * same physical particle in subsequent frames and links the positions into trajectories <br>
     * The length of the particles next array will be reset here according to the current linkrange <br>
     * Adapted from Ingo Oppermann implementation <br>
     * Refactored by Janick Cardinale, ETHZ, 1.6.2012 <br>
     * Optimized with ideas from Mark Kittisopikul, UT Southwestern
     */
    @Override
    public boolean linkParticles(MyFrame[] frames, linkerOptions l) {
        final int numOfFrames = frames.length;
        
        // set the length of the particles next array according to the linkrange
        // it is done now since link range can be modified after first run
        for (int fr = 0; fr < numOfFrames; fr++) {
            for (int pr = 0; pr < frames[fr].getParticles().size(); pr++) {
                frames[fr].getParticles().elementAt(pr).next = new int[l.linkrange];
            }
        }
        
        int currLinkRange = (numOfFrames <= l.linkrange) ? (numOfFrames - 1) : l.linkrange;

        for (int m = 0; m < numOfFrames - currLinkRange; m++) {
            IJ.showStatus("Linking Frame " + (m + 1));
            logger.debug("Linking Frame: " + (m + 1) + "/" + numOfFrames);
            int nop = frames[m].getParticles().size();
            for (int i = 0; i < nop; i++) {
                frames[m].getParticles().elementAt(i).special = false;
                for (int n = 0; n < l.linkrange; n++) {
                    frames[m].getParticles().elementAt(i).next[n] = -1;
                }
            }

            for (int n = 0; n < currLinkRange; n++) {
                float max_cost = (n + 1) * l.displacement * (n + 1) * l.displacement;
                int nop_next = frames[m + (n + 1)].getParticles().size();

                /* Set up the cost matrix */
                float[][] cost = new float[nop + 1][nop_next + 1];

                // The association/relation matrix g
                boolean[][] g = new boolean[nop + 1][nop_next + 1];
                // g_x stores the index of the currently associated particle, and vice versa. It is another representation for the association matrix g.
                int[] g_x = new int[nop_next + 1];
                int[] g_y = new int[nop + 1];

                Vector<Particle> p1 = frames[m].getParticles();
                Vector<Particle> p2 = frames[m + (n + 1)].getParticles();

                /* Fill in the costs */
                for (int i = 0; i < nop; i++) {
                    for (int j = 0; j < nop_next; j++) {
                        final float distance_sq = (p1.elementAt(i).iX - p2.elementAt(j).iX) * (p1.elementAt(i).iX - p2.elementAt(j).iX) + (p1.elementAt(i).iY - p2.elementAt(j).iY)
                                * (p1.elementAt(i).iY - p2.elementAt(j).iY) + (p1.elementAt(i).iZ - p2.elementAt(j).iZ) * (p1.elementAt(i).iZ - p2.elementAt(j).iZ);

                        cost[i][j] = (float) (distance_sq * l.l_s + l.l_f
                                * Math.cbrt(((p1.elementAt(i).m0 - p2.elementAt(j).m0) * (p1.elementAt(i).m0 - p2.elementAt(j).m0)) + (p1.elementAt(i).m2 - p2.elementAt(j).m2)
                                        * (p1.elementAt(i).m2 - p2.elementAt(j).m2)));

                        if (l.force == true) {
                            if (p1.elementAt(i).distance >= 0.0) {
                                final float lx = (p2.elementAt(j).iX - p1.elementAt(i).iX) / (n + 1) - p1.elementAt(i).lx;
                                final float ly = (p2.elementAt(j).iY - p1.elementAt(i).iY) / (n + 1) - p1.elementAt(i).ly;
                                final float lz = (p2.elementAt(j).iZ - p1.elementAt(i).iZ) / (n + 1) - p1.elementAt(i).lz;

                                final float f_magn_sq = lx * lx + ly * ly + lz * lz;
                                cost[i][j] += l.l_d * f_magn_sq;
                            }
                        }
                        else if (l.straight_line == true && p1.elementAt(i).distance >= 0.0) {
                            // Calculate the module
                            final float l1_m = p1.elementAt(i).linkModule();

                            final float lx1 = p1.elementAt(i).lx / l1_m;
                            final float ly1 = p1.elementAt(i).ly / l1_m;
                            final float lz1 = p1.elementAt(i).lz / l1_m;

                            float lx2 = (p2.elementAt(j).iX - p1.elementAt(i).iX + p1.elementAt(i).lxa);
                            float ly2 = (p2.elementAt(j).iY - p1.elementAt(i).iY + p1.elementAt(i).lya);
                            float lz2 = (p2.elementAt(j).iZ - p1.elementAt(i).iZ + p1.elementAt(i).lza);

                            final float l2_m = (float) Math.sqrt(lx2 * lx2 + ly2 * ly2 + lz2 * lz2);

                            if (l2_m >= l.r_sq) {
                                lx2 /= l2_m;
                                ly2 /= l2_m;
                                lz2 /= l2_m;

                                final float cos_phi = lx1 * lx2 + ly1 * ly2 + lz1 * lz2;
                                cost[i][j] += (cos_phi - 1) * (cos_phi - 1) * l.displacement * l.displacement;
                            }
                        }
                    }
                }

                for (int i = 0; i < nop + 1; i++) {
                    cost[i][nop_next] = max_cost;
                }
                for (int j = 0; j < nop_next + 1; j++) {
                    cost[nop][j] = max_cost;
                }
                cost[nop][nop_next] = 0.0f;

                /* Initialize the relation matrix */
                IJ.showStatus("Linking Frame " + (m + 1) + ": Initializing Relation matrix");
                logger.debug("Linking Frame: " + (m + 1) + "/" + numOfFrames + " - Initializing Relation matrix");
                
                // okv is a helper vector in the initialization phase. It keeps track of the empty columns in g.
                boolean[] okv = new boolean[nop_next + 1];
                for (int i = 0; i < okv.length; i++) {
                    okv[i] = true;
                }
                
                for (int i = 0; i < nop; i++) { // Loop over the x-axis
                    IJ.showProgress(i, nop);
                    double min = max_cost;
                    int prev = -1;
                    for (int j = 0; j < nop_next; j++) { // Loop over the y-axis without the dummy

                        /* Let's see if we can use this coordinate */
                        if (okv[j] && min > cost[i][j]) {
                            min = cost[i][j];
                            if (prev >= 0) {
                                okv[prev] = true;
                                g[i][prev] = false;
                            }

                            okv[j] = false;
                            g[i][j] = true;

                            prev = j;
                        }
                    }

                    /* Check if we have a dummy particle */
                    if (min == max_cost) {
                        if (prev >= 0) {
                            okv[prev] = true;
                            g[i][prev] = false;
                        }
                        g[i][nop_next] = true;
                        okv[nop_next] = false;
                    }
                }

                /* Look for columns that are zero */
                for (int j = 0; j < nop_next; j++) {
                    int ok = 1;
                    for (int i = 0; i < nop + 1; i++) {
                        if (g[i][j]) {
                            ok = 0;
                        }
                    }
                    if (ok == 1) {
                        g[nop][j] = true;
                    }
                }

                /* Build xv and yv, a speedup for g */
                for (int i = 0; i < nop + 1; i++) {
                    // for (j = 0; j < nop_next+1; j++) {
                    for (int j = 0; j < nop_next + 1; j++) {
                        if (g[i][j]) {
                            g_x[j] = i;
                            g_y[i] = j;
                        }
                    }
                }
                g_x[nop_next] = nop;
                g_y[nop] = nop_next;

                /* The relation matrix is initilized */

                /* Now the relation matrix needs to be optimized */
                IJ.showStatus("Linking Frame " + (m + 1) + ": Optimizing Relation matrix");
                logger.debug("Linking Frame: " + (m + 1) + "/" + numOfFrames + " - Optimizing Relation matrix");
                
                double min = -1.0;
                
                // Stopping condition variables. If optimization does not proceed (minimum value is not changed over MaximumNumberOfSameMins times)
                // optimization is stopped. Number of maximum same min-values is currently chosen without investigation and just set to number of particles
                // but values like 10 works as good as it. (It is not clear if after some iterations value can be changed).
                final int MaximumNumberOfSameMinValues = nop;
                double lastMin = min;
                int countSameMinValues = 0;
                
                while (min < 0.0 && countSameMinValues < MaximumNumberOfSameMinValues) {
                    min = 0.0;
                    int prev_i = 0, prev_j = 0, prev_x = 0, prev_y = 0;
                    for (int i = 0; i < nop + 1; i++) {
                        for (int j = 0; j < nop_next + 1; j++) {
                            if (i == nop && j == nop_next) {
                                continue;
                            }

                            if (g[i][j] == false && cost[i][j] <= max_cost) {
                                // Calculate the reduced cost

                                // Look along the x-axis, including the dummy particles
                                int x = g_x[j];

                                // Look along the y-axis, including the dummy particles
                                int y = g_y[i];

                                // z is the reduced cost
                                double z = cost[i][j] + cost[x][y] - cost[i][y] - cost[x][j];

                                if (z > -1.0e-10) {
                                    z = 0.0;
                                }
                                if (z < min) {
                                    min = z;

                                    prev_i = i;
                                    prev_j = j;
                                    prev_x = x;
                                    prev_y = y;
                                }
                            }
                        }
                    }

                    if (min < 0.0) {
                        g[prev_i][prev_j] = true;
                        g_x[prev_j] = prev_i;
                        g_y[prev_i] = prev_j;
                        g[prev_x][prev_y] = true;
                        g_x[prev_y] = prev_x;
                        g_y[prev_x] = prev_y;
                        g[prev_i][prev_y] = false;
                        g[prev_x][prev_j] = false;

                        // ensure the dummies still map to each other
                        g_x[nop_next] = nop;
                        g_y[nop] = nop_next;
                    }
                    
                    if (lastMin == min) {
                        countSameMinValues++;
                    }
                    else {
                        lastMin = min;
                        countSameMinValues = 0;
                    }
                }
                logger.debug("Done.");

                /* After optimization, the particles needs to be linked */
                for (int i = 0; i < nop; i++) {
                    for (int j = 0; j < nop_next; j++) {
                        if (g[i][j] == true) {
                            p1.elementAt(i).next[n] = j;

                            // Calculate the square distance and store the normalized linking vector

                            if (l.force == true) {
                                p2.elementAt(j).lx = (p2.elementAt(j).iX - p1.elementAt(i).iX) / (n + 1);
                                p2.elementAt(j).ly = (p2.elementAt(j).iY - p1.elementAt(i).iY) / (n + 1);
                                p2.elementAt(j).lz = (p2.elementAt(j).iZ - p1.elementAt(i).iZ) / (n + 1);

                                // We do not use distance is just to indicate that the particle has a link vector
                                p2.elementAt(j).distance = 1.0f;
                            }
                            else if (l.straight_line == true) {
                                final float distance_sq = (float) Math.sqrt((p1.elementAt(i).iX - p2.elementAt(j).iX) * (p1.elementAt(i).iX - p2.elementAt(j).iX) + (p1.elementAt(i).iY - p2.elementAt(j).iY)
                                        * (p1.elementAt(i).iY - p2.elementAt(j).iY) + (p1.elementAt(i).iZ - p2.elementAt(j).iZ) * (p1.elementAt(i).iZ - p2.elementAt(j).iZ));

                                if (distance_sq >= l.r_sq) {
                                    p2.elementAt(j).lx = (p2.elementAt(j).iX - p1.elementAt(i).iX) + p1.elementAt(i).lxa;
                                    p2.elementAt(j).ly = (p2.elementAt(j).iY - p1.elementAt(i).iY) + p1.elementAt(i).lya;
                                    p2.elementAt(j).lz = (p2.elementAt(j).iZ - p1.elementAt(i).iZ) + p1.elementAt(i).lza;
                                }
                                else {
                                    // Propagate the previous link vector

                                    p2.elementAt(j).lx = p1.elementAt(i).lx;
                                    p2.elementAt(j).ly = p1.elementAt(i).ly;
                                    p2.elementAt(j).lz = p1.elementAt(i).lz;

                                    p2.elementAt(j).lxa += (p2.elementAt(j).iX - p1.elementAt(i).iX) + p1.elementAt(i).lxa;
                                    p2.elementAt(j).lya += (p2.elementAt(j).iY - p1.elementAt(i).iY) + p1.elementAt(i).lya;
                                    p2.elementAt(j).lza += (p2.elementAt(j).iZ - p1.elementAt(i).iZ) + p1.elementAt(i).lza;

                                    if (p2.elementAt(j).linkModuleASq() >= l.r_sq) {
                                        p2.elementAt(j).lx = p2.elementAt(j).lxa;
                                        p2.elementAt(j).ly = p2.elementAt(j).lya;
                                        p2.elementAt(j).lz = p2.elementAt(j).lza;

                                        p2.elementAt(j).lxa = 0;
                                        p2.elementAt(j).lya = 0;
                                        p2.elementAt(j).lza = 0;
                                    }
                                }

                                p2.elementAt(j).distance = (float) Math.sqrt(distance_sq);
                            }
                        }
                    }
                }
            }

            if (m == (numOfFrames - currLinkRange - 1) && currLinkRange > 1) {
                currLinkRange--;
            }
        }

        /* At the last frame all trajectories end */
        for (int i = 0; i < frames[numOfFrames - 1].getParticles().size(); i++) {
            frames[numOfFrames - 1].getParticles().elementAt(i).special = false;
            for (int n = 0; n < l.linkrange; n++) {
                frames[numOfFrames - 1].getParticles().elementAt(i).next[n] = -1;
            }
        }

        return true;
    }
}
