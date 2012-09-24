package mosaic.region_competition;

import java.util.Arrays;

public class Point
{
	
	public int x[];
	private int dim;	
	
	public Point(int dimension)
	{
		this.dim = dimension;
		this.x = new int[dim];
	}
	
	private Point(){};

	/**
	 * Constructs a Point by copying the coords
	 * @param coords
	 */
	public Point(int coords[]) 
	{
		this.dim = coords.length;
		this.x = coords.clone();
	}
	
	/**
	 * Makes a Point from an Array, without copying the array. 
	 * @param array Coordinates of the Point
	 * @return Point
	 */
	public static Point CopyLessArray(int array[])
	{
		Point p = new Point();
		p.dim = array.length;
		p.x=array;
		return p;
	}
	
//	public static Point PointWithDim(int dimension)
//	{
//		Point p = new Point();
//		p.init(dimension);
//		return p;
//	}
//	
//	private void init(int dimension)
//	{
//		dim = dimension;
//		x = new int[dim];
//	}
	
//	private void set(int p[])
//	{
//		//TODO efficiency
//		x=p.clone();
//	}
	
	public Point add(Point p)
	{
		Point result = new Point(dim);
		for(int i=0; i<dim; i++)
		{
			result.x[i]= x[i]+p.x[i];
		}
		return result;
	}
	
	public Point sub(Point p)
	{
		Point result = new Point(dim);
		for(int i=0; i<dim; i++)
		{
			result.x[i]= x[i]-p.x[i];
		}
		return result;
	}
	
	public Point mult(int f)
	{
		Point result = new Point(dim);
		for(int i=0; i<dim; i++)
		{
			result.x[i]= (x[i]*f);
		}
		return result;
	}
	
	/**
	 * integer division
	 */
	public Point div(int f)
	{
		Point result = new Point(dim);
		for(int i=0; i<dim; i++)
		{
			result.x[i]= (x[i]/f);
		}
		return result;
	}
	
	
	@Override
	public String toString() 
	{
		String result="[";
		int i=0;
		for(i=0; i<x.length-1; i++)
		{
			result=result+x[i]+", ";
		}
		result=result+x[i]+"]";
		return result;
	}

	@Override public boolean equals(Object o) 
	{
		return Arrays.equals(x, ((Point)o).x);
//		return x.equals(((Point)o).x);
//		for(int i=0; i<dim; i++)
//		{
//			if(this.x[i]!=((Point)o).x[i]) return false;
//		}
//		return true;
	};
	
	@Override
	protected Object clone() throws CloneNotSupportedException
	{
		return new Point(this.x);
	}
	
	@Override
	public int hashCode() {
		int sum=0;
		for(int i=0; i<dim; i++)
		{
			sum=sum*1024+x[i];
		}
		return sum;
	}

	public int getDimension()
	{
		return dim;
	}
	
	public interface PointFactoryInterface<T>
	{
		T pointFromArray(int array[]);
		T copylessPointFromArray(int array[]);
	}
	
	public static class PointFactory implements PointFactoryInterface<Point>
	{
		@Override
		public Point pointFromArray(int vec[])
		{
			return new Point(vec);
		}

		@Override
		public Point copylessPointFromArray(int[] array)
		{
			return Point.CopyLessArray(array);
		}
	}
	
}

