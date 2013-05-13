package mosaic.region_competition.energies;

import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.Point;
import mosaic.region_competition.Settings;
import mosaic.region_competition.energies.Energy.EnergyResult;

public class ImageModel
{
	Energy e_data;
	Energy e_length; 
	Energy e_merge;
	
	Settings settings;
	
	public ImageModel(Energy e_data, Energy e_length, Energy e_merge, Settings settings)
	{
		this.e_data = e_data;
		this.e_length = e_length;
		this.e_merge = e_merge;
		this.settings = settings;
	}
	
	public Energy getEdata()
	{
		return e_data;
	}
	
	public EnergyFunctionalType getEdataType()
	{
		return settings.m_EnergyFunctional;
	}
	
	public void updateStatisticsAtStart()
	{
		//TODO
		// not used yet
	}
	
	public void updateStatisticsWhenJump()
	{
		//TODO
		// not used yet
	}
	
	public EnergyResult CalculateEnergyDifferenceForLabel(Point aContourIndex, ContourParticle aContourPointPtr, int aToLabel)
	{
			float m_EnergyRegionCoeff = settings.m_EnergyRegionCoeff;
			
			float vCurrentImageValue = aContourPointPtr.intensity;
			int vCurrentLabel = aContourPointPtr.label;
			Double vEnergy = 0.0;
			Boolean vMerge = false;
			
			EnergyResult vV;

	        /// Calculate the change in energy due to the change of intensity when changing
	        /// from one label 'from' to another 'to'.
			if (m_EnergyRegionCoeff != 0)
			{
				vV = e_data.CalculateEnergyDifference(aContourIndex, aContourPointPtr, aToLabel);
				vEnergy += m_EnergyRegionCoeff * vV.energyDifference;
				// vMerge may be null here and will be set below at the merge energy. 
				vMerge = vV.merge;
	        }
			
			
			// Contour Length (Regularization //
			float m_EnergyContourLengthCoeff = settings.m_EnergyContourLengthCoeff;
			if (m_EnergyContourLengthCoeff != 0 && e_length != null) 
			{
				
				vV = e_length.CalculateEnergyDifference(aContourIndex, aContourPointPtr, aToLabel);
		        Double eCurv = vV.energyDifference;
		        vEnergy += m_EnergyContourLengthCoeff * eCurv;
			}
			
			
	        /// add a balloon force and a constant outward flow. If fronts were
	        /// touching, no constant flow is imposed (cancels out).
	        if(vCurrentLabel == 0)  // growing
	        {
	            vEnergy -= settings.m_ConstantOutwardFlow;
	            if (settings.m_EnergyFunctional == EnergyFunctionalType.e_PS)
	            {
	            	if (settings.m_BalloonForceCoeff > 0) { // outward flow
	            		vEnergy -= settings.m_BalloonForceCoeff * vCurrentImageValue;
	            	} else {
	            		vEnergy -= -settings.m_BalloonForceCoeff * (1 - vCurrentImageValue);
	            	}
	            }
	        } else if (aToLabel == 0) // shrinking
	        {
	            vEnergy += settings.m_ConstantOutwardFlow;
	        }
	        
	        
	        /// For the full-region based energy models, register competing regions
	        /// undergo a merge.
			
			if(e_merge!=null) // use e_merge explicitly
			{
				vV = e_merge.CalculateEnergyDifference(aContourIndex, aContourPointPtr, aToLabel);
				vMerge = vV.merge;
			}
	        
	        return new EnergyResult(vEnergy, vMerge);

	    }
}

