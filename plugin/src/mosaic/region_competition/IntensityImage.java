package mosaic.region_competition;

import java.util.Iterator;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class IntensityImage
{
	public Img<FloatType> dataIntensity;
//	IndexIterator iterator;
	public ImageStack imageIP;
	private RandomAccessible< FloatType> infAccess;
	private RandomAccess< FloatType > infAccessIt;
	private Cursor<FloatType> curDi;
	
	
//	private int width;
//	private int height;
//	private int dim;
	private int[] dimensions;
	private int size;
	
	public static float volume_image(Img <FloatType> img)
	{
		float Vol = 0.0f;
		Cursor<FloatType> cur = img.cursor();
		
		while( cur.hasNext() )
		{
			cur.fwd();
			
			Vol += cur.get().get();
			
		}
		
		return Vol;
	}
	
	public static void rescale_image(Img <FloatType> img,float r)
	{		
		Cursor<FloatType> cur = img.cursor();
		
		while( cur.hasNext() )
		{
			cur.fwd();
			
			cur.get().set((float)cur.get().get()*r);
			
		}			
	}
	
	public static void normalize_image(Img <FloatType> img)
	{
		
		double minimum = Double.POSITIVE_INFINITY;
		double maximum = Double.NEGATIVE_INFINITY;
		
		Cursor<FloatType> cur = img.cursor();
		
		int dims [] = new int[img.numDimensions()];
		
		while( cur.hasNext() )
		{
			cur.fwd();
			
			float tmp = cur.get().get();
			
			if(tmp>maximum) maximum=tmp;
			if(tmp<minimum) minimum=tmp;
		}
		
		double range = maximum-minimum;
		cur.reset();
		
		while( cur.hasNext() )
		{
			cur.fwd();
			
			cur.get().set((float)((cur.get().get() - minimum)/range));
			
		}			
	}
	
	public IntensityImage(ImageStack img)
	{
		imageIP = img;
		imageIP = normalize(imageIP);
		
		ImageProcessor proc = imageIP.getProcessor(1);
		Object test = proc.getPixels();
		if(! (test instanceof float[]))
		{
			throw new RuntimeException("ImageProcessor has to be of type FloatProcessor");
		}
		
//		int[] dims = dimensionsFromIP(ip);
//		initDimensions(dims);
		
//		this.imageIP = normalize(ip);
		
		initIntensityData(this.imageIP);
		long dim[] = new long [dataIntensity.numDimensions()];
		dataIntensity.dimensions(dim);
		dimensions = new int [dataIntensity.numDimensions()];
		for (int i = 0 ; i < dim.length ; i++)	{dimensions[i] = (int) dim[i];}
		
		// Extends bound
		
        infAccess = Views.extendValue( dataIntensity, new FloatType( 0 ) );
        infAccessIt = infAccess.randomAccess();
        curDi = dataIntensity.cursor();
         
         
         // Recover
         
        IntensityImageR(new ImagePlus("Recover",img));
	}
	
	public IntensityImage(ImagePlus img)
	{
		this(img.getStack());
/*		imageIP = normalize(imageIP);
		
		ImageProcessor proc = imageIP.getProcessor(1);
		Object test = proc.getPixels();
		if(! (test instanceof float[]))
		{
			throw new RuntimeException("ImageProcessor has to be of type FloatProcessor");
		}
		
//		int[] dims = dimensionsFromIP(ip);
//		initDimensions(dims);
		
//		this.imageIP = normalize(ip);
		
		initIntensityData(this.imageIP);
		long dim[] = new long [dataIntensity.numDimensions()];
		dataIntensity.dimensions(dim);
		iterator = new IndexIterator(dim);*/
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
		
		/* Get image iterator to set pixels */
		
		RandomAccess<FloatType> vCrs = dataIntensity.randomAccess();
		
		int nSlices = img.getSize();
		float[] pixels;
		int crd[] = new int [dims.length];
		if (dims.length == 3)
		{
			for(int i=1; i<=nSlices; i++)
			{
				crd[2] = i-1;
				pixels = (float[])img.getPixels(i);
				for(int y=0; y<dims[1]; y++)
				{
					crd[1] = y;
					for(int x=0; x<dims[0]; x++)
					{
						crd[0] = x;
						vCrs.setPosition(crd);
						vCrs.get().set(pixels[y*dims[0]+x]);
					}
				}
			}
		}
		else
		{
			pixels = (float[])img.getPixels(1);
			for(int y=0; y<dims[1]; y++)
			{
				crd[1] = y;
				for(int x=0; x<dims[0]; x++)
				{
					crd[0] = x;
					vCrs.setPosition(crd);
					vCrs.get().set(pixels[y*dims[0]+x]);
				}
			}
		}
	}
	
	public int getDim()
	{
		return dataIntensity.numDimensions();
	}
	
	public static ImageStack normalize(ImageStack ip)
	{
//		final FloatType min = new FloatType();
//		final FloatType max = new FloatType();
//        final Iterator< FloatType > iterator = img.iterator();
        
        // initialize min and max with the first image value
/*        FloatType type = iterator.next();
 
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
	
		ImagePlus dataNormalizedIP;
		
		// scale all values in all slices to floats between 0.0 and 1.0
		int nSlices = ip.getSize();
		
		double minimum = Double.POSITIVE_INFINITY;
		double maximum = Double.NEGATIVE_INFINITY;
		
		for(int i=1; i<=nSlices; i++)
		{
			ImageProcessor p = ip.getProcessor(i);
			ImageStatistics stat = p.getStatistics();
			
			double min = stat.min;
			double max = stat.max;
			
			if(max>maximum) maximum=max;
			if(min<minimum) minimum=min;
		}
        
		double range = maximum-minimum;
		
		ImageStack normalizedStack = new ImageStack(ip.getWidth(), ip.getHeight());
		
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
        }*/
        

		
/*		double range = maximum-minimum;
		
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
		
		// scale all values in all slices to floats between 0.0 and 1.0
		int nSlices = ip.getSize();
		
		double minimum = Double.POSITIVE_INFINITY;
		double maximum = Double.NEGATIVE_INFINITY;
		
		ImageStack normalizedStack = new ImageStack(ip.getWidth(), ip.getHeight());
		
		for(int i=1; i<=nSlices; i++)
		{
			ImageProcessor p = ip.getProcessor(i);
			ImageStatistics stat = p.getStatistics();
			
			double min = stat.min;
			double max = stat.max;
			
			if(max>maximum) maximum=max;
			if(min<minimum) minimum=min;
		}
		
		double range = maximum-minimum;
		
		for(int i=1; i<=nSlices; i++)
		{
			ImageProcessor p = ip.getProcessor(i);
			/*p.setColorModel(null);*/ // force IJ to directly convert to float (else it would first go to RGB)
			FloatProcessor fp = (FloatProcessor)p.convertToFloat();
			fp.subtract(minimum);
			fp.multiply(1.0/range);
//			IJ.run(ip, "Divide...", "value=1.250000000");
			
			double newMax = fp.getStatistics().max;
			double newMin = fp.getStatistics().min;
			System.out.println("intensity max = "+newMax);
			System.out.println("intensity min = "+newMin);
			
			String oldTitle = ip.getSliceLabel(i);
			normalizedStack.addSlice(oldTitle, fp);
			
//			stack.setPixels(fp.getPixels(), i);
		}
		
		return normalizedStack;		
	}
	
	
	
	
	
	/**
	 * returns the image data of the originalIP at Point p
	 */
	public float get(Point p)
	{
		infAccessIt.setPosition(p.x);
		return infAccessIt.get().get();
	}
	
	public float get(int idx)
	{		
		curDi.reset();
		curDi.jumpFwd(idx+1);
		
		return curDi.get().get();
	}
	
	
	public int[] getDimensions()
	{
		return dimensions;
	}

	public static int[] dimensionsFromIS(ImageStack ip)
	{
		// IJ 1.46r bug, force to update internal dim 
		// call before getNDimensions() or it won't return the correct value
		int nsl = ip.getSize();
		int dim;
		
		if (nsl != 1)
		{dim = 3;}
		else
		{dim = 2;}
		
		int[] dims = new int[dim];
		
		dims[0]=ip.getWidth();
		dims[1]=ip.getHeight();
		if(dim==3)
		{
			dims[2]=ip.getSize();
		}
		
		return dims;
	}
	
	
