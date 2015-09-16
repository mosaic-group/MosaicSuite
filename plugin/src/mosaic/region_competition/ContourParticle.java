package mosaic.region_competition;


import java.util.LinkedList;
import java.util.List;

import mosaic.core.utils.Point;


public class ContourParticle {

    boolean newlyCreated;

    public int label = 0; // absLabel
    public float intensity = 0.0f;

    public int candidateLabel = 0;
    public double energyDifference = 0;

    boolean isDaughter = false;
    boolean isMother = false;
    int referenceCount = 0;

    boolean m_processed = false;

    private List<Point> motherList = new LinkedList<Point>();
    private List<Point> daughterList = new LinkedList<Point>();
    private List<Integer> testedList = new LinkedList<Integer>();

    List<Point> getMotherList() {
        return motherList;
    }

    List<Point> getDaughterList() {
        return daughterList;
    }

    List<Integer> getTestedList() {
        return testedList;
    }

    boolean hasLabelBeenTested(int aLabel) {
        return testedList.contains(aLabel);
    }

    void setLabelHasBeenTested(int aLabel) {
        testedList.add(aLabel);
    }

    @Override
    public String toString() {
        return "L=" + label + " val=" + intensity + " L'=" + candidateLabel;
    }
}
