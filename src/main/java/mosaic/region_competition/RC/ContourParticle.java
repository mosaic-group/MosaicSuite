package mosaic.region_competition.RC;


import java.util.LinkedList;
import java.util.List;

import mosaic.core.imageUtils.Point;

/**
 * Class representing countour particle. 
 */
public class ContourParticle {
    // absolute label
    public int label = 0;
    public int candidateLabel = 0;
    public float intensity = 0.0f;
    public double energyDifference = Double.MAX_VALUE;

    // mother - daughter indicators
    // Particle can have any combination of both.
    boolean isMother = false; 
    boolean isDaughter = false;
    
    int referenceCount = 0;
    boolean isProcessed = false;

    private final List<Point> motherList = new LinkedList<Point>();
    private final List<Point> daughterList = new LinkedList<Point>();
    private final List<Integer> testedList = new LinkedList<Integer>();

    public ContourParticle(int aLabel, float aIntensity) {
        label = aLabel;
        intensity = aIntensity;
    }
    
    List<Point> getMotherList() {
        return motherList;
    }

    void addMother(Point aMother) {
        motherList.add(aMother);
    }
    
    List<Point> getDaughterList() {
        return daughterList;
    }

    void addDaughter(Point aDaughter) {
        daughterList.add(aDaughter);
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