////// Old class Recover
	
	
	public float[] dataIntensityR;
	IndexIterator iteratorR;
	public ImagePlus imageIPR;
	
	private int widthR;
	private int heightR;
	private int dimR;
	private int[] dimensionsR;
	private int sizeR;
	
	public void IntensityImageR(ImagePlus ip)
	{
		this.imageIPR = ip;
		
		int[] dims = dimensionsFromIP(ip);
		initDimensionsR(dims);
		iteratorR = new IndexIterator(dims);
		
		this.imageIPR = normalizeR(ip);
		initIntensityDataR(imageIPR);
	}
	
	
	private void initDimensionsR(int[] dims)
	{
		this.dimensionsR = dims;
		this.dimR = dimensionsR.length;
		if(dimR>3)
		{
			throw new RuntimeException("Dim > 3 not supported");
		}
		
		this.widthR = dims[0];
		this.heightR = dims[1];
		
		size=1;
		for(int i=0; i<dimR; i++)
		{
			size *= dimensions[i];
		}
	}
	
	
	private void initIntensityDataR(ImagePlus ip)
	{
		ImageProcessor proc = ip.getProcessor();
		Object test = proc.getPixels();
		if(! (test instanceof float[])){
			throw new RuntimeException("ImageProcessor has to be of type FloatProcessor");
		}
		
		
		int nSlices = ip.getNSlices();
		int area = widthR*heightR;
		
		dataIntensityR = new float[size];
		
		ImageStack stack = ip.getStack();
		
		float[] pixels;
		for(int i=1; i<=nSlices; i++)
		{
			pixels = (float[])stack.getPixels(i);
			for(int y=0; y<heightR; y++)
			{
				for(int x=0; x<widthR; x++)
				{
					dataIntensityR[(i-1)*area+y*widthR+x] = pixels[y*widthR+x];
				}
			}
		}
	}
	
	
	public static ImagePlus normalizeR(ImagePlus ip)
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
			/*p.setColorModel(null);*/ // force IJ to directly convert to float (else it would first go to RGB)
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
	public float getR(Point p)
	{
		return get(iteratorR.pointToIndex(p));
	}
	
	public float getR(int idx)
	{
		return dataIntensityR[idx];
	}
	
	
	public int[] getDimensionsR()
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
