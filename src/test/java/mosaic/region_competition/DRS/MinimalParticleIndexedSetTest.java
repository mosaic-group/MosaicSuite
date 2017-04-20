package mosaic.region_competition.DRS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class MinimalParticleIndexedSetTest {

    @Test
    public void testAll() {
        MinimalParticleIndexedSet s = new MinimalParticleIndexedSet();
        
        // Basic test
        assertEquals(0, s.size());
        assertTrue(s.erase(new MinimalParticle(0, 0, 0)) == false);
        
        // Prepare some more particles
        MinimalParticle p1 = new MinimalParticle(1, 1, 1);
        MinimalParticle p1b = new MinimalParticle(1, 1, 4);
        MinimalParticle p2 = new MinimalParticle(2, 2, 2);
        MinimalParticle p3 = new MinimalParticle(3, 3, 3);
        
        s.insert(p1);
        assertEquals(1, s.size());
        assertTrue(s.erase(p1) == true);
        assertEquals(0, s.size());
        assertEquals(p1, s.getLastDeletedElement());
        
        
        // Play with larger number of particles
        s.insert(p1);
        s.insert(p2);
        s.insert(p3);
        assertEquals(3, s.size());
        
        // Test find method
        assertEquals(1, s.find(p2));
        assertEquals("If no particle is found size of container should be returned", 3, s.find(new MinimalParticle(4, 4, 4)));
        
        // Check if indexes are moved correctly when particle in "middle" is removed
        assertEquals(2, s.find(p3));
        s.erase(p2);
        assertEquals(2, s.size());
        assertEquals(1, s.find(p3));
        
        // Leve only one element in a container
        s.erase(p3);
        assertEquals(1, s.size());
        
        // Exchange p1 with p1b (they are same from equals() point of view)
        s.insert(p1b);
        assertEquals(p1, s.getLastDeletedElement());
        assertEquals(p1b, s.elementAt(0));

        // Removing last existing element
        assertEquals(true, s.erase(p1b));
        assertEquals(0, s.size());
        
        // Removing on empty container
        assertEquals(false, s.erase(p1b));
    }
    
    @Test
    public void testJoin() {
        MinimalParticle p1 = new MinimalParticle(1, 1, 1);
        MinimalParticle p2 = new MinimalParticle(2, 2, 2);
        MinimalParticle p2b = new MinimalParticle(2, 2, 3);
        MinimalParticle p3 = new MinimalParticle(3, 3, 3);
        
        MinimalParticleIndexedSet s1 = new MinimalParticleIndexedSet();
        MinimalParticleIndexedSet s2 = new MinimalParticleIndexedSet();
        
        // Create two sets with common element (equal but with different proposal value).
        s1.insert(p1);
        s1.insert(p2);
        
        s2.insert(p2b);
        s2.insert(p3);
        
        assertEquals(2, s1.size());
        s1.join(s2);
        assertEquals(3, s1.size());
        assertEquals(p2b, s1.elementAt(1));
        assertEquals(p2, s1.getLastDeletedElement());
    }
}
