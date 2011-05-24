package mosaic.region_competition;

import java.util.LinkedList;
import java.util.List;


public class ContourParticle 
{
	int label = 0;				// absLabel
	int intensity = 0;
	int candidateLabel = 0;
	float energyDifference = 0f;
	
	boolean isDaughter = false;
	boolean ismother = false;
	int referenceCount = 0;
	
	//TODO ? list of ContourParticle or Point?
	private List<Point> motherList=null;
	private List<Point> daughterList=null;
	
	List<Point> getMotherList()
	{
		if(motherList==null)
		{
			motherList = new LinkedList<Point>();
		}
		return motherList;
	}
	
	List<Point> getDaughterList()
	{
		if(daughterList==null)
		{
			daughterList = new LinkedList<Point>();
		}
		return daughterList;
	}

	
	private List<Integer> testedList = new LinkedList<Integer>();
    boolean hasLabelBeenTested(int aLabel) {
        return testedList.contains(aLabel);
    }

    void setLabelHasBeenTested(int aLabel) {
        testedList.add(aLabel);
    }
	
	
	
	void reset()
	{
		label = 0;
		intensity = 0;
		candidateLabel = 0;
		energyDifference = 0f;
		
		isDaughter = false;
		ismother = false;
		referenceCount = 0;
		
		testedList.clear();
	}
	
	
	//TODO Ref to Point?
	
	@Override
	public String toString() 
	{
		return 	"L: "		+ label +
				" val: "	+ intensity+ 
				"L'"		+ candidateLabel+
				" deltaE"	+ energyDifference;
	}
	
}