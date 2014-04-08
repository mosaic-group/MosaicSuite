package mosaic.core.binarize;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import mosaic.core.utils.IntervalsListDouble;
import mosaic.core.utils.IntervalsListInteger;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Point;

/**
 * 
 * Basically is a binarized image view (with view we mean that the image is
 * not computed) of an image, based on the definition of intervals of an Integer
 * imgLib2 image
 *  
 *  1 = in the interval
 *  0 = outside the interval
 * 
 * A {@link MultipleThresholdImageFunction} convenience class to access a {@link LabelImage} 
 * 
 * @author Pietro Incardona
 * 
 */

public class BinarizedIntervalImgLib2Double<T extends RealType<T>> extends IntervalsListDouble implements BinarizedImage
{
	Img<T> labelImage;
	RandomAccess<T> raLb;
	
	public BinarizedIntervalImgLib2Double(Img<T> aLabelImage) 
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
	
	/**
	 * 
	 * Evaluate the binarized image at index
	 * 
	 * @param index where to evaluate
	 * @return true or false
	 * 
	 */
	
	@Override
	public boolean EvaluateAtIndex(int index)
	{
		int id[] = MosaicUtils.getCoord(index,labelImage);
		raLb.localize(id);
		double value = raLb.get().getRealDouble();
		
		return Evaluate(value);
	}
	

	/**
	 * 
	 * Evaluate the binarized image at Point p
	 * 
	 * @param p Point where to evaluate
	 * @return true or false
	 * 
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


