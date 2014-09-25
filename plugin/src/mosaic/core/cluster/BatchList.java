package mosaic.core.cluster;

/**
 * 
 * A list of all the batch system classes implemented
 * 
 * @author Pietro Incardona
 *
 */

public class BatchList
{
	/**
	 * 
	 * Given a string it return a batch system
	 * 
	 * @param bc String that identify the batch system
	 * @param cp ClusterProfile
	 * @return BatchInterface object
	 */
	
	public static BatchInterface getBatchSystem(String bc, ClusterProfile cp)
	{
		if (bc.equals("LSF"))
		{
			return new LSFBatch(cp);
		}
		return null;
	}
	
	/**
	 * 
	 * Return a list of all implemented batch system
	 * 
	 * @return A list of all implemented batch systems
	 */
	
	public static String [] getList()
	{
		return new String[]{"LSF"};
	}
}