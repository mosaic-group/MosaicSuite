package mosaic.region_competition;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class UnitCubeCCCounter
{
	//TODO not used?
//	boolean[] m_NeighborhoodConnectivityTest;
	boolean[] m_ConnectivityTest;
	
	char[] m_Image;
	Connectivity TConnectivity;
	private int dimension;
	
	public UnitCubeCCCounter(Connectivity TConnectivity) 
	{
		this.TConnectivity = TConnectivity;
		this.dimension = TConnectivity.Dimension();
		//TODO überflüssig? (siehe setimage)
		m_Image = new char[TConnectivity.GetNeighborhoodSize()];
		
		
		//TODO not used?
//		m_NeighborhoodConnectivityTest = CreateConnectivityTest(TNeighborhoodConnectivity);
		m_ConnectivityTest = CreateConnectivityTest(TConnectivity);
	}
	
	void SetImage(char[] subImage)
	{
		m_Image=Arrays.copyOf(subImage, subImage.length);
	}
	
	int connectedComponents()
	{
		
		//TODO dummy variable
		
		Connectivity conn = TConnectivity;

        int neighborhoodSize = conn.GetNeighborhoodSize();
        int seed = 0;
        // Find first seed
        while (seed != neighborhoodSize &&
                (m_Image[seed] == 0 || !m_ConnectivityTest[seed])) {
            seed++;
        }

//        std::vector<bool> processed(neighborhoodSize, false);
        boolean[] vProcessed_new = new boolean[neighborhoodSize];
        Arrays.fill(vProcessed_new, false);

        int nbCC = 0;
        while (seed != neighborhoodSize) {
            ++nbCC;
            vProcessed_new[seed] = true;
            
            Queue<Integer> q = new LinkedList<Integer>();
            q.add(seed);

            while (!q.isEmpty()) {
                int current = q.poll();
                // For each neighbor check if m_UnitCubeNeighbors is true.
                for (int neighbor = 0; neighbor < neighborhoodSize; neighbor++) {
                    if (!vProcessed_new[neighbor] && m_Image[neighbor] != 0 &&
                            isUnitCubeNeighbors(conn, current, neighbor)) {
                            //TODO: rather than checking if m_Image[neighbor] != 0 one
                            //should check if m_Image[neighbor] == currentLabelvalue ???
                        q.add(neighbor);
                        vProcessed_new[neighbor] = true;
                    }
                }
            }

            // Look for next seed
            while (seed != neighborhoodSize &&
                    (vProcessed_new[seed] || m_Image[seed] == 0 || !m_ConnectivityTest[seed])) 
            {
                ++seed;
            }
        }
		vProcessed_new=null;
        return nbCC;
    
	}
	
	
	private boolean isUnitCubeNeighbors(Connectivity conn, int current, int neighbor)
	{
		Point pCurrent = ofsToPoint(current);
		Point pNeighbor = ofsToPoint(neighbor);
		Point diff = pCurrent.sub(pNeighbor);
		return conn.isInNeighborhood(diff);
	}
    
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
	
	public static int pointToOfs(Point p, int dim)
	{
		int offset = 0;
		int factor = 1;
		for(int i = 0; i < dim; ++i) {
			offset += factor * (p.x[i] + 1);
			factor *= 3;
		}

		return offset;
	}
	
    
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

	boolean[] CreateConnectivityTest(Connectivity TConnectivity) 
	{
		//TODO arg not necessary
		//TODO dummy cariable
		Connectivity connectivity = TConnectivity;
		//END dummy
		
        int neighborhoodSize = connectivity.GetNeighborhoodSize();
        boolean[] test = new boolean[neighborhoodSize];
        for (int i = 0; i < neighborhoodSize; ++i) {
        	// TODO directly isInNeighborhood(i);
        	//TODO debug
        	Point p = ofsToPoint(i);
        	boolean bool = connectivity.isInNeighborhood(p);
            test[i] = bool;
        }

        return test;
    }
	
	
    
}


class UnitCubeNeighbors
{
	boolean neighborsInUnitCube[][];
	
	public UnitCubeNeighbors(UnitCubeCCCounter unitCubeCCCounter, Connectivity connectivity, Connectivity neighborhoodConnectivity) 
	{
		int neighborhoodSize = connectivity.GetNeighborhoodSize();
		//TODO initialized?
		neighborsInUnitCube = new boolean[neighborhoodSize][neighborhoodSize];
		
		
		//TODO ist das nicht symmetrisch? --> nur bis zur hälfte berechnen, spiegeln
		for(int i=0; i<neighborhoodSize; i++)
		{
			Point p1 = unitCubeCCCounter.ofsToPoint(i);
			if(neighborhoodConnectivity.isInNeighborhood(p1))
			{
				for(int j=0; j<neighborhoodSize; j++)
				{
					Point p2 = unitCubeCCCounter.ofsToPoint(j);
					//TODO ??? why add?
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
				//TODO maybe fill with false?
			}
		}

	}
	
	
	
	
	
	
	
	
	
	
}









