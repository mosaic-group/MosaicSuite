package mosaic.region_competition.DRS;


public class MinimalParticle {
    public int iIndex;
    public int iCandidateLabel;
    public float iProposal;
    
    MinimalParticle(int aIndex, int aCandidateLabel, float aProposal) {
        iIndex = aIndex;
        iCandidateLabel = aCandidateLabel; 
        iProposal = aProposal;
    }
    
    MinimalParticle(MinimalParticle aMp) {
        iIndex = aMp.iIndex;
        iCandidateLabel = aMp.iCandidateLabel; 
        iProposal = aMp.iProposal;
    }
    
    MinimalParticle() {
        // set values that are unusual
        this(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
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
        MinimalParticle other = (MinimalParticle) obj;
        if (iCandidateLabel != other.iCandidateLabel) return false;
        if (iIndex != other.iIndex) return false;
        return true;
    }
    
    @Override
    public String toString() {
        return "(" + iIndex + ", " + iCandidateLabel + ", " + iProposal +")";
    }
}
