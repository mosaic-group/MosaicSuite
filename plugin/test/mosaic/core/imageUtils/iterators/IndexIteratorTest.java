package mosaic.core.imageUtils.iterators;

import static org.junit.Assert.*;

import org.junit.Test;

import mosaic.core.imageUtils.Point;


public class IndexIteratorTest {

    @Test
    public void testSize() {
        {
            SpaceIterator ii = new SpaceIterator(4, 3);
            assertEquals(12, ii.getSize());
        }
        {
            SpaceIterator ii = new SpaceIterator(3, 4);
            assertEquals(12, ii.getSize());
        }
        {
            SpaceIterator ii = new SpaceIterator(4, 3, 5);
            assertEquals(60, ii.getSize());
        }
    }

    @Test
    public void testPointToIndexToPoint() {
        {
            SpaceIterator ii = new SpaceIterator(4, 3);
            assertEquals(5, ii.pointToIndex(new Point(1,1)));
            assertEquals(new Point(1,1), ii.indexToPoint(5));
        }
        {
            SpaceIterator ii = new SpaceIterator(3, 4);
            assertEquals(4, ii.pointToIndex(new Point(1,1)));
            assertEquals(new Point(1,1), ii.indexToPoint(4));
        }
        {
            SpaceIterator ii = new SpaceIterator(4, 3, 5);
            assertEquals(29, ii.pointToIndex(new Point(1,1,2)));
            assertEquals(new Point(1,1,2), ii.indexToPoint(29));
        }
    }
    
    @Test
    public void testIsInBound() {
        {
            SpaceIterator ii = new SpaceIterator(4, 2);
            assertTrue(ii.isInBound(new Point(0,0)));
            assertTrue(ii.isInBound(new Point(3,1)));
            assertTrue(ii.isInBound(new Point(3,0)));
            assertTrue(ii.isInBound(new Point(0,1)));
            assertFalse(ii.isInBound(new Point(-1,-1)));
            assertFalse(ii.isInBound(new Point(4,2)));
            
            assertTrue(ii.isInBound(0));
            assertTrue(ii.isInBound(7));
            assertTrue(ii.isInBound(3));
            assertFalse(ii.isInBound(-1));
            assertFalse(ii.isInBound(8));
        }
        {
            SpaceIterator ii = new SpaceIterator(4, 3, 5);
            assertTrue(ii.isInBound(new Point(3,0,4)));
            assertTrue(ii.isInBound(new Point(3,2,4)));
            assertTrue(ii.isInBound(new Point(0,0,0)));
            assertFalse(ii.isInBound(new Point(-1,-1,-1)));
            assertFalse(ii.isInBound(new Point(4,3,5)));
            
            assertTrue(ii.isInBound(0));
            assertTrue(ii.isInBound(59));
            assertTrue(ii.isInBound(30));
            assertFalse(ii.isInBound(-1));
            assertFalse(ii.isInBound(60));
        }
    }
    
    @Test
    public void testGetDimensions() {
        SpaceIterator ii = new SpaceIterator(4, 2);
        assertArrayEquals(new int[] {4, 2}, ii.getDimensions());
        assertEquals(2, ii.getNumOfDimensions());
    }
    
    @Test
    public void testGetIndexIterable() {
        // 8 elements
        int[] referenceArray = {1, 1, 1, 1, 1, 1, 1, 1};

        SpaceIterator ii = new SpaceIterator(4, 2);
        
        // Create empty/zeroed array and increase value for each index.
        // Each element should be visited exactly once.
        int[] testArray = new int[ii.getSize()];
        for (int idx : ii.getIndexIterable()) {
            testArray[idx]++;
        }
        
        // Finally arrays should be equal
        assertArrayEquals(referenceArray, testArray);
    }
    
    @Test
    public void testGetPointIterable() {
        // 9 elements
        int[] referenceArray = {1, 1, 1, 1, 1, 1, 1, 1, 1};

        SpaceIterator ii = new SpaceIterator(3, 3);
        
        // Create empty/zeroed array and increase value for each index.
        // Each element should be visited exactly once.
        int[] testArray = new int[ii.getSize()];
        for (Point point : ii.getPointIterable()) {
            testArray[ii.pointToIndex(point)]++;
        }
        
        // Finally arrays should be equal
        assertArrayEquals(referenceArray, testArray);
    }
}
