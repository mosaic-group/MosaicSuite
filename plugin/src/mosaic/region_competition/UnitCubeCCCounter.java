package mosaic.region_competition;

import java.util.LinkedList;
import java.util.Queue;

public class UnitCubeCCCounter
{
	//TODO not used?
	boolean[] m_NeighborhoodConnectivityTest;
	boolean[] m_ConnectivityTest;
	
	boolean offsetNeighbors[][];
	
	char[] m_Image;
	Connectivity TConnectivity;
	Connectivity TNeighborhoodConnectivity;
	private int dimension;
	
	public UnitCubeCCCounter(Connectivity TConnectivity, Connectivity TNeighborhoodConnectivity) 
	{
		this.TConnectivity = TConnectivity;
		this.TNeighborhoodConnectivity = TNeighborhoodConnectivity;
		
		this.dimension = TConnectivity.getDim();
		
		//TODO m_NeighborhoodConnectivityTest not used?
		m_NeighborhoodConnectivityTest = CreateConnectivityTest(TNeighborhoodConnectivity);
		m_ConnectivityTest = CreateConnectivityTest(TConnectivity);
		
//		offsetNeighbors = UnitCubeNeighbors(this, TConnectivity, TNeighborhoodConnectivity);
//		offsetNeighbors = UnitCubeNeighborsSTS(this, TConnectivity, TNeighborhoodConnectivity);
	}
	
	/**
	 * Set the sub image (data of unitcube)
	 * midpoint has to be 0!
	 * @param data of unitcube as linear array
	 */
	void SetImage(char[] subImage)
	{
		m_Image=subImage.clone();
	}
	
	/**
	 * Computes the number of connected components in the subimage set with SetImage
	 * @return Number of connected components
	 */
	int connectedComponents()
	{
		// TODO immer FG conn?
		//TODO neu geschrieben, noch korrekt?
		
		Connectivity conn = TConnectivity;
		int neighborhoodSize = conn.GetNeighborhoodSize();

		boolean[] visited = new boolean[neighborhoodSize];
//		java default: is initialized to false
//		Arrays.fill(vProcessed_new, false);
		
		Queue<Integer> q = new LinkedList<Integer>();
		
		int nbCC = 0;
		for(int i=0; i<neighborhoodSize; i++)
		{
			//find next seed
			if(m_Image[i] == 0 || !m_ConnectivityTest[i] || visited[i])
			{
				// i is not a seed
				// since it is BG || not a neighbor of midpoint|| already visited
				continue;
			}
			
			// i is a seed now
			int seed = i;
			visited[seed] = true;
			
			nbCC++; 	// increase number of connected components

			q.clear();
			q.add(seed);

			// "Floodfill" seed, setting visited pixels to processed 
			while(!q.isEmpty()) 
			{
				int current = q.poll();
				
				// For each pixel in subimage, check if it is a neighbor of current.
				for(int neighbor = 0; neighbor < neighborhoodSize; neighbor++) 
				{
					if(!visited[neighbor] && m_Image[neighbor] != 0 && isUnitCubeNeighbors(conn, current, neighbor)) 
					{
						// TODO: rather than checking if m_Image[neighbor] != 0 one
						// should check if m_Image[neighbor] == currentLabelvalue ???
						visited[neighbor] = true;
						q.add(neighbor);
					}
				}
			} //while

		}
		return nbCC;
	}
	
	
	/**
	 * Determines if two points are neighbors
	 * @param conn		Connectivity for which neighborhood should be determined
	 * @param current	Offset as integer index
	 * @param neighbor	An other offset as integer index
	 * @return			True, if the two Points are neighbors within the Connectivity
	 */
	private boolean isUnitCubeNeighbors(Connectivity conn, int current, int neighbor)
	{
		// precalculate this for each combination of points
		// precalculation is slower!
//		boolean precalculated = offsetNeighbors[current][neighbor];
		
		Point pCurrent = ofsIndexToPoint(current);
		Point pNeighbor = ofsIndexToPoint(neighbor);
//		return conn.isNeighborhoodOfs(pCurrent.sub(pNeighbor));
		boolean onthefly = conn.areNeighbors(pCurrent, pNeighbor);
		
//		if(precalculated != onthefly)
//			System.out.println("precalculated and onthefly not the same");
		return onthefly;
		
	}
    
	
	
/**
 * Converts an offset in the context of the dimension of this UnitCube
 * @param p 	Point representing an offset, eg. [-1,-1]
 * @return 		The same offset as integer index, eg. 0
 */
	protected int pointToOfs(Point p)
	{
		int offset = 0;
		int factor = 1;
		for(int i = 0; i < dimension; i++) {
			offset += factor * (p.x[i] + 1);
			factor *= 3;
		}
		return offset;
	}
	
    
	/**
	 * Converts an integer (midpoint-) offset to a Point offset
	 * @param offset integer value in [0, m_NeighborhoodSize] 
	 * @return Point/Vector representation of the offset (e.g [-1,-1] for the upper left corner in 2D)
	 */
	private Point ofsIndexToPoint(int offset)
	{
		return TConnectivity.ofsIndexToPoint(offset);
		
		// COPIED FROM Connectivity
//		int remainder = offset;
//		int x[] = new int[dimension];
//	
//		for(int i = 0; i < dimension; ++i) 
//		{
//			x[i] = remainder % 3;	// x for this dimension
//			remainder -= x[i]; 		// 
//			remainder /= 3;			// get rid of this dimension
//			x[i]--;					// x would have range [0, 1, 2]
//									// but we want ofs from midpoint [-1,0,1]
//		}
//		return Point.CopyLessArray(x);
	}
	
