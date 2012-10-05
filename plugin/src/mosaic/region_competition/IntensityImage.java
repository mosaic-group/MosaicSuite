package mosaic.region_competition;

import java.util.Iterator;
import ij.ImagePlus;
import ij.ImageStack;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;

public class IntensityImage
{
	public Img<FloatType> dataIntensity;
	IndexIterator iterator;
	public ImageStack imageIP;
	
	private int width;
	private int height;
//	private int dim;
//	private int[] dimensions;
	private int size;
	
	public IntensityImage(ImageStack img)
	{

	}
	
	public IntensityImage(ImagePlus img)
	{
		this.imageIP = img.getImageStack();
		
//		int[] dims = dimensionsFromIP(ip);
//		initDimensions(dims);
		
//		this.imageIP = normalize(ip);
		
		initIntensityData(this.imageIP);
		long dim[] = new long [dataIntensity.numDimensions()];
		dataIntensity.dimensions(dim);
		iterator = new IndexIterator(dim);
	}
	
	private void initIntensityData(ImageStack img)
	{
/*		ImageProcessor proc = ip.getProcessor();
		Object test = proc.getPixels();
		if(! (test instanceof float[])){
			throw new RuntimeException("ImageProcessor has to be of type FloatProcessor");
		}
		
		
		int nSlices = ip.getNSlices();
		int area = width*height;*/
		
		ImgFactory< FloatType > imgFactory = new ArrayImgFactory< FloatType >();
		
		int[] dims = dimensionsFromIS(img);
		dataIntensity = imgFactory.create(dims, new FloatType());
		
		RandomAccess<FloatType> vCrs = dataIntensity.randomAccess();
		
		int nSlices = img.getSize();
		float[] pixels;
		int crd[] = new int [dims.length];
		for(int i=1; i<=nSlices; i++)
		{
			crd[0] = i-1;
			pixels = (float[])img.getPixels(i);
			for(int y=0; y<height; y++)
			{
				crd[1] = y;
				for(int x=0; x<width; x++)
				{
					crd[2] = x;
					vCrs.setPosition(crd);
					vCrs.get().set(pixels[y*width+x]);
				}
			}
		}
		
		normalize(dataIntensity);
	}
	
	public int getDim()
	{
		return dataIntensity.numDimensions();
	}
	
	public static void normalize(/*ImagePlus ip*/Img<FloatType> img)
	{
		final FloatType min = new FloatType();
		final FloatType max = new FloatType();
        final Iterator< FloatType > iterator = img.iterator();
        
        // initialize min and max with the first image value
        FloatType type = iterator.next();
 
        min.set( type );
        max.set( type );
 
        // loop over the rest of the data and determine min and max value
        while ( iterator.hasNext() )
        {
            // we need this type more than once
            type = iterator.next();
 
            if ( type.compareTo( min ) < 0 )
                min.set( type );
 
            if ( type.compareTo( max ) > 0 )
                max.set( type );
        }
		
        Cursor<FloatType> crs = img.cursor();
        float range = max.get() - min.get();
        
        while ( crs.hasNext() )
        {
            // Increment
            crs.fwd();
            
			crs.get().set(crs.get().get() - min.get());
			crs.get().set(crs.get().get() * 1.0f/range);
            
            if ( type.compareTo( min ) < 0 )
                min.set( type );
 
            if ( type.compareTo( max ) > 0 )
                max.set( type );
        }
        
/*		ImagePlus dataNormalizedIP;
		
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
		
		return dataNormalizedIP;*/
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
		Cursor<FloatType> dI = dataIntensity.cursor();
		dI.jumpFwd(idx);
		return dI.get().get();
	}
	
	
	public int[] getDimensions()
	{
		int dim[] = new int [dataIntensity.numDimensions()];
		return dim;
	}

	public static int[] dimensionsFromIS(ImageStack ip)
	{
		// IJ 1.46r bug, force to update internal dim 
		// call before getNDimensions() or it won't return the correct value
		int nsl = ip.getSize();
		int dim;
		
		if (nsl != 1)
		{dim = 2;}
		else
		{dim = 3;}
		
		int[] dims = new int[dim];
		
		dims[0]=ip.getWidth();
		dims[1]=ip.getHeight();
		if(dim==3)
		{
			dims[2]=ip.getSize();
		}
		
		return dims;
	}
	
	
}
