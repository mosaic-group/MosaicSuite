package mosaic.core.utils;

import mosaic.core.utils.Point;

public class MultipleThresholdIntenityImageFunction extends MultipleThresholdImageFunction
{
	
	IntensityImage image;

	public MultipleThresholdIntenityImageFunction(IntensityImage image)
	{
		this.image = image;
	}
	@Override
	public boolean EvaluateAtIndex(int index)
	{
		//Check bounds for intensity image
		if(index<0 || index >= image.dataIntensity.length)
			return false;
		
		float value = image.get(index);
		return Evaluate(value);
	}

	@Override
	public boolean EvaluateAtIndex(Point p)
	{
		if (image.isOutOfBound(p) == true)
			return false;
		float value = image.get(p);
		return Evaluate(value);
	}
	
}