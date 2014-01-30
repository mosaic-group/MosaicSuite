package mosaic.core.cluster;

import mosaic.core.cluster.LSFBatch.LSFJob;

enum OutputType
{
	LAUNCH,
	STATUS
}

interface BatchInterface extends ShellProcessOutput
{
	public String runCommand(String tdir);
	public String getScript(String img_script_, String batch_script , String session_id, double Ext, int njob);
	public int getJobID();
	public void createJobStatus();
	public String statusJobCommand();
	void setJobStatus(JobStatus [] jb_);
	void setOutputType(OutputType tp);
	public void waitParsing();
	public void reset();
	JobStatus[] getJobStatus();
	public BatchInterface[] getAllJobs(SecureShellSession ss);
	public int getNJobs();
	public JobStatus[] getJobsStatus();
	public void clean(SecureShellSession ss);
	public String getDir();
}