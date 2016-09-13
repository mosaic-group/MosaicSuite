package mosaic.core.imageUtils;


import java.util.Iterator;

import mosaic.utils.math.MathOps;


/**
 * Connectivity class
 * @author Stephan Semmler
 * 
 * It provides "offset Points" or "index" of neighbors connectivity information.
 *  
 * For example 2D 4-connectivity would be:
 * Indexes:  Point offsets:
 * 0 1 2     -1,-1   0,-1   1,-1
 * 3 4 5     -1, 0   0, 0   1, 0
 * 6 7 8     -1, 1   0, 1   1, 1
 * 
 * Neighbor indexes: 1 3 5 7
 * Neighbor point offsets: 0,-1  -1,0  1,0  0,1
 */
public class Connectivity {
    // Input parameters
    private final int iNumOfDimensions;
    private final int iConnectivity;
    
    // Internal data
    private final int iNeighborhoodSize;
    protected final int iNumOfNeighbors;
    protected final Point[] iPointOffsetsNeighbors;
    protected final int[] iIndexedNeighbors;

    /**
     * @param aNumOfDimensions 2 for 2D, 3 for 3D, ...
     * @param aConnectivity type of connectivity in range [0, aNumOfDimensions - 1].
     *                      For aNumDimensions-1 it has minimum available connectivity, when number is decreased
     *                      connectivity is "one dimension more lax" and with 0 it reaches maximum possible connectivity.
     *                      Example: for 2D - 1 is a 4-way; 0 is a 8-way connectivity
     *                               for 3D - 2 is a 6-way; 1 is a 18-way; 3  is a 26-way connectivity
     */
    public Connectivity(final int aNumOfDimensions, final int aConnectivity) {
        iNumOfDimensions = aNumOfDimensions;
        iConnectivity = aConnectivity;

        iNeighborhoodSize = (int) Math.pow(3, aNumOfDimensions);
        iNumOfNeighbors = computeNumberOfNeighbors();

        iPointOffsetsNeighbors = new Point[iNumOfNeighbors];
        iIndexedNeighbors = new int[iNumOfNeighbors];

        initOffsets();
    }

    /**
     * @return Corresponding foreground/background connectivity
     *         such they are compatible (Jordan's theorem)
     */
    public Connectivity getComplementaryConnectivity() {
        if (iConnectivity == iNumOfDimensions - 1) {
            return new Connectivity(iNumOfDimensions, 0);
        }
        return new Connectivity(iNumOfDimensions, iNumOfDimensions - 1);
    }

    /**
     * This is NOT the corresponding BG connectivity.
     * It returns a connectivity that's "one dimension more lax",
     * that is, reaches minimal more neighbors than THIS connectivity
     * 
     * @return A new created NeighborhoodConnectivity
     */
    public Connectivity getIncreasedConnectivity() {
        final int newConnectivity = (iConnectivity == 0) ? 0 : iConnectivity - 1;
        return new Connectivity(iNumOfDimensions, newConnectivity);
    }

