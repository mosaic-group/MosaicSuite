package mosaic.core.imageUtils;

import static org.junit.Assert.*;

import org.junit.Test;

import mosaic.core.binarize.BinarizedIntervalLabelImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.iterators.SpaceIterator;


public class FloodFillTest {

    @Test
    public void testIterable() {
        int[] img = new int[] { 0, 0, 2, 2, 3, 3, 
                                0, 0, 2, 2, 3, 3,
                                2, 2, 2, 2, 2, 0,
                                2, 0, 3, 3, 3, 3};
        
        int[] expected = new int[] { 0, 0, 8, 8, 3, 3, 
                                     0, 0, 8, 8, 3, 3,
                                     8, 8, 8, 8, 8, 0,
                                     8, 0, 3, 3, 3, 3};
        
        LabelImage li = new LabelImage(img, new int[] {6, 4});
        BinarizedIntervalLabelImage area = new BinarizedIntervalLabelImage(li);
        area.AddOneValThreshold(2);
        
        // Relabel 2 to 8 with FloodFill
        FloodFill ff = new FloodFill(li, area, new Point(3,1));
        assertEquals(10, ff.size());
        for (Integer index : ff) {
            li.setLabel(index, 8);
        }
        
        for (Integer index : new SpaceIterator(li.getDimensions()).getIndexIterable() ) {
            assertEquals(expected[index], li.getLabel(index));
        }
    }

    @Test
    public void testIterator() {
        int[] img = new int[] { 0, 0, 2, 2, 3, 3, 
                                0, 0, 2, 2, 3, 3,
                                2, 2, 2, 2, 2, 0,
                                2, 0, 3, 3, 3, 3};
        
        int[] expected = new int[] { 0, 0, 8, 8, 3, 3, 
                                     0, 0, 8, 8, 3, 3,
                                     8, 8, 8, 8, 8, 0,
                                     8, 0, 3, 3, 3, 3};
        
        LabelImage li = new LabelImage(img, new int[] {6, 4});
        BinarizedIntervalLabelImage area = new BinarizedIntervalLabelImage(li);
        area.AddOneValThreshold(2);
        
        // Relabel 2 to 8 with FloodFill
        FloodFill ff = new FloodFill(li, area, new Point(3,1));
        while (ff.hasNext()) {
            li.setLabel(ff.next(), 8);
        }
        
        for (Integer index : new SpaceIterator(li.getDimensions()).getIndexIterable() ) {
            assertEquals(expected[index], li.getLabel(index));
        }
    }
    
    @Test
    public void testSeedOutsideWantedArea() {
        int[] img = new int[] { 0, 0, 2, 2, 3, 3, 
                                0, 0, 2, 2, 3, 3,
                                2, 2, 2, 2, 2, 0,
                                2, 0, 3, 3, 3, 3};
        
        // Expect no change
        int[] expected = new int[] { 0, 0, 2, 2, 3, 3, 
                                     0, 0, 2, 2, 3, 3,
                                     2, 2, 2, 2, 2, 0,
                                     2, 0, 3, 3, 3, 3};
        
        LabelImage li = new LabelImage(img, new int[] {6, 4});
        BinarizedIntervalLabelImage area = new BinarizedIntervalLabelImage(li);
        area.AddOneValThreshold(2);
        
        // Label at seed point is not in thresholded area
        FloodFill ff = new FloodFill(li, area, new Point(0,0));
        while (ff.hasNext()) {
            li.setLabel(ff.next(), 8);
        }
        
        for (Integer index : new SpaceIterator(li.getDimensions()).getIndexIterable() ) {
            assertEquals(expected[index], li.getLabel(index));
        }
    }
}
