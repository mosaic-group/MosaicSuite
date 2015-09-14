package mosaic.region_competition.initializers;

import mosaic.core.utils.IntensityImage;
import mosaic.region_competition.LabelImageRC;

/*
 * Abstract initializer class for initializers that depend on the input image
 */
abstract class DataDrivenInitializer extends Initializer
{
	IntensityImage intensityImage;
	public DataDrivenInitializer(IntensityImage intensityImage, LabelImageRC labelImage)
	{
		super(labelImage);
		this.intensityImage = intensityImage;
	}
}
