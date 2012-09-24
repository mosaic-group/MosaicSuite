package mosaic.region_competition;


/**
 *	Interface for a {@link MultipleThresholdFunction} to hold its own Image
 */
public abstract class MultipleThresholdImageFunction extends MultipleThresholdFunction
{
	public abstract boolean EvaluateAtIndex(int index);
	public abstract boolean EvaluateAtIndex(Point p);
}


