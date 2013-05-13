package mosaic.region_competition.initializers;

import mosaic.region_competition.IntensityImage;
import mosaic.region_competition.LabelImage;

/*
 * Abstract initializer class for initializers that depend on the input image
 */
public abstract class DataDrivenInitializer extends Initializer
{
	IntensityImage intensityImage;
	public DataDrivenInitializer(IntensityImage intensityImage, LabelImage labelImage)
	{
		super(labelImage);
		this.intensityImage = intensityImage;
		// TODO Auto-generated constructor stub
	}
}
