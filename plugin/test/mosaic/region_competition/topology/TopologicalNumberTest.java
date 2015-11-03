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
            List<TopologicalNumberResult> result = tn.getTopologicalNumbersForAllAdjacentLabels(new Point(4,1));
            
            assertEquals(2, result.size());
            assertEquals(new TopologicalNumberResult(2, 1, 1), result.get(0));
            assertEquals(new TopologicalNumberResult(3, 2, 2), result.get(1));
        }
        {   // 3 region-labels around point
            List<TopologicalNumberResult> result = tn.getTopologicalNumbersForAllAdjacentLabels(new Point(2, 2));
            System.out.println(result);
            assertEquals(3, result.size());
            assertEquals(new TopologicalNumberResult(1, 1, 1), result.get(0));
            assertEquals(new TopologicalNumberResult(2, 1, 1), result.get(1));
            assertEquals(new TopologicalNumberResult(3, 1, 1), result.get(1));
        }
        
    }

}
