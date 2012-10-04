package mosaic.region_competition.deprecated;

import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.LabelImage;
import mosaic.region_competition.Point;
import mosaic.region_competition.energies.Energy.InternalEnergy;

public class E_CurvatureBasedGradientFlow_sphis extends InternalEnergy// implements SettingsListener
{
	
	SphereBitmapImageSource_sphis sphereMaskIterator;
	
	public E_CurvatureBasedGradientFlow_sphis(LabelImage labelImage, int rad)
	{
		super(labelImage);
		initSphere(rad); 
	}
	
	private void initSphere(int rad)
	{
		sphereMaskIterator = new SphereBitmapImageSource_sphis(labelImage, rad , 2*rad+1); //+1 for symmetry
	}
	
	@Override
	public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel)
	{
		int fromLabel = contourParticle.label;
		double flow = CalculateCurvatureBasedGradientFlow(labelImage, contourPoint, fromLabel, toLabel);
		return new EnergyResult(flow, null);
	}

	double CalculateCurvatureBasedGradientFlow(LabelImage aLabelImage, 
			Point aIndex, int aFrom, int aTo) 
	{
		double result = sphereMaskIterator.GenerateData(aIndex, aFrom, aTo);
//		double result = sphereMaskIterator.GenerateData2(aIndex, aFrom, aTo);

		return result;
	}
		
	
	
	@Override
	public Object atStart()
	{
		// TODO Auto-generated method stub
		return null;
	}

//	@Override
//	public void settingsChanged(Settings settings)
//	{
//		initSphere((int)settings.m_CurvatureMaskRadius);
//	}


}
