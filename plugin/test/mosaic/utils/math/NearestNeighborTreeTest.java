package mosaic.utils.math;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import javax.vecmath.Point3d;

import org.junit.Test;

import mosaic.utils.math.NearestNeighborTree;


public class NearestNeighborTreeTest {

    @Test
    public void testGetDistnacesToNearestNeighbors() {
        // Base Points form triangle (0, 0) (2, 1) (4, 0)
        Point3d[] testBasePoints = new Point3d[] {new Point3d(0, 0, 0), new Point3d(2, 1, 0), new Point3d(4, 0, 0)};
        
        // Input points move on bottom line of triangle
        Point3d[] inputPoints = new Point3d[] {new Point3d(0, 0, 0), new Point3d(0.5, 0, 0), new Point3d(1, 0, 0), new Point3d(1.5, 0, 0), new Point3d(2, 0, 0)};
        double[] expectedDistances = new double[] {0, 0.5, 1, Math.sqrt(1*1 + 0.5 * 0.5), 1};

        NearestNeighborTree nnt = new NearestNeighborTree(testBasePoints);
        double[] result = nnt.getDistancesToNearestNeighbors(inputPoints);
        assertArrayEquals(expectedDistances, result, 0.001);
    }
    
    @Test
    public void testGetDistnaceToNearestNeighbor() {
        // Base Points form triangle (0, 0) (2, 1) (4, 0)
        Point3d[] testBasePoints = new Point3d[] {new Point3d(0, 0, 0), new Point3d(2, 1, 0), new Point3d(4, 0, 0)};
        
        NearestNeighborTree nnt = new NearestNeighborTree(testBasePoints);
        assertEquals(1, nnt.getDistanceToNearestNeighbor(new Point3d(2, 2, 0)), 0.001);
        assertEquals(1, nnt.getDistanceToNearestNeighbor(new Point3d(2, 1, -1)), 0.001);
    }
}
