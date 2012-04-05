package mosaic.region_competition;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;



class FloodFill implements Iterable<Point>
{
	
	LabelImage labelImage;
	
	Stack<Point> stack;
	Set<Point> checkedSet;
	
	public FloodFill(LabelImage labelImage, MultipleThresholdImageFunction foo, Point seed) 
	{
		this.labelImage = labelImage;
		
		stack = new Stack<Point>();
		checkedSet = new HashSet<Point>();
		
		//TODO what's about the seed point? set on fire / flood or not? test MultipleThresholdImageFunction?
		stack.add(seed);

		
		while(!stack.isEmpty())
		{
			Point p = stack.pop();
			
			for(Point q: labelImage.getConnFG().iterateNeighbors(p))
			{
				if(foo.EvaluateAtIndex(labelImage.iterator.pointToIndex(q)) && !isPointChecked(q))
				{
					stack.add(q);
				}
			}
			setPointChecked(p);
		}
		
	}
	
	private void setPointChecked(Point p)
	{
		checkedSet.add(p);
	}
	
	private boolean isPointChecked(Point p)
	{
		return checkedSet.contains(p);
	}

	
	
	//Iterable
	@Override
	public Iterator<Point> iterator()
	{
		return checkedSet.iterator();
	}
	
	
}








