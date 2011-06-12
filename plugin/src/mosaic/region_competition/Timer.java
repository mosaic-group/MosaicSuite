package mosaic.region_competition;

public class Timer 
{
	
	private long start;
	private long end;
	private long duration=0;
	
	public long tic()
	{
		duration = 0;
		start=System.nanoTime(); 
		return start;
	}
	
	public long toc()
	{
		end = System.nanoTime();
		duration = end-start;
		return (duration)/1000;
	}
	
	public long lastResult()
	{
		return duration/1000;
	}

}
