package mosaic.core.particleLinking;


import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import ij.IJ;
import mosaic.core.detection.Particle;


/**
 * Second phase of the algorithm - Identifies points corresponding to the same physical particle in subsequent frames. 
 * Adapted from Ingo Oppermann implementation.
 * Refactored by Janick Cardinale, ETHZ, 1.6.2012
 * Optimized with ideas from Mark Kittisopikul, UT Southwestern
 * Refactored by Krzysztof Gonciarz
 */
public class ParticleLinkerBestOnePerm implements ParticleLinker {
    private static final Logger logger = Logger.getLogger(ParticleLinkerBestOnePerm.class);

    @Override
    public boolean linkParticles(List<Vector<Particle>> aParticles, LinkerOptions aLinkOpts) {
        logger.info("Linking options:" + " maxDisplacement: " + aLinkOpts.maxDisplacement + ", linkRange: " + aLinkOpts.linkRange + 
                    ", straightLine: " + aLinkOpts.straightLine + 
                    ", minSquaredDisplacementForAngleCalculation: " + aLinkOpts.minSquaredDisplacementForAngleCalculation + 
                    ", force: " + aLinkOpts.force + 
                    ", lSpace: " + aLinkOpts.lSpace + ", lFeature: " + aLinkOpts.lFeature + ", lDynamic: " + aLinkOpts.lDynamic);

        final int NumOfFrames = aParticles.size();
        final int LinkRange = aLinkOpts.linkRange;
       
        for (int currFrame = 0; currFrame < NumOfFrames; ++currFrame) {
            int currLinkRange = (currFrame < NumOfFrames - LinkRange) ? LinkRange : NumOfFrames - currFrame - 1;
            logInfo("----- Linking Frame " + (currFrame + 1) + "/" + NumOfFrames + " linkRange: " + currLinkRange + " ----------------------");

            Vector<Particle> p1 = aParticles.get(currFrame);
            int numOfParticles = p1.size();
            initParticlesLinkData(LinkRange, p1);

            for (int currLinkLevel = 1; currLinkLevel <= currLinkRange; ++currLinkLevel) {
                Vector<Particle> p2 = aParticles.get(currFrame + currLinkLevel);
                int numOfLinkParticles = p2.size();

                final float maxCost = (float) Math.pow(currLinkLevel * aLinkOpts.maxDisplacement, 2);
               
                // --------------------------------------------------------------------------------
                logInfo("Initializing cost matrix: " + (currFrame + 1) + "/" + NumOfFrames + " with frame: " + (currFrame + currLinkLevel + 1));
                float[][] cost = initCostMatrix(aLinkOpts, p1, p2, currLinkLevel, maxCost);

                // --------------------------------------------------------------------------------
                logInfo("Initializing Relation matrix: " + (currFrame + 1) + "/" + NumOfFrames);
               
                // The association/relation matrix g, rows representing particles from current frame and columns particles 
                // from the next frame, 'true' means that particles are connected/linked
                boolean[][] g = initRelationMatrix(numOfParticles, numOfLinkParticles, maxCost, cost);

                // gX/gY stores the index of the currently associated particle (column/row wise) 
                int[][] helperVectors = initHelperVectors(numOfParticles, numOfLinkParticles, g);
                int[] gX = helperVectors[0];
                int[] gY = helperVectors[1];
               
                // --------------------------------------------------------------------------------
                logInfo("Optimizing Relation matrix: " + (currFrame + 1) + "/" + NumOfFrames);
                optimizeRelationMatrix(numOfParticles, numOfLinkParticles, maxCost, cost, g, gX, gY);
               
                // --------------------------------------------------------------------------------
                logInfo("Linking particles: " + (currFrame + 1) + "/" + NumOfFrames);
                linkParticles(aLinkOpts, p1, p2, currLinkLevel, gY, numOfParticles, numOfLinkParticles);
            }
        }

        return true;
    }

