package mosaic.core.imageUtils.iterators;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import mosaic.core.imageUtils.Point;


public class MappingIteratorTest {

    @Test
    public void testIterator() {
        { 
            int[] indexArray = {0, 1, 10, 11, 20, 21};
            boolean[] dimChangeArray = {false, false, true, false, true, false};
            int[] dimensions = new int []{10, 10};
            int[] offset = new int [] {2, 1};
            MappingIterator ri = new MappingIterator(dimensions, new int [] {2, 3}, offset);
            SpaceIterator ii = new SpaceIterator(dimensions);
            
            int i = 0;
            while(ri.hasNext()) {
                int idx = ri.next();
                Point p = ri.getPoint();
                boolean dimChange = ri.hasDimensionWrapped();
                assertEquals(indexArray[i], idx);
                assertEquals(ii.indexToPoint(idx).add(new Point(offset)), p);
                assertEquals(dimChangeArray[i], dimChange);
                
                ++i;
            }
        }
    }
}
