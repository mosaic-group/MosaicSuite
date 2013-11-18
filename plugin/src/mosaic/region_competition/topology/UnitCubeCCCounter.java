package mosaic.region_competition.topology;

import java.util.LinkedList;
import java.util.Queue;

import mosaic.core.utils.Point;

public class UnitCubeCCCounter
{
	boolean[] m_ConnectivityTest;
	
//	boolean offsetNeighbors[][];
	boolean m_UnitCubeNeighbors[][];
	
	char[] m_Image;
	Connectivity TConnectivity;
	Connectivity TNeighborhoodConnectivity;
	
	public UnitCubeCCCounter(Connectivity TConnectivity) 
	{
		this.TConnectivity = TConnectivity;
		this.TNeighborhoodConnectivity = TConnectivity.getNeighborhoodConnectivity();
		
		m_ConnectivityTest = CreateConnectivityTest(TConnectivity);
		
		m_UnitCubeNeighbors = initUnitCubeNeighbors(TConnectivity, 
				TNeighborhoodConnectivity);
	}
	
	
	/**
	 * Set the sub image (data of unitcube)
	 * midpoint has to be 0!
	 * @param data of unitcube as linear array
	 */
	public void SetImage(char[] subImage)
	{
		m_Image=subImage.clone();
	}
	
	
	/**
	 * @param conn
	 * @return Boolean array, entry at position <tt>i</tt> indicating 
	 * if offset i is in neighborhood for <tt>conn</tt>
	 */
	private static boolean[] CreateConnectivityTest(Connectivity conn) 
	{
        int neighborhoodSize = conn.GetNeighborhoodSize();
        boolean[] result = new boolean[neighborhoodSize];
        
		for(int i = 0; i < neighborhoodSize; i++) 
		{
			result[i] = conn.isNeighborhoodOfs(i);
		}
        return result;
    }
	
	
	public int connectedComponents()
	{

		int neighborhoodSize = TConnectivity.GetNeighborhoodSize();
		int seed = 0;
		// Find first seed
		while(seed != neighborhoodSize && (m_Image[seed] == 0 || !m_ConnectivityTest[seed])){
			++seed;
		}

		boolean vProcessed_new[] = new boolean[neighborhoodSize];
		Queue<Integer> q = new LinkedList<Integer>();

		int nbCC = 0;
		while(seed != neighborhoodSize)
		{
			++nbCC;
			vProcessed_new[seed] = true;

			q.clear();
			q.add(seed);

			while(!q.isEmpty())
			{
				int current = q.poll();

				// For each neighbor check if m_UnitCubeNeighbors is true.
				for(int neighbor = 0; neighbor < neighborhoodSize; ++neighbor)
				{
					if(!vProcessed_new[neighbor] && m_Image[neighbor] != 0 && m_UnitCubeNeighbors[current][neighbor])
					{
						q.add(neighbor);
						vProcessed_new[neighbor] = true;
					}
				}
			}

			// Look for next seed
			while(seed != neighborhoodSize && (vProcessed_new[seed] || m_Image[seed] == 0 || !m_ConnectivityTest[seed]))
			{
				++seed;
			}
		}
		return nbCC;
	}
	
	
	/**
	 * Computes the number of connected components in the subimage set with SetImage
	 * @return Number of connected components
	 */
	public int connectedComponentsSTS()
	{
		
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
					if(!visited[neighbor] && m_Image[neighbor] != 0 
							// statically or dynamically
//							&& isUnitCubeNeighbors(conn, current, neighbor)) 
						    && m_UnitCubeNeighbors[current][neighbor]) 
						
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
		
		Point pCurrent = conn.ofsIndexToPoint(current);
		Point pNeighbor = conn.ofsIndexToPoint(neighbor);
//		return conn.isNeighborhoodOfs(pCurrent.sub(pNeighbor));
		boolean onthefly = conn.areNeighbors(pCurrent, pNeighbor);
		
//		if(precalculated != onthefly)
//			System.out.println("precalculated and onthefly not the same");
		return onthefly;
		
	}

	/**
	 * Precalculates neighborhood within the unit cube and stores them into boolean array. 
	 * Access array by the integer offsets for the points to be checked. 
	 * Array at position idx1, idx2 is true, 
	 * if idx1, idx2 are unit cube neighbors with respect to their connectivities <br>
	 * <b>WARNING!</b> this does not generate symmetric solutions, but it's the lamy solution... 
	 * eg [0,3] is false but [3, 0] is true in ITK (3,2)
	 * @param connectivity Connectivity to be checked
	 * @param neighborhoodConnectivity Neighborhood connectivity. This has to be more lax (reach more neighbors) than connectivity
	 * @return 
	 */
	private static boolean[][] initUnitCubeNeighbors(Connectivity connectivity, Connectivity neighborhoodConnectivity)
	{
		int neighborhoodSize = connectivity.GetNeighborhoodSize();
		boolean neighborsInUnitCube[][] = new boolean[neighborhoodSize][neighborhoodSize];
		
		int dim = connectivity.getDim();

		for(int neighbor1 = 0; neighbor1 < neighborhoodSize; neighbor1++)
		{
			Point p1 = connectivity.ofsIndexToPoint(neighbor1);

			if(neighborhoodConnectivity.isNeighborhoodOfs(p1))
			{
				for(int neighbor2 = 0; neighbor2 < neighborhoodSize; neighbor2++)
				{
					Point p2 = connectivity.ofsIndexToPoint(neighbor2);

					Point sum = p1.add(p2);
					int sumOffset = connectivity.pointToOffset(sum);

					boolean inUnitCube = true;
					for(int d = 0; d < dim && inUnitCube; d++)
					{
						if(sum.x[d] < -1 || sum.x[d] > +1){
							inUnitCube = false;
						}
					}

					if(inUnitCube && connectivity.areNeighbors(p1, sum)){
						neighborsInUnitCube[neighbor1][sumOffset] = true;
					}
				}
			}
		}
		return neighborsInUnitCube;
	}
	
