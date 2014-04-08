package mosaic.core.binarize;

import mosaic.core.binarize.BinarizedImage;
import mosaic.core.utils.IntervalsListInteger;
import mosaic.core.utils.LabelImage;
import mosaic.core.utils.Point;


/**
 * 
 * Basically is a binarized image view (with view we mean that the image is
 * not computed) of an image, based on the definition of intervals of a
 * LabelImage image
 *  
 *  1 = in the interval
 *  0 = outside the interval 
 *  
 * @author Stephan Semmler, refactored Pietro Incardona
 *
 */

public class BinarizeIntervalLabelImage extends IntervalsListInteger implements BinarizedImage
{
	LabelImage labelImage;
	
	public BinarizeIntervalLabelImage(LabelImage aLabelImage) 
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
		int value = labelImage.getLabel(index);
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
		int value = labelImage.getLabel(p);
		return Evaluate(value);
	}
}