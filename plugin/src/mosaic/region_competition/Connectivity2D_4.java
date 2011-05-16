package mosaic.region_competition;

public class Connectivity2D_4 extends Connectivity
{
	static
	{
		int neighbors[][] = {{-1,0}, {1, 0}, {0,-1}, {0, 1}};
		neighborsP = new Point[neighbors.length];
		neighborsP[0]= new Point(neighbors[0]);
		neighborsP[1]= new Point(neighbors[1]);
		neighborsP[2]= new Point(neighbors[2]);
		neighborsP[3]= new Point(neighbors[3]);
	}
}