package mosaic.ia.utils;

import ij.ImagePlus;
import ij.gui.Plot;
import ij.process.FloatProcessor;

public class PlotUtils {
	
	public static void plotDoubleArray(String title,double [] xvalues, double [] yvalues)
	{
		Plot plot=new Plot(title,"d","Probability (may not be normalized)",xvalues,yvalues);
		plot.show();
		
	}
	
	public static void plotDoubleArrayPts(String title,String xlabel, String ylabel, double [] xvalues, double [] yvalues)
	{
		Plot plot=new Plot(title,xlabel,ylabel,xvalues,yvalues);
		plot.addPoints(xvalues, yvalues, Plot.CIRCLE);
		plot.show();
		
	}
	
	public static void histPlotDoubleArray_imageJ(String title, double [] array)
	{
		histPlotDoubleArray_imageJ(title,array,256);
	}
	

	public static void histPlotDoubleArray_imageJ(String title, double [] array, int bins)
	{
		float floatArray [][]=new float[array.length][1];
		for (int i=0;i<array.length;i++)
		{
			floatArray[i][0]=(float)array[i];
			
		}	
		FloatProcessor hist=new FloatProcessor(floatArray);
	    new ij.gui.HistogramWindow(title,new ImagePlus(title,hist),bins);
	}

	public static void plotDoubleArray(String title, double[] xvalues, int[] yvalues) {
		// TODO Auto-generated method stub
		
		double [] ydouble = new double[yvalues.length];
		for (int i=0;i<ydouble.length;i++)
			ydouble[i]=yvalues[i];
		Plot plot=new Plot(title,"d","Y(int)",xvalues,ydouble);
	
		plot.show();
	}
	
	

}
