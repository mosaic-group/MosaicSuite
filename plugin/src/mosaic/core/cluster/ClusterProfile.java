package mosaic.core.cluster;

import mosaic.core.utils.DataCompression.Algorithm;

public interface ClusterProfile
{
	/**
	 * 
	 * Get the username to access the cluster
	 * 
	 * @return the username
	 */
	
	public String getUsername();
	
	
	/**
	 * 
	 * Set the username to access the cluster
	 * 
	 * @param Username to set
	 */
	
	public void setUsername(String Username);
	
	/**
	 * 
	 * Get the password to access the cluster
	 * 
	 * @return the password
	 */
	
	public String getPassword();
	
	
	/**
	 * 
	 * Set the password to access the cluster
	 * 
	 * @param Password
	 */
	
	public void setPassword(String Password);
	
	/**
	 * 
	 * Set the type of hardware to use for the jobs
	 * 
	 * @param Acc_
	 */
	
	public void setAcc(hw Acc_);
	
	/**
	 * 
	 * Get the Address to access the cluster
	 * 
	 * @return the string of the address to access the cluster
	 * 
	 */
	
	public String getAccessAddress();
	
	/**
	 * 
	 * Set the address to access the cluster
	 * 
	 * @param AccessAddress_
	 */
	
	public void setAccessAddress(String AccessAddress_);
	
	/**
	 * 
	 * Get the profile name (in general is the name of the cluster)
	 * 
	 * @return
	 */
	
	public String getProfileName();
	
	/**
	 * 
	 * Set the profile name (in general is the name of the cluster)
	 * 
	 * @return
	 */
	
	public void setProfileName(String ProfileName_);
	
	/**
	 * 
	 * Get the directory to run jobs. the (*) are replaced with
	 * the username
	 * 
	 * @return
	 */
	
	public String getRunningDir();
	
	/**
	 * 
	 * Set the Running dir.
	 * (*) are replaced with the username
	 * 
	 * @param RunningDir_
	 */
	
	public void setRunningDir(String RunningDir_);
	public String getImageJCommand();
	
	/**
	 * 
	 * Set the command to run imageJ/fiji
	 * 
	 * @param ImageJCommand_
	 */
	
	public void setImageJCommand(String ImageJCommand_);
	
	/**
	 * 
	 * Get the queue suitable to run a jobs for x minutes
	 * 
	 * @param minutes
	 * @return
	 */
	
	public String getQueue(double minutes);
	
	/**
	 * 
	 * Set the expiration time of a queue
	 * 
	 * @param minutes
	 * @param name of the queue
	 */
	
	public void setQueue(double minutes, String name);
	
	/**
	 * 
	 * Get the interface to control the Batch system of the cluste
	 * 
	 * @return BatchInterface
	 */
	
	public BatchInterface getBatchSystem();
	public void setBatchSystem(BatchInterface bc_);
	
	/**
	 * 
	 * Set the a compressor algorithm for the cluster
	 * 
	 * @param a Batch system
	 */
	
	void setCompressor(Algorithm a);
	
	/**
	 * 
	 * Check if the cluster has a specified compressor
	 * 
	 * @param a Compressor algorithm
	 * @return true if present
	 */
	
	boolean hasCompressor(Algorithm a);

	/**
	 * 
	 * Return all the queues in this cluster
	 * 
	 * @return
	 */
	
	QueueProfile[] getQueues(hw Acc_);
}
