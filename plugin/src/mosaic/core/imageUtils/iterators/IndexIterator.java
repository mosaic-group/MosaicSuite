package mosaic.core.imageUtils.iterators;


import java.util.Iterator;

import mosaic.core.imageUtils.Point;


/**
 * IndexIterator is a class to iterate on a Hypercube
 * @author Pietro Incardona
 */
public class IndexIterator {

    private final int iDimensions[];
    private final int iNumOfDimensions;
    protected final int iSize;

    /**
     * Create the index iterator
     * @param aDimensions (width/height/depth/...)
     */
    public IndexIterator(int... aDimensions) {
        iDimensions = aDimensions.clone();
        iNumOfDimensions = iDimensions.length;
        
        int tempSize = 1;
        for (int i = 0; i < iNumOfDimensions; ++i) {
            tempSize *= iDimensions[i];
        }
        iSize = tempSize;
    }

    /**
     * Get the total size of the iterator (iterated space)
     * @return total number of pixels = width*height*...
     */
    public int getSize() {
        return iSize;
    }

    /**
     * Converts a Point index into an integer index
     * @param aPoint Point index
     * @return integer index
     */
    public int pointToIndex(Point aPoint) {
        int idx = 0;
        int fac = 1;
        for (int i = 0; i < iNumOfDimensions; ++i) {
            idx += fac * aPoint.iCoords[i];
            fac *= iDimensions[i];
        }

        return idx;
    }

    /**
     * Converts an integer index into a Point index
     * @param aIndex integer index
     * @return Point - a point representation of input index
     */
    public Point indexToPoint(int aIndex) {
        int index = aIndex;
        final int x[] = new int[iNumOfDimensions];

        for (int i = 0; i < iNumOfDimensions; ++i) {
            final int r = index % iDimensions[i];
            x[i] = r;
            index -= r;
            index = index / iDimensions[i];
        }

        return new Point(x);
    }

    /**
     * Check is the point is inside the boundary
     * @param aPoint Point index
     * @return true, if aPoint is within bounds of this Iterator
     */
    public boolean isInBound(Point aPoint) {
        for (int d = 0; d < iNumOfDimensions; ++d) {
            if (aPoint.iCoords[d] < 0 || aPoint.iCoords[d] >= iDimensions[d]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check is the index is inside the boundary
     * @param aIndex index
     * @return true, if aIndex is within bounds of this Iterator
     */
    public boolean isInBound(int aIndex) {
        if (aIndex < 0 || aIndex >= getSize()) return false;
        return true;
    }
    
    /**
     * @return number of dimensions of iterator
     */
    public int getNumOfDimensions() {
        return iNumOfDimensions;
    }
    
    /**
     * @return dimensions array (with sizes of each dimension)
     */
    public int[] getDimensions() {
        return iDimensions;
    }
    
    /**
     * Get an iterator of Point of the defined region
     * @return an iterator of points
     */
    public Iterator<Point> getPointIterator() {
        return new Iterator<Point>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < iSize;
            }

            @Override
            public Point next() {
                return indexToPoint(i++);
            }

            @Override
            public void remove() { /* not needed */ }
        };
    }

    /**
     * Get an iterator of Point of the defined region
     * @return Iterable<Point>
     */
    public Iterable<Point> getPointIterable() {
        return new Iterable<Point>() {

            @Override
            public Iterator<Point> iterator() {
                return getPointIterator();
            }
        };
    }

    /**
     * Get an iterator of Integer of the defined region
     * @return Iterator<Point>
     */
    public Iterator<Integer> getIndexIterator() {
        return new Iterator<Integer>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < iSize;
            }

            @Override
            public Integer next() {
                return (i++);
            }

            @Override
            public void remove() { /* not needed */ }
        };
    }

    /**
     * Get an Iterable of integer for the region
     * @return Iterable<Integer>
     */
    public Iterable<Integer> getIndexIterable() {
        return new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return getIndexIterator();
            }
        };
    }
}
