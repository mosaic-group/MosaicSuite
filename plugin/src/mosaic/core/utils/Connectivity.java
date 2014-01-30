package mosaic.core.utils;

import java.util.ArrayList;
import java.util.Iterator;
import mosaic.core.utils.Point;

/**
 * 
 * Connectivity class, iterate across the neighborhood af a point
 * 
 * @author Stephan Semmler
 *
 */

public class Connectivity implements Iterable<Point>
{
	private int VDim;				// dimension
	private int VCellDim;			// connectivity
	private int m_NeighborhoodSize;	// complete neighborhood (size of unitcube)
	
	private int nNeighbors;				// m_NumberOfNeighbors
	private Point[] neighborsP;
	private int[] neighborsOfs;
	
	/**
	 * 
	 * 
	 * 
	 * @param VDim Cell dimension 2 for 2D, 3 for 3D , ..... and so on
	 * @param VCellDim type of connectyvity  2D, (1 is a 4 way 2, 8 way)
	 *        3D, (1, 8 way, 2 18 way, 3 26 way)
	 */
	public Connectivity(int VDim, int VCellDim) 
	{
		this.VDim = VDim;
		this.VCellDim = VCellDim;
		
		m_NeighborhoodSize = (int)Math.pow(3, VDim);
		nNeighbors = ComputeNumberOfNeighbors();
		
		neighborsP = new Point[nNeighbors]; 
		neighborsOfs = new int[nNeighbors];
		
		initOffsets();
	}
	
	
	private Connectivity neighborhoodConnectivity;
	private void initNeighborhoodConnectivity()
	{
		int VCellDimN = (VCellDim==0) ? 0 : VCellDim-1;
		neighborhoodConnectivity = new Connectivity(VDim, VCellDimN);
	}
	
	
	/**
	 * This is NOT the corresponding BG connectivity. 
	 * It is used for UnitCubeCCCounter and Topo numbers. 
	 * It returns a connectivity that's "one dimension more lax", 
	 * that is, reaches minimal more neighbors than THIS connectivity
	 * @return A new created NeighborhoodConnectivity
	 */
	public Connectivity getNeighborhoodConnectivity()
	{
		if(neighborhoodConnectivity==null)
		{
			initNeighborhoodConnectivity();
		}
		return neighborhoodConnectivity;
	}
	
	
	/**
	 * @return Corresponding foreground/background connectivity 
	 * such they are compatible (Jordan's theorem) <br>
	 * (d,d-1) if this is not, and (d,0) otherwise
	 */
	public Connectivity getComplementaryConnectivity()
	{
		//TODO zwischenspeichern
		
		if(VCellDim == VDim-1)
			return new Connectivity(VDim, 0);
		else
			return new Connectivity(VDim, VDim-1);
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
			Point p = ofsIndexToPoint(i);
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
	 * @param ofs Point representing offset to midpoint
	 * @return true if ofs is in neighborhood
	 */
	public boolean isNeighborhoodOfs(Point ofs)
	{
		for(Point p : neighborsP) 
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
	public boolean areNeighbors(Point p1, Point p2)
	{
		return isNeighborhoodOfs(p1.sub(p2));
	}
	
	
	/**
	 * Converts an integer (midpoint-) offset to a Point offset
	 * @param offset integer value in [0, m_NeighborhoodSize] 
	 * @return Point/Vector representation of the offset (e.g [-1,-1] for the upper left corner in 2D)
	 */
	public Point ofsIndexToPoint(int offset)
	{
		int remainder = offset;
		int x[] = new int[VDim];
	
		for(int i = 0; i < this.VDim; ++i) 
		{
			x[i] = remainder % 3;	// x for this dimension
			remainder -= x[i]; 		// 
			remainder /= 3;			// get rid of this dimension
			x[i]--;					// x would have range [0, 1, 2]
									// but we want ofs from midpoint [-1,0,1]
		}
		return Point.CopyLessArray(x);
	}
	
	/**
	 * Converts an a Point offset to an integer (midpoint-) offset <br>
	 * Inverse to ofsIndexToPoint()
	 * @param p Point offset
	 * @return Integer offset
	 */
	public int pointToOffset(Point p)
	{
		 int offset=0;
		 int factor=1;
		 for(int i=0; i<VDim; i++)
		 {
			 offset += factor * (p.x[i]+1);
			 factor*=3;
		 }
		 return offset;
	}

	/**
	 * @return Number of elements in the unit cube
	 */
	public int GetNeighborhoodSize()
	{
		return m_NeighborhoodSize;
	}
	
	/**
	 * @return The number of actual neighbors of the connectivity
	 */
	public int getNNeighbors()
	{
		return nNeighbors;
	}

	/**
	 * @return Dimension of the unitcube
	 */
	public int getDim()
	{
		return VDim;
	}

	
	/**
	 * Counts the number of zeros in the coordinates of a Point
	 * @param p A point representing an offset to the midpoint
	 * @return number of zeros
	 */
	private static int countZeros(Point p)
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
	private static int factorial(int n)
	{
		int fac = 1;
		for(int i = 1; i <= n; i++) {
			fac = i * fac;
		}
		return fac;
	}
	
	@Override
	public String toString()
	{
		String result = "Connectivity ("+VDim+", "+VCellDim+")";
		result = result + " = " + nNeighbors + "-Connectivity";
		return result;
	}
	
	
	///////////////////// Iterators /////////////////////////////

	
	/**
	 * Iterates over the neighbors of the midpoint, represented as Point offsets
	 * @return Neighbors as Point offsets
	 */
	@Override
	public Iterator<Point> iterator() {
		return new OfsIterator();
	}
	
	public Iterable<Integer> itOfsInt()
	{
		return new Iterable<Integer>() {
			
			@Override
			public Iterator<Integer> iterator()
			{
				return new OfsIteratorInt();
			}
		};
	}
	
	/**
	 * Iterates over the neighbors of Point p in the context of this connectivity
	 */
	public Iterable<Point> iterateNeighbors(final Point p)
	{
		return new Iterable<Point>() 
		{
			@Override
			public Iterator<Point> iterator()
			{
				return new NeighborIterator(p);
			}
		};
	}
	
	
	/**
	 * Iterator class to iterate over the neighborhood, 
	 * returning <b>Point offsets</b> to the neighbors. <br>
	 * Doesn't allow to remove() an element. 
	 */
	private class OfsIterator implements Iterator<Point> 
	{
		private int cursor=0;
		
		@Override
		public boolean hasNext() {
			return (cursor < nNeighbors);
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
	
	private class OfsIteratorInt 
		implements Iterator<Integer> 
	{
		private int cursor=0;
		
//		@Override
		public boolean hasNext() {
			return (cursor < nNeighbors);
		}

//		@Override
		public Integer next() {
			int result = neighborsOfs[cursor];
			cursor++;
			return result;
		}

//		@Override
		public void remove() {
			// do nothing
		}
	}
	
	
	/**
	 * Iterator class to iterate through neighbors of a point
	 */
	private class NeighborIterator extends OfsIterator
	{
		private Point point;
		
		/**
		 * @param p Arbitrary Point p
		 */
		private NeighborIterator(Point p){
			this.point=p;
		}
		
		@Override
		public Point next() {
			Point ofs = super.next();
			return point.add(ofs);
		}
	}
	
	
	
	private static ArrayList<Connectivity[]> connectivities = new ArrayList<Connectivity[]>();
	/**
	 * @param VDim
	 * @param VCellDim
	 * @return Singleton connectivity of type (VDim, VCellDim)
	 */
	public static Connectivity getConnectivity(int VDim, int VCellDim)
	{
		// ensure there is place for connectivities of dimension VDim 
		connectivities.ensureCapacity(VDim+1);
		
		int size = connectivities.size();
		while(size<=VDim)
		{
			size++;
			connectivities.add(new Connectivity[size]);
		}
		
		Connectivity[] conns = connectivities.get(VDim);
		Connectivity conn = conns[VCellDim];
		
		if(conn==null){
			conn = new Connectivity(VDim, VCellDim);
			conns[VCellDim]=conn;
		}
		return conn;
	}
	
	
	
	public static boolean test()
	{
		Connectivity c;
		c = Connectivity.getConnectivity(1, 1);
		c = Connectivity.getConnectivity(3, 2);
		c = Connectivity.getConnectivity(3, 2);
		c = Connectivity.getConnectivity(3, 1);
		c = Connectivity.getConnectivity(3, 0);
		c = Connectivity.getConnectivity(2, 2);
		c = Connectivity.getConnectivity(2, 2);
		c = Connectivity.getConnectivity(2, 1);
		c = Connectivity.getConnectivity(0, 0);
		
		System.out.println(c);
		
		return true;
	}
	
}

