package mosaic.core.cluster;

import mosaic.core.cluster.JobStatus.jobS;


abstract class JobStatus
{
	enum jobS
	{
		PENDING,
		RUNNING,
		COMPLETE,
		FAILED,
		UNKNOWN
	};
	
	public int job_id;
	private jobS js;
	private jobS js_N;
	
	static boolean allComplete(JobStatus jb[])
	{
		for (int i = 0 ; i < jb.length ; i++)
		{
			if (jb[i] != null && jb[i].getStatus() != jobS.COMPLETE)
			{
				return false;
			}
		}
		
		return true;
	}
	
	jobS getStatus()
	{
		return js;
	}
	
	jobS getNotifiedStatus()
	{
		return js_N;
	}
	
	void setNotifiedStatus(jobS js_)
	{
		js_N = js_;
	}
	
	void setStatus(jobS js_)
	{
		js = js_;
	}
	
	void setID(int id)
	{
		job_id = id;
	}
	
	int getID()
	{
		return job_id;
	}
};
