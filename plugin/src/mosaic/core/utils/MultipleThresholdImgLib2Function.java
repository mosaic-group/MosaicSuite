package mosaic.core.utils;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import mosaic.core.utils.Point;

/**
 * 
 * Basically is a binarized image view (with view we mean that the image is
 * not computed) of an image, based on the definition of intervals.
 *  
 *  1 = in the interval
 *  0 = outside the interval
 * 
 * A {@link MultipleThresholdImageFunction} convenience class to access a {@link LabelImage} 
 * 
 * @author Pietro Incardona
 * 
 */

public class MultipleThresholdImgLib2Function<T extends RealType<T>> extends MultipleThresholdImageFunction
{
	Img<T> labelImage;
	RandomAccess<T> raLb;
	
	public MultipleThresholdImgLib2Function(Img<T> aLabelImage) 
	{
		super();
		SetInputImage(aLabelImage);
//		m_NThresholds = 0;
//		m_Thresholds = new ArrayList<Pair<Double,Double>>();
	}
	
	void SetInputImage(Img<T> labelImage)
	{
		this.labelImage = labelImage;
	}
	
	@Override
	public boolean EvaluateAtIndex(int index)
	{
		int id[] = MosaicUtils.getCoord(index,labelImage);
		raLb.localize(id);
		double value = raLb.get().getRealDouble();
		
		return Evaluate(value);
	}
	
	/**
	 * With labelImage, it is guaranteed that index does not exceeds bounds
	 */
	@Override
	public boolean EvaluateAtIndex(Point p)
	{		
		for (int i = 0 ; i < labelImage.numDimensions() ; i++)
		{
			if (p.x[i] >= labelImage.dimension(i))
				return false;
		}
		
		raLb.localize(p.x);
		
		double value = raLb.get().getRealDouble();
		return Evaluate(value);
	}
}
