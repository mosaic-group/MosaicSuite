package mosaic.region_competition.initializers;

import mosaic.region_competition.LabelImage;

public class ZeroInitializer extends Initializer
{
	
	int value = 0;

	public ZeroInitializer(LabelImage labelImage)
	{
		super(labelImage);
	}

	@Override
	public void initDefault()
	{
		int size = labelImage.getSize();
		for(int i=0; i<size; i++)
		{
			labelImage.setLabel(i, value);
		}
	}
	
}
