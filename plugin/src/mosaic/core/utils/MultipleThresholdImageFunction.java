package mosaic.core.utils;

import mosaic.core.utils.Point;


/**
 *	Interface for a {@link MultipleThresholdFunction} to hold its own Image
 */
public abstract class MultipleThresholdImageFunction extends MultipleThresholdFunction
{
	public abstract boolean EvaluateAtIndex(int index);
	public abstract boolean EvaluateAtIndex(Point p);
}


