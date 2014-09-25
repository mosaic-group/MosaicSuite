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
	/**
	 * 
	 * Evaluate the value of the pixel at point index (index is linearized)
	 * 
	 * @param index id of the point
	 * @return true or false (1 or 0) of the binarization of the image
	 */
	public abstract boolean EvaluateAtIndex(int index);
	
	
	/**
	 * 
	 * Evaluate the value of the pixel at one point
	 * 
	 * @param p Point
	 * @return the value of the pixel 0 or 1 (true pr false)
	 */
	public abstract boolean EvaluateAtIndex(Point p);
}


