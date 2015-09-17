package mosaic.ia.nn;

import ij.ImagePlus;

import java.util.Vector;

import javax.vecmath.Point3d;

//import mosaic.core.detection.Particle;

public class DistanceCalculationsCoords extends DistanceCalculations {
	
	
	public DistanceCalculationsCoords(Point3d []X, Point3d [] Y,ImagePlus mask,double xmin,double ymin,double zmin, double xmax,double ymax, double zmax, double gridSize,double kernelWeightq,int discretizationSize) {
		super(mask, gridSize,kernelWeightq,discretizationSize);
		this.X=X;
		this.Y=Y;
		x1=xmin;
		y1=ymin;
		z1=zmin;
		y2=ymax;
		x2=xmax;
		z2=zmax;
		
	}
	private Point3d [] X, Y; //unfiltered points
	private double x1,x2,y1,y2,z1,z2; //ask for users input, if no mask. currently, force mask for csv.
	boolean boundarySet=false;
	@Override
	public void calcDistances() {
		
		particleXSetCoord=applyMaskandgetCoordinates(X);
		particleYSetCoord=applyMaskandgetCoordinates(Y);
		if (boundarySet==true)
		{
			particleXSetCoord=applyBoundaryandgetCoordinates(particleXSetCoord);
			particleYSetCoord=applyBoundaryandgetCoordinates(particleYSetCoord);
		}
	//	DGrid=genD_grid();
		 genStateDensityForCoords();
		calcD();
		
		
	}
	private void  genStateDensityForCoords()
	{
		 stateDensity(x1,y1,z1,x2,y2,z2);
	}
//	private float[] genD_grid() {
//		
//			return genCubeGridDist(x1,y1,z1,x2,y2,z2);
//		
//	}
	
	
	
	private boolean isInsideBoundary(double [] coords)
	{
		
		
			if (coords[0]>=x1 && coords[0]<=x2 && coords[1]>=y1 && coords[1]<=y2 && coords[2]>=z1 && coords[2]<=z2)
			{
				
			           return true;
			}
			return false;
	}
	
	 
	 private  Point3d[] applyBoundaryandgetCoordinates(Point3d [] points) //if mask==null, dont use this. this is used to filter the point3d array with the mask.

		{
		 
		 	Vector<Point3d> vectorPoints=new Vector<Point3d>();
		 	double [] coords=new double[3];
		 	int count=0;
			for (int i=0;i<points.length;i++)
			{
				points[i].get(coords);
				if (isInsideBoundary(coords))
						{
						vectorPoints.add(points[i]);
						count++;
						}
			}
			return vectorPoints.toArray(new Point3d[count]);

		}
	 
	

}
