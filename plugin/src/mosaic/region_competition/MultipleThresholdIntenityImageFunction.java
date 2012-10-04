package mosaic.region_competition;

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
		// Check bounds for intensity image
		if(!image.iterator.isInBound(p))
			return false;
		
		float value = image.get(p);
		return Evaluate(value);
	}
	
}