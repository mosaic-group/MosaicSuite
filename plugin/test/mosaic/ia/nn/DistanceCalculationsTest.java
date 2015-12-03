package mosaic.ia.nn;

import static org.junit.Assert.assertArrayEquals;

import javax.vecmath.Point3d;

import org.junit.Test;

import mosaic.ia.utils.DistanceCalculations;
import mosaic.ia.utils.DistanceCalculationsCoords;

public class DistanceCalculationsTest {

    @Test
    public void testDistanceCalcNoMask() {
        // X points location:
        //
        // x....
        // ....x
        // .....
        // .....
        Point3d[] x = {new Point3d(0,0,0), new Point3d(4, 1, 0)};
        // Y points location:
        // 
        // .....
        // .yy..
        // .y...
        // .....
        Point3d[] y = {new Point3d(1,1,0), new Point3d(1,2,0), new Point3d(2,1,0)};
        
        DistanceCalculations distnace = new DistanceCalculationsCoords(x, y, null, 0, 0, 0, 4, 3, 0, 1, 0.1, 6);
        
        double[] expectedDistrDistance = new double [] {0.0, 0.47140452079103173, 0.9428090415820635, 1.4142135623730951, 1.885618083164127, 2.3570226039551585};
        double[] expectedDistrProbability = new double [] {0.0015607242998035755, 0.0017377573240700906, 0.0018430677023493958, 0.0018620686526394756, 0.001792101074028884, 0.0016430426951461739};
        double[] expectedDistances = new double [] {1.4142135623730951, 2.0};
        
        assertArrayEquals(expectedDistances, distnace.getDistancesOfX(), 0.001);
        assertArrayEquals(expectedDistrDistance, distnace.getDistancesDistribution(), 0.001);
        assertArrayEquals(expectedDistrProbability, distnace.getProbabilityDistribution(), 0.001);
    }
    
    @Test
    public void testDistanceCalcWithMask() {
        // X points location:
        //
        // .....
        // x...x
        // ...x.
        // .....
        Point3d[] x = {new Point3d(0, 1, 0), new Point3d(4, 1, 0), new Point3d(3, 2, 0)};
        // Y points location:
        // 
        // .....
        // .yy..
        // .y...
        // .....
        Point3d[] y = {new Point3d(1,1,0), new Point3d(1,2,0), new Point3d(2,1,0)};
        // Mask:
        //
        // mmmm.
        // mmmm.
        // mmmm.
        // .....
        //
        // mask in format [z][x][y]
        float[][][] mask = new float[][][] {{{1, 1, 1, 0},
                                             {1, 1, 1, 0},
                                             {1, 1, 1, 0},
                                             {1, 1, 1, 0},
                                             {0, 0, 0, 0}}};
        DistanceCalculations distnace = new DistanceCalculationsCoords(x, y, mask, 0, 0, 0, 4, 3, 0, 1, 10, 6);
        
        double[] expectedDistrDistance = new double [] {0.0, 0.23570226039551587, 0.47140452079103173, 0.7071067811865476, 0.9428090415820635, 1.1785113019775793};
        double[] expectedDistrProbability = new double [] {0.007746622133147363, 0.0014498461816291142, 1.2891544052106777E-5, 0.0011649896134780927, 0.014048348079784988, 0.0074635225313599285};
        double[] expectedDistances = new double [] {1.0, Math.sqrt(2.0)};
        
        assertArrayEquals(expectedDistances, distnace.getDistancesOfX(), 0.001);
        assertArrayEquals(expectedDistrDistance, distnace.getDistancesDistribution(), 0.001);
        assertArrayEquals(expectedDistrProbability, distnace.getProbabilityDistribution(), 0.001);
    }
}
