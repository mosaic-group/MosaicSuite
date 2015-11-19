package mosaic.core.imageUtils;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.core.imageUtils.masks.Mask;
import mosaic.core.imageUtils.masks.SphereMask;


public class MaskOnSpaceMapperTest {

    MaskOnSpaceMapper it;
    char[] image;
    int sizeX;
    int sizeY;
    
    @Before
    public void setup() {
        sizeX = 10;
        sizeY =  9;
        Mask sm = new SphereMask(2.5f, 5, new float[] {1, 1});
        it = new MaskOnSpaceMapper(sm, new int[] {sizeX , sizeY});
        
        image = new char[sizeX * sizeY];
        for (int i = 0; i < image.length; ++i) image[i] = '-';
    }
    
    @Test
    public void testRegularIteration() {
        String expected =   "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "-----888--\n" + 
                            "----8---8-\n" + 
                            "----8---8-\n" + 
                            "----8---8-\n" + 
                            "-----888--\n" + 
                            "----------\n";
        it.setUpperLeft(new Point(4,3));
        while(it.hasNext()) {
            int idx = it.next();
            image[idx] = '8';
        }
        String result = generateAsciiImage();
        assertEquals(expected, result);
    }

    @Test
    public void testCrop() {
        String expected =   "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "-----888--\n" + 
                            "----8---8-\n" + 
                            "----8---8-\n";
        
        it.setUpperLeft(new Point(4,6));
        while(it.hasNext()) {
            int idx = it.next();
            image[idx] = '8';
        }
        String result = generateAsciiImage();
        assertEquals(expected, result);
    }
    
    @Test
    public void testCrop3() {
        String expected =   "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "888-------\n" + 
                            "---8------\n" + 
                            "---8------\n";
        
        it.setUpperLeft(new Point(-1,6));
        while(it.hasNext()) {
            int idx = it.next();
            image[idx] = '8';
        }
        String result = generateAsciiImage();
        assertEquals(expected, result);
    }
    
    @Test
    public void testCrop2() {
        String expected =   "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "-------888\n" + 
                            "------8---\n" + 
                            "------8---\n";
        
        it.setUpperLeft(new Point(6,6));
        while(it.hasNext()) {
            int idx = it.next();
            image[idx] = '8';
        }
        String result = generateAsciiImage();
        assertEquals(expected, result);
    }
    
    @Test
    public void testDoubleDrawing() {
        String expected =   "--8---8---\n" + 
                            "---888----\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "-----888--\n" + 
                            "----8---8-\n" + 
                            "----8---8-\n";
        
        it.setUpperLeft(new Point(4,6));
        while(it.hasNext()) {
            int idx = it.next();
            image[idx] = '8';
        }
        it.setUpperLeft(new Point(2,-3));
        while(it.hasNext()) {
            int idx = it.next();
            image[idx] = '8';
        }
        
        
        String result = generateAsciiImage();
        assertEquals(expected, result);
    }
    
    @Test
    public void testDoubleDrawingWithPoint() {
        String expected =   "--8---8---\n" + 
                            "---888----\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "-----888--\n" + 
                            "----8---8-\n" + 
                            "----8---8-\n";
        SpaceIterator si = new SpaceIterator(sizeX, sizeY);
        
        it.setMiddlePoint(new Point(6,8));
        while(it.hasNext()) {
            Point point = it.nextPoint();
            image[si.pointToIndex(point)] = '8';
        }
        it.setMiddlePoint(new Point(4,-1));
        while(it.hasNext()) {
            Point point = it.nextPoint();
            image[si.pointToIndex(point)] = '8';
        }
        
        
        String result = generateAsciiImage();
        assertEquals(expected, result);
    }
    
    @Test
    public void testSetMidPointWithEvenSizeMask() {
        String expected =   "-8888-----\n" + 
                            "8----8----\n" + 
                            "8----8----\n" + 
                            "8----8----\n" + 
                            "8----8----\n" + 
                            "-8888-----\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------\n";
        
        SpaceIterator si = new SpaceIterator(sizeX, sizeY);
        Mask sm = new SphereMask(3f, 6, new float[] {1, 1});
        it = new MaskOnSpaceMapper(sm, new int[] {sizeX , sizeY});
        
        it.setMiddlePoint(new Point(3,3));
        while(it.hasNext()) {
            Point point = it.nextPoint();
            image[si.pointToIndex(point)] = '8';
        }
        
        String result = generateAsciiImage();
        assertEquals(expected, result);
    }

    private String generateAsciiImage() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < sizeY; ++y) {
            for (int x = 0; x < sizeX; ++x) {
                sb.append(image[x + y * sizeX]);
            }
            sb.append('\n');
        }
        String result = sb.toString();
        return result;
    }
}