    /**
     * @param aOffset - Point representing offset to midpoint
     * @return true if aOffset is in neighborhood
     */
    public boolean isNeighbor(Point aOffset) {
        for (final Point p : iPointOffsetsNeighbors) {
            if (aOffset.equals(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param aIndex - index of point in unit cube
     * @return true if aIndex is in neighborhood
     */
    public boolean isNeighbor(int aIndex) {
        for (final int idx : iIndexedNeighbors) {
            if (aIndex == idx) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts an index to a Point offset
     * @param aIndex integer value in [0, iNeighborhoodSize - 1]
     * @return Point representation of the offset (e.g [-1,-1] for the upper left corner in 2D)
     */
    public Point toPoint(int aIndex) {
        int remainder = aIndex;
        final int[] x = new int[iNumOfDimensions];
    
        for (int i = 0; i < iNumOfDimensions; ++i) {
            // x for this dimension
            x[i] = remainder % 3;
            // x would have range [0, 1, 2] but we want offsets from midpoint [-1,0,1]
            x[i]--;
            // get rid of this dimension
            remainder /= 3;
        }
        
        return new Point(x);
    }

    /**
     * Converts an a Point offset to an index in unit cube
     * 
     * @param iParams Point offset
     * @return Integer offset
     */
    public int toIndex(Point aOffset) {
        int offset = 0;
        int factor = 1;
        for (int i = 0; i < iNumOfDimensions; ++i) {
            // +1 since we want to move offsets from [-1, 0, 1] to [0, 1, 2]
            offset += factor * (aOffset.iCoords[i] + 1);
            factor *= 3;
        }
        return offset;
    }

    /**
     * @return Number of elements in the unit cube (including middle point)
     */
    public int getNeighborhoodSize() {
        return iNeighborhoodSize;
    }

    /**
     * @return The number of actual neighbors of the connectivity
     */
    public int getNumOfNeighbors() {
        return iNumOfNeighbors;
    }

    /**
     * @return Dimension of the connectivity
     */
    public int getNumOfDimensions() {
        return iNumOfDimensions;
    }

    @Override
    public String toString() {
        return "Connectivity (" + iNumOfDimensions + "D, " + iNumOfNeighbors + "-connectivity)";
    }

    /**
     * @return Number of neighbors (Number of points connected to the midpoint)
     */
    private int computeNumberOfNeighbors() {
        int numberOfNeighbors = 0;
        for (int i = iConnectivity; i <= iNumOfDimensions - 1; ++i) {
            numberOfNeighbors += MathOps.factorial(iNumOfDimensions) * (1 << (iNumOfDimensions - i)) / 
                                (MathOps.factorial(iNumOfDimensions - i) * MathOps.factorial(i));
        }
        return numberOfNeighbors;
    }

    /**
     * Calculate the offsets of neighbors for the connectivity specified in the constructor
     */
    private void initOffsets() {
        int currentNbNeighbors = 0;
    
        for (int i = 0; i < iNeighborhoodSize; ++i) {
            final Point p = toPoint(i);
            final int numberOfZeros = p.numOfZerosInCoordinates();
    
            if (numberOfZeros != iNumOfDimensions && numberOfZeros >= iConnectivity) {
                iPointOffsetsNeighbors[currentNbNeighbors] = p;
                iIndexedNeighbors[currentNbNeighbors] = i;
                ++currentNbNeighbors;
            }
        }
    }

    /**
     * Iterates over the neighbors of the midpoint, represented as Point offsets
     * @return Neighbors as Point offsets
     */
    public Iterable<Point> iterator() {
        return new Iterable<Point>() {

            @Override
            public Iterator<Point> iterator() {
                return new OfsIterator();
            }
        };
    }
    

    /**
     * Iterator class to iterate over the neighborhood,
     * returning <b>Point offsets</b> to the neighbors. <br>
     * Doesn't allow to remove() an element.
     */
    private class OfsIterator implements Iterator<Point> {
        private int cursor = 0;

        protected OfsIterator() {}

        @Override
        public boolean hasNext() {
            return cursor < iNumOfNeighbors;
        }

        @Override
        public Point next() {
            return iPointOffsetsNeighbors[cursor++];
        }

        @Override
        public void remove() {
            // do nothing
        }
    }
    
    /**
     * Iterates over the neighbors of Point p in the context of this connectivity
     */
    public Iterable<Point> iterateNeighbors(final Point p) {
        return new Iterable<Point>() {
            
            @Override
            public Iterator<Point> iterator() {
                return new NeighborIterator(p);
            }
        };
    }

    /**
     * Iterator class to iterate through neighbors of a point
     */
    private class NeighborIterator extends OfsIterator {

        private final Point point;

        /**
         * @param p Arbitrary Point p
         */
        protected NeighborIterator(Point p) {
            this.point = p;
        }

        @Override
        public Point next() {
            final Point ofs = super.next();
            return point.add(ofs);
        }
    }

    /**
     * Iterates through all neighbors of point.
     * @return indices of neighbors
     */
    public Iterable<Integer> itOfsInt() {
        return new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new OfsIteratorInt();
            }
        };
    }

    private class OfsIteratorInt implements Iterator<Integer> {
        protected OfsIteratorInt() {}
        private int cursor = 0;

        @Override
        public boolean hasNext() {
            return (cursor < iNumOfNeighbors);
        }

        @Override
        public Integer next() {
            final int result = iIndexedNeighbors[cursor];
            cursor++;
            return result;
        }

        @Override
        public void remove() {
            // do nothing
        }
    }
}
