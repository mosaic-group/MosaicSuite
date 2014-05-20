package mosaic.region_competition.energies;

import ij.measure.Calibration;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.LabelImageRC;
import mosaic.core.utils.Point;
import mosaic.region_competition.energies.Energy.InternalEnergy;

public class E_CurvatureFlow extends InternalEnergy// implements SettingsListener
{
	
	CurvatureBasedFlow curv;
	
	
	public E_CurvatureFlow(LabelImageRC labelImage, int rad, Calibration cal)
	{
		super(labelImage);
		curv = new CurvatureBasedFlow(rad, labelImage,cal);
	}
	
	@Override
	public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel)
	{
		int fromLabel = contourParticle.label;
		double flow = curv.generateData(contourPoint, fromLabel, toLabel);
		return new EnergyResult(flow, null);
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





