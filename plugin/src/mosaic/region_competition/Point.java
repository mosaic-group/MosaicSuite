package mosaic.region_competition;

import java.util.Arrays;

public class Point 
{
	
	public int x[];		//TODO private?
	private int dim;
	
	public Point() {}
	
	public Point(int... coords) {
		
		//TODO efficiency
		init(coords.length);
		int i=0;
		for(int coord:coords)
		{
			x[i]=coord;
			i++;
		}
	}
	
	public static Point PointWithDim(int dimension)
	{
		Point p= new Point();
		p.init(dimension);
		return p;
	}
	
	private void init(int dimension)
	{
		dim = dimension;
		x= new int[dim];
	}
	
//	private void set(int p[])
//	{
//		//TODO efficiency
//		x=p.clone();
//	}
	
	public Point add(Point p)
	{
		Point result = new Point();
		result.init(dim);
		for(int i=0; i<dim; i++)
		{
			result.x[i]= x[i]+p.x[i];
		}
		return result;
	}
	
	public Point sub(Point p)
	{
		Point result = new Point();
		result.init(dim);
		for(int i=0; i<dim; i++)
		{
			result.x[i]= x[i]-p.x[i];
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
	public int hashCode() {
		// TODO write own hashCode
		int sum=0;
		for(int i=0; i<dim; i++)
		{
			sum=sum*1024+x[i];
		}
		return sum;
//		return super.hashCode();
	}

	public int getDimension()
	{
		return dim;
	}
}
