package mosaic.region_competition.DRS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MinimalParticleIndexedSet implements Iterable<MinimalParticle> {
    private HashMap<MinimalParticle, Integer> iMap = new HashMap<>();
    ArrayList<MinimalParticle> iParticles = new ArrayList<>();
    
    /**
     * @return size of container
     */
    int size() { return iMap.size(); }
    
    /**
     * @return index of aParticle or if not found first not used index (size of container)
     */
    int find(MinimalParticle aParticle) {
        Integer index = iMap.get(aParticle);
        if (index == null) { return iMap.size(); }
        return index;
    }
    
    /**
     * @return true if aParticle is in container
     */
    boolean contains(MinimalParticle aParticle) {
        return iMap.containsKey(aParticle);
    }
    
    /**
     * Inserts particle into container
     * @param aParticle - particle to be inserted
     * @return index of inserted particle
     */
    MinimalParticle insert(MinimalParticle aParticle) {
        Integer index = iMap.get(aParticle);
        MinimalParticle lastRemovedElement = null;
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
    MinimalParticle get(int aIndex) {
        return iParticles.get(aIndex);
    }
    
    /**
     * Removes aParticle. Change indices to keep them continues so may invalidate previous index to particle.
     * @return removed MinimalParticle if existed or null otherwise
     */
    MinimalParticle erase(MinimalParticle aParticle) {
        Integer index = iMap.get(aParticle);
        if (index == null) {
            return null;
        }
        MinimalParticle lastRemovedElement = iParticles.get(index);
        int lastElementIndex = iParticles.size() - 1;
        if (lastElementIndex != index) {
            // Move last element in a place of removed one.
            MinimalParticle lastParticle = iParticles.get(lastElementIndex);
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
    public Iterator<MinimalParticle> iterator() {
        return new Iterator<MinimalParticle>() {
            private int idx = 0;
            
            @Override
            public boolean hasNext() {
                return idx < iParticles.size();
            }

            @Override
            public MinimalParticle next() {
                return iParticles.get(idx++);
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
