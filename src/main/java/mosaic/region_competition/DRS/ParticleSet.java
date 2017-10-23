package mosaic.region_competition.DRS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ParticleSet implements Iterable<Particle> {
    private HashMap<Particle, Integer> iMap = new HashMap<>();
    protected ArrayList<Particle> iParticles = new ArrayList<>();
    
    /**
     * @return size of container
     */
    int size() { return iMap.size(); }
    
    /**
     * @return index of aParticle or if not found -1
     */
    int getIndex(Particle aParticle) {
        Integer index = iMap.get(aParticle);
        if (index == null) { return -1; }
        return index;
    }
    
    /**
     * @return true if aParticle is in container
     */
    boolean contains(Particle aParticle) {
        return iMap.containsKey(aParticle);
    }
    
    /**
     * Inserts particle into container
     * @param aParticle - particle to be inserted
     * @return index of inserted particle
     */
    Particle insert(Particle aParticle) {
        Integer index = iMap.get(aParticle);
        Particle lastRemovedElement = null;
        if (index == null) {
            iParticles.add(aParticle);
            iMap.put(aParticle, iMap.size());
        }
        else {
            lastRemovedElement = iParticles.set(index, aParticle);
            iMap.replace(aParticle, index);
        }
        return lastRemovedElement;
    }
    
    /**
     * @return particle at aIndex
     */
    Particle get(int aIndex) {
        return iParticles.get(aIndex);
    }
    
    /**
     * Removes aParticle. Change indices to keep them continues so may invalidate previous index to particle.
     * @return removed MinimalParticle if existed or null otherwise
     */
    Particle erase(Particle aParticle) {
        Integer index = iMap.get(aParticle);
        if (index == null) {
            return null;
        }
        Particle lastRemovedElement = iParticles.get(index);
        int lastElementIndex = iParticles.size() - 1;
        if (lastElementIndex != index) {
            // Move last element in a place of removed one.
            Particle lastParticle = iParticles.get(lastElementIndex);
            iParticles.set(index, lastParticle);
            iMap.replace(lastParticle, index);
        }
        iParticles.remove(lastElementIndex);
        iMap.remove(aParticle);

        return lastRemovedElement;
    }
    
    @Override
    public String toString() {
        return " MAP/VECsize: " + iMap.size() +"/" + iParticles.size() + " mapElements:\n" + iMap;
    }

    @Override
    public Iterator<Particle> iterator() {
        return new Iterator<Particle>() {
            private int idx = 0;
            
            @Override
            public boolean hasNext() {
                return idx < iParticles.size();
            }

            @Override
            public Particle next() {
                return iParticles.get(idx++);
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
