package mosaic.region_competition.initializers;

import mosaic.region_competition.LabelImageRC;

/**
 * Abstract Initializer class that do not depend on input image
 */
abstract class Initializer
{
	LabelImageRC labelImage;
	int dim;
	int[] dimensions;
	
	public Initializer(LabelImageRC labelImage)
	{
		setLabelImage(labelImage);
	}
	
	public void setLabelImage(LabelImageRC labelImage)
	{
		this.labelImage = labelImage;
		this.dim = labelImage.getDim();
		this.dimensions = labelImage.getDimensions();
		
	}
	public abstract void initDefault();
}
