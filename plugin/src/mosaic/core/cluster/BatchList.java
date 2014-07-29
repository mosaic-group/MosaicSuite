package mosaic.core.cluster;

class BatchList
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
}