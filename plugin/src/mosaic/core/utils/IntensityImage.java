package mosaic.core.utils;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

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
	
	public boolean isOutOfBound(Point p)
	{
		for (int i = 0 ; i < p.x.length ; i++)
		{if (p.x[i] < 0) return true; if (p.x[i] >= dimensions[i]) return true;}
		return false;
	}
	
	public int getDim()
	{
		return dim;
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
		if (dim==3)
		{
			dims[2]=ip.getSize();
		}
		
		return dims;
	}
	
	/**
	 * 
	 * Close the image
	 * 
	 */
	
	public void close()
	{
		if (imageIP != null)
			imageIP.close();
	}
	
	public static Img <FloatType> convertToImg(ImageStack stack)
	{	
		ImageProcessor proc = stack.getProcessor(1);
		Object test = proc.getPixels();
		if (! (test instanceof float[]))
		{
			throw new RuntimeException("ImageProcessor has to be of type FloatProcessor");
		}
		
//		int[] dims = dimensionsFromIP(ip);
//		initDimensions(dims);
		
//		this.imageIP = normalize(ip);
		
		ImgFactory< FloatType > imgFactory = new ArrayImgFactory< FloatType >();
		
		int[] dims = dimensionsFromIS(stack);
		Img<FloatType> dataIntensity = imgFactory.create(dims, new FloatType());
		
		/* Get image iterator to set pixels */
		
		RandomAccess<FloatType> vCrs = dataIntensity.randomAccess();
		
		int nSlices = stack.getSize();
		float[] pixels;
		int crd[] = new int [dims.length];
		if (dims.length == 3)
		{
			for (int i=1; i<=nSlices; i++)
			{
				crd[2] = i-1;
				pixels = (float[])stack.getPixels(i);
				for (int y=0; y<dims[1]; y++)
				{
					crd[1] = y;
					for (int x=0; x<dims[0]; x++)
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
			pixels = (float[])stack.getPixels(1);
			for (int y=0; y<dims[1]; y++)
			{
				crd[1] = y;
				for (int x=0; x<dims[0]; x++)
				{
					crd[0] = x;
					vCrs.setPosition(crd);
					vCrs.get().set(pixels[y*dims[0]+x]);
				}
			}
		}
		
		return dataIntensity;
	}
	
	/**
	 * 
	 * Calculate the sum of all pixels
	 * 
	 * @param ip
	 * @return
	 */
	
	public static <T extends RealType<T>> double volume_image(Img<T> ip)
	{
		double Vol = 0.0f;
		Cursor<T> cur = ip.cursor();
		
		while ( cur.hasNext() )
		{
			cur.fwd();
			
			Vol += cur.get().getRealDouble();
			
		}
		
		return Vol;
	}
	
	/**
	 * 
	 * It rescale all the pixels of a factor r
	 * 
	 * @param image_psf Image
	 * @param r factor to rescale
	 */
	
	public static <T extends RealType<T>> void rescale_image(Img<T> image_psf,float r)
	{		
		Cursor<T> cur = image_psf.cursor();
		
		while ( cur.hasNext() )
		{
			cur.fwd();
			
			cur.get().setReal(cur.get().getRealFloat()*r);
			
		}			
	}
	
	/**
	 * 
	 * Initialize an intensity image from an ImgLib2
	 * 
	 * @param ip
	 */
	
	public <T extends RealType<T>> IntensityImage(Img<T> ip, Class<T> cls)
	{
		this(ip,cls,true);
	}
	
	/**
	 * 
	 * Initialize an intensity image from an ImgLib2
	 * 
	 * @param ip
	 */
	
	public <T extends RealType<T>> IntensityImage(Img<T> ip, Class<T> cls, boolean nrm)
	{
		this.imageIP = null;
		
		int[] dims = MosaicUtils.getImageIntDimensions(ip);
		initDimensions(dims);
		iterator = new IndexIterator(dims);
		
		initIntensityData(ip, cls, nrm);
	}
	
	/**
	 * 
	 * Initialize an intensity image from an Image Plus
	 * 
	 * @param ip
	 */
	
	public IntensityImage(ImagePlus ip)
	{
		this(ip, true);
	}
	
	/**
	 * 
	 * Initialize an intensity image from an Image Plus
	 * choosing is normalizing or not
	 * 
	 * @param ip ImagePlus
	 * @param nrm true normalize false don' t
	 */
	
	public IntensityImage(ImagePlus ip, boolean nrm)
	{
		this.imageIP = ip;
		
		int[] dims = dimensionsFromIP(ip);
		initDimensions(dims);
		iterator = new IndexIterator(dims);
		
		if (nrm == true)
			this.imageIP = normalize(ip);
		initIntensityData(imageIP);
	}
	
	
	private void initDimensions(int[] dims)
	{
		this.dimensions = dims;
		this.dim = dimensions.length;
		if (dim>3)
		{
			throw new RuntimeException("Dim > 3 not supported");
		}
		
		this.width = dims[0];
		this.height = dims[1];
		
		size=1;
		for (int i=0; i<dim; i++)
		{
			size *= dimensions[i];
		}
	}
	
	/**
	 * 
	 * Initialize intensity data
	 * 
	 * @param ip Image
	 */
	
	private <T extends RealType<T>> void initIntensityData(Img<T> ip, Class<T> cls, boolean nrm)
	{
		RandomAccess<T> ra = ip.randomAccess();
		
		// Allocate data intensity
		
		dataIntensity = new float[size];
		
		// Calculate min/max
		
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		Cursor<T> cur = ip.cursor();

		while (cur.hasNext())
		{
			cur.next();
			if ( cur.get().getRealDouble() > max)
			{
				max = cur.get().getRealDouble();
			}
			else if (cur.get().getRealDouble() < min)
			{
				min = cur.get().getRealDouble();
			}
		}
		
		
		// Create a region iterator
		
		RegionIterator rg = new RegionIterator(MosaicUtils.getImageIntDimensions(ip));
		
		// load the image
		
		while (rg.hasNext())
		{
			Point p = rg.getPoint();
			int id = rg.next();
			
			ra.setPosition(p.x);
			dataIntensity[id] = (float) ((ra.get().getRealFloat() - min)/max);
		}
	}
	
	private void initIntensityData(ImagePlus ip)
	{
		ImageProcessor proc = ip.getProcessor();
		Object test = proc.getPixels();
		if (! (test instanceof float[])){
			throw new RuntimeException("ImageProcessor has to be of type FloatProcessor");
		}
		
		
		int nSlices = ip.getStackSize();
		int area = width*height;
		
		dataIntensity = new float[size];
		
		ImageStack stack = ip.getStack();
		
		float[] pixels;
		for (int i=1; i<=nSlices; i++)
		{
			pixels = (float[])stack.getPixels(i);
			for (int y=0; y<height; y++)
			{
				for (int x=0; x<width; x++)
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
		
		for (int i=1; i<=nSlices; i++)
		{
			ImageProcessor p = stack.getProcessor(i);
			ImageStatistics stat = p.getStatistics();
			
			double min = stat.min;
			double max = stat.max;
			
			if (max>maximum) maximum=max;
			if (min<minimum) minimum=min;
		}
		
		double range = maximum-minimum;
		
		if (range == 0.0)
		{
			if (maximum != 0.0)
			{range = maximum; minimum = 0.0;}
			else
			{range = 1.0; minimum = 0.0;}
		}
		
		for (int i=1; i<=nSlices; i++)
		{
			ImageProcessor p = stack.getProcessor(i);
//			p.setColorModel(null); // force IJ to directly convert to float (else it would first go to RGB)
			FloatProcessor fp = (FloatProcessor)p.convertToFloat();
			fp.subtract(minimum);
			fp.multiply(1.0/range);
//			IJ.run(ip, "Divide...", "value=1.250000000");
			
//			double newMax = fp.getStatistics().max;
//			double newMin = fp.getStatistics().min;
//			System.out.println("intensity max = "+newMax);
//			System.out.println("intensity min = "+newMin);
			
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
	
	public float getSafe(Point p)
	{
		for (int i = 0 ; i < p.x.length ; i++)	
		{if (p.x[i] < 0) return 0.0f; if (p.x[i] >= dimensions[i]) return 0.0f;}
		
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

	/**
	 * 
	 * Get an ImgLib2 from a intensity image
	 * 
	 * @return an ImgLib2 image
	 * 
	 */
	
	public <T extends NativeType<T> & RealType<T> > Img<T> getImgLib2(Class<T> cls)
	{
		long lg[] = new long[getDim()];
		
		// Take the size
		
		ImgFactory< T > imgFactory = new ArrayImgFactory< T >( );
		
		for (int i = 0 ; i < getDim() ; i++)
		{
			lg[i] = getDimensions()[i];
		}
		
        // create an Img of the same type of T and size of the imageLabel
        
        Img<T> it = null;
		try {
			it = imgFactory.create(lg , cls.newInstance() );
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		RandomAccess<T> randomAccess_it = it.randomAccess();
		
		// Region iterator
		
		RegionIterator ri = new RegionIterator(getDimensions());
		
		while (ri.hasNext())
		{
			Point p = ri.getPoint();
			int id = ri.next();
			
			randomAccess_it.setPosition(p.x);
			randomAccess_it.get().setReal(dataIntensity[id]);
		}
		
		return it;
	}
	
	public static int[] dimensionsFromIP(ImagePlus ip)
	{
		// IJ 1.46r bug, force to update internal dim 
		// call before getNDimensions() or it won't return the correct value
		ip.getStackSize(); 
		
		int dim = ip.getNDimensions();
		int[] dims = new int[dim];
		
		for (int i=0; i<2; i++)
		{
			dims[i]=ip.getDimensions()[i];
		}
		if (dim==3)
		{
			dims[2]=ip.getStackSize();
		}
		
		return dims;
	}
	
	
}
