package mosaic.region_competition;

import java.util.Iterator;

public abstract class Connectivity implements Iterable<Point>
{
	private int size;
	static Point neighborsP[];
	
//	private Iterator<Point> ofsIterator;
//	private Iterator<Point> neighborIterator;
	
	// in subclasses
	//	static
	//	{
	//		int neighbors[][] = {{-1,0}, {1, 0}, {0,-1}, {0, 1}};
	//		neighborsP = new Point[neighbors.length];
	//		neighborsP[0]= new Point(neighbors[0]);
	//		neighborsP[1]= new Point(neighbors[1]);
	//		neighborsP[2]= new Point(neighbors[2]);
	//		neighborsP[3]= new Point(neighbors[3]);
	//	}
	
	
	public Connectivity() 
	{
		size=neighborsP.length;
//		ofsIterator = new OfsIterator();
	}

	@Override
	public Iterator<Point> iterator() {
		return new OfsIterator();
	}
	
	Iterable<Point> getNeighborIterable(Point point)
	{
		return new ConnectivityNeighborIterable(point);
	}
	
	/**
	 * Iterates through all neighbors and returns offsets
	 * @author Stephan
	 */
	class OfsIterator implements Iterator<Point> {
		
		private int cursor=0;
		
		@Override
		public boolean hasNext() {
			if (cursor < size)
				return true;
			else
				return false;
		}

		@Override
		public Point next() {
			Point result = neighborsP[cursor];
			cursor++;
			return result;
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub
			// do nothing
		}

	}
	
	/**
	 * Iterates through neighbors and returns Points 
	 * @author Stephan
	 */
	class ConnectivityNeighborIterable implements Iterable<Point>
	{
		Point point;
		public ConnectivityNeighborIterable(Point p) 
		{
			point = p;
		}
		@Override
		public Iterator<Point> iterator() 
		{
			return new NeighborIterator();
		}
		
		
		//TODO now thats ugly. inner inner class inheriting from inner class
		class NeighborIterator extends OfsIterator
		{
			@Override
			public Point next() {
				Point ofs = super.next();
				return point.add(ofs);
			}
		}
		
	}
	
}

