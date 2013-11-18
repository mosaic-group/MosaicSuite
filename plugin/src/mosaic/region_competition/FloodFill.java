package mosaic.region_competition;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import mosaic.core.utils.Point;
import mosaic.region_competition.topology.Connectivity;


public class FloodFill implements Iterable<Point>
{
	Connectivity conn;
	
	Stack<Point> stack;
	Set<Point> checkedSet;
	
	public FloodFill(Connectivity conn, MultipleThresholdImageFunction foo, Point seed) 
	{
		this.conn = conn;
		
		stack = new Stack<Point>();
		checkedSet = new HashSet<Point>();
		stack.add(seed);
		
		while(!stack.isEmpty())
		{
			Point p = stack.pop();
			for(Point q: conn.iterateNeighbors(p))
			{
				if(foo.EvaluateAtIndex(q) && !isPointChecked(q))
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








