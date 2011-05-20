package mosaic.region_competition;

import java.util.LinkedList;
import java.util.List;


public class ContourParticle 
{
	int label = 0;				// absLabel
	int intensity = 0;
	int candidateLabel = 0;
	float energyDifference = 0f;
	
	boolean daughterFlag = false;
	private List<ContourParticle> motherList=null;
	private List<ContourParticle> daughterList=null;
	
	List<ContourParticle> getMotherList()
	{
		if(motherList==null)
		{
			motherList = new LinkedList<ContourParticle>();
		}
		return motherList;
	}
	
	List<ContourParticle> getDaughterList()
	{
		if(daughterList==null)
		{
			daughterList = new LinkedList<ContourParticle>();
		}
		return daughterList;
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