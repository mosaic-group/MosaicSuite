package mosaic.region_competition;

import java.util.Iterator;

public class Connectivity implements Iterator<Point>, Iterable<Point>
{
	private int cursor;
	
	static int neighbors[][] = {{-1,0}, {1, 0}, {0,-1}, {0, 1}};
	static private int size;
	static Point neighborsP[];
	static // void initPoints()
	{
		size=neighbors.length;
		neighborsP = new Point[size];
		neighborsP[0]= new Point(neighbors[0]);
		neighborsP[1]= new Point(neighbors[1]);
		neighborsP[2]= new Point(neighbors[2]);
		neighborsP[3]= new Point(neighbors[3]);
	}
	
	
	public Connectivity() 
	{
//		size=neighbors.length;
		cursor=0;
//		initPoints();
	}
	

	@Override
	public boolean hasNext() 
	{
		if(cursor<size) return true;
		else return false;
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

	@Override
	public Iterator<Point> iterator() {
		return this;
	}
}

