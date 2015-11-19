package mosaic.core.imageUtils;

import static org.junit.Assert.*;

import org.junit.Test;


public class PointTest {

    @Test
    public void testConstructor() {
        {   // Construct via passed variable num of arguments
            Point p = new Point(1, 3, 5);
            assertArrayEquals(new int[] {1, 3, 5}, p.iCoords);
            assertEquals(3, p.getNumOfDimensions());
        }
        {   // Construct via array (point should use this array reference)
            int[] coords = {3, 8};
            Point p = new Point(coords);
            assertEquals(2, p.getNumOfDimensions());
            assertArrayEquals(new int[] {3, 8}, p.iCoords);
            p.iCoords[0] = 7;
            assertArrayEquals(new int[] {7, 8}, p.iCoords);
            assertArrayEquals(new int[] {7, 8}, coords);
        }
        {   // Create point from other point (coordinates should be copied)
            Point p1 = new Point(1, 3, 5);
            Point p2 = new Point(p1);
            p1.iCoords[0] = 4;
            assertArrayEquals(new int[] {4, 3, 5}, p1.iCoords);
            assertArrayEquals(new int[] {1, 3, 5}, p2.iCoords);
        }
        {   // Create point from other point (coordinates should be copied)
            Point p1 = new Point(1, 3, 5);
            Point p2 = new Point(p1);
            p2.iCoords[0] = 4;
            assertArrayEquals(new int[] {1, 3, 5}, p1.iCoords);
            assertArrayEquals(new int[] {4, 3, 5}, p2.iCoords);
        }
    }

    @Test
    public void testIsIndise() {
        {  
            Point p = new Point(3, 8);
            assertTrue(p.isInside(4, 9));
            assertFalse(p.isInside(3, 8));
        }
        {  
            Point p = new Point(-3, -8);
            assertTrue(p.isInside(0, 0));
        }
    }
    
    @Test 
    public void testDistance() {
        Point p1 = new Point(1, 1);
        Point p2 = new Point(5, 4);
        assertEquals(5, p1.distance(p2), 0.0001);
        assertEquals(5, p2.distance(p1), 0.0001);
    }
    
    @Test 
    public void testAdd() {
        {   // Add point to point
            int[] coord1 = {1, 1};
            int[] coord2 = {4, 3};
            Point p1 = new Point(coord1.clone());
            Point p2 = new Point(coord2.clone());
            Point p3 = p1.add(p2);

            // Check if properly added
            assertArrayEquals(new int[] {5, 4}, p3.iCoords);

            // These should not be changed
            assertArrayEquals(coord1, p1.iCoords);
            assertArrayEquals(coord2, p2.iCoords);
        }
        {   // Add scalar to each element
            int[] coord1 = {1, 2};
            Point p1 = new Point(coord1.clone());
            Point p3 = p1.add(3);

            // Check if properly added
            assertArrayEquals(new int[] {4, 5}, p3.iCoords);

            // These should not be changed
            assertArrayEquals(coord1, p1.iCoords);
        }
    }
    
    @Test 
    public void testSub() {
        {   // Sub point to point
            int[] coord1 = {1, 1};
            int[] coord2 = {4, 3};
            Point p1 = new Point(coord1.clone());
            Point p2 = new Point(coord2.clone());
            Point p3 = p1.sub(p2);

            // Check if properly added
            assertArrayEquals(new int[] {-3, -2}, p3.iCoords);

            // These should not be changed
            assertArrayEquals(coord1, p1.iCoords);
            assertArrayEquals(coord2, p2.iCoords);
        }
        {   // Sub scalar from each element
            int[] coord1 = {1, 2};
            Point p1 = new Point(coord1.clone());
            Point p3 = p1.sub(3);

            // Check if properly subtract
            assertArrayEquals(new int[] {-2, -1}, p3.iCoords);

            // These should not be changed
            assertArrayEquals(coord1, p1.iCoords);
        }
    }
    
    @Test 
    public void testMult() {
        int[] coord1 = {2, 1};
        Point p1 = new Point(coord1.clone());
        Point p2 = p1.mult(3);

        // Check if properly added
        assertArrayEquals(new int[] {6, 3}, p2.iCoords);

        // These should not be changed
        assertArrayEquals(coord1, p1.iCoords);
    }
    
    @Test 
    public void testDiv() {
        {
            int[] coord1 = {10, 8};
            Point p1 = new Point(coord1.clone());
            Point p2 = p1.div(2);

            // Check if properly added
            assertArrayEquals(new int[] {5, 4}, p2.iCoords);

            // These should not be changed
            assertArrayEquals(coord1, p1.iCoords);
        }
        {
            int[] coord1 = {10, 8};
            float[] scaling = {2.5f, 4};
            Point p1 = new Point(coord1.clone());
            Point p2 = p1.div(scaling);

            // Check if properly added
            assertArrayEquals(new int[] {4, 2}, p2.iCoords);

            // These should not be changed
            assertArrayEquals(coord1, p1.iCoords);
        }
    }
    
    @Test 
    public void testZeroOneNumOfZeros() {
        int[] coord1 = {3, 0, 2, 0, 1, 0, 0};
        Point p1 = new Point(coord1.clone());

        assertEquals(4, p1.numOfZerosInCoordinates());
        p1.zero();
        assertEquals(7, p1.numOfZerosInCoordinates());
    }
    
    @Test 
    public void testEquals() {
        Point p1 = new Point(1, 2, 0, 4, 7);
        Point p2 = new Point(1, 2, 0, 4, 7);
        assertEquals(p1, p2);
    }
    
    @Test 
    public void testToString() {
        Point p1 = new Point(1, 2, 0, 4, 7);
        assertEquals("[1, 2, 0, 4, 7]", p1.toString());
    }
}
