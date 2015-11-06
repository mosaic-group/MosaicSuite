package mosaic.core.imageUtils.iterators;

import static org.junit.Assert.*;

import org.junit.Test;

import mosaic.core.imageUtils.Point;


public class MaskIteratorTest {

    @Test
    public void testIterator() {
        {   // Test region going out of boundary on the bottom/right
            
            // 9, 4 elements representing region space
            int[] referenceArray = {1, 1, 0, 0, 0, 0, 0, 0, 0, 
                                    1, 1, 0, 0, 0, 0, 0, 0, 0, 
                                    0, 0, 0, 0, 0, 0, 0, 0, 0,
                                    0, 0, 0, 0, 0, 0, 0, 0, 0};
            
            MaskIterator mi = new MaskIterator(new int []{4, 3}, new int [] {9, 4}, new int [] {2, 1});
            
            // Create empty/zeroed array and increase value for each index.
            // Each element should be visited exactly once.
            int[] testArray = new int[referenceArray.length];
            while(mi.hasNext()) {
                testArray[mi.next()]++;
            }
            
            // Finally arrays should be equal
            assertArrayEquals(referenceArray, testArray);
        }
    }
    
    @Test
    public void testGetPoint() {
        int[] dimensions = {9, 4};
        MaskIterator mi = new MaskIterator(new int []{4, 3}, dimensions, new int [] {2, 1});
        IndexIterator ii = new IndexIterator(dimensions);
        
        while(mi.hasNext()) {
            int index = mi.next();
            Point p = mi.getPoint();
            assertEquals(ii.indexToPoint(index), p);
        }
    }
}
