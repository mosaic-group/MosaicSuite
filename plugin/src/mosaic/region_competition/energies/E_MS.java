package mosaic.region_competition.energies;

import java.util.HashMap;

import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.IntensityImage;
import mosaic.region_competition.LabelImage;
import mosaic.region_competition.LabelInformation;
import mosaic.region_competition.Point;
import mosaic.region_competition.energies.Energy.ExternalEnergy;

public class E_MS extends ExternalEnergy
{
	public E_MS(LabelImage labelImage, IntensityImage intensityImage, HashMap<Integer, LabelInformation> labelMap)
	{
		super(labelImage, intensityImage);
		this.labelMap = labelMap;
	}

	private HashMap<Integer, LabelInformation> labelMap;
	
	/**
	 * Here we have the possibility to either put the current pixel
	 * value to the BG, calculate the BG-mean and then calculate the
	 * squared distance of the pixel to both means, BG and the mean
	 * of the region (where the pixel currently still belongs to).
	 *
	 * The second option is to remove the pixel from the region and
	 * calculate the new mean of this region. Then compare the squared
	 * distance to both means. This option needs a region to be larger
	 * than 1 pixel/voxel.
	 */
	@Override
	public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel)
	{
		int fromLabel = contourParticle.label;
		float aValue = contourParticle.intensity;

		LabelInformation to = labelMap.get(toLabel);
		LabelInformation from = labelMap.get(fromLabel);
		
//		if(from.var<0)
//			from.var=0;
//		if(to.var<0)
//			to.var=0;
//		
		double aNewToMean = (to.mean*to.count + aValue)/(to.count+1);
		final double M_PI = Math.PI;
		double energy =  (aValue-aNewToMean)*(aValue-aNewToMean)/(2.0*to.var) 
				+ 0.5 * Math.log(2.0*M_PI*to.var) 
				- (	(aValue-from.mean)*(aValue-from.mean)/(2.0*from.var) + 0.5*Math.log(2.0*M_PI*from.var) );
		
		if(Double.isNaN(energy)|| Double.isInfinite(energy))
		{
			// this will be due to negative variances in the log. 
			// and this is a numerical issue for labels with small number of pixels
			System.out.println("MS energy is nan");
//			return new EnergyResult(0, false);
			throw new RuntimeException("energy in E_MS is NaN");
		}
		return new EnergyResult(energy, false);
	}

	@Override
	public Object atStart()
	{
		// TODO Auto-generated method stub
		return null;
	}

}



