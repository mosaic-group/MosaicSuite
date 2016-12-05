package mosaic.core.particleLinking;


import java.util.Vector;

import org.apache.log4j.Logger;

import ij.IJ;
import mosaic.core.detection.Particle;


public class ParticleLinkerGreedy extends ParticleLinker {
    private static final Logger logger = Logger.getLogger(ParticleLinkerGreedy.class);

    @Override
    protected void link(Vector<Particle> p1, Vector<Particle> p2, LinkerOptions aLinkOpts, int currFrame, final int NumOfFrames, int numOfParticles, int numOfLinkParticles, int currLinkLevel, final float maxCost) {
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

    private void linkParticles(LinkerOptions aLinkOpts, Vector<Particle> p1, Vector<Particle> p2, int aLinkLevel, int[] gY, int aNumOfParticles, int aNumOfLinkParticles) {
        for (int i = 0; i < aNumOfParticles; ++i) {
            int j = gY[i];
            if (j != aNumOfLinkParticles) { // if not linked to dummy particle
                Particle pA = p1.elementAt(i);
                Particle pB = p2.elementAt(j);

                pA.next[aLinkLevel - 1] = j; // levels are in range 1..LinkRange
                handleCostFeatures(pA, pB, aLinkOpts, aLinkLevel);
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
                cost[i][j] = linkCost(pA, pB, aLinkOpts, aLinkLevel);
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
