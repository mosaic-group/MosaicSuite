package mosaic.core.binarize;

import mosaic.core.utils.Point;


/**
 * 
 * This interface Binarize the original image
 *
 * @author Pietro Incardona
 */

public interface BinarizedImage
{
	public abstract boolean EvaluateAtIndex(int index);
	public abstract boolean EvaluateAtIndex(Point p);
}


