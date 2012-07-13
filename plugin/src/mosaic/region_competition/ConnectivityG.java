package mosaic.region_competition;

import java.util.ArrayList;
import java.util.Iterator;

public class ConnectivityG<T extends Point> implements Iterable<T>
{
	PointFactoryInterface<T> factory;
	
	private int VDim;				// dimension
	private int VCellDim;			// connectivity
	private int m_NeighborhoodSize;	// complete neighborhood (size of unitcube)
	
	private int nNeighbors;				// m_NumberOfNeighbors
	private ArrayList<T> neighborsP;
	private int[] neighborsOfs;
	
	public ConnectivityG(int VDim, int VCellDim, PointFactoryInterface<T> factory) 
	{
		this.factory=factory;
		this.VDim = VDim;
		this.VCellDim = VCellDim;
		
		m_NeighborhoodSize = (int)Math.pow(3, VDim);
		nNeighbors = ComputeNumberOfNeighbors();
		
		neighborsP = new ArrayList<T>(nNeighbors); 
		neighborsOfs = new int[nNeighbors]; //TODO not used yet
		
		initOffsets();
	}

	
	/**
	 * @return Number of neighbors (Number of points connected to the midpoint)
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
	 * Calculate the offsets of neighbors for the connectivity specified in the constructor 
	 * and saves them in the form of Point offsets (neighborsP) and integer offsets (neighborsOfs)
	 */
	private void initOffsets()
	{
		int currentNbNeighbors = 0;

		for(int i = 0; i < m_NeighborhoodSize; ++i) 
		{
			T p = OffsetToPoint(i);

			int numberOfZeros = countZeros(p);

			if(numberOfZeros != VDim && numberOfZeros >= VCellDim) 
			{
				neighborsP.set(currentNbNeighbors, p); // [currentNbNeighbors] = p;
				neighborsOfs[currentNbNeighbors] = i;
				currentNbNeighbors++;
			}
		}
	}
	

	/**
	 * @param ofs Point representing offset to midpoint
	 * @return true if ofs is in neighborhood
	 */
	public boolean isNeighborhoodOfs(T ofs)
	{
		for(T p : neighborsP) 
		{
			if(ofs.equals(p)) {
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * @param ofs Offset represented as integer offset (in the context of a unit cube)
	 * @return true if ofs is in neighborhood
	 */
	public boolean isNeighborhoodOfs(int ofs)
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
	
	
	/**
	 * Checks if two points are neighbors in this connectivity
	 * Dimensions of the points have to match 
	 * the dimension of the connectivity (VDim) 
	 * 
	 * @param p1 Arbitrary point
	 * @param p2 Arbitrary point
	 * @return true, if they are neighbors
	 */
	public boolean areNeighbors(T p1, T p2)
	{
		//TODO auskommentiert
		throw new Error("Auskommentiert");
//		return isNeighborhoodOfs(p1.sub(p2));
	}
	
	
	/**
	 * Converts an integer offset to a point offset
	 * @param offset integer value in [0, m_NeighborhoodSize] 
	 * @return Point/Vector representation of the offset (e.g [-1,-1] for the upper left corner in 2D)
	 */
	public T OffsetToPoint(int offset)
	{
		int remainder = offset;
//		T p =  Point.PointWithDim(this.VDim);
//		T p = factory.factory(VDim);
//		int x[] = p.x;
		int x[] = new int[VDim];
	
		for(int i = 0; i < VDim; ++i) 
		{
			x[i] = remainder % 3;		// x for this dimension
			remainder -= x[i]; 		// 
			remainder /= 3;				// get rid of this dimension
			x[i]--;					// x would have range [0, 1, 2]
										// but we want ofs from midpoint [-1,0,1]
		}
		
		return factory.pointFromArray(x);
	}

	/**
	 * @return Number of elements in the unitcube
	 */
	public int GetNeighborhoodSize()
	{
		return m_NeighborhoodSize;
	}

	/**
	 * @return Dimension of the unitcube
	 */
	public int Dimension()
	{
		return VDim;
	}

	/**
	 * Counts the number of zeros in the coordinates of a Point
	 * @param p A point representing an offset to the midpoint
	 * @return number of zeros
	 */
	private int countZeros(T p)
	{
		int count=0;
		for(int i: p.x)
		{
			if(i==0) count++;
		}
		return count;
	}

	
	/**
	 * computes the factorial of n
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
	 * Iterates through the neighbors of the midpoint, represented as Point offsets
	 * @return Neighbors as Point offsets
	 */
	@Override
	public Iterator<T> iterator() {
		return new OfsIterator();
	}
	
	
	/**
	 * Iterates through the neighbors of Point p in the context of this connectivity
	 */
	Iterable<T> iterateNeighbors(final T p)
	{
		return new Iterable<T>() 
		{
			@Override
			public Iterator<T> iterator()
			{
				return new NeighborIterator(p);
			}
		};
	}
	
	
	/**
	 * Iterator class to iterate through the neighborhood, 
	 * returning <b>Point offsets</b> to the neighbors. <br>
	 * Doesn't allow to remove() an element. 
	 */
	class OfsIterator implements Iterator<T> 
	{
		private int cursor=0;
		
		@Override
		public boolean hasNext() {
			return (cursor < nNeighbors);
		}

		@Override
		public T next() {
			T result = neighborsP.get(cursor);
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
		T point;
		
		/**
		 * @param p Arbitrary Point p
		 */
		NeighborIterator(T p)
		{
			this.point=p;
		}
		
		@Override
		public T next() {
			T ofs = super.next();
			return (T) ofs.add(point);
		}
	}
	
}

