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
	
	static int countComplete(JobStatus jb[])
	{
		int ncc = 0;
		
		for (int i = 0 ; i < jb.length ; i++)
		{
			if (jb[i] == null || jb[i].getStatus() == jobS.COMPLETE || jb[i].getStatus() == jobS.UNKNOWN  || jb[i].getStatus() == jobS.FAILED )
				ncc++;
		}
		
		return ncc;
	}
	
	static boolean allComplete(JobStatus jb[])
	{
		for (int i = 0 ; i < jb.length ; i++)
		{
			if (jb[i] != null && (jb[i].getStatus() != jobS.COMPLETE && jb[i].getStatus() != jobS.UNKNOWN  && jb[i].getStatus() != jobS.FAILED))
			{
				return false;
			}
		}
		
		return true;
	}

	/**
	 * 
	 * Get the status of the job
	 * 
	 * @return jobS enum
	 */
	
	jobS getStatus()
	{
		return js;
	}
	
	/**
	 * 
	 * Get the notified status of the job (used by Cluster status stack
	 * to understand if it has to change the status of the job from
	 * the previous time )
	 * 
	 * @return
	 */
	
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
