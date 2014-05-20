package mosaic.core.ImagePatcher;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIterator;


/**
 * 
 * This class store the patch of an image
 * 
 * @author Pietro Incardona
 *
 */

public class ImagePatch<T extends NativeType<T> & NumericType<T>, E extends NativeType<E> & IntegerType<E>>
{
	Img<T> it;
	Img<E> lb;
	Img<E> rs;
	
	int margins[];
	Point p1;
	Point p2;
	
	/**
	 * 
	 * Create an image patch
	 * 
	 * @param margins
	 */
	
	ImagePatch(int dim)
	{
		p1 = new Point(dim);
		p2 = new Point(dim);
		
		// Initialize point
		
		for (int i = 0 ; i < dim ; i++)
		{
			p1.x[i] = Integer.MAX_VALUE;
			p2.x[i] = Integer.MIN_VALUE;
		}
	}
	
	/**
	 * 
	 * Extends the patch to include the point
	 * 
	 * @param Point p
	 */
	
	void extendPoint(Point p)
	{
		// check if lower bound respected
		
		for (int i = 0; i < p.getDimension() ; i++)
		{
			// lower bound
			
			if (p.x[i] < p1.x[i])
			{
				p1.x[i] = p.x[i];
			}
			
			// upper bound
			
			if (p.x[i] > p2.x[i])
			{
				p2.x[i] = p.x[i];
			}
		}
	}
	
	/**
	 * 
	 * Add point to P1
	 * 
	 * @param p
	 */
	
	void SubToP1(int p[])
	{
		for (int i = 0 ; i < p.length ; i++)
			p1.x[i] -= p[i];
	}
	
	/**
	 * 
	 * Add Point to P2
	 * 
	 * @param p
	 */
	
	void AddToP2(int p[])
	{
		for (int i = 0 ; i < p.length ; i++)
			p2.x[i] += p[i];
	}
	
	/**
	 * 
	 * Create the patch from an image (It copy the portion of the region)
	 * 
	 * @param img source image
	 * @param lbl optionally a label image
	 */
	
	void createPatch(Img<T> img, Img<E> lbl)
	{
		RandomAccess<T> randomAccess = img.randomAccess();
		RandomAccess<E> randomAccess_lb = null;
		if (lbl != null )
		{randomAccess_lb = lbl.randomAccess();}
		
		// Get the image dimensions
		
		int[] dimensions = MosaicUtils.getImageIntDimensions(img);
		
		// Crop p1 and p2 to remain internally
		
		for (int i = 0 ; i < p1.x.length ; i++)
		{
			if (p1.x[i] < 0)
				p1.x[i] = 0;
			if (p2.x[i] > dimensions[i])
				p2.x[i] = dimensions[i];
		}
		
		// create region iterators and patch image
		
		Point sz = p2.sub(p1);
		
        ImgFactory< T > imgFactory = new ArrayImgFactory< T >( );
        ImgFactory< E > imgFactory_lbl = new ArrayImgFactory< E >( );
        
        // create an Img of the same type of T and create the patch
        
        it = imgFactory.create(sz.x , img.firstElement() );
		RandomAccess<T> randomAccess_it = it.randomAccess();
        
		RandomAccess<E> randomAccess_it_lb = null;
		if (lbl != null)
		{
			lb = imgFactory_lbl.create(sz.x , lbl.firstElement() );
			randomAccess_it_lb = lb.randomAccess();
		}
			
		RegionIterator rg_b = new RegionIterator(sz.x);
		RegionIterator rg = new RegionIterator(dimensions, sz.x, p1.x);
		while(rg.hasNext())
		{
			Point p = rg.getPoint();
			Point pp = rg_b.getPoint();
			rg.next();
			rg_b.next();
			
			randomAccess.setPosition(p.x);
			if (randomAccess_lb != null)
				randomAccess_lb.setPosition(p.x);
			
			randomAccess_it.setPosition(pp.x);
			if (randomAccess_it_lb != null)
				randomAccess_it_lb.setPosition(pp.x);
			
			randomAccess_it.get().set(randomAccess.get());
			
			if (randomAccess_it_lb != null)
				randomAccess_it_lb.get().set(randomAccess_lb.get());
		}
	}

	/**
	 * 
	 * Set the image result for the patche
	 * 
	 * @param img
	 */
	
	public void setResult(Img<E> img) 
	{
		rs = img;
	}
	
	/**
	 * 
	 * Show the patch
	 * 
	 */
	
	public void show()
	{
		ImageJFunctions.show( it );
	}
};