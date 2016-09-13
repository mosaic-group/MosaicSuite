package mosaic.region_competition.topology;


import java.util.ArrayDeque;
import java.util.Queue;

import mosaic.core.imageUtils.Connectivity;
import mosaic.core.imageUtils.Point;

/**
 * Class responsible for calculating number of connected components in provided unit cube image.
 */
class UnitCubeConnectedComponenetsCounter {
    
    private final Connectivity iConnectivity;
    private final Connectivity iNeighborhoodConnectivity;
    
    private final boolean[] iIndex2NeighbourMap;
    private final boolean[][] iUnitCubeNeighborsMap;

    // Queue allocated in constructor for performance reasons.
    private final Queue<Integer> iQueue;
    
    // Input (unit cube) image for processing.
    private char[] iImage;

    /**
     * @param aConnectivity Connectivity for searching connected components
     */
    public UnitCubeConnectedComponenetsCounter(Connectivity aConnectivity) {
        iConnectivity = aConnectivity;
        iNeighborhoodConnectivity = aConnectivity.getIncreasedConnectivity();

        iIndex2NeighbourMap = createIndex2NeighbourMap();
        iUnitCubeNeighborsMap = crateUnitCubeNeighborsMap();
        iQueue = new ArrayDeque<Integer>(iConnectivity.getNeighborhoodSize());
    }

    /**
     * Set the sub image (data of unit cube).
     * @param aImage of unit cube as linear array of pixels
     */
    public UnitCubeConnectedComponenetsCounter SetImage(char[] aImage) {
        iImage = aImage.clone();
        return this;
    }

    /**
     * @param aConnectivity
     * @return Boolean array, entry at position <tt>i</tt> indicating if offset i is in neighborhood for <tt>conn</tt>
     */
    private boolean[] createIndex2NeighbourMap() {
        final int neighborhoodSize = iConnectivity.getNeighborhoodSize();
        final boolean[] result = new boolean[neighborhoodSize];

        for (int i = 0; i < neighborhoodSize; ++i) {
            result[i] = iConnectivity.isNeighbor(i);
        }
        
        return result;
    }

    /**
     * Precalculates neighborhood within the unit cube and stores them into boolean array.
     * Access array by the integer offsets for the points to be checked.
     * Array at position idx1, idx2 is true, if idx1, idx2 are unit cube neighbors with respect to their connectivities.
     *
     * @param aConnectivity Connectivity to be checked
     * @param aNeighborhoodConnectivity Neighborhood connectivity. This has to be
     *                                  more lax (reach more neighbors) than connectivity
     * @return
     */
     boolean[][] crateUnitCubeNeighborsMap() {
        final int neighborhoodSize = iConnectivity.getNeighborhoodSize();
        final boolean neighborsInUnitCube[][] = new boolean[neighborhoodSize][neighborhoodSize];
        final int numOfDimensions = iConnectivity.getNumOfDimensions();
    
        for (int neighbor1 = 0; neighbor1 < neighborhoodSize; ++neighbor1) {
            if (iNeighborhoodConnectivity.isNeighbor(neighbor1)) {
                // neighbour1 goes through whole increased neighborhood
                
                for (int neighbor2 = 0; neighbor2 < neighborhoodSize; ++neighbor2) {
                    // around neighbour1/p1 we crate new points and we check if this is inside our initial unit cube
                    final Point p1 = iConnectivity.toPoint(neighbor1);
                    final Point p2 = iConnectivity.toPoint(neighbor2);
                    final Point sum = p1.add(p2);
                    
                    boolean inUnitCube = true;
                    for (int d = 0; d < numOfDimensions && inUnitCube; d++) {
                        if (sum.iCoords[d] < -1 || sum.iCoords[d] > +1) {
                            inUnitCube = false;
                        }
                    }

                    // If yes, and p2 is valid offset in terms our connectivity, and in addition 
                    // final point is valid point in increased neighborhood we have found valid neighbour to neighbour
                    // positions.
                    if (inUnitCube && iConnectivity.isNeighbor(p2) && iNeighborhoodConnectivity.isNeighbor(sum)) {
                        final int sumOffset = iConnectivity.toIndex(sum);
                        neighborsInUnitCube[neighbor1][sumOffset] = true;
                    }
                }
            }
        }
        
        return neighborsInUnitCube;
    }

    /**
     * Calculates number of connected components in unit cube. One connected component are all
     * points which are != 0 and are neighbours in terms of connectivity.
     * @return
     */
    public int getNumberOfConnectedComponents() {
        int numOfConnectedComponents = 0;

        final int neighborhoodSize = iConnectivity.getNeighborhoodSize();
        final boolean processedIndices[] = new boolean[neighborhoodSize];

        iQueue.clear();
        int seed = 0;
        while (seed < neighborhoodSize) {
            // Find seed - point which is 'neighbor' in terms of connectivity and has non 0 value and was not processed yet.
            while (seed < neighborhoodSize && !(iImage[seed] != 0 && iIndex2NeighbourMap[seed] && !processedIndices[seed])) {
                ++seed;
            }
            if (seed >= neighborhoodSize) break;
            
            // Found sth which is a (part of) component. 
            ++numOfConnectedComponents;
            processedIndices[seed] = true;

            // Traverse from seed to all neighborhood points and marked them as processed (ther are all
            // part of one connected component).
            iQueue.add(seed);
            while (!iQueue.isEmpty()) {
                final int current = iQueue.poll();
                for (int neighbor = 0; neighbor < neighborhoodSize; ++neighbor) {
                    if (!processedIndices[neighbor] && iImage[neighbor] != 0 && iUnitCubeNeighborsMap[current][neighbor]) {
                        iQueue.add(neighbor);
                        processedIndices[neighbor] = true;
                    }
                }
            }
        }
        
        return numOfConnectedComponents;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() +  " (Base: " + iConnectivity + ", Increased: " + iNeighborhoodConnectivity + ")";
    }
}
