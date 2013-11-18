package mosaic.region_competition;

import mosaic.core.utils.Point;

/**
 * A {@link MultipleThresholdImageFunction} convenience class to access a {@link LabelImage} 
 */
public class MultipleThresholdLabelImageFunction extends MultipleThresholdImageFunction
{
	LabelImage labelImage;
	
	public MultipleThresholdLabelImageFunction(LabelImage aLabelImage) 
	{
		super();
		SetInputImage(aLabelImage);
//		m_NThresholds = 0;
//		m_Thresholds = new ArrayList<Pair<Double,Double>>();
	}
	
	void SetInputImage(LabelImage labelImage)
	{
		this.labelImage = labelImage;
	}
	
	@Override
	public boolean EvaluateAtIndex(int index)
	{
		double value = labelImage.getLabel(index);
		return Evaluate(value);
	}
	
	/**
	 * With labelImage, it is guaranteed that index does not exceeds bounds
	 */
	@Override
	public boolean EvaluateAtIndex(Point p)
	{
		if (labelImage.isOutOfBound(p) == true)
			return false;
		double value = labelImage.getLabel(p);
		return Evaluate(value);
	}
}