package mosaic.core.particleLinking;


import java.util.List;
import java.util.Vector;

import ij.IJ;
import mosaic.core.detection.Particle;


public class ParticleLinkerHun implements ParticleLinker {

    /**
     * Second phase of the algorithm - <br>
     * Identifies points corresponding to the
     * same physical particle in subsequent frames and links the positions into trajectories <br>
     * The length of the particles next array will be reset here according to the current linkrange <br>
     * Hungarian Pietro Incardona
     */

    private float cost_link(Particle p1, Particle p2, LinkerOptions l, int n, double max_cost) {
        double cost = 0.0;
        final float distance_sq = (p1.iX - p2.iX) * (p1.iX - p2.iX) + (p1.iY - p2.iY) * (p1.iY - p2.iY) + (p1.iZ - p2.iZ) * (p1.iZ - p2.iZ);

        cost = (float) (distance_sq * l.lSpace + l.lFeature * Math.cbrt(((p1.m0 - p2.m0) * (p1.m0 - p2.m0)) + (p1.m2 - p2.m2) * (p1.m2 - p2.m2)));

        if (l.force == true) {
            if (p1.distance >= 0.0) {
                final float lx = (p2.iX - p1.iX) / (n + 1) - p1.lx;
                final float ly = (p2.iY - p1.iY) / (n + 1) - p1.ly;
                final float lz = (p2.iZ - p1.iZ) / (n + 1) - p1.lz;

                final float f_magn_sq = lx * lx + ly * ly + lz * lz;
                cost += l.lDynamic * f_magn_sq;
            }
            else {
                // This is a fresh particle we have no idea where is going

                cost += max_cost / 3.0;
            }
        }
        else if (l.straightLine == true && p1.distance >= 0.0) {
            // Calculate the module

            final float l1_m = p1.linkModule();

            final float lx1 = p1.lx / l1_m;
            final float ly1 = p1.ly / l1_m;
            final float lz1 = p1.lz / l1_m;

            float lx2 = (p2.iX - p1.iX + p1.lxa);
            float ly2 = (p2.iY - p1.iY + p1.lya);
            float lz2 = (p2.iZ - p1.iZ + p1.lza);

            final float l2_m = (float) Math.sqrt(lx2 * lx2 + ly2 * ly2 + lz2 * lz2);

            if (l2_m >= l.minSquaredDisplacementForAngleCalculation) {
                lx2 /= l2_m;
                ly2 /= l2_m;
                lz2 /= l2_m;

                final float cos_phi = lx1 * lx2 + ly1 * ly2 + lz1 * lz2;

                cost += (cos_phi - 1) * (cos_phi - 1) * l.maxDisplacement * l.maxDisplacement;
            }
        }
        return (float) cost;
    }

