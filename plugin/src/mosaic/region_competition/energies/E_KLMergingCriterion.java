package mosaic.region_competition.energies;

import java.util.HashMap;

import mosaic.core.utils.Point;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.LabelInformation;
import mosaic.region_competition.energies.Energy.ExternalEnergy;

public class E_KLMergingCriterion extends ExternalEnergy
{

	float m_RegionMergingThreshold;
	int bgLabel;
	HashMap<Integer, LabelInformation> labelMap;

	public E_KLMergingCriterion(
			HashMap<Integer, LabelInformation> labelMap, 
			int bgLabel, 
			float m_RegionMergingThreshold)
	{
		super(null, null);
		this.labelMap=labelMap;
		this.bgLabel = bgLabel;
		this.m_RegionMergingThreshold = m_RegionMergingThreshold;
	}

	@Override
	public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel)
	{
		int fromLabel = contourParticle.label;
		boolean merge = CalculateMergingEnergyForLabel(fromLabel, toLabel);
		return new EnergyResult(null, merge);
	}
	
	
	boolean CalculateMergingEnergyForLabel(int aLabelA, int aLabelB)
	{
		// store this event to check afterwards if we should merge
		// the 2 regions.
		if(aLabelA != bgLabel && aLabelB != bgLabel) // we are competeing.
		{ 
			/// test if merge should be performed:
			double value = CalculateKLMergingCriterion(aLabelA, aLabelB);
//			debug("KL: it="+m_iteration_counter+" "+aLabelA+" "+aLabelB+" "+value);
			if(value < m_RegionMergingThreshold)
			{
				return true;
			}
		}
		return false;
	}

	
	double CalculateKLMergingCriterion(int L1, int L2)
	{
		LabelInformation aL1 = labelMap.get(L1);
		LabelInformation aL2 = labelMap.get(L2);

		double vMu1 = aL1.mean;
		double vMu2 = aL2.mean;
		double vVar1 = aL1.var;
		double vVar2 = aL2.var;
		int vN1 = aL1.count;
		int vN2 = aL2.count;

//		debug("l1="+L1+" L2="+L2);
		
		double result = CalculateKLMergingCriterion(vMu1, vMu2, vVar1, vVar2, vN1, vN2);
		return result;

	}
	
	public static double CalculateKLMergingCriterion(double aMu1, double aMu2, double aVar1, double aVar2, int aN1, int aN2)
	{
		double vMu12 = (aN1 * aMu1 + aN2 * aMu2) / (aN1 + aN2);
		
//		System.out.println(vMu12);

		double vSumOfSq1 = aVar1 * (aN1 - 1) + aN1 * aMu1 * aMu1;
		double vSumOfSq2 = aVar2 * (aN2 - 1) + aN2 * aMu2 * aMu2;

		double vVar12 = (1.0 / (aN1 + aN2 - 1.0))
				* (vSumOfSq1 + vSumOfSq2 - (aN1 + aN2) * vMu12 * vMu12);
		
		if(vVar12<=0)
		{
//			System.out.print("vVar12==0");
			debug("vVar12==0");
			return 0;
		}
		if(aVar1<0)
			aVar1=0;
		if(aVar2<0)
			aVar2=0;

		double vDKL1 = (aMu1 - vMu12) * (aMu1 - vMu12) / (2.0 * vVar12) + 0.5
				* (aVar1 / vVar12 - 1.0 - Math.log(aVar1 / vVar12));

		double vDKL2 = (aMu2 - vMu12) * (aMu2 - vMu12) / (2.0 * vVar12) + 0.5
				* (aVar2 / vVar12 - 1.0 - Math.log(aVar2 / vVar12));

		double result = vDKL1 + vDKL2;
		if(Double.isNaN(result))
		{
			debug("CalculateKLMergingCriterion is NaN");
			throw new RuntimeException("Double.isNaN in CalculateKLMergingCriterion");
		}
		return result;
	}
	
	static void debug(String s)
	{
		System.out.println(s);
	}

	@Override
	public Object atStart()
	{
		return null;
	}

}
