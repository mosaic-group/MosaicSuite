package mosaic.region_competition.initializers;

import mosaic.region_competition.LabelImage;

/**
 * Abstract Initializer class that do not depend on input image
 */
public abstract class Initializer
{
	LabelImage labelImage;
	int dim;
	int[] dimensions;
	
	public Initializer(LabelImage labelImage)
	{
		setLabelImage(labelImage);
	}
	
	public void setLabelImage(LabelImage labelImage)
	{
		this.labelImage = labelImage;
		this.dim = labelImage.getDim();
		this.dimensions = labelImage.getDimensions();
		
	}
	public abstract void initDefault();
}
