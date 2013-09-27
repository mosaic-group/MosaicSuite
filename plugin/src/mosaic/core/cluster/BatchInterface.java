package mosaic.core.cluster;

import mosaic.core.cluster.LSFBatch.job_sts;


interface BatchInterface
{
	public String runCommand(String tdir);
	public String getScript(String img_script_, String batch_script , String session_id, int njob);
	public void parseStatus(String prs, JobStatus jobs[]);
	public int getJobID(String id);
	public JobStatus [] createJobStatus(int n);
	public String statusJobCommand(int ID);
}