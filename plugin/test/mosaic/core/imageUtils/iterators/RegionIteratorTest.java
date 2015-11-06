package mosaic.core.imageUtils.iterators;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

import mosaic.core.imageUtils.Point;


public class RegionIteratorTest {

    @Test
    public void testIterator() {
        {   // Test region going out of boundary on the bottom/right
            
            // 4 x 3 elements
            int[] referenceArray = {0, 0, 0, 0, 
                                    0, 0, 1, 1, 
                                    0, 0, 1, 1};
            
            RegionIterator ri = new RegionIterator(new int []{4, 3}, new int [] {5, 8}, new int [] {2, 1});
            
            // Create empty/zeroed array and increase value for each index.
            // Each element should be visited exactly once.
            int[] testArray = new int[referenceArray.length];
            while(ri.hasNext()) {
                testArray[ri.next()]++;
            }
            // Finally arrays should be equal
            assertArrayEquals(referenceArray, testArray);
        }
        {   // Test region going out of boundary on the left/top
            
            // 4 x 3 elements
            int[] referenceArray = {1, 1, 0, 0, 
                                    0, 0, 0, 0, 
                                    0, 0, 0, 0};
            
            RegionIterator ri = new RegionIterator(new int []{4, 3}, new int [] {4, 3}, new int [] {-2, -2});
            
            // Create empty/zeroed array and increase value for each index.
            // Each element should be visited exactly once.
            int[] testArray = new int[referenceArray.length];
            while(ri.hasNext()) {
                testArray[ri.next()]++;
            }
            // Finally arrays should be equal
            assertArrayEquals(referenceArray, testArray);
        }
        {   // Test region smaller than input region
            
            // 4 x 3 elements
            int[] referenceArray = {0, 0, 0, 0, 
                                    0, 1, 1, 0, 
                                    0, 0, 0, 0};
            
            RegionIterator ri = new RegionIterator(new int []{4, 3}, new int [] {2, 1}, new int [] {1, 1});
            
            // Create empty/zeroed array and increase value for each index.
            // Each element should be visited exactly once.
            int[] testArray = new int[referenceArray.length];
            while(ri.hasNext()) {
                testArray[ri.next()]++;
            }
            // Finally arrays should be equal
            assertArrayEquals(referenceArray, testArray);
        }
        {   // Test region fill the whole input region
            
            // 4 x 3 elements
            int[] referenceArray = {1, 1, 1, 1, 
                                    1, 1, 1, 1, 
                                    1, 1, 1, 1};
            
            RegionIterator ri = new RegionIterator(new int []{4, 3});

            // Create empty/zeroed array and increase value for each index.
            // Each element should be visited exactly once.
            int[] testArray = new int[referenceArray.length];
            while(ri.hasNext()) {
                testArray[ri.next()]++;
            }
            // Finally arrays should be equal
            assertArrayEquals(referenceArray, testArray);
        }
        {   // Test region bigger than input (having input "inside")
            
            // 4 x 3 elements
            int[] referenceArray = {1, 1, 1, 1, 
                                    1, 1, 1, 1, 
                                    1, 1, 1, 1};
            
            RegionIterator ri = new RegionIterator(new int []{4, 3}, new int [] {100, 100}, new int[] {-10, -20});
            
            // Create empty/zeroed array and increase value for each index.
            // Each element should be visited exactly once.
            int[] testArray = new int[referenceArray.length];
            while(ri.hasNext()) {
                testArray[ri.next()]++;
            }
            // Finally arrays should be equal
            assertArrayEquals(referenceArray, testArray);
        }
    }
    
    @Test
    public void testGetPoint() {
        int[] dimensions = {6, 4};
        RegionIterator ri = new RegionIterator(dimensions, new int [] {15, 8}, new int [] {2, 1});
        IndexIterator ii = new IndexIterator(dimensions);
        
        while(ri.hasNext()) {
            int index = ri.next();
            Point p = ri.getPoint();
            assertEquals(ii.indexToPoint(index), p);
        }
    }
    
    @Test
    public void testGetSize() {
        {
            RegionIterator ri = new RegionIterator(new int []{4, 3}, new int [] {4, 3}, new int [] {-2, -2});
            assertEquals(2, ri.getSize());
        }
        {
            RegionIterator ri = new RegionIterator(new int []{4, 3}, new int [] {10, 32}, new int [] {-2, -2});
            assertEquals(12, ri.getSize());
        }
    }
    
    @Test
    public void testIteratorR() {
        { 
            int[] indexArray = {0, 1, 10, 11, 20, 21};
            boolean[] dimChangeArray = {false, false, true, false, true, false};
            int[] dimensions = new int []{10, 10};
            int[] offset = new int [] {2, 1};
            RegionIterator ri = new RegionIterator(dimensions, new int [] {2, 3}, offset);
            IndexIterator ii = new IndexIterator(dimensions);
            
            int i = 0;
            while(ri.hasNext()) {
                int idx = ri.nextRmask();
                Point p = ri.getPoint();
                boolean dimChange = ri.getRMask();
                assertEquals(indexArray[i], idx);
                assertEquals(ii.indexToPoint(idx).add(new Point(offset)), p);
                assertEquals(dimChangeArray[i], dimChange);
                
                ++i;
            }
        }
    }
}
