package mosaic.ia.utils;

import javax.vecmath.Point3d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import weka.estimators.KernelEstimator;

public class IAPUtils {

	public static double [] calculateCDF(double [] qofD)
	{
		double [] QofD=new double[qofD.length];
		double sum=0;
	
		
		for(int i=0;i<qofD.length;i++)
		{
			QofD[i]=sum+qofD[i];
			sum=sum+qofD[i];
		}

		System.out.println("QofD before norm:"+QofD[QofD.length-1]);
		for(int i=0;i<qofD.length;i++)
		{
			QofD[i]=QofD[i]/QofD[QofD.length-1];
		}
		System.out.println("QofD after norm:"+QofD[QofD.length-1]);
		
		return QofD;
	}

	public static double [] calcMinMaxXYZ(Point3d [] points) // returns array with 6 fieldds, minx,miny,minz,maxx,maxy,maxz
	{
		double minx=Float.MAX_VALUE,miny=Float.MAX_VALUE,minz=Float.MAX_VALUE,maxx=Float.MIN_VALUE,maxy=Float.MIN_VALUE,maxz=Float.MIN_VALUE;
		double [] temp=new double[3] ;
		for(int i=0;i<points.length;i++)
		{
		  points[i].get(temp);
		  if(temp[0]<minx)
			   minx=temp[0];
		   if(temp[1]<miny)
			   miny=temp[1];
		   if(temp[2]<minz)
			   minz=temp[2];
		   if(temp[0]>maxx)
			   maxx=temp[0];
		   if(temp[1]>maxy)
			   maxy=temp[1];
		   if(temp[2]>maxz)
			   maxz=temp[2];
		}
		 double [] minMax={minx,miny,minz,maxx,maxy,maxz};
		return minMax;
	}
	
	public static float [][][] imageTo3Darray(ImagePlus image)
	{
		
		ImageStack is=image.getStack();	
		ImageProcessor imageProc;//,mgp;
		
		float [][][] image3d=new float[is.getSize()][is.getWidth()][is.getHeight()];
	
		for(int k=0;k<is.getSize();k++)
		{
			imageProc=is.getProcessor(k+1);
//			mgp=mgs.getProcessor(k+1);
	
			image3d[k]=imageProc.getFloatArray();
		}
		
	/*	for(int i=0;i<is.getSize();i++)
		{
			for(int j=0;j<is.getWidth();j++)
			{
				for(int k=0;k<is.getHeight();k++)
					System.out.print(image3d[i][j][k]+" ");

				System.out.println("\n");
			}
			System.out.println("\n");
		}
		*/
		return image3d;
		
	}
	
	public static double [] getMinMaxMeanD(double [] D)
	{
		double [] minMax=new double[3];
		minMax[0]=Double.MAX_VALUE;  //min
		minMax[1]=Double.MIN_VALUE;//max
		minMax[2]=0;//mean
		for(int i=0;i<D.length;i++)
		{
			if(D[i]<minMax[0])
				minMax[0]=D[i];
			if(D[i]>minMax[1])
				minMax[1]=D[i];
			minMax[2]=minMax[2]+D[i];			
		}
		minMax[2]=minMax[2]/D.length;
		return minMax;
	}
	
	
	
	public static double linearInterpolation(double yl, double xl, double yr, double xr, double x)
	{
		double m=(yl-yr)/(xl-xr);
		double c=yl-m*xl;
		return m*x+c;
		
	}
	
	public static KernelEstimator createkernelDensityEstimator(double [] distances,double weight)
	{	
		double precision=100d;
		KernelEstimator ker=new KernelEstimator(1/precision);
		System.out.println("Weight:"+weight);
		for (int i=0;i<distances.length;i++)
			ker.addValue(distances[i], weight); //weight is important, since bandwidth is calculated with it: http://stackoverflow.com/questions/3511012/how-ist-the-bandwith-calculated-in-weka-kernelestimator-class
		//depending on the changes to the grid, this might have to be changed.
	//	System.out.println("Added values to kernel:"+ke.getNumKernels());
		return ker;
	}
	public static KernelEstimator createkernelDensityEstimator(float [] distances,double weight)
	{	
		double precision=100d;
		KernelEstimator ker=new KernelEstimator(1/precision);
		System.out.println("Weight:"+weight);
		for (int i=0;i<distances.length;i++)
			ker.addValue(distances[i], weight); //weight is important, since bandwidth is calculated with it: http://stackoverflow.com/questions/3511012/how-ist-the-bandwith-calculated-in-weka-kernelestimator-class
		//depending on the changes to the grid, this might have to be changed.
	//	System.out.println("Added values to kernel:"+ke.getNumKernels());
		return ker;
	}

}
