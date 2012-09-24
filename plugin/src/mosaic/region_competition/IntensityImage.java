package mosaic.region_competition;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

public class IntensityImage
{
	public float[] dataIntensity;
	IndexIterator iterator;
	public ImagePlus imageIP;
	
	private int width;
	private int height;
	private int dim;
	private int[] dimensions;
	private int size;
	
	public IntensityImage(ImagePlus ip)
	{
		this.imageIP = ip;
		
		int[] dims = dimensionsFromIP(ip);
		initDimensions(dims);
		iterator = new IndexIterator(dims);
		
		this.imageIP = normalize(ip);
		initIntensityData(imageIP);
	}
	
	
	private void initDimensions(int[] dims)
	{
		this.dimensions = dims;
		this.dim = dimensions.length;
		if(dim>3)
		{
			throw new RuntimeException("Dim > 3 not supported");
		}
		
		this.width = dims[0];
		this.height = dims[1];
		
		size=1;
		for(int i=0; i<dim; i++)
		{
			size *= dimensions[i];
		}
	}
	
	
	private void initIntensityData(ImagePlus ip)
	{
		ImageProcessor proc = ip.getProcessor();
		Object test = proc.getPixels();
		if(! (test instanceof float[])){
			throw new RuntimeException("ImageProcessor has to be of type FloatProcessor");
		}
		
		
		int nSlices = ip.getNSlices();
		int area = width*height;
		
		dataIntensity = new float[size];
		
		ImageStack stack = ip.getStack();
		
		float[] pixels;
		for(int i=1; i<=nSlices; i++)
		{
			pixels = (float[])stack.getPixels(i);
			for(int y=0; y<height; y++)
			{
				for(int x=0; x<width; x++)
				{
					dataIntensity[(i-1)*area+y*width+x] = pixels[y*width+x];
				}
			}
		}
	}
	
	
	public static ImagePlus normalize(ImagePlus ip)
	{
		ImagePlus dataNormalizedIP;
		
		// scale all values in all slices to floats between 0.0 and 1.0
		int nSlices = ip.getStackSize();
		
		ImageStack stack = ip.getStack();
		
		double minimum = Double.POSITIVE_INFINITY;
		double maximum = Double.NEGATIVE_INFINITY;
		
		ImageStack normalizedStack = new ImageStack(stack.getWidth(), stack.getHeight());
		
		for(int i=1; i<=nSlices; i++)
		{
			ImageProcessor p = stack.getProcessor(i);
			ImageStatistics stat = p.getStatistics();
			
			double min = stat.min;
			double max = stat.max;
			
			if(max>maximum) maximum=max;
			if(min<minimum) minimum=min;
		}
		
		double range = maximum-minimum;
		
		for(int i=1; i<=nSlices; i++)
		{
			ImageProcessor p = stack.getProcessor(i);
			p.setColorModel(null); // force IJ to directly convert to float (else it would first go to RGB)
			FloatProcessor fp = (FloatProcessor)p.convertToFloat();
			fp.subtract(minimum);
			fp.multiply(1.0/range);
//			IJ.run(ip, "Divide...", "value=1.250000000");
			
			double newMax = fp.getStatistics().max;
			double newMin = fp.getStatistics().min;
			System.out.println("intensity max = "+newMax);
			System.out.println("intensity min = "+newMin);
			
			String oldTitle = stack.getSliceLabel(i);
			normalizedStack.addSlice(oldTitle, fp);
			
//			stack.setPixels(fp.getPixels(), i);
		}
		
		stack = normalizedStack;
		
		dataNormalizedIP = new ImagePlus("Normalized", stack);
		
		return dataNormalizedIP;
	}
	
	
	
	
	
	/**
	 * returns the image data of the originalIP at Point p
	 */
	public float get(Point p)
	{
		return get(iterator.pointToIndex(p));
	}
	
	public float get(int idx)
	{
		return dataIntensity[idx];
	}
	
	
	public int[] getDimensions()
	{
		return this.dimensions;
	}

	public static int[] dimensionsFromIP(ImagePlus ip)
	{
		// IJ 1.46r bug, force to update internal dim 
		// call before getNDimensions() or it won't return the correct value
		ip.getNSlices(); 
		
		int dim = ip.getNDimensions();
		int[] dims = new int[dim];
		
		for(int i=0; i<2; i++)
		{
			dims[i]=ip.getDimensions()[i];
		}
		if(dim==3)
		{
			dims[2]=ip.getNSlices();
		}
		
		return dims;
	}
	
	
}
