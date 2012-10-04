package mosaic.region_competition;

import java.util.Iterator;

public class IndexIterator
{
	
	private int dimensions[];
	private int dim;
	private int size;
	
	/**
	 * 
	 * @param dims integer array of dimensions (width/height/depth/...)
	 */
	public IndexIterator(long[] dims)
	{
		init(dims);
	}
	
	/**
	 * 
	 * @param dims integer array of dimensions (width/height/depth/...)
	 */
	public IndexIterator(int[] dims)
	{
		init(dims);
	}
	
	private void init(int[] dims)
	{
		dimensions = dims.clone();
		dim = dimensions.length;
		
		size=1;
		for(int i=0; i<dim; i++)
		{
			size*=dimensions[i];
		}
	}

	private void init(long[] dims)
	{
		dim = dimensions.length;
		for (int i = 0 ; i < dims.length ; i++) {dimensions[i] = (int)dims[i];}
		
		size=1;
		for(int i=0; i<dim; i++)
		{
			size*=dimensions[i];
		}
	}
	
	/**
	 * 
	 * @return total number of pixels = width*height*...
	 */
	public int getSize()
	{
		return size;
	}
	
	/**
	 * Converts a Point index into an integer index
	 * @param p Point index
	 * @return integer index
	 */
	public int pointToIndex(Point p)
	{
		// TODO test
	
		int idx = 0;
		int fac = 1;
		for(int i = 0; i < dim; i++) {
			idx += fac * p.x[i];
			fac *= dimensions[i];
		}
	
		return idx;
	}
	
	/**
	 * Converts an integer index into a Point index
	 * @param idx integer index
	 * @return Point index
	 */
	public Point indexToPoint(int idx)
	{
		int index=idx;
		int x[] = new int[this.dim];
		
		for(int i = 0; i < dim; i++) 
		{
			int r=index%dimensions[i];
			x[i]=r;
			index=index-r;
			index=index/dimensions[i];
		}
		
		Point result = Point.CopyLessArray(x);
		
		//TODO !!! test, is this correct?
		int dummy=pointToIndex(result);
		if(dummy!=idx)
		{
			System.out.println("indexToPoint is not correct");
			return null;
		}
		return result;
	}
	
	/**
	 * 
	 * @param p Point index
	 * @return true, if Point is within bounds of this Iterator
	 */
	public boolean isInBound(Point p)
	{
		for(int d=0; d<dim; d++)
		{
			if(p.x[d]<0 || p.x[d]>=dimensions[d])
			{
				return false;
			}
		}
		return true;
	}
	
	
	public Iterator<Point> getPointIterator()
	{
		return new Iterator<Point>() {
			
			int i=0;

			@Override
			public boolean hasNext()
			{
				return i<size;
			}

			@Override
			public Point next()
			{
				return indexToPoint(i++);
			}

			@Override
			public void remove()
			{
				// not needed
			}
		};
	}
	
	
	/**
	 * Iterable for extended for-loops
	 * @return Iterable<Point>
	 */
	public Iterable<Point> getPointIterable()
	{
		return new Iterable<Point>() {

			@Override
			public Iterator<Point> iterator()
			{
				return getPointIterator();
			}
		};
	}
	
	
	public Iterator<Integer> getIndexIterator()
	{
		return new Iterator<Integer>() {
			
			int i=0;

			@Override
			public boolean hasNext()
			{
				return i<size;
			}

			@Override
			public Integer next()
			{
				//TODO +++ 
				return (i++);
			}

			@Override
			public void remove()
			{
				// not needed
			}
		};
	}
	
	
	/**
	 * Iterable for extended for-loops, returns Integer indexes
	 * @return Iterable<Integer>
	 */
	public Iterable<Integer> getIndexIterable()
	{
		return new Iterable<Integer>() {

			@Override
			public Iterator<Integer> iterator()
			{
				return getIndexIterator();
			}
		};
	}
	
	
}
