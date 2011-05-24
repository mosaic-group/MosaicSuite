package mosaic.region_competition;

public class Timer 
{
	
	long start;
	long end;
	
	public long tic()
	{
		start=System.nanoTime(); 
		return start;
	}
	
	public long toc()
	{
		end = System.nanoTime();
		return (end-start)/1000;
	}

}
