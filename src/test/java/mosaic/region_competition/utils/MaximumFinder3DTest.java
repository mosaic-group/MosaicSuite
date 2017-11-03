package mosaic.region_competition.utils;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import mosaic.core.imageUtils.Point;
import mosaic.regions.utils.MaximumFinder3D;


public class MaximumFinder3DTest {

    @Test
    public void testWithEdgeExcluded() {
        float[] ip = new float[1000];
        ip[100*5 + 10*5 + 5] = 1;
        ip[100*8 + 10*5 + 2] = 1;
        ip[100*9 + 10*5 + 0] = 1; // will be excluded (on edge)
        MaximumFinder3D mf = new MaximumFinder3D(new int[] {10, 10, 10});
        
        List<Point> maxPoints = mf.getMaximaPointList(ip, 0, true /* exclude edges */);
        assertEquals(2, maxPoints.size());
        assertTrue(maxPoints.contains(new Point(5,5,5)));
        assertTrue(maxPoints.contains(new Point(2,5,8)));
    }

    @Test
    public void testWithEdgeIncluded() {
        float[] ip = new float[1000];
        ip[100*5 + 10*5 + 5] = 1;
        ip[100*8 + 10*5 + 2] = 1;
        ip[100*9 + 10*5 + 0] = 1;
        MaximumFinder3D mf = new MaximumFinder3D(new int[] {10, 10, 10});
        
        List<Point> maxPoints = mf.getMaximaPointList(ip, 0, false /* include edges */);
        assertEquals(3, maxPoints.size());
        assertTrue(maxPoints.contains(new Point(5,5,5)));
        assertTrue(maxPoints.contains(new Point(2,5,8)));
        assertTrue(maxPoints.contains(new Point(0,5,9)));
    }
}
