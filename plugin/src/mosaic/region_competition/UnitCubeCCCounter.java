package mosaic.region_competition;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class UnitCubeCCCounter
{
	//TODO not used?
	boolean[] m_NeighborhoodConnectivityTest;
	boolean[] m_ConnectivityTest;
	
	char[] m_Image;
	Connectivity TConnectivity;
	Connectivity TNeighborhoodConnectivity;
	private int dimension;
	
	public UnitCubeCCCounter(Connectivity TConnectivity, Connectivity TNeighborhoodConnectivity) 
	{
		this.TConnectivity = TConnectivity;
		this.TNeighborhoodConnectivity = TNeighborhoodConnectivity;
		
		this.dimension = TConnectivity.Dimension();
		
		//TODO m_NeighborhoodConnectivityTest not used?
		m_NeighborhoodConnectivityTest = CreateConnectivityTest(TNeighborhoodConnectivity);
		m_ConnectivityTest = CreateConnectivityTest(TConnectivity);
	}
	
	/**
	 * Set the sub image (data of unitcube)
	 * midpoint has to be 0!
	 * @param data of unitcube as linear array
	 */
	void SetImage(char[] subImage)
	{
		m_Image=Arrays.copyOf(subImage, subImage.length);
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

		boolean[] vProcessed_new = new boolean[neighborhoodSize];
		// TODO is fill necessary?
		Arrays.fill(vProcessed_new, false);
		int nbCC = 0;

		for(int i=0; i<neighborhoodSize; i++)
		{
			if(m_Image[i] == 0 || !m_ConnectivityTest[i] ||  vProcessed_new[i])
			{
				// i is not a seed
				continue;
			}
			
			int seed = i;
			
			nbCC++; 	// 
			vProcessed_new[seed] = true;

			Queue<Integer> q = new LinkedList<Integer>();
			q.add(seed);

			// "Floodfill" seed, setting pixels to processed 
			while(!q.isEmpty()) 
			{
				int current = q.poll();
				
				// For each pixel in subimage, check if it is a neighbor of current.
				for(int neighbor = 0; neighbor < neighborhoodSize; neighbor++) 
				{
					if(!vProcessed_new[neighbor] && m_Image[neighbor] != 0 && isUnitCubeNeighbors(conn, current, neighbor)) 
					{
						// TODO: rather than checking if m_Image[neighbor] != 0 one
						// should check if m_Image[neighbor] == currentLabelvalue ???
						q.add(neighbor);
						vProcessed_new[neighbor] = true;
					}
				}
			}

		}
		
		vProcessed_new = null;
		return nbCC;
	}
	
	
	int connectedComponentsOLD()
	{
		// TODO dummy variable.
		// TODO immer FG conn?
		//TODO merge "findseed-whileloops"
		
		Connectivity conn = TConnectivity;
		int neighborhoodSize = conn.GetNeighborhoodSize();
		int seed = 0;

		boolean[] vProcessed_new = new boolean[neighborhoodSize];
		// TODO is fill necessary?
		Arrays.fill(vProcessed_new, false);
		int nbCC = 0;

		// Find first seed
		while(seed != neighborhoodSize && (m_Image[seed] == 0 || !m_ConnectivityTest[seed])) {
			seed++;
		}
		
		// "Floodfill" seed, setting pixels to processed 
		while(seed < neighborhoodSize)
		{
			nbCC++;
			vProcessed_new[seed] = true;

			Queue<Integer> q = new LinkedList<Integer>();
			q.add(seed);

			while(!q.isEmpty()) 
			{
				int current = q.poll();
				
				// For each pixel in subimage, check if it is a neighbor of current.
				for(int neighbor = 0; neighbor < neighborhoodSize; neighbor++) 
				{
					if(!vProcessed_new[neighbor] && m_Image[neighbor] != 0 && isUnitCubeNeighbors(conn, current, neighbor)) 
					{
						// TODO: rather than checking if m_Image[neighbor] != 0 one
						// should check if m_Image[neighbor] == currentLabelvalue ???
						q.add(neighbor);
						vProcessed_new[neighbor] = true;
					}
				}
			}

			// Look for next seed (not connected with previous seed == unprocessed)
			while(seed != neighborhoodSize && (vProcessed_new[seed] || m_Image[seed] == 0 || !m_ConnectivityTest[seed])) {
				++seed;
			}
		}
		vProcessed_new = null;
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
		//TODO precalculate this for each combination of points
		
		Point pCurrent = ofsToPoint(current);
		Point pNeighbor = ofsToPoint(neighbor);
		Point diff = pCurrent.sub(pNeighbor);
		return conn.isInNeighborhood(diff);
	}
    
	
	
/**
 * Converts an offset in the context of the dimension of this UnitCube
 * @param p 	Point representing an offset
 * @return 		The same offset as integer index
 */
	public int pointToOfs(Point p)
	{
		int offset = 0;
		int factor = 1;
		for(int i = 0; i < this.dimension; ++i) {
			offset += factor * (p.x[i] + 1);
			factor *= 3;
		}

		return offset;
	}
	
//	public int pointToOfs(Point p)
//	{
//		return pointToOfs(p, this.dimension);
//	}
	
//	public static int pointToOfs(Point p, int dim)
//	{
//		int offset = 0;
//		int factor = 1;
//		for(int i = 0; i < dim; ++i) {
//			offset += factor * (p.x[i] + 1);
//			factor *= 3;
//		}
//
//		return offset;
//	}
	
    
	/**
	 * Converts an offset in the context of the dimension of this UnitCube
	 * @param offset Offset represented by an integer index
	 * @return The offset represented by a Point
	 */
	public Point ofsToPoint(int offset)
	{
		int remainder = offset;
		Point p = Point.PointWithDim(this.dimension);

		for(int i = 0; i < this.dimension; ++i) {
			p.x[i] = remainder % 3;
			remainder -= p.x[i];
			remainder /= 3;
			p.x[i]--;
		}

		return p;
	}

	/**
	 * @param TConnectivity
	 * @return Boolean array, entry at position <tt>i</tt> indicating if offset i is in neighborhood for <tt>TConnectivity</tt>
	 */
	private boolean[] CreateConnectivityTest(Connectivity TConnectivity) 
	{
		//TODO dummy variable
		Connectivity conn = TConnectivity;
		//END dummy
		
        int neighborhoodSize = conn.GetNeighborhoodSize();
        boolean[] test = new boolean[neighborhoodSize];
		for(int i = 0; i < neighborhoodSize; i++) 
		{
			// via point
//			Point p = ofsToPoint(i);
//			boolean bool = conn.isInNeighborhood(p);
//			test[i] = bool;
			
			//TODO to be tested
			//directly via index
			test[i] = conn.isInNeighborhood(i);
		}

        return test;
    }
    
	
	
	
	public static boolean [][] UnitCubeNeighbors(UnitCubeCCCounter unitCubeCCCounter, Connectivity connectivity, Connectivity neighborhoodConnectivity) 
	{
		int neighborhoodSize = connectivity.GetNeighborhoodSize();
		
		boolean neighborsInUnitCube[][];
		//TODO initialize?
		neighborsInUnitCube = new boolean[neighborhoodSize][neighborhoodSize];
		
		//TODO ist das nicht symmetrisch? --> nur bis zur haelfte berechnen, spiegeln
		for(int i=0; i<neighborhoodSize; i++)
		{
			Point p1 = unitCubeCCCounter.ofsToPoint(i);
			if(neighborhoodConnectivity.isInNeighborhood(p1))
			{
				for(int j=0; j<neighborhoodSize; j++)
				{
					Point p2 = unitCubeCCCounter.ofsToPoint(j);
					//TODO ??? why add? read itk UnitCubeNeighbors. dont understand
					Point sum = p1.add(p2);
					int sumOffset = unitCubeCCCounter.pointToOfs(sum);
					
					boolean inUnitCube = true;
					for(int dim=0; dim < connectivity.Dimension() && inUnitCube; dim++)
					{
						if (sum.x[dim] < -1 || sum.x[dim] > +1) {
							inUnitCube = false;
						}
					}
                    if (inUnitCube && connectivity.areNeighbors(p1, sum)) {
                        neighborsInUnitCube[i][sumOffset] = true;
					}
				}
			} 
			else 
			{
				// TODO maybe fill with false?
			}
		}
		
		return neighborsInUnitCube;

	}
	
	
	public boolean[][] UnitCubeNeighborsSTS(UnitCubeCCCounter unitCubeCCCounter, 
			Connectivity connectivity, Connectivity neighborhoodConnectivity)
	{

		int neighborhoodSize = connectivity.GetNeighborhoodSize();

		boolean neighborsInUnitCube[][];
		neighborsInUnitCube = new boolean[neighborhoodSize][neighborhoodSize];

		for(int i = 0; i < neighborhoodSize; i++) {
			for(int j = 0; j < neighborhoodSize; j++) {

				if(isUnitCubeNeighbors(connectivity, i, j)) {
					neighborsInUnitCube[i][j] = true;
				} else {
					neighborsInUnitCube[i][j] = false;
				}
			}
		}

		return neighborsInUnitCube;

	}
	
	
}


