package mosaic.region_competition;


import java.util.LinkedList;
import java.util.List;

import mosaic.core.image.Point;

/**
 * Class representing countour particle. 
 */
public class ContourParticle {
    // absolute label
    public int label = 0;
    public int candidateLabel = 0;
    public float intensity = 0.0f;
    public double energyDifference = 0;

    // mother - daughter indicators
    // Particle can have any combination of both.
    boolean isMother = false; 
    boolean isDaughter = false;
    
    int referenceCount = 0;
    boolean isProcessed = false;

    private final List<Point> motherList = new LinkedList<Point>();
    private final List<Point> daughterList = new LinkedList<Point>();
    private final List<Integer> testedList = new LinkedList<Integer>();

    List<Point> getMotherList() {
        return motherList;
    }

    List<Point> getDaughterList() {
        return daughterList;
    }

    boolean hasLabelBeenTested(int aLabel) {
        return testedList.contains(aLabel);
    }

    void setTestedLabel(int aLabel) {
        testedList.add(aLabel);
    }

    void clearLists() {
        motherList.clear(); 
        daughterList.clear();
        testedList.clear();
    }
    
    @Override
    public String toString() {
        return "[L=" + label + " val=" + intensity + " L'=" + candidateLabel + "]";
    }
}
