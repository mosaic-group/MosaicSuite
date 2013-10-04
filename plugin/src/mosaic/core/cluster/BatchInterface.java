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
	public String getScript(String img_script_, String batch_script , String session_id, int njob);
	public int getJobID();
	public JobStatus [] createJobStatus(int n);
	public String statusJobCommand();
	void setJobStatus(JobStatus [] jb_);
	void setOutputType(OutputType tp);
	public void waitParsing(int np);
	public void reset();
	JobStatus[] getJobStatus();
}