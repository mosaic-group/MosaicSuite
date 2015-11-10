package mosaic.core.imageUtils.masks;

import static org.junit.Assert.*;

import org.junit.Test;

import mosaic.core.imageUtils.iterators.MappingIterator;


public class BallMaskTest {

    @Test
    public void testCircleOddRadiusOddSize() {
        String expected =   "-----------\n" + 
                            "-----------\n" + 
                            "-----------\n" + 
                            "----888----\n" + 
                            "---88888---\n" + 
                            "---88888---\n" + 
                            "---88888---\n" + 
                            "----888----\n" + 
                            "-----------\n" + 
                            "-----------\n" + 
                            "-----------";
        
        int size = 11;
        Mask cm = new BallMask(2.5f, size, new float[] {1, 1});
        MappingIterator mi = new MappingIterator(new int[] {size,size}, new int[]{size,size}, new int[]{0,0});
        StringBuilder sb = new StringBuilder();
        while(mi.hasNext()) {
            int idx = mi.next();
            if (mi.hasDimensionWrapped()) sb.append('\n');
            sb.append(cm.isInMask(idx) ? "8" : "-");
        }
        String result = sb.toString();
        assertEquals(expected, result);
    }
    
    @Test
    public void testCircleOddRadiusEvenSize() {
        String expected =   "----------\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "---8888---\n" + 
                            "---8888---\n" + 
                            "---8888---\n" + 
                            "---8888---\n" + 
                            "----------\n" + 
                            "----------\n" + 
                            "----------";
        
        int size = 10;
        Mask cm = new BallMask(2.5f, size, new float[] {1, 1});
        MappingIterator mi = new MappingIterator(new int[] {size,size}, new int[]{size,size}, new int[]{0,0});
        StringBuilder sb = new StringBuilder();
        while(mi.hasNext()) {
            int idx = mi.next();
            if (mi.hasDimensionWrapped()) sb.append('\n');
            sb.append(cm.isInMask(idx) ? "8" : "-");
        }
        String result = sb.toString();
        assertEquals(expected, result);
    }
    
    @Test
    public void testCircleEvenRadiusEvenSize() {
        String expected =   "--8888--\n" + 
                            "-888888-\n" + 
                            "88888888\n" + 
                            "88888888\n" + 
                            "88888888\n" + 
                            "88888888\n" + 
                            "-888888-\n" + 
                            "--8888--";
        
        int size = 8;
        Mask cm = new BallMask(4f, size, new float[] {1, 1});
        MappingIterator mi = new MappingIterator(new int[] {size,size}, new int[]{size,size}, new int[]{0,0});
        StringBuilder sb = new StringBuilder();
        while(mi.hasNext()) {
            int idx = mi.next();
            if (mi.hasDimensionWrapped()) sb.append('\n');
            sb.append(cm.isInMask(idx) ? "8" : "-");
        }
        String result = sb.toString();
        assertEquals(expected, result);
    }
    
    @Test
    public void testCircleEvenRadiusOddSize() {
        String expected =   "----8----\n" + 
                            "--88888--\n" + 
                            "-8888888-\n" + 
                            "-8888888-\n" + 
                            "888888888\n" + 
                            "-8888888-\n" + 
                            "-8888888-\n" + 
                            "--88888--\n" + 
                            "----8----";
        
        int size = 9;
        Mask cm = new BallMask(4f, size, new float[] {1, 1});
        MappingIterator mi = new MappingIterator(new int[] {size,size}, new int[]{size,size}, new int[]{0,0});
        StringBuilder sb = new StringBuilder();
        while(mi.hasNext()) {
            int idx = mi.next();
            if (mi.hasDimensionWrapped()) sb.append('\n');
            sb.append(cm.isInMask(idx) ? "8" : "-");
        }
        String result = sb.toString();
        assertEquals(expected, result);
    }
    
    @Test
    public void testCircleOddRadiusOddSizeScaling() {
        String expected =   "-----------\n" + 
                            "-----------\n" + 
                            "-----------\n" + 
                            "----888----\n" + 
                            "---88888---\n" + 
                            "---88888---\n" + 
                            "---88888---\n" + 
                            "----888----\n" + 
                            "-----------\n" + 
                            "-----------\n" + 
                            "-----------";
        
        int size = 11;
        Mask cm = new BallMask(5f, size, new float[] {2, 2});
        MappingIterator mi = new MappingIterator(new int[] {size,size}, new int[]{size,size}, new int[]{0,0});
        StringBuilder sb = new StringBuilder();
        while(mi.hasNext()) {
            int idx = mi.next();
            if (mi.hasDimensionWrapped()) sb.append('\n');
            sb.append(cm.isInMask(idx) ? "8" : "-");
        }
        String result = sb.toString();
        assertEquals(expected, result);
        assertEquals(21, cm.getNumOfFgPoints());
        assertArrayEquals(new int[] {11, 11}, cm.getDimensions());
    }
    
    @Test
    public void testCircleEvenRadiusEvenSize3D() {
        String expected =   "--------\n" + 
                            "--------\n" + 
                            "--------\n" + 
                            "--------\n" + 
                            "--------\n" + 
                            "--------\n" + 
                            "--------\n" + 
                            "--------\n" + 
                            
                            "--------\n" + 
                            "--------\n" + 
                            "---88---\n" + 
                            "--8888--\n" + 
                            "--8888--\n" + 
                            "---88---\n" + 
                            "--------\n" + 
                            "--------\n" + 
                            
                            "--------\n" + 
                            "---88---\n" + 
                            "--8888--\n" + 
                            "-888888-\n" + 
                            "-888888-\n" + 
                            "--8888--\n" + 
                            "---88---\n" + 
                            "--------\n" + 
                            
                            "--------\n" + 
                            "--8888--\n" + 
                            "-888888-\n" + 
                            "-888888-\n" + 
                            "-888888-\n" + 
                            "-888888-\n" + 
                            "--8888--\n" + 
                            "--------\n" + 
                            
                            "--------\n" + 
                            "--8888--\n" + 
                            "-888888-\n" + 
                            "-888888-\n" + 
                            "-888888-\n" + 
                            "-888888-\n" + 
                            "--8888--\n" + 
                            "--------\n" + 
                            
                            "--------\n" + 
                            "---88---\n" + 
                            "--8888--\n" + 
                            "-888888-\n" + 
                            "-888888-\n" + 
                            "--8888--\n" + 
                            "---88---\n" + 
                            "--------\n" + 
                            
                            "--------\n" + 
                            "--------\n" + 
                            "---88---\n" + 
                            "--8888--\n" + 
                            "--8888--\n" + 
                            "---88---\n" + 
                            "--------\n" + 
                            "--------\n" + 
                            
                            "--------\n" + 
                            "--------\n" + 
                            "--------\n" + 
                            "--------\n" + 
                            "--------\n" + 
                            "--------\n" + 
                            "--------\n" + 
                            "--------";
        
        int size = 8;
        Mask cm = new BallMask(3f, size, new float[] {1, 1, 1});
        MappingIterator mi = new MappingIterator(new int[] {size,size,size}, new int[]{size,size,size}, new int[]{0,0,0});
        StringBuilder sb = new StringBuilder();
        while(mi.hasNext()) {
            int idx = mi.next();
            if (mi.hasDimensionWrapped()) sb.append('\n');
            sb.append(cm.isInMask(idx) ? "8" : "-");
        }
        String result = sb.toString();
        assertEquals(expected, result);
    }
}
