package mosaic.ia;

import static org.junit.Assert.assertArrayEquals;

import org.scijava.vecmath.Point3d;

import org.junit.Test;

import mosaic.ia.DistanceCalculations;
import mosaic.ia.DistanceCalculationsCoords;

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
        
        DistanceCalculations distnace = new DistanceCalculationsCoords(x, y, null, 0, 0, 0, 4, 3, 0, 1, 0.1, 0.2, 6);
        
        double[] expectedDistrDistance = new double [] {0.0, 0.565685424949238, 1.131370849898476, 1.697056274847714, 2.262741699796952, 2.82842712474619};
        double[] expectedDistrProbability = new double [] {0.0015607242998035755, 0.0017377573240700906, 0.0018430677023493958, 0.0018620686526394756, 0.001792101074028884, 0.0016430426951461739};
        double[] expectedDistances = new double [] {1.4142135623730951, 2.0};
        
        assertArrayEquals(expectedDistances, distnace.getNearestNeighborsDistancesXtoY(), 0.001);
        assertArrayEquals(expectedDistrDistance, distnace.getContextQdDistancesGrid(), 0.001);
        assertArrayEquals(expectedDistrProbability, distnace.getContextQdPdf(), 0.001);
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
        DistanceCalculations distnace = new DistanceCalculationsCoords(x, y, mask, 0, 0, 0, 4, 3, 0, 1, 10, 0.2, 6);
        
        double[] expectedDistrDistance = new double [] {0.0, 0.282842712474619, 0.565685424949238, 0.848528137423857, 1.131370849898476, 1.414213562373095};
        double[] expectedDistrProbability = new double [] {0.007746622133147363, 6.935786582542096E-4, 5.286465676707438E-5, 0.007755440676114195, 0.00995053493008019, 0.007830073345685824};
        double[] expectedDistances = new double [] {1.0, Math.sqrt(2.0)};
        
        assertArrayEquals(expectedDistances, distnace.getNearestNeighborsDistancesXtoY(), 0.001);
        assertArrayEquals(expectedDistrDistance, distnace.getContextQdDistancesGrid(), 0.001);
        assertArrayEquals(expectedDistrProbability, distnace.getContextQdPdf(), 0.001);
    }
}
