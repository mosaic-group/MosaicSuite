package mosaic.region_competition.energies;

import java.util.HashMap;

import mosaic.core.utils.Point;
import mosaic.region_competition.Algorithm;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.Settings;

public class OscillationDetection
{
	Algorithm algorithm; 
	
    int m_OscillationsNumberHist[];
    double m_OscillationsEnergyHist[];
    int m_OscillationHistoryLength;
    
    float m_AcceptedPointsReductionFactor;
    
	public OscillationDetection(Algorithm algo, Settings settings)
	{
		this.algorithm = algo; 
		
		this.m_OscillationHistoryLength=settings.m_OscillationHistoryLength;
		this.m_AcceptedPointsReductionFactor = settings.m_AcceptedPointsReductionFactor;
		
        m_OscillationsNumberHist = new int[m_OscillationHistoryLength];
        m_OscillationsEnergyHist = new double[m_OscillationHistoryLength];
    	
		for(int vI = 0; vI < m_OscillationHistoryLength; vI++) {
			m_OscillationsNumberHist[vI] = 0;
			m_OscillationsEnergyHist[vI] = 0;
		}
	}
	

	/**
	 * Detect oscillations and store values in history.
	 */
	public boolean DetectOscillations(HashMap<Point, ContourParticle> m_Candidates)
	{
		
		boolean result = false;
		        
        double vSum = SumAllEnergies(m_Candidates);
        debug("sum of energies: "+vSum);
        debug("num of candidat: "+m_Candidates.size());
		for(int vI = 0; vI < m_OscillationHistoryLength; vI++) 
		{
			double vSumOld = m_OscillationsEnergyHist[vI];

			if(m_Candidates.size() == m_OscillationsNumberHist[vI]
					&& Math.abs(vSum - vSumOld) <= 1e-5 * Math.abs(vSum)) 
			{
				/// here we assume that we're oscillating, 
				// so we decrease the acceptance factor:
				result = true;
				algorithm.m_AcceptedPointsFactor *= m_AcceptedPointsReductionFactor;
				debug("nb of accepted points reduced to: " + algorithm.m_AcceptedPointsFactor);

			}
		}

		/// Shift the old elements:
		//TODO sts maybe optimize by modulo list?
		for (int vI = 1; vI < m_OscillationHistoryLength; vI++) {
		    m_OscillationsEnergyHist[vI-1] = m_OscillationsEnergyHist[vI];
		    m_OscillationsNumberHist[vI-1] = m_OscillationsNumberHist[vI];
		}

		/// Fill the new elements:
		m_OscillationsEnergyHist[m_OscillationHistoryLength-1] = vSum;
		m_OscillationsNumberHist[m_OscillationHistoryLength-1] = m_Candidates.size();
		
		return result;
	}
	
	protected double SumAllEnergies(HashMap<Point, ContourParticle> aContainer) 
	{
	    double vTotalEnergyDiff = 0;
	    
	    for (ContourParticle vPointIterator : aContainer.values()) {
	        vTotalEnergyDiff += vPointIterator.energyDifference;
	    }
	    return vTotalEnergyDiff;
	}
	
	public static void debug(String s)
	{
		System.out.println(s);
	}
	
}