	/**
	 * Precalculation with non-lamy method. just checks if the both are connected
	 * @param connectivity
	 * @param neighborhoodConnectivity
	 * @return
	 */
	private static boolean[][] UnitCubeNeighborsSTS(
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
				if(areUnitCubeNeighbors 
						// TODO lamy solution doesn't seem to be symmetric
						// eg [0,3] is false but [3, 0] is true in ITK (3,2)
						// uncommenting following lines "swaps" in THIS solution exactly these values, 
						// that are non symmetric in LAMY
//						&& neighborhoodConnectivity.isNeighborhoodOfs(i)
//						&& neighborhoodConnectivity.isNeighborhoodOfs(j)
						)
				{
					neighborsInUnitCube[i][j] = true;
				} else {
					neighborsInUnitCube[i][j] = false;
				}
			}
		}

		return neighborsInUnitCube;

	}
	
	
	
	@Override
	public String toString()
	{
		String result="UnitCubeCCCounter " +
			TConnectivity + " " + 
			TNeighborhoodConnectivity;
		
		return result;
	}
	
	
	
	
	
	
	
	///////////////////////////// test ////////////////////////////////////////
	
	
	
	public static boolean test()
	{
		// this is the result of the itk code
		int itk3DFG[][] = {
				{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
				{1,0,1,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
				{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
				{1,0,0,0,1,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
				{0,1,0,1,0,1,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0},
				{0,0,1,0,1,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0},
				{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
				{0,0,0,0,1,0,1,0,1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0},
				{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
				{1,0,0,0,0,0,0,0,0,0,1,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0},
				{0,1,0,0,0,0,0,0,0,1,0,1,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0},
				{0,0,1,0,0,0,0,0,0,0,1,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0},
				{0,0,0,1,0,0,0,0,0,1,0,0,0,1,0,1,0,0,0,0,0,1,0,0,0,0,0},
				{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
				{0,0,0,0,0,1,0,0,0,0,0,1,0,1,0,0,0,1,0,0,0,0,0,1,0,0,0},
				{0,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,1,0,0,0,0,0,0,0,1,0,0},
				{0,0,0,0,0,0,0,1,0,0,0,0,0,1,0,1,0,1,0,0,0,0,0,0,0,1,0},
				{0,0,0,0,0,0,0,0,1,0,0,0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,1},
				{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
				{0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,1,0,1,0,1,0,0,0,0},
				{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
				{0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,1,0,1,0,0},
				{0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,1,0,1,0,1,0,1,0},
				{0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,1,0,1,0,0,0,1},
				{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
				{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,1,0,1,0,1},
				{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}};
		
		int itk3dbg[][] = {
				{0,1,0,1,1,0,0,0,0,1,1,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0},
				{1,0,1,1,1,1,0,0,0,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0},
				{0,1,0,0,1,1,0,0,0,0,1,1,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0},
				{1,1,0,0,1,0,1,1,0,1,1,0,1,1,0,1,1,0,0,0,0,0,0,0,0,0,0},
				{1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0},
				{0,1,1,0,1,0,0,1,1,0,1,1,0,1,1,0,1,1,0,0,0,0,0,0,0,0,0},
				{0,0,0,1,1,0,0,1,0,0,0,0,1,1,0,1,1,0,0,0,0,0,0,0,0,0,0},
				{0,0,0,1,1,1,1,0,1,0,0,0,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0},
				{0,0,0,0,1,1,0,1,0,0,0,0,0,1,1,0,1,1,0,0,0,0,0,0,0,0,0},
				{1,1,0,1,1,0,0,0,0,0,1,0,1,1,0,0,0,0,1,1,0,1,1,0,0,0,0},
				{1,1,1,1,1,1,0,0,0,1,0,1,1,1,1,0,0,0,1,1,1,1,1,1,0,0,0},
				{0,1,1,0,1,1,0,0,0,0,1,0,0,1,1,0,0,0,0,1,1,0,1,1,0,0,0},
				{1,1,0,1,1,0,1,1,0,1,1,0,0,1,0,1,1,0,1,1,0,1,1,0,1,1,0},
				{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
				{0,1,1,0,1,1,0,1,1,0,1,1,0,1,0,0,1,1,0,1,1,0,1,1,0,1,1},
				{0,0,0,1,1,0,1,1,0,0,0,0,1,1,0,0,1,0,0,0,0,1,1,0,1,1,0},
				{0,0,0,1,1,1,1,1,1,0,0,0,1,1,1,1,0,1,0,0,0,1,1,1,1,1,1},
				{0,0,0,0,1,1,0,1,1,0,0,0,0,1,1,0,1,0,0,0,0,0,1,1,0,1,1},
				{0,0,0,0,0,0,0,0,0,1,1,0,1,1,0,0,0,0,0,1,0,1,1,0,0,0,0},
				{0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,0,0,0,1,0,1,1,1,1,0,0,0},
				{0,0,0,0,0,0,0,0,0,0,1,1,0,1,1,0,0,0,0,1,0,0,1,1,0,0,0},
				{0,0,0,0,0,0,0,0,0,1,1,0,1,1,0,1,1,0,1,1,0,0,1,0,1,1,0},
				{0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,1},
				{0,0,0,0,0,0,0,0,0,0,1,1,0,1,1,0,1,1,0,1,1,0,1,0,0,1,1},
				{0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,1,1,0,0,0,0,1,1,0,0,1,0},
				{0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,0,0,0,1,1,1,1,0,1},
				{0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,1,1,0,0,0,0,1,1,0,1,0}};
		
		
		Connectivity fg =  new Connectivity(3, 2);
		Connectivity fgN = new Connectivity(3, 1);
		fgN = fg.getNeighborhoodConnectivity();
		Connectivity bg =  new Connectivity(3, 0);
		Connectivity bgN = new Connectivity(3, 0);
		bgN = bg.getNeighborhoodConnectivity();
		
		boolean[][] their;
		boolean[][] mine;
		boolean same;
		
		
		// FG
		mine = UnitCubeCCCounter.UnitCubeNeighborsSTS(fg, fgN);
//		mine = UnitCubeCCCounter.UnitCubeNeighbors(fg, fgN);
		their = UnitCubeCCCounter.initUnitCubeNeighbors(fg, fgN);
		
		System.out.println("FG mine");
		printArray(mine);
		System.out.println("FG their");
		printArray(their);
		
		same = compare(their, mine);
		if(same)
			System.out.println("FG was ok");
		else
			System.out.println("FG was BAAAAAD");
		
		
		// FG compare with itk
		// (3,2) (3,1) for FG seems to be correct
		System.out.println("compare theirs with itk");
		same = compare(intToBooleanArray(itk3DFG), their);
		System.out.println("compare theirs with itk");
		if(same)
			System.out.println("FG was ok");
		else
			System.out.println("FG was BAAAAAD");
		
		
		// BG
		boolean same2=true;
		mine = UnitCubeCCCounter.UnitCubeNeighborsSTS(bg, bgN);
//		mine = UnitCubeCCCounter.UnitCubeNeighbors(bg, bgN);
		their = UnitCubeCCCounter.initUnitCubeNeighbors(bg, bgN);
		System.out.println("bg mine");
		printArray(mine);
		System.out.println("bg their");
		printArray(their);
		
		same2 = compare(their, mine);
		if(same2)
			System.out.println("BG was ok");
		else
			System.out.println("BG was BAAAAAD");
		
		
		// compare with itk
		// (3,0) (3,0) for BG seems to be correct
		System.out.println("compare itk with theirs");
		same2 = compare(intToBooleanArray(itk3dbg), their);
		System.out.println("compare theirs with itk");
		if(same2)
			System.out.println("BG was ok");
		else
			System.out.println("BG was BAAAAAD");
		
		return same&&same2;
	}
	
	private static void printArray(boolean[][] array)
	{
		System.out.println("array "+array.length);
		
		for(int i=0; i<array.length; i++)
		{
			for(int j=0; j<array[i].length; j++)
			{
				System.out.println(i+"\t"+j+"\t" + (array[i][j] ? 1 : 0) );
			}
		}
	}
	
	private static boolean[][] intToBooleanArray(int[][] array)
	{
		boolean[][] b = new boolean[array.length][];
		
		for(int i=0; i<array.length; i++)
		{
			b[i] = new boolean[array[i].length];
			for(int j=0; j<array[i].length; j++)
			{
				b[i][j] = (array[i][j] == 1) ? true : false;
			}
		}
		
		return b;
	}
	
	private static boolean compare(boolean[][] their, boolean[][] mine)
	{
		boolean same = true;
		
		for(int i=0; i<their.length; i++)
		{
			for(int j=0; j<their[i].length; j++)
			{
				if(their[i][j]==mine[i][j]){
//					System.out.println("same");
				}
				else{
					System.out.println("different ("+i+" "+j+"): "+
							" their=" + ((their[i][j])? 1 : 0) +
							" mine=" + ((mine[i][j])? 1 : 0) + 
							"");
					same=false;
				}
			}
		} // for
		
		return same;
	}
}