    @Override
    public boolean linkParticles(List<Vector<Particle>> aParticles, LinkerOptions l) {
        int frames_number = aParticles.size();
        int m, i, j, nop, nop_next, n;
        int curr_linkrange;

        if (l.linkRange > 1) {
            IJ.error("Error Hungarian linker for now does not support link range > 1");
            return false;
        }

        /**
         * okv is a helper vector in the initialization phase. It keeps track of the empty columns
         * in g.
         */
        boolean[] okv;
        float max_cost;
        /** The cost matrix - TODO: it is quite sparse and one should take advantage of that. */
        Vector<Particle> p1, p2;

        // set the length of the particles next array according to the linkrange
        // it is done now since link range can be modified after first run
        for (int fr = 0; fr < frames_number; fr++) {
            for (int pr = 0; pr < aParticles.get(fr).size(); pr++) {
                aParticles.get(fr).elementAt(pr).next = new int[l.linkRange];
            }
        }
        curr_linkrange = l.linkRange;

        /* If the linkrange is too big, set it the right value */
        if (frames_number < (curr_linkrange + 1)) {
            curr_linkrange = frames_number - 1;
        }

        // max_cost = displacement * displacement;

        for (m = 0; m < frames_number - curr_linkrange; m++) {
            IJ.showStatus("Linking Frame " + (m + 1));
            nop = aParticles.get(m).size();
            for (i = 0; i < nop; i++) {
                aParticles.get(m).elementAt(i).special = false;
                for (n = 0; n < l.linkRange; n++) {
                    aParticles.get(m).elementAt(i).next[n] = -1;
                }
            }

            for (n = 0; n < curr_linkrange; n++) {
                max_cost = (n + 1) * l.maxDisplacement * (n + 1) * l.maxDisplacement;

                nop_next = aParticles.get(m + (n + 1)).size();

                okv = new boolean[nop_next + 1];
                for (i = 0; i < okv.length; i++) {
                    okv[i] = true;
                }

                /* Set g to zero - not necessary */
                // for (i = 0; i< g.length; i++) g[i] = false;

                p1 = aParticles.get(m);
                p2 = aParticles.get(m + (n + 1));
                // p1 = frames[m].particles;
                // p2 = frames[m + (n + 1)].particles

                // Create a bipartite graph

                final BipartiteMatcher bpm = new BipartiteMatcher(nop + nop_next);

                //

                /* Fill in the costs */
                for (i = 0; i < nop + nop_next; i++) {
                    for (j = 0; j < nop + nop_next; j++) {
                        if (i < nop && j < nop_next) {
                            final double cost = cost_link(p1.elementAt(i), p2.elementAt(j), l, n, max_cost);
                            bpm.setWeight(i, j, max_cost - cost);
                        }
                        else {
                            bpm.setWeight(i, j, 0.0);
                        }
                    }
                }

                final int[] mac = bpm.getMatching();

                /* After optimization, the particles needs to be linked */
                for (i = 0; i < nop + nop_next; i++) {
                    // Adjust mac[i]

                    if (i >= nop || mac[i] >= nop_next) {
                        continue;
                    }

                    p1 = aParticles.get(m);
                    p2 = aParticles.get(m + 1);

                    if (cost_link(p1.elementAt(i), p2.elementAt(mac[i]), l, 0, max_cost) >= max_cost) {
                        continue;
                    }

                    // link particle

                    p1.elementAt(i).next[n] = mac[i];

                    // if linked to a dummy particle does not calculate

                    if (mac[i] >= nop_next) {
                        continue;
                    }

                    // Calculate the square distance and store the normalized linking vector

                    if (l.force == true) {
                        p2.elementAt(mac[i]).lx = (p2.elementAt(mac[i]).iX - p1.elementAt(i).iX) / (n + 1);
                        p2.elementAt(mac[i]).ly = (p2.elementAt(mac[i]).iY - p1.elementAt(i).iY) / (n + 1);
                        p2.elementAt(mac[i]).lz = (p2.elementAt(mac[i]).iZ - p1.elementAt(i).iZ) / (n + 1);

                        // We do not use distance is just to indicate that the particle has a link vector

                        p2.elementAt(mac[i]).distance = 1.0f;
                    }
                    else if (l.straightLine == true) {
                        final float distance_sq = (float) Math.sqrt((p1.elementAt(i).iX - p2.elementAt(mac[i]).iX) * (p1.elementAt(i).iX - p2.elementAt(mac[i]).iX)
                                + (p1.elementAt(i).iY - p2.elementAt(mac[i]).iY) * (p1.elementAt(i).iY - p2.elementAt(mac[i]).iY) + (p1.elementAt(i).iZ - p2.elementAt(mac[i]).iZ)
                                * (p1.elementAt(i).iZ - p2.elementAt(mac[i]).iZ));

                        if (distance_sq >= l.minSquaredDisplacementForAngleCalculation) {
                            p2.elementAt(mac[i]).lx = (p2.elementAt(mac[i]).iX - p1.elementAt(i).iX) + p1.elementAt(i).lxa;
                            p2.elementAt(mac[i]).ly = (p2.elementAt(mac[i]).iY - p1.elementAt(i).iY) + p1.elementAt(i).lya;
                            p2.elementAt(mac[i]).lz = (p2.elementAt(mac[i]).iZ - p1.elementAt(i).iZ) + p1.elementAt(i).lza;
                        }
                        else {
                            // Propagate the previous link vector

                            p2.elementAt(mac[i]).lx = p1.elementAt(i).lx;
                            p2.elementAt(mac[i]).ly = p1.elementAt(i).ly;
                            p2.elementAt(mac[i]).lz = p1.elementAt(i).lz;

                            p2.elementAt(mac[i]).lxa += (p2.elementAt(mac[i]).iX - p1.elementAt(i).iX) + p1.elementAt(i).lxa;
                            p2.elementAt(mac[i]).lya += (p2.elementAt(mac[i]).iY - p1.elementAt(i).iY) + p1.elementAt(i).lya;
                            p2.elementAt(mac[i]).lza += (p2.elementAt(mac[i]).iZ - p1.elementAt(i).iZ) + p1.elementAt(i).lza;

                            if (p2.elementAt(mac[i]).linkModuleASq() >= l.minSquaredDisplacementForAngleCalculation) {
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

            if (m == (frames_number - curr_linkrange - 1) && curr_linkrange > 1) {
                curr_linkrange--;
            }
        }

        /* At the last frame all trajectories end */
        for (i = 0; i < aParticles.get(frames_number - 1).size(); i++) {
            aParticles.get(frames_number - 1).elementAt(i).special = false;
            for (n = 0; n < l.linkRange; n++) {
                aParticles.get(frames_number - 1).elementAt(i).next[n] = -1;
            }
        }

        return true;
    }
}
