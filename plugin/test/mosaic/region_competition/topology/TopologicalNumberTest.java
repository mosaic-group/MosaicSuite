package mosaic.region_competition.topology;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import mosaic.core.image.LabelImage;
import mosaic.core.image.Point;
import mosaic.region_competition.topology.TopologicalNumber.TopologicalNumberResult;


public class TopologicalNumberTest {

    @Test
    public void testGetTopologicalNumbersForAllAdjacentLabels() {
        // Label image and two test points below are taken from paper
        // "Discrete Region Competition for Unknown Numbers of Connected Regions"
        
        int[] img = new int[] { 1, 1, 2, 2, 3, 3, 
                                1, 1, 2, 2, 3, 3,
                                1, 1, 2, 2, 3, 0,
                                0, 0, 3, 3, 3, 3};
        LabelImage li = new LabelImage(new int[] {6, 4});
        for (int i = 0; i < li.getSize(); ++i) li.setLabel(i, img[i]);
        
        TopologicalNumber tn = new TopologicalNumber(li);
        
        {   // Testing non-FG-simple point
            Point testedPoint = new Point(4, 1);
            List<TopologicalNumberResult> result = tn.getTopologicalNumbersForAllAdjacentLabels(testedPoint);
            
            assertEquals(2, result.size());
            assertEquals(new TopologicalNumberResult(2, 1, 1), result.get(0));
            assertEquals(new TopologicalNumberResult(3, 2, 2), result.get(1));
            
            assertFalse(tn.isPointFgSimple(testedPoint));
        }
        {   // 3 region-labels around point
            Point testedPoint = new Point(2, 2);
            List<TopologicalNumberResult> result = tn.getTopologicalNumbersForAllAdjacentLabels(testedPoint);
            
            assertEquals(3, result.size());
            assertEquals(new TopologicalNumberResult(1, 1, 1), result.get(0));
            assertEquals(new TopologicalNumberResult(2, 1, 1), result.get(1));
            assertEquals(new TopologicalNumberResult(3, 1, 1), result.get(2));
            
            assertTrue(tn.isPointFgSimple(testedPoint));
        }
    }
    
    @Test
    public void testGetTopologicalNumbersForAllAdjacentLabels3D() {
        // Label image and two test points below are taken from paper
        int[] img = new int[] { 0, 0, 0, 
                                0, 0, 0,
                                0, 0, 0,
                                
                                1, 1, 1, 
                                1, 1, 1,
                                1, 1, 1,
                                
                                0, 0, 0, 
                                0, 0, 0,
                                0, 0, 0,
                              };
        LabelImage li = new LabelImage(new int[] {3, 3, 3});
        for (int i = 0; i < li.getSize(); ++i) li.setLabel(i, img[i]);
        
        TopologicalNumber tn = new TopologicalNumber(li);
        
        {   // 3D - point on surface of same labels
            Point testedPoint = new Point(1, 1, 1);
            List<TopologicalNumberResult> result = tn.getTopologicalNumbersForAllAdjacentLabels(testedPoint);
            System.out.println(result);
            assertEquals(2, result.size());
            assertEquals(new TopologicalNumberResult(1, 1, 2), result.get(0));
            
            assertFalse(tn.isPointFgSimple(testedPoint));
        }
    }

}
