package mosaic.region_competition.energies;

import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.Point;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.LabelImageRC;


public abstract class Energy
{
	public abstract Object atStart();
	
	/**
	 * @return EnergyResult, entries (energy or merge) are null if not calculated by this energy
	 */
	public abstract EnergyResult CalculateEnergyDifference(Point contourPoint, 
			ContourParticle contourParticle, int toLabel);

	public static class EnergyResult
	{
		public EnergyResult(Double energy, Boolean merge)
		{
			this.energyDifference = energy;
			this.merge = merge;
		}
		public Double energyDifference;
		public Boolean merge;
	}

	/**
	 * Responsible for regularization
	 * Independent of image I
	 */
	static abstract class InternalEnergy extends Energy
	{
		protected LabelImageRC labelImage;
		public InternalEnergy(LabelImageRC labelImage)
		{
			this.labelImage = labelImage;
		}
	}

	/**
	 * Responsible for data fidelity
	 */
	static abstract class ExternalEnergy extends Energy
	{
		protected IntensityImage intensityImage;
		protected LabelImageRC labelImage;
		public ExternalEnergy(LabelImageRC labelImage, IntensityImage intensityImage)
		{
			this.labelImage = labelImage;
			this.intensityImage = intensityImage;
		}
	}
}

