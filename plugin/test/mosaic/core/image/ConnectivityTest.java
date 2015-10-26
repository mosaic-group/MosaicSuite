package mosaic.core.image;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


public class ConnectivityTest {

    @Test
    public void testIntIterator2D() {
        List<Integer> neighbourOffsets = new ArrayList<Integer>();
        Connectivity c = new Connectivity(2, 1);
        
        // Add all offsets without middle one.
        // 0 1 2
        // 3 4 5
        // 6 7 8
        neighbourOffsets.add(1);
        neighbourOffsets.add(3);
        neighbourOffsets.add(5);
        neighbourOffsets.add(7);
        
        // Tested function, we should iterate (and remove) all offsets
        for (Integer i : c.itOfsInt()) {
            neighbourOffsets.remove(i);
        }
        
        assertTrue("Container should be empty", neighbourOffsets.isEmpty());
    }
    
    @Test
    public void testIntIterator3D() {
        List<Integer> neighbourOffsets = new ArrayList<Integer>();
        Connectivity c = new Connectivity(3, 0);
        
        // Add all offsets and exclude middle one, for 3D we have 3^3 points indexed from 0-26
        for (int i = 0; i < 3*3*3; ++i) {
            if (i != 13) neighbourOffsets.add(i);
        }
        
        // Tested function, we should iterate (and remove) all offsets
        for (Integer i : c.itOfsInt()) {
            neighbourOffsets.remove(i);
        }
        
        assertTrue("Container should be empty", neighbourOffsets.isEmpty());
    }
}
