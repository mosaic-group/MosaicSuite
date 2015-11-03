package mosaic.region_competition.topology;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import mosaic.core.image.Connectivity;


public class UnitCubeCCCounterTest {

    @Test
    public void testCrateUnitCubeNeighborsMap() {
        boolean[][] expected = new boolean[][] {
                    {false, true, false, true, false, false, false, false, false},
                    {true, false, true, false, false, false, false, false, false},
                    {false, true, false, false, false, true, false, false, false},
                    {true, false, false, false, false, false, true, false, false},
                    {false, false, false, false, false, false, false, false, false},
                    {false, false, true, false, false, false, false, false, true},
                    {false, false, false, true, false, false, false, true, false},
                    {false, false, false, false, false, false, true, false, true},
                    {false, false, false, false, false, true, false, true, false},
        };
        
        Connectivity conn = new Connectivity(2, 1); // 2D 4-connected
        UnitCubeConnectedComponenetsCounter counter = new UnitCubeConnectedComponenetsCounter(conn);
       
        // Tested method
        boolean[][] initUnitCubeNeighbors = counter.crateUnitCubeNeighborsMap();
        
        // Check if we have found proper neighbour mappings
        assertEquals(Arrays.deepToString(expected), Arrays.deepToString(initUnitCubeNeighbors));

        // Additional check - map should be symmetric (just in case expected table is wrong ;-) ).
        int size = initUnitCubeNeighbors[0].length;
        for (int i = 0; i < size; ++i) {
            for (int j = 0; j < i; ++j) {
                assertEquals("["+i+"]"+"["+j+"]", initUnitCubeNeighbors[i][j], initUnitCubeNeighbors[j][i]);
            }
        }
    }
    
    @Test
    public void testToString() {
        Connectivity conn = new Connectivity(2, 1); // 2D 4-connected
        UnitCubeConnectedComponenetsCounter counter = new UnitCubeConnectedComponenetsCounter(conn);
        assertEquals("UnitCubeConnectedComponenetsCounter (Base: Connectivity (2D, 4-connectivity), Increased: Connectivity (2D, 8-connectivity))", counter.toString());
    }
    
    @Test
    public void testConnectedComponents() {
        {   // 1 component
            char[]   inputImg = {0, 1, 1,
                                 0, 0, 1,
                                 1, 1, 1};
            Connectivity conn = new Connectivity(2, 1); // 2D 4-connected
            UnitCubeConnectedComponenetsCounter counter = new UnitCubeConnectedComponenetsCounter(conn);
            
            int result = counter.SetImage(inputImg).getNumberOfConnectedComponents();
            
            assertEquals(1, result);
        }
        {   // 2 components
            char[]   inputImg = {1, 0, 1,
                                 1, 1, 1,
                                 0, 1, 1};
            Connectivity conn = new Connectivity(2, 1); // 2D 4-connected
            UnitCubeConnectedComponenetsCounter counter = new UnitCubeConnectedComponenetsCounter(conn);
            
            int result = counter.SetImage(inputImg).getNumberOfConnectedComponents();
            
            assertEquals(2, result);
        }
        {   // 3 components
            char[]   inputImg = {0, 1, 0,
                                 1, 0, 1,
                                 1, 1, 0};
            Connectivity conn = new Connectivity(2, 1); // 2D 4-connected
            UnitCubeConnectedComponenetsCounter counter = new UnitCubeConnectedComponenetsCounter(conn);
            
            int result = counter.SetImage(inputImg).getNumberOfConnectedComponents();
            
            assertEquals(3, result);
        }
        {   // 4 components
            char[]   inputImg = {0, 1, 0,
                                 1, 0, 1,
                                 1, 1, 0};
            Connectivity conn = new Connectivity(2, 1); // 2D 4-connected
            UnitCubeConnectedComponenetsCounter counter = new UnitCubeConnectedComponenetsCounter(conn);
            
            int result = counter.SetImage(inputImg).getNumberOfConnectedComponents();
            
            assertEquals(3, result);
        }
        {   // 1 component 2D 8-connected
            char[]   inputImg = {0, 1, 0,
                                 1, 0, 1,
                                 1, 1, 0};
            Connectivity conn = new Connectivity(2, 0); // 2D 8-connected
            UnitCubeConnectedComponenetsCounter counter = new UnitCubeConnectedComponenetsCounter(conn);
            
            int result = counter.SetImage(inputImg).getNumberOfConnectedComponents();
            
            assertEquals(1, result);
        }
    }
}
