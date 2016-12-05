package mosaic.core.particleLinking;


import java.util.Vector;

import org.apache.log4j.Logger;

import ij.IJ;
import mosaic.core.detection.Particle;


public class ParticleLinkerHungarian extends ParticleLinker {
    private static final Logger logger = Logger.getLogger(ParticleLinkerHungarian.class);
    
    @Override
    protected void link(Vector<Particle> p1, Vector<Particle> p2, LinkerOptions aLinkOpts, int currFrame, final int NumOfFrames, int numOfParticles, int numOfLinkParticles, int currLinkLevel, final float maxCost) {
        // --------------------------------------------------------------------------------
        logInfo("Initializing cost: " + (currFrame + 1) + "/" + NumOfFrames + " with frame: " + (currFrame + currLinkLevel + 1));
        int n = numOfParticles > numOfLinkParticles ? numOfParticles : numOfLinkParticles;
        // Extend graph by number of input particles to allow it to not link to output particles
        // in case to big cost (dummy particles)
        n +=  numOfParticles;
        final BipartiteMatcher bm = new BipartiteMatcher(n);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (i < numOfParticles && j < numOfLinkParticles) {
                    double cost = linkCost(p1.elementAt(i), p2.elementAt(j), aLinkOpts, currLinkLevel);
                    bm.setWeight(i, j, -cost); // matcher is maximizing, therefore negative cost is applied.
                }
                else {
                    bm.setWeight(i, j, -maxCost);
                }
            }
        }
        
        // --------------------------------------------------------------------------------
        logInfo("Optimizing: " + (currFrame + 1) + "/" + NumOfFrames);
        final int[] matchingResult = bm.getMatching();

        // --------------------------------------------------------------------------------
        logInfo("Linking particles: " + (currFrame + 1) + "/" + NumOfFrames);
        for (int i = 0; i < numOfParticles; ++i) {
            // if not linked to dummy particle
            if (matchingResult[i] < numOfLinkParticles && matchingResult[i] >= 0) {
                Particle pA = p1.elementAt(i);
                Particle pB = p2.elementAt(matchingResult[i]);
                pA.next[currLinkLevel - 1] = matchingResult[i]; // levels are in range 1..LinkRange
                handleCostFeatures(pA, pB, aLinkOpts, currLinkLevel); 
            }
        }
    }
    
    private void logInfo(String aLogStr) {
        IJ.showStatus(aLogStr);
        logger.info(aLogStr);
    }
}
