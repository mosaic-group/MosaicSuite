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
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}
}
