package mosaic.core.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


public class ConnectivityTest {
    
    @Test
    public void testGetNumOfNeighbors() {
        assertEquals("2D, 4-conn", 4, new Connectivity(2, 1).getNumOfNeighbors());
        assertEquals("2D, 8-conn", 8, new Connectivity(2, 0).getNumOfNeighbors());
        
        assertEquals("3D, 6-conn", 6, new Connectivity(3, 2).getNumOfNeighbors());
        assertEquals("3D, 18-conn", 18, new Connectivity(3, 1).getNumOfNeighbors());
        assertEquals("3D, 26-conn", 26, new Connectivity(3, 0).getNumOfNeighbors());
    }
    
    @Test
    public void testGetNumOfDimensions() {
        assertEquals("2D", 2, new Connectivity(2, 1).getNumOfDimensions());
        assertEquals("3D", 3, new Connectivity(3, 0).getNumOfDimensions());
    }
    
    @Test
    public void testGetNeighborhoodSize() {
        assertEquals("2D", 9, new Connectivity(2, 1).getNeighborhoodSize());
        assertEquals("3D", 27, new Connectivity(3, 0).getNeighborhoodSize());
    }
    
    @Test
    public void testToIndex() {
        assertEquals("2D", 7, new Connectivity(2, 1).toIndex(new Point(0, 1)));
        assertEquals("3D", 13, new Connectivity(3, 0).toIndex(new Point(0, 0, 0)));
    }
    
    @Test
    public void testToPoint() {
        assertEquals("2D", new Point(1, -1), new Connectivity(2, 1).toPoint(2));
        assertEquals("3D", new Point(0, 0, -1), new Connectivity(3, 0).toPoint(4));
    }
    
    @Test
    public void testIsNeighbor() {
        assertTrue("2D", new Connectivity(2, 1).isNeighbor(1));
        assertFalse("2D", new Connectivity(2, 1).isNeighbor(2));
        assertTrue("2D", new Connectivity(2, 0).isNeighbor(2));
        
        assertTrue("2D", new Connectivity(2, 1).isNeighbor(new Point(0, -1)));
        assertFalse("2D", new Connectivity(2, 1).isNeighbor(new Point(1, -1)));
        assertTrue("2D", new Connectivity(2, 0).isNeighbor(new Point(1, -1)));
    }
    
    @Test
    public void testGetIncreasedConnectivity() {
        assertEquals("2D", 8, new Connectivity(2, 1).getIncreasedConnectivity().getNumOfNeighbors());
        assertEquals("3D", 26, new Connectivity(3, 1).getIncreasedConnectivity().getNumOfNeighbors());
    }
    
    @Test
    public void testGetComplementaryConnectivity() {
        assertEquals("2D", 8, new Connectivity(2, 1).getComplementaryConnectivity().getNumOfNeighbors());
        assertEquals("3D", 6, new Connectivity(3, 1).getComplementaryConnectivity().getNumOfNeighbors());
    }
    
    @Test
    public void testToString() {
        assertEquals("2D", "Connectivity (2D, 4-connectivity)", new Connectivity(2, 1).toString());
    }
    
    @Test
    public void testIndexIterator2D() {
        Connectivity c = new Connectivity(2, 1);
        
        // Add all offsets without middle one.
        // 0 1 2
        // 3 4 5
        // 6 7 8
        List<Integer> neighbourOffsets = new ArrayList<Integer>();
        neighbourOffsets.add(1);
        neighbourOffsets.add(3);
        neighbourOffsets.add(5);
        neighbourOffsets.add(7);
        
        // Tested function, we should iterate (and remove) all offsets
        // Test will fail if index is not existing or if not all indexes were removed
        verifyIfAllElementsAdded(c.itOfsInt(), neighbourOffsets);
    }
    
    @Test
    public void testIndexIterator3D() {
        Connectivity c = new Connectivity(3, 0);
        
        // Add all offsets and exclude middle one, for 3D we have 3^3 points indexed from 0-26
        List<Integer> neighbourOffsets = new ArrayList<Integer>();
        for (int i = 0; i < 3*3*3; ++i) {
            if (i != 13) neighbourOffsets.add(i);
        }
        
        // Tested function, we should iterate (and remove) all offsets.
        // Test will fail if index is not existing or if not all indexes were removed
        verifyIfAllElementsAdded(c.itOfsInt(), neighbourOffsets);
    }
    
    @Test
    public void testPointIterator1D() {
        List<Point> neighbourOffsets = new ArrayList<Point>();
        Connectivity c = new Connectivity(1, 0);
        
        // Add all offsets without middle one (left and right point).
        neighbourOffsets.add(new Point(-1));
        neighbourOffsets.add(new Point(1));
        
        // Tested function, we should iterate (and remove) all offsets
        // Test will fail if index is not existing or if not all indexes were removed
        verifyIfAllElementsAdded(c.iterator(), neighbourOffsets);
    }
    
    @Test
    public void testPointIterator2D() {
        List<Point> neighbourOffsets = new ArrayList<Point>();
        Connectivity c = new Connectivity(2, 1);
        
        // Add all offsets without middle one (left and right point).
        neighbourOffsets.add(new Point(0, -1));
        neighbourOffsets.add(new Point(-1, 0));
        neighbourOffsets.add(new Point(1, 0));
        neighbourOffsets.add(new Point(0, 1));
        
        // Tested function, we should iterate (and remove) all offsets
        // Test will fail if index is not existing or if not all indexes were removed
        verifyIfAllElementsAdded(c.iterator(), neighbourOffsets);
    }
    
    /**
     * Iterates through all elements given by aIterator and removes them from aContainer. 
     * It is expected that all elements are found in container and after removing all elements given
     * by iterator container will be empty.
     * @param aIterator
     * @param aContainer
     */
    private void verifyIfAllElementsAdded(Iterable<?> aIterator, List<?> aContainer) {
        for (Object i : aIterator) {
            assertTrue("Element [" + i + "] not found", aContainer.remove(i));
        }
        assertTrue("Container should be empty", aContainer.isEmpty());
    }
}