	/**
	 * Converts an integer (midpoint-) offset to a Point offset
	 * @param offset integer value in [0, m_NeighborhoodSize] 
	 * @return Point/Vector representation of the offset (e.g [-1,-1] for the upper left corner in 2D)
	 */
	private static Point ofsIndexToPoint(int offset, int dim)
	{
//		return TConnectivity.ofsIndexToPoint(offset);
		
		// COPIED FROM Connectivity
		int remainder = offset;
		int x[] = new int[dim];
	
		for(int i = 0; i < dim; ++i) 
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
	 * @param conn
	 * @return Boolean array, entry at position <tt>i</tt> indicating 
	 * if offset i is in neighborhood for <tt>conn</tt>
	 */
	private boolean[] CreateConnectivityTest(Connectivity conn) 
	{
        int neighborhoodSize = conn.GetNeighborhoodSize();
        boolean[] test = new boolean[neighborhoodSize];
		for(int i = 0; i < neighborhoodSize; i++) 
		{
			test[i] = conn.isNeighborhoodOfs(i);
		}

        return test;
    }
    
	
	// precalculate, but this is slower...
	public static boolean[][] UnitCubeNeighbors(UnitCubeCCCounter unitCubeCCCounter, Connectivity connectivity, Connectivity neighborhoodConnectivity) 
	{
		int neighborhoodSize = connectivity.GetNeighborhoodSize();
		
		boolean neighborsInUnitCube[][];
		neighborsInUnitCube = new boolean[neighborhoodSize][neighborhoodSize];
		
		for(int i=0; i<neighborhoodSize; i++)
		{
			Point p1 = unitCubeCCCounter.ofsIndexToPoint(i);
			if(neighborhoodConnectivity.isNeighborhoodOfs(p1))
			{
				for(int j=0; j<neighborhoodSize; j++)
				{
					Point p2 = unitCubeCCCounter.ofsIndexToPoint(j);
					//TODO ??? why add? read itk UnitCubeNeighbors. dont understand
					Point sum = p1.add(p2);
					int sumOffset = unitCubeCCCounter.pointToOfs(sum);
					
					boolean inUnitCube = true;
					int dim = connectivity.getDim();
					for(int d=0; d < dim && inUnitCube; d++)
					{
						if (sum.x[d] < -1 || sum.x[d] > +1) {
							inUnitCube = false;
						}
					}
                    if (inUnitCube && connectivity.areNeighbors(p1, sum)) {
                        neighborsInUnitCube[i][sumOffset] = true;
					}
				}
			} 
		}
		
		return neighborsInUnitCube;

	}
	
	
	public static boolean[][] UnitCubeNeighborsSTS(UnitCubeCCCounter unitCubeCCCounter, 
			Connectivity connectivity, Connectivity neighborhoodConnectivity)
	{

		int neighborhoodSize = connectivity.GetNeighborhoodSize();

		boolean neighborsInUnitCube[][];
		neighborsInUnitCube = new boolean[neighborhoodSize][neighborhoodSize];

		for(int i = 0; i < neighborhoodSize; i++) {
			for(int j = 0; j < neighborhoodSize; j++) {
				
				boolean areUnitCubeNeighbors = connectivity.areNeighbors(
						connectivity.ofsIndexToPoint(i), 
						connectivity.ofsIndexToPoint(j));
//				if(isUnitCubeNeighbors(connectivity, i, j)) {
				if(areUnitCubeNeighbors){
					neighborsInUnitCube[i][j] = true;
				} else {
					neighborsInUnitCube[i][j] = false;
				}
			}
		}

		return neighborsInUnitCube;

	}
	
	public static boolean test()
	{
		Connectivity fg = new Connectivity(2, 1);
		Connectivity bg = new Connectivity(2, 0);
		UnitCubeCCCounter ccc;
		ccc = new UnitCubeCCCounter(fg, bg);
		
		boolean[][] their;
		boolean[][] mine;
		boolean same;
		
		// FG BG
		their = UnitCubeCCCounter.UnitCubeNeighbors(ccc, fg, bg);
		mine = UnitCubeCCCounter.UnitCubeNeighborsSTS(ccc, fg, bg);
		same = compare(their, mine);
		
		if(same)
			System.out.println("first was ok");
		else
			System.out.println("first was BAAAAAD");
		
		// BG FG
		
		boolean same2=true;
		ccc = new UnitCubeCCCounter(bg, fg);
		their = UnitCubeCCCounter.UnitCubeNeighbors(ccc, bg, fg);
		mine = UnitCubeCCCounter.UnitCubeNeighborsSTS(ccc, bg, fg);
		same2 = compare(their, mine);
		
		if(same)
			System.out.println("second was ok");
		else
			System.out.println("second was BAAAAAD");
		
		return same&&same2;
	}
	
	private static boolean compare(boolean[][] their, boolean[][] mine)
	{
		boolean same = true;
		
		for(int i=0; i<their.length; i++)
		{
			for(int j=0; j<their[i].length; j++)
			{
				if(their[i][j]==mine[i][j])
				{
//					System.out.println("same");
				}
				else
				{
					System.out.println("different ("+i+" "+j+"): mine=" + mine[i][j] + " their=" + their[i][j]);
					same=false;
				}
			}
		}
		
		return same;
		
	}
	
}


