package mosaic.core.particleLinking;


import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import ij.IJ;
import mosaic.core.detection.Particle;


public abstract class ParticleLinker {
    private static final Logger logger = Logger.getLogger(ParticleLinker.class);
    
    /**
     * Second phase of the algorithm - <br>
     * Identifies points corresponding to the
     * same physical particle in subsequent frames and links the positions into trajectories <br>
     * The length of the particles next array will be reset here according to the current linkrange
     */
    public boolean linkParticles(List<Vector<Particle>> aParticles, LinkerOptions aLinkOpts) {
        logger.info("Linking options:" + " maxDisplacement: " + aLinkOpts.maxDisplacement + ", linkRange: " + aLinkOpts.linkRange + 
                ", straightLine: " + aLinkOpts.straightLine + 
                ", minSquaredDisplacementForAngleCalculation: " + aLinkOpts.minSquaredDisplacementForAngleCalculation + 
                ", force: " + aLinkOpts.force + 
                ", lSpace: " + aLinkOpts.lSpace + ", lFeature: " + aLinkOpts.lFeature + ", lDynamic: " + aLinkOpts.lDynamic);

        final int NumOfFrames = aParticles.size();
        int LinkRange = aLinkOpts.linkRange;
        for (int currFrame = 0; currFrame < NumOfFrames; ++currFrame) {
            for (Particle p : aParticles.get(currFrame)) {
                p.lx = p.ly = p.lz = 0;
                p.lxa = p.lya = p.lza = 0;
            }
        }
        for (int currFrame = 0; currFrame < NumOfFrames; ++currFrame) {     
            int currLinkRange = (currFrame < NumOfFrames - LinkRange) ? LinkRange : NumOfFrames - currFrame - 1;
            logInfo("----- Linking Frame " + (currFrame + 1) + "/" + NumOfFrames + " linkRange: " + currLinkRange + " ----------------------");            

            Vector<Particle> p1 = aParticles.get(currFrame);
            int numOfParticles = p1.size();
            initParticlesLinkData(LinkRange, p1);
            
            for (int currLinkLevel = 1; currLinkLevel <= currLinkRange; ++currLinkLevel) {
                Vector<Particle> p2 = aParticles.get(currFrame + currLinkLevel);
                int numOfLinkParticles = aParticles.get(currFrame + (currLinkLevel )).size();
                
                final float maxCost = (float) Math.pow(currLinkLevel * aLinkOpts.maxDisplacement, 2);

                link(p1, p2, aLinkOpts, currFrame, NumOfFrames, numOfParticles, numOfLinkParticles, currLinkLevel, maxCost);
            }
        }
        
        return true;
    }
    
    protected abstract void link(Vector<Particle> p1, Vector<Particle> p2, LinkerOptions aLinkOpts, int currFrame, final int NumOfFrames, int numOfParticles, int numOfLinkParticles, int currLinkLevel, final float maxCost);
    
    public float linkCost(Particle pA, Particle pB, LinkerOptions aLinkOpts, int aLinkLevel) {
        float dx = pB.iX - pA.iX;
        float dy = pB.iY - pA.iY;
        float dz = pB.iZ - pA.iZ;
        float distanceSq = dx*dx + dy*dy + dz*dz;
        
        float dm0 = pB.m0 - pA.m0;
        float dm2 = pB.m2 - pA.m2;
        float momentsDist = (float) Math.cbrt(dm0 * dm0 + dm2 * dm2);
        
        float linkCost = distanceSq * aLinkOpts.lSpace + momentsDist * aLinkOpts.lFeature;

        if (aLinkOpts.force == true && pA.distance >= 0.0) {
            final float lx = dx / aLinkLevel - pA.lx;
            final float ly = dy / aLinkLevel - pA.ly;
            final float lz = dz / aLinkLevel - pA.lz;

            final float dynamicCost = lx * lx + ly * ly + lz * lz;
            linkCost += aLinkOpts.lDynamic * dynamicCost;
        }
        else if (aLinkOpts.straightLine == true && pA.distance >= 0.0) {
            float lx2 = dx + pA.lxa;
            float ly2 = dy + pA.lya;
            float lz2 = dz + pA.lza;
            final float l2_m = (float) Math.sqrt(lx2 * lx2 + ly2 * ly2 + lz2 * lz2);
            final float l1_m = (float) Math.sqrt(pA.lx * pA.lx + pA.ly * pA.ly + pA.lz * pA.lz);                                

            if (l2_m >= aLinkOpts.minSquaredDisplacementForAngleCalculation && l1_m > 0) {
                final float lx1 = pA.lx / l1_m;
                final float ly1 = pA.ly / l1_m;
                final float lz1 = pA.lz / l1_m;
                
                lx2 /= l2_m;
                ly2 /= l2_m;
                lz2 /= l2_m;

                final float cosPhi = lx1 * lx2 + ly1 * ly2 + lz1 * lz2;
                linkCost += (cosPhi - 1) * (cosPhi - 1) * aLinkOpts.maxDisplacement * aLinkOpts.maxDisplacement;
            }
        }

        return linkCost;
    }
    
    protected void handleCostFeatures(Particle pA, Particle pB, LinkerOptions aLinkOpts, int aLinkLevel) {
        float dx = pB.iX - pA.iX;
        float dy = pB.iY - pA.iY;
        float dz = pB.iZ - pA.iZ;
        
        if (aLinkOpts.force == true) {
            // Store the normalized linking vector
            pB.lx = dx / aLinkLevel;
            pB.ly = dy / aLinkLevel;
            pB.lz = dz / aLinkLevel;

            // We do not use distance is just to indicate that the particle has a link vector
            pB.distance = 1.0f;
        }
        else if (aLinkOpts.straightLine == true) {
            float distanceSq = dx*dx + dy*dy + dz*dz;
            if (distanceSq >= aLinkOpts.minSquaredDisplacementForAngleCalculation) {
                pB.lx = dx + pA.lxa;
                pB.ly = dy + pA.lya;
                pB.lz = dz + pA.lza;
            }
            else {
                // Propagate the previous link vector
                pB.lx = pA.lx;
                pB.ly = pA.ly;
                pB.lz = pA.lz;

                pB.lxa += dx + pA.lxa;
                pB.lya += dy + pA.lya;
                pB.lza += dz + pA.lza;
                float lengthSq = pB.lxa * pB.lxa + pB.lya * pB.lya + pB.lza * pB.lza;
                
                if (lengthSq >= aLinkOpts.minSquaredDisplacementForAngleCalculation) {
                    pB.lx = pB.lxa;
                    pB.ly = pB.lya;
                    pB.lz = pB.lza;

                    pB.lxa = 0;
                    pB.lya = 0;
                    pB.lza = 0;
                }
            }
            pB.distance = (float) Math.sqrt(distanceSq);
        }
    }    
    
    protected void initParticlesLinkData(final int aLinkRange, Vector<Particle> aParticles) {
        for (Particle p : aParticles) {
            p.special = false;
            p.next = new int[aLinkRange];
            for (int n = 0; n < aLinkRange; ++n) {
                p.next[n] = -1;
            }
        }
    }
    
    private void logInfo(String aLogStr) {
        IJ.showStatus(aLogStr);
        logger.info(aLogStr);
    }    
}