    private void linkParticles(LinkerOptions aLinkOpts, Vector<Particle> p1, Vector<Particle> p2, int aLinkLevel, int[] gY, int aNumOfParticles, int aNumOfLinkParticles) {
        for (int i = 0; i < aNumOfParticles; ++i) {
            int j = gY[i];
            if (j != aNumOfLinkParticles) { // if not linked to dummy particle
                Particle pA = p1.elementAt(i);
                Particle pB = p2.elementAt(j);

                pA.next[aLinkLevel - 1] = j; // levels are in range 1..LinkRange

                if (aLinkOpts.force == true) {
                    // Store the normalized linking vector
                    pB.lx = (pB.iX - pA.iX) / aLinkLevel;
                    pB.ly = (pB.iY - pA.iY) / aLinkLevel;
                    pB.lz = (pB.iZ - pA.iZ) / aLinkLevel;

                    // We do not use distance is just to indicate that the particle has a link vector
                    pB.distance = 1.0f;
                }
                else if (aLinkOpts.straightLine == true) {
                    float distanceSq = (pA.iX - pB.iX) * (pA.iX - pB.iX) + (pA.iY - pB.iY) * (pA.iY - pB.iY) + (pA.iZ - pB.iZ) * (pA.iZ - pB.iZ);
                    if (distanceSq >= aLinkOpts.minSquaredDisplacementForAngleCalculation) {
                        pB.lx = (pB.iX - pA.iX) + pA.lxa;
                        pB.ly = (pB.iY - pA.iY) + pA.lya;
                        pB.lz = (pB.iZ - pA.iZ) + pA.lza;
                    }
                    else {
                        // Propagate the previous link vector
                        pB.lx = pA.lx;
                        pB.ly = pA.ly;
                        pB.lz = pA.lz;

                        pB.lxa += (pB.iX - pA.iX) + pA.lxa;
                        pB.lya += (pB.iY - pA.iY) + pA.lya;
                        pB.lza += (pB.iZ - pA.iZ) + pA.lza;

                        if (pB.linkModuleASq() >= aLinkOpts.minSquaredDisplacementForAngleCalculation) {
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
        }
    }

    public void optimizeRelationMatrix(int aNumOfParticles, int aNumOfLinkParticles, final float aMaxCost, float[][] aCostMatrix, boolean[][] g, int[] gX, int[] gY) {
        while (true) {
            double minDeltaCost = 0.0;
            int iOld = -1, jOld = -1, xOld = -1, yOld = -1;
            
            // Find the best possible optimization in current loop
            for (int i = 0; i < aNumOfParticles + 1; ++i) {
                for (int j = 0; j < aNumOfLinkParticles + 1; ++j) {
                    if (i == aNumOfParticles && j == aNumOfLinkParticles) { // if dummy2dummy skip
                        continue;
                    }
                    // For every not assigned (i, j) check if cost of (i, y) and (x, j) is bigger than possible 
                    // (i, j) and (x, y) -> if yes, switch
                    if (g[i][j] == false && aCostMatrix[i][j] <= aMaxCost) {
                        int x = gX[j];
                        int y = gY[i];

                        // Calculate the reduced cost of switching links as z = (new cost) - (old cost)
                        double newDeltaCost = (aCostMatrix[i][j] + aCostMatrix[x][y]) - (aCostMatrix[i][y] + aCostMatrix[x][j]);
                        
                        if (newDeltaCost < minDeltaCost) {
                            minDeltaCost = newDeltaCost;
                            iOld = i;
                            jOld = j;
                            xOld = x;
                            yOld = y;
                        }
                    }
                }
            }
            
            // If optimization is found apply it.
            if (minDeltaCost < 0.0) {
                // relink to new minimized cost
                g[iOld][yOld] = false;
                g[xOld][jOld] = false;
                g[iOld][jOld] = true;
                g[xOld][yOld] = true;
                gY[iOld] = jOld;
                gX[jOld] = iOld;
                gY[xOld] = yOld;
                gX[yOld] = xOld;
                // ensure the dummies still map to each other
                gX[aNumOfLinkParticles] = aNumOfParticles;
                gY[aNumOfParticles] = aNumOfLinkParticles;
            }
            
            // If there are no possible optimizations break loop.
            if (minDeltaCost >= 0) break;
        }
    }

    /**
     * Create cost matrix with one additional row (representing 'link from' dummy particle) and one more column
     * (representing 'link to' dummy particle).
     * @return cost matrix
     */
    private float[][] initCostMatrix(LinkerOptions aLinkOpts, Vector<Particle> p1, Vector<Particle> p2, int aLinkLevel, final float aMaxCost) {
        int numOfParticles = p1.size();
        int numOfLinkParticles = p2.size();
        float[][] cost = new float[numOfParticles + 1][numOfLinkParticles + 1];
                
        // Fill in the costs
        for (int i = 0; i < numOfParticles; ++i) {
            for (int j = 0; j < numOfLinkParticles; ++j) {
                Particle pA = p1.elementAt(i);
                Particle pB = p2.elementAt(j);
                
                float distanceSq = (pA.iX - pB.iX) * (pA.iX - pB.iX) + (pA.iY - pB.iY) * (pA.iY - pB.iY) + (pA.iZ - pB.iZ) * (pA.iZ - pB.iZ);
                float momentsDist = (float) Math.cbrt((pA.m0 - pB.m0) * (pA.m0 - pB.m0) + (pA.m2 - pB.m2) * (pA.m2 - pB.m2));
                cost[i][j] = distanceSq * aLinkOpts.lSpace + momentsDist * aLinkOpts.lFeature;

                if (aLinkOpts.force == true && pA.distance >= 0.0) {
                    final float lx = (pB.iX - pA.iX) / aLinkLevel - pA.lx;
                    final float ly = (pB.iY - pA.iY) / aLinkLevel - pA.ly;
                    final float lz = (pB.iZ - pA.iZ) / aLinkLevel - pA.lz;

                    final float dynamicCost = lx * lx + ly * ly + lz * lz;
                    cost[i][j] += aLinkOpts.lDynamic * dynamicCost;
                }
                else if (aLinkOpts.straightLine == true && pA.distance >= 0.0) {
                    float lx2 = (pB.iX - pA.iX + pA.lxa);
                    float ly2 = (pB.iY - pA.iY + pA.lya);
                    float lz2 = (pB.iZ - pA.iZ + pA.lza);
                    final float l2_m = lx2 * lx2 + ly2 * ly2 + lz2 * lz2;

                    if (l2_m >= aLinkOpts.minSquaredDisplacementForAngleCalculation) {
                        final float l1_m = pA.linkModule();                                
                        final float lx1 = pA.lx / l1_m;
                        final float ly1 = pA.ly / l1_m;
                        final float lz1 = pA.lz / l1_m;
                        
                        lx2 /= l2_m;
                        ly2 /= l2_m;
                        lz2 /= l2_m;

                        final float cosPhi = lx1 * lx2 + ly1 * ly2 + lz1 * lz2;
                        cost[i][j] += (cosPhi - 1) * (cosPhi - 1) * aLinkOpts.maxDisplacement * aLinkOpts.maxDisplacement;
                    }
                }
            }
        }
        // Last column of cost matrix
        for (int i = 0; i < numOfParticles; ++i) {
            cost[i][numOfLinkParticles] = aMaxCost;
        }
        // Last row of cost matrix
        for (int i = 0; i < numOfLinkParticles; ++i) {
            cost[numOfParticles][i] = aMaxCost;
        }
        // right/low corner of cost matrix - make dummy particles always linking to each other
        cost[numOfParticles][numOfLinkParticles] = 0.0f;
        
        return cost;
    }

    private void initParticlesLinkData(final int aLinkRange, Vector<Particle> aParticles) {
        int numOfParticles = aParticles.size();
        for (int i = 0; i < numOfParticles; ++i) {
            Particle p = aParticles.elementAt(i);
            p.special = false;
            p.next = new int[aLinkRange];
            for (int n = 0; n < aLinkRange; ++n) {
                p.next[n] = -1;
            }
        }
    }

    private int[][] initHelperVectors(int aNumOfParticles, int aNumOfLinkParticles, boolean[][] g) {
        int[] gX = new int[aNumOfLinkParticles + 1];
        int[] gY = new int[aNumOfParticles + 1];
        
        for (int i = 0; i < aNumOfParticles + 1; ++i) {
            for (int j = 0; j < aNumOfLinkParticles + 1; ++j) {
                if (g[i][j]) {
                    gX[j] = i;
                    gY[i] = j;
                }
            }
        }
        // Link dummy to dummy
        gX[aNumOfLinkParticles] = aNumOfParticles;
        gY[aNumOfParticles] = aNumOfLinkParticles;
        
        return new int[][] {gX, gY};
    }

    private boolean[][] initRelationMatrix(int aNumOfParticles, int aNumOfLinkParticles, final float aMaxCost, float[][] aCostMatrix) {
        boolean[][] g = new boolean[aNumOfParticles + 1][aNumOfLinkParticles + 1];    
        boolean[] isColumnAssigned = new boolean[aNumOfLinkParticles + 1];
        
        for (int i = 0; i < aNumOfParticles; ++i) { // Loop over the y-axis without the dummy
            IJ.showProgress(i, aNumOfParticles);
            double min = aMaxCost;
            int prev = -1;
            for (int j = 0; j < aNumOfLinkParticles; ++j) { // Loop over the x-axis without the dummy
                if (!isColumnAssigned[j] && aCostMatrix[i][j] < min) {
                    min = aCostMatrix[i][j];
                    if (prev >= 0) {
                        isColumnAssigned[prev] = false;
                        g[i][prev] = false;
                    }
                    isColumnAssigned[j] = true;
                    g[i][j] = true;
                    prev = j;
                }
            }

            // Link it to dummy particle if nothing better found.
            if (min == aMaxCost) {
                g[i][aNumOfLinkParticles] = true;
                isColumnAssigned[aNumOfLinkParticles] = true;
            }
        }

        // Look for columns that are zero and link them to 'from' dummy particle
        for (int j = 0; j < aNumOfLinkParticles; ++j) {
            boolean isLinked = false;
            for (int i = 0; i < aNumOfParticles + 1; ++i) {
                if (g[i][j]) {
                    isLinked = true;
                    break;
                }
            }
            if (!isLinked) {
                // Link dummy particle to it
                g[aNumOfParticles][j] = true;
            }
        }
        g[aNumOfParticles][aNumOfLinkParticles] = true; // link dummy to dummy
        
        return g;
    }

    private void logInfo(String aLogStr) {
        IJ.showStatus(aLogStr);
        logger.info(aLogStr);
    }
}
