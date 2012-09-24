package mosaic.ia.nn;

import ij.ImagePlus;
import java.util.Vector;

import javax.vecmath.Point3d;

import mosaic.ia.utils.IAPUtils;
import mosaic.ia.utils.ImageProcessUtils;



//this class should take images/mask/coords and return D, dgrid
//DistanceClassesImage and DistanceClassesCoords extend this.

public abstract class DistanceCalculations { 
	
	
	
	protected double [] D;
	protected float [] DGrid;

	protected ImagePlus mask;

	private float [][][] maskImage3d;
	private double gridSize;
	
	protected Point3d[] particleXSetCoord;
	protected Point3d[] particleYSetCoord;
	

	
	
	public DistanceCalculations(ImagePlus mask, double gridSize) {
		super();
		this.mask = mask;
		this.gridSize = gridSize;
		if(mask!=null)
			maskImage3d=IAPUtils.imageTo3Darray(mask);
	}



	




	public double[] getD() {
		return D;
	}








	public float[] getDGrid() {
		return DGrid;
	}








	protected boolean isInsideMask(double [] coords)
	{
		
		try{
		if(maskImage3d[(int) Math.floor(coords[2])][(int) Math.floor(coords[1])][(int) Math.floor(coords[0])]>0)
		{
			
		           return true;
		}
		}
		catch(ArrayIndexOutOfBoundsException e) // point outside array.
		{
			return false;
		}
		
	return false;
		
	}
	
	
	
	
	

	

	 protected float [] genCubeGridDist(double x1,double y1,double z1, double x2, double y2, double z2){ //diagonal ends of the cube
	// need to perfect the size part.
		 
		int x_size=(int)Math.floor((Math.abs(x1-x2)+1)/gridSize);  //x1=0,x2=0=> x_size=1. 
		int y_size=(int)Math.floor((Math.abs(y1-y2)+1)/gridSize);
		int z_size=(int)Math.floor((Math.abs(z1-z2)+1)/gridSize);
		
		if(z_size==(int)Math.floor(1/gridSize)) //2D
			z_size=1;
		
		System.out.println("x_size,y_size,z_size"+x_size+","+y_size+","+z_size);
		int total_size=x_size*y_size*z_size;
		Vector<Float> griddVector =new Vector<Float>();
		float [] griddd=null;
	//	Point3d [] grid=new Point3d[total_size];
		double [] tempPosition=new double[3];
			KDTreeNearestNeighbor kdtnn=new KDTreeNearestNeighbor();
			kdtnn.createKDTree(particleYSetCoord);
		
		int lastIndex=0;
		if(mask==null)
		{
			griddd=new float[total_size];
		for(int i=0;i<x_size;i++)
		{
			for(int j=0;j<y_size;j++)
			{
				for(int k=0;k<z_size;k++) //shitty code
				{
					tempPosition[0]=x1+i*gridSize;  //x1+ in the case of coords, if the coords are not starting at 0... 
					tempPosition[1]=y1+j*gridSize;
					tempPosition[2]=z1+k*gridSize;
					
					try {
						griddd[lastIndex]=(float) kdtnn.getNNDistance(new Point3d(tempPosition));
					} catch (Exception e) {
						e.printStackTrace();
					}
					lastIndex++;
				//	System.out.println(tempPosition[0]+","+tempPosition[1]+","+tempPosition[2]);
				}
			}
		}
		}
		else
		{
			for(int i=0;i<x_size;i++)
			{
				for(int j=0;j<y_size;j++)
				{
					for(int k=0;k<z_size;k++) 
					{
						tempPosition[0]=x1+i*gridSize;  //x1+ in the case of coords, if the coords are not starting at 0... 
						tempPosition[1]=y1+j*gridSize;
						tempPosition[2]=z1+k*gridSize;
						
						if(isInsideMask(tempPosition))
						{
						try {
							griddVector.add((float) kdtnn.getNNDistance(new Point3d(tempPosition)));
						} catch (Exception e) {
							e.printStackTrace();
						}
						lastIndex++;
						}
					//	System.out.println(tempPosition[0]+","+tempPosition[1]+","+tempPosition[2]);
					}
				}
			}
			griddd=new float[lastIndex];
			for(int i=0;i<lastIndex;i++)
			{
				Float f=griddVector.get(i);
				griddd[i] = (f != null ? f : Float.NaN); 
			}
		}
	
	//	ImageProcessUtils.KDTreeDistCalc(grid,particleYSetCoord);
		return griddd;
			
		
	}
	 
	 public abstract void calcDistances();

	 protected  void calcD()
	 {
			D=ImageProcessUtils.KDTreeDistCalc(particleXSetCoord, particleYSetCoord);
			
	 }
	 
	 
	 protected  Point3d[] applyMaskandgetCoordinates(Point3d [] points) //if mask==null, dont use this. this is used to filter the point3d array with the mask.

		{
		 	if(mask==null)
		 		return points;
		 	Vector<Point3d> vectorPoints=new Vector<Point3d>();
		 	double [] coords=new double[3];
		 	int count=0;
			for(int i=0;i<points.length;i++)
			{
				points[i].get(coords);
				if(isInsideMask(coords))
						{
						vectorPoints.add(points[i]);
						count++;
						}
			}
			return vectorPoints.toArray(new Point3d[count]);

		}
		

}
