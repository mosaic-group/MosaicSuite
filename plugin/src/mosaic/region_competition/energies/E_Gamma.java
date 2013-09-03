package mosaic.region_competition.energies;

import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.LabelImage;
import mosaic.region_competition.Point;
import mosaic.region_competition.energies.Energy.InternalEnergy;
import mosaic.region_competition.topology.Connectivity;

public class E_Gamma extends InternalEnergy
{
	public E_Gamma(LabelImage labelImage)
	{
		super(labelImage);
	}

	@Override
	public Object atStart()
	{
		// nothing to do
		return null;
	}
	
	
	@Override
	public EnergyResult 
	CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle,
			int toLabel)
	{
		Point pIndex = contourPoint;
		int pLabel = contourParticle.candidateLabel;
		
		Connectivity conn = labelImage.getConnFG();
		
		int nSameNeighbors=0;
		for(Point neighbor:conn.iterateNeighbors(pIndex))
		{
			int neighborLabel=labelImage.getLabelAbs(neighbor);
			if(neighborLabel==pLabel)
			{
				nSameNeighbors++;
			}
		}
		
		//TODO is this true? conn.getNNeighbors
		int nOtherNeighbors = conn.getNNeighbors()-nSameNeighbors;
		double dGamma = (nOtherNeighbors - nSameNeighbors)/(double)conn.GetNeighborhoodSize();
		return new EnergyResult(dGamma, false);
	}

}