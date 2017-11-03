package mosaic.regions.DRS;


public class Particle {
    public final int iIndex;
    public int iCandidateLabel;
    public float iProposal;
    
    Particle(int aIndex, int aCandidateLabel, float aProposal) {
        iIndex = aIndex;
        iCandidateLabel = aCandidateLabel; 
        iProposal = aProposal;
    }
    
    Particle(Particle aMp) {
        iIndex = aMp.iIndex;
        iCandidateLabel = aMp.iCandidateLabel; 
        iProposal = aMp.iProposal;
    }
    
    Particle() {
        // set values that are unusual
        this(Integer.MIN_VALUE, Integer.MIN_VALUE, 0);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + iCandidateLabel;
        result = prime * result + iIndex;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Particle other = (Particle) obj;
        if (iCandidateLabel != other.iCandidateLabel) return false;
        if (iIndex != other.iIndex) return false;
        return true;
    }
    
    @Override
    public String toString() {
        return "(" + iIndex + ", " + iCandidateLabel + ", " + iProposal +")";
    }
}
