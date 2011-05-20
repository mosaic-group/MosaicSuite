package mosaic.region_competition;

public class Point 
{
	int x[];
	private int dim;
	
	private Point() {}
	
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
	
//	public Point sub(Point p)
//	{
//		Point result = new Point();
//		result.init(dim);
//		for(int i=0; i<dim; i++)
//		{
//			result.x[i]= x[i]-p.x[i];
//		}
//		return result;
//	}
	
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


}
