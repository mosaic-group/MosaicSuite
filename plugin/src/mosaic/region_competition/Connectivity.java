package mosaic.region_competition;

import java.util.Iterator;

public class Connectivity implements Iterable<Point>
{
	private int VDim;				// dimension
	private int VCellDim;			// connectivity
	private int m_NeighborhoodSize;	// complete neighborhood (size of unitcube)
	
	private int nNeighbors;				// m_NumberOfNeighbors
	private Point[] neighborsP;
	private int[] neighborsOfs;
	
	public Connectivity(int VDim, int VCellDim) 
	{
		this.VDim = VDim;
		this.VCellDim = VCellDim;
		
		m_NeighborhoodSize = (int)Math.pow(3, VDim);
		nNeighbors = ComputeNumberOfNeighbors();
		
		neighborsP = new Point[nNeighbors]; 
		neighborsOfs = new int[nNeighbors]; //TODO not used yet
		
		initOffsets();
		
	}

	/**
	 * Calculate the offsets of neighbors for the connectivity specified in the constructor 
	 */
	private void initOffsets()
	{
		int currentNbNeighbors = 0;

		for(int i = 0; i < m_NeighborhoodSize; ++i) 
		{
			Point p = OffsetToPoint(i);

			int numberOfZeros = countZeros(p);

			if(numberOfZeros != VDim && numberOfZeros >= VCellDim) 
			{
				neighborsP[currentNbNeighbors] = p;
				neighborsOfs[currentNbNeighbors] = i;
				currentNbNeighbors++;
			}
		}

	}
	
	/**
	 * @return Number of neighbors
	 */
	private int ComputeNumberOfNeighbors()
	{
		int numberOfNeighbors = 0;
		for(int i = VCellDim; i <= VDim - 1; ++i) 
		{
			numberOfNeighbors += 
				factorial(VDim)/(factorial(VDim - i) * factorial(i)) * (1<<(VDim-i));
		}

		return numberOfNeighbors;
	}

	

	/**
	 * @param ofs Point representing offset to midpoint
	 * @return 
	 */
	public boolean isInNeighborhood(Point ofs)
	{
		for(Point p : neighborsP) 
		{
			if(ofs.equals(p)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isInNeighborhood(int ofs)
	{
		//TODO to be tested
		for(int idx : neighborsOfs) 
		{
			if(ofs==idx) {
				return true;
			}
		}
		return false;
	}
	
	
	
	public boolean areNeighbors(Point p1, Point p2)
	{
		return isInNeighborhood(p1.sub(p2));
	}
	
	
	public Point OffsetToPoint(int offset)
	{
		//TODO !!! duplicated in unitCube
		
		int remainder = offset;
		Point p = Point.PointWithDim(this.VDim);
	
		for(int i = 0; i < this.VDim; ++i) {
			p.x[i] = remainder % 3;
			remainder -= p.x[i];
			remainder /= 3;
			p.x[i]--;
		}
	
		return p;
	}

	public int GetNeighborhoodSize()
	{
		return m_NeighborhoodSize;
	}

	public int Dimension()
	{
		return this.VDim;
	}

	/**
	 * @param p Neighbor offset
	 * @return number of Zeros in the coordinates of Point p
	 */
	private int countZeros(Point p)
	{
		int count = 0;
		for(int i: p.x)
		{
			if(i==0) count++;
		}
		return count;
	}

	
	/**
	 * computes factorial of n
	 * @param n 
	 * @return n!
	 */
	private int factorial(int n)
	{
		int fac = 1;
		for(int i = 1; i <= n; i++) {
			fac = i * fac;
		}
		return fac;
	}

	
	/**
	 * Iterates through the offsets of the neighbors of this connectivity
	 */
	@Override
	public Iterator<Point> iterator() {
		return new OfsIterator();
	}
	
	/**
	 * Iterates through the neighbors of Point point in the context of this connectivity
	 */
	Iterable<Point> itNeighborsOf(final Point point)
	{
		return new Iterable<Point>() 
		{
			@Override
			public Iterator<Point> iterator()
			{
				return new NeighborIterator(point);
			}
		};
	}
	
	/**
	 * Iterator class to iterate through the offsets of the neighbors. 
	 */
	class OfsIterator implements Iterator<Point> 
	{
		
		private int cursor=0;
		
		@Override
		public boolean hasNext() {
			if (cursor < nNeighbors)
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
			// do nothing
		}

	}
	
	/**
	 * Iterator class to iterate through neighbors of a point
	 */
	class NeighborIterator extends OfsIterator
	{
		Point point;
		NeighborIterator(Point p)
		{
			this.point=p;
		}
		
		@Override
		public Point next() {
			Point ofs = super.next();
			return point.add(ofs);
		}
	}
	
	

	
//	Iterable<Point> itNeighborsOf(final Point point)
//	{
//		return new ConnectivityNeighborIterable(point);
//	}
	
//	/**
//	 * Iterates through neighbors and returns Points 
//	 * @author Stephan
//	 */
//	class ConnectivityNeighborIterable implements Iterable<Point>
//	{
//		Point point;
//		public ConnectivityNeighborIterable(Point p) 
//		{
//			point = p;
//		}
//		@Override
//		public Iterator<Point> iterator() 
//		{
//			return new NeighborIterator(point);
//		}
//	}
	
}

