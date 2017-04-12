package mosaic.region_competition.DRS;

import java.util.ArrayList;
import java.util.HashMap;

public class ConstIndexedHashSet {
    private HashMap<MinimalParticle, Integer> iMap = new HashMap<>();
    private ArrayList<MinimalParticle> iParticles = new ArrayList<>();
    private MinimalParticle iLastRemovedElement = null;
    
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
    int insert(MinimalParticle aParticle) {
        Integer index = iMap.get(aParticle);
        if (index == null) {
            index = iMap.size();
            iMap.put(aParticle, index);
            iParticles.add(aParticle);
        }
        else {
            iLastRemovedElement = iParticles.get(index);
            iMap.remove(aParticle);
            iMap.put(aParticle, index);
            iParticles.set(index, aParticle);
        }
        return index;
    }
    
    /**
     * @return particle at aIndex
     */
    MinimalParticle elementAt(int aIndex) {
        return iParticles.get(aIndex);
    }
    
    /**
     * Join provided aSet to this one.
     * @param aSet
     * @return this
     */
    ConstIndexedHashSet join(ConstIndexedHashSet aSet) {
        for(int i = 0; i < aSet.size(); ++i) {
            insert(aSet.elementAt(i));
        }
        return this;
    }
    
    /**
     * @return last removed element from container
     */
    MinimalParticle getLastDeletedElement() {
        return iLastRemovedElement;
    }
    
    /**
     * Removes aParticle. Change indices to keep them continues so may invalidate previous index to particle.
     * @return true if aParticle was in container, false otherwise.
     */
    boolean erase(MinimalParticle aParticle) {
        Integer index = iMap.get(aParticle);
        if (index == null) {
            return false;
        }
        
        iLastRemovedElement = iParticles.get(index);

        /// We move the last element:
        int lastElementIndex = iParticles.size() - 1;
        iMap.replace(iParticles.get(lastElementIndex), index);
        iMap.remove(aParticle);
        
        // Update the vector: move the last element to the element to delete and remove the last element.
        iParticles.set(index, iParticles.get(lastElementIndex));
        iParticles.remove(lastElementIndex);
        
        return true;
    }
    
    @Override
    public String toString() {
        return "MAP:\n" + iMap + "\nVEC:\n" + iParticles;
    }
}
