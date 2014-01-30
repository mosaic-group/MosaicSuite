package mosaic.ia.nn;

import ij.ImagePlus;

import java.util.Random;
import java.util.Vector;

import javax.vecmath.Point3d;

import weka.estimators.KernelEstimator;

import mosaic.ia.utils.IAPUtils;
import mosaic.ia.utils.ImageProcessUtils;
import mosaic.ia.utils.PlotUtils;



//this class should take images/mask/coords and return D, dgrid
//DistanceClassesImage and DistanceClassesCoords extend this.

public abstract class DistanceCalculations { 
	
	
	
	protected double [] D;
	protected double [][] qOfD;

	public double[][] getqOfD() {
		return qOfD;
	}


	protected ImagePlus mask;

	private float [][][] maskImage3d;
	private double gridSize;
	
	protected Point3d[] particleXSetCoord;
	protected Point3d[] particleYSetCoord;
	
    protected double zscale=1;
    protected double xscale=1;
    protected double yscale=1;
	
    protected double kernelWeightq;
    protected int discretizationSize;
	
	public DistanceCalculations(ImagePlus mask, double gridSize,double kernelWeightq,int discretizationSize) {
		super();
		this.mask = mask;
		this.gridSize = gridSize;
		this.kernelWeightq=kernelWeightq;
		this.discretizationSize=discretizationSize;
		if(mask!=null)
			maskImage3d=IAPUtils.imageTo3Darray(mask);
	}



	




	public double[] getD() {
		return D;
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
	
	
	
	
	
	protected float [] genRandomDdist(double xmax,double ymax,double zmax)
	{
		KDTreeNearestNeighbor kdtnn=new KDTreeNearestNeighbor();
		kdtnn.createKDTree(particleYSetCoord);
		//assume image. 
	//	Point3d [] xPtsRand =new Point3d[particleXSetCoord.length];
		
		float [] distRand=new float[particleXSetCoord.length*1000];
		for(int i=0; i<1000; i++) // 1000 MC runs
		{
			
			for(int j=0;j<particleXSetCoord.length;j++)
			{
				Random rn = new Random(System.nanoTime());
			//	System.out.println("Running "+j);
				try {
					distRand[i*particleXSetCoord.length+j]=(float) kdtnn.getNNDistance(new Point3d(rn.nextDouble()*xmax,rn.nextDouble()*ymax,0));
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
		}
		
		return distRand;
		
		
	}
	
//this method should return a double array , 1st row: values of dgrid - q(min)-q(max) /1000, and 2nd: pdf(normalized) at these points.
	protected  void stateDensity(double x1,double y1,double z1, double x2, double y2, double z2) // dgrid is 1/1000 of min-max
	{ 
		
		double precision = 100d;
		KernelEstimator ker = new KernelEstimator(1 / precision);
		
		
			
	
		
		int x_size=(int)Math.floor((Math.abs(x1-x2)+1)*xscale/gridSize);  //x1=0,x2=0=> x_size=1. 
		int y_size=(int)Math.floor((Math.abs(y1-y2)+1)*yscale/gridSize);
		int z_size=(int)Math.floor((Math.abs(z1-z2)+1)*zscale/gridSize);
		
		if(z_size==(int)Math.floor(1/gridSize)) //2D
			z_size=1;
		
		System.out.println("x_size,y_size,z_size"+x_size+","+y_size+","+z_size);
	
		double [] q=new double[discretizationSize];
		qOfD=new double[2][discretizationSize];
		double max=Double.MIN_VALUE,min=Double.MAX_VALUE;
		double distance;
			double [] tempPosition=new double[3];
			KDTreeNearestNeighbor kdtnn=new KDTreeNearestNeighbor();
			kdtnn.createKDTree(particleYSetCoord);
		if(mask==null)
		{
			
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
						distance=kdtnn.getNNDistance(new Point3d(tempPosition));
						if(distance>max)
							max=distance;
						if(distance<min)
							min=distance;
						ker.addValue(distance,kernelWeightq);
					} catch (Exception e) {
						e.printStackTrace();
					}
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
							distance=kdtnn.getNNDistance(new Point3d(tempPosition));
							if(distance>max)
								max=distance;
							if(distance<min)
								min=distance;
							ker.addValue(distance,kernelWeightq);
						} catch (Exception e) {
							e.printStackTrace();
						}
						}
					}
				}
			}
		}
		
		
		// make dgrid
		
		qOfD[0][0] = 0;

		double bin_size = (max - min) / discretizationSize;
		
		q[0] = ker.getProbability(qOfD[1][0]); // how does this work?

		
		for (int i = 1; i < discretizationSize; i++) {
			qOfD[0][i] = qOfD[0][i - 1] + bin_size;
			q[i] = ker.getProbability(qOfD[0][i]); 
	//		System.out.println("q,i"+q[i]+","+i);
		}
		qOfD[1]=IAPUtils.normalize(q);
//		return qOfD;
		
		for (int i = 0; i < discretizationSize; i++) {
		//System.out.println("q,qofD0,qofD1,i"+q[i]+","+qOfD[0][i]+","+qOfD[1][i]+","+i);
		}
				
	}

	 protected float [] genCubeGridDist(double x1,double y1,double z1, double x2, double y2, double z2){ //diagonal ends of the cube
	// need to perfect the size part.
		 
		int x_size=(int)Math.floor((Math.abs(x1-x2)+1)*xscale/gridSize);  //x1=0,x2=0=> x_size=1. 
		int y_size=(int)Math.floor((Math.abs(y1-y2)+1)*yscale/gridSize);
		int z_size=(int)Math.floor((Math.abs(z1-z2)+1)*zscale/gridSize);
		
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
	//	PlotUtils.histPlotDoubleArray_imageJ("HistYvsYcoords", kdtnn.getNNDistances(particleYSetCoord));
		int lastIndex=0;
		if(mask==null)
		{
	//		griddd=genRandomDdist(x2, y2, z2);
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
			
		//return genRandomDdist(x_size, y_size, z_size);
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
						vectorPoints.add(new Point3d(coords[0]*xscale,coords[1]*yscale,coords[2]*zscale));
						//vectorPoints.add(points[i]);
						count++;
						}
			}
			return vectorPoints.toArray(new Point3d[count]);

		}
		

}
