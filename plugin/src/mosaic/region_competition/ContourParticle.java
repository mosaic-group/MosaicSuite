package mosaic.region_competition;


public class ContourParticle 
{
	int label = 0;
	int intensity = 0;
	int candidateLabel = 0;
	float energyDifference = 0f;
	
	@Override
	public String toString() 
	{
		return 	"L: "		+ label +
				" val: "	+ intensity+ 
				"L'"		+ candidateLabel+
				" deltaE"	+ energyDifference;
	}
	
}