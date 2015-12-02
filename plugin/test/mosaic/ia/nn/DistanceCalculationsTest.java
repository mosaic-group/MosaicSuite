package mosaic.ia.nn;

import static org.junit.Assert.assertArrayEquals;

import javax.vecmath.Point3d;

import org.junit.Test;

public class DistanceCalculationsTest {

    @Test
    public void test() {
        Point3d[] x = {new Point3d(0,0,0), new Point3d(4, 1, 0)};
        Point3d[] y = {new Point3d(1,1,0), new Point3d(1,2,0), new Point3d(2,1,0)};
        
        DistanceCalculations distnace = new DistanceCalculationsCoords(x, y, null, 0, 0, 0, 4, 3, 0, 1, 0.1, 6);
        double[][] getqOfD = distnace.getProbabilityDistribution();
        double[] expectedDistrDistance = new double [] {0.0, 0.47140452079103173, 0.9428090415820635, 1.4142135623730951, 1.885618083164127, 2.3570226039551585};
        double[] expectedDistrProbability = new double [] {0.0015607242998035755, 0.0017377573240700906, 0.0018430677023493958, 0.0018620686526394756, 0.001792101074028884, 0.0016430426951461739};
        double[] expectedDistances = new double [] {1.4142135623730951, 2.0};
        assertArrayEquals("Distance", expectedDistances, distnace.getDistancesOfX(), 0.001);
        assertArrayEquals("Distance", expectedDistrDistance, getqOfD[0], 0.001);
        assertArrayEquals("Distance", expectedDistrProbability, getqOfD[1], 0.001);
    }

}
