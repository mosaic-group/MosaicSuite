package mosaic.region_competition;

public class Timer 
{
	
	private long start;
	private long end;
	private long duration=0;
	
	/**
	 * starts the timer 
	 * @return starttime in ms
	 */
	public long tic()
	{
		duration = 0;
		start=System.nanoTime(); 
		return start/1000;
	}
	
	/**
	 * @return time elapsed since tic() in ms
	 */
	public long toc()
	{
		end = System.nanoTime();
		duration = end-start;
		return (duration)/1000;
	}
	
//	public void pause()
//	{
//		end = System.nanoTime();
//		duration += end-start;
//	}
//	public void resume()
//	{
//		start = System.nanoTime();
//	}
	
	public long lastResult()
	{
		return duration/1000;
	}
	

}
