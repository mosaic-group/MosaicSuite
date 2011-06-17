package mosaic.region_competition;

public class ConnectivityOLD_2D_8 extends ConnectivityOLD
{
	static
	{
		int neighbors[][] = {
				{-1,-1}, {0,-1}, {1,-1}, 
				{-1, 0},         {1, 0},
				{-1, 1}, {0, 1}, {1, 1}};
		neighborsP = new Point[neighbors.length];
		neighborsP[0]= new Point(neighbors[0]);
		neighborsP[1]= new Point(neighbors[1]);
		neighborsP[2]= new Point(neighbors[2]);
		neighborsP[3]= new Point(neighbors[3]);
		neighborsP[4]= new Point(neighbors[4]);
		neighborsP[5]= new Point(neighbors[5]);
		neighborsP[6]= new Point(neighbors[6]);
		neighborsP[7]= new Point(neighbors[7]);
	}
}