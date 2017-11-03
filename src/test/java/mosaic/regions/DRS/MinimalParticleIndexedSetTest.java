package mosaic.regions.DRS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import mosaic.regions.DRS.Particle;
import mosaic.regions.DRS.ParticleSet;


public class MinimalParticleIndexedSetTest {

    @Test
    public void testAll() {
        ParticleSet s = new ParticleSet();
        
        // Basic test
        assertEquals(0, s.size());
        assertTrue(s.erase(new Particle(0, 0, 0)) == null);
        
        // Prepare some more particles
        Particle p1 = new Particle(1, 1, 1);
        Particle p1b = new Particle(1, 1, 4);
        Particle p2 = new Particle(2, 2, 2);
        Particle p3 = new Particle(3, 3, 3);
        
        s.insert(p1);
        assertEquals(1, s.size());
        assertTrue(s.erase(p1) != null);
        assertEquals(0, s.size());
        
        
        // Play with larger number of particles
        s.insert(p1);
        s.insert(p2);
        s.insert(p3);
        assertEquals(3, s.size());
        
        // Test find method
        assertEquals(1, s.getIndex(p2));
        assertEquals("If no particle is found -1 should be returned", -1, s.getIndex(new Particle(4, 4, 4)));
        
        // Check if indexes are moved correctly when particle in "middle" is removed
        assertEquals(2, s.getIndex(p3));
        s.erase(p2);
        assertEquals(2, s.size());
        assertEquals(1, s.getIndex(p3));
        
        // Leve only one element in a container
        s.erase(p3);
        assertEquals(1, s.size());
        
        // Exchange p1 with p1b (they are same from equals() point of view)
        s.insert(p1b);
        assertEquals(p1b, s.get(0));

        // Removing last existing element
        assertEquals(p1b, s.erase(p1b));
        assertEquals(0, s.size());
        
        // Removing on empty container
        assertEquals(null, s.erase(p1b));
    }
    
    @Test
    public void testJoin() {
        Particle p1 = new Particle(1, 1, 1);
        Particle p2 = new Particle(2, 2, 2);
        Particle p2b = new Particle(2, 2, 3);
        Particle p3 = new Particle(3, 3, 3);
        
        ParticleSet s1 = new ParticleSet();
        ParticleSet s2 = new ParticleSet();
        
        // Create two sets with common element (equal but with different proposal value).
        s1.insert(p1);
        s1.insert(p2);
        
        s2.insert(p2b);
        s2.insert(p3);
        
        assertEquals(2, s1.size());
    }
}
