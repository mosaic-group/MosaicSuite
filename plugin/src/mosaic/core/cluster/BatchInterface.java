package mosaic.core.cluster;

import mosaic.core.cluster.LSFBatch.LSFJob;


interface BatchInterface
{
	public String runCommand(String tdir);
	public String getScript(String img_script_, String batch_script , String session_id, int njob);
	public String parseStatus(String prs, JobStatus jobs[]);
	public int getJobID(String id);
	public JobStatus [] createJobStatus(int n);
	public String statusJobCommand(int ID);
	void setJobStatus(JobStatus [] jb_);	
	JobStatus[] getJobStatus();
}