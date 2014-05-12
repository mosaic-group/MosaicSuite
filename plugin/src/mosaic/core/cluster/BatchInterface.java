package mosaic.core.cluster;

import mosaic.core.cluster.LSFBatch.LSFJob;

enum OutputType
{
	LAUNCH,
	STATUS
}

interface BatchInterface extends ShellProcessOutput
{
	/**
	 * 
	 * Get the string to run a job array. This is a script based run
	 * 
	 * @param tdir script location
	 * @return
	 */
	public String runCommand(String tdir);
	
	/**
	 * 
	 * Create a batch script to run imageJ/fiji on the cluster
	 * 
	 * @param img_script_ imageJ macro script
	 * @param session_id String that identify the session
	 * @param Ext estimated execution time
	 * @param njob number of jobs
	 * @param ns number of slot to allocate per process
	 * @return
	 */
	
	public String getScript(String img_script_ , String session_id, double Ext, int njob, int ns);
	
	/**
	 * 
	 * Set job ID
	 * 
	 * @param id integer id
	 */
	
	public void setJobID(int id);
	
	/**
	 * 
	 * Return the jobID of the launched jobArray
	 * 
	 * @return Integer identifying the job
	 * 
	 */
	
	public int getJobID();
	
	/**
	 * 
	 * Create internally an array of jobs, a class that implements this 
	 * interface is suppose to store also the status of a jobArray
	 * 
	 */
	
	public void createJobStatus();
	
	/**
	 * 
	 * Get the command string to issue in order to get the jobArray status
	 * 
	 * @return command String
	 */
	
	public String statusJobCommand();
	
	/**
	 * 
	 * Set internally the status of an item in the jobArray
	 * 
	 */
	
	void setJobStatus(JobStatus [] jb_);
	
	/**
	 * 
	 * A class that implement this interface is suppose to parse the output
	 * produced by the batch system during several phases ( For now two phase are
	 * required LAUNCH (Job creation) and STATUS (Job status request))
	 * 
	 * @param tp parsing phase
	 * @see ShellProcessOutput
	 * 
	 */
	
	void setOutputType(OutputType tp);
	
	/**
	 * 
	 * Wait to complete the parsing of the output produced by the command
	 *  performed
	 * 
	 */
	
	public void waitParsing();
	
	/**
	 * 
	 * Before each command that require the status of a job this method
	 * automatically is called
	 * 
	 */
	
	public void reset();
	
	JobStatus[] getJobStatus();
	
	/**
	 * 
	 * Each Batch interface identify one or none JobArray. A class implementing
	 * this interface is suppose to has the capabilities to require the list of
	 * jobsArray running on the cluster, and producing a BatchInterface for each
	 * of them. Return null if the feature is not supported
	 * 
	 * @param ss shell session
	 * @param command (get all jobs related to this command)
	 * @return an array of all jobs array
	 */
	
	public BatchInterface[] getAllJobs(SecureShellSession ss, String command);
	
	/**
	 * 
	 * Get the number of jobs Array
	 * 
	 * @return
	 */
	
	public int getNJobs();
	
	/**
	 * 
	 * Get the status of each item of the job array
	 * 
	 * @return
	 */
	
	public JobStatus[] getJobsStatus();
	
	/**
	 * 
	 * The class that implement is suppose to cleanup the environment like,
	 * removing all temporal files created on the cluster
	 * 
	 * @param ss remote shell session
	 */
	
	public void clean(SecureShellSession ss);
	
	
	
	public String getDir();
}