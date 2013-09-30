package mosaic.core.cluster;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mosaic.core.cluster.JobStatus.jobS;


class LSFBatch implements BatchInterface, ShellProcessOutput
{
	LSFJob [] jb;
	String script;
	ClusterProfile cp;
	
	class LSFJob extends JobStatus
	{
		public String job_id;
		public String user;
		public String stat;
		public String queue;
		public String exe_host;
		public String job_name ;
		public String Sub_time;
		
		boolean allComplete(LSFJob jb[], int jobsID)
		{
			for (int i = 0 ; i < jb.length ; i++)
			{
				if (jb[i] != null)
					continue;
				
				if (jb[i].job_id.equals(((Integer)jobsID).toString()) == true)
				{
					return false;
				}
			}
			return true;
		}
	};
	
	
	public LSFBatch(ClusterProfile cp_)
	{
		cp = cp_;
	}
	
	@Override
	public String getScript(String img_script_, String batch_script , String session_id, int njob) 
	{
		script = session_id;
		return new String("#!/bin/bash \n" +
		"#BSUB -q short \n" +
		"#BSUB -n 4 \n" +
		"#BSUB -J \"" + session_id + "[1-" + njob  + "]\" \n" +
		"#BSUB -o " + session_id + ".out.%J \n" +
		"\n" +
		"echo \"running " + script + " on index $LSB_JOBINDEX\" \n" +
		cp.getRunningDir() + "Fiji.app/ImageJ-linux64" + " --headless -batch " + img_script_ + " $LSB_JOBINDEX");
		
		// TODO Auto-generated method stub
	}
	
	@Override
	public String runCommand(String tdir)
	{
		return new String("bsub < " + script);
	}
	
	public String statusJobCommand(int ID)
	{
		return new String("bjobs " + ID);
	}
	
	public JobStatus [] createJobStatus(int n)
	{
		return new LSFJob[n];
	}
	
	public int jobArrayID(String aID)
	{
		Pattern jobID = Pattern.compile("\\x5B[0-9]+\\x5D");
		
		Matcher matcher = jobID.matcher(aID);
		if (matcher.find())
		{
			String sub = matcher.group(0);
			sub = sub.substring(1,sub.length()-1);
			return Integer.parseInt(sub);
		}
		
		return 0;
	}
	
	public jobS jobArrayStatus(String aID)
	{
		if (aID.equals("PEND"))
		{
			return jobS.PENDING;
		}
		else if (aID.equals("RUN"))
		{
			return jobS.RUNNING;
		}
		else if (aID.equals("DONE"))
		{
			return jobS.COMPLETE;
		}
		return jobS.UNKNOWN;
	}
	
	/**
	 * 
	 *	Parse the LSF status bjobs JOBID command
	 *
	 * @param prs String to parse
	 * @param jobs array with the updated status of the jobs
	 * @return String the string with the unparsed part
	 */
	
	public String parseStatus(String prs, JobStatus jobs_[])
	{
		boolean unparse_last = true;
		LSFJob[] jobs = (LSFJob [])jobs_;
		
		if (prs.endsWith("\n"))
			unparse_last = false;
		
		String[] elements = prs.split("\n");
		Vector<Vector<String>> Clm_flt = new Vector<Vector<String>>();
		
		for (int i = 0 ; i < elements.length ; i++)
		{
			Vector<String> vt = new Vector<String>();
			String [] sub_elements = elements[i].split(" ");
			for (int j = 0 ; j < sub_elements.length ; j++)
			{
				if (sub_elements[j].length() != 0)
					vt.add(sub_elements[j]);
			}
			if (vt.size() == 10)
			{
				String str = new String(vt.get(vt.size()-3) + " "+ vt.get(vt.size()-2) + " " + vt.get(vt.size()-1));
				vt.remove(vt.size()-1);
				vt.remove(vt.size()-1);
				vt.remove(vt.size()-1);
				vt.add(str);
			}
			
			int ja_id = 0;
			if (vt.size() > 7)
				ja_id = jobArrayID(vt.get(6));
			
			if (ja_id != 0)
			{
				ja_id = ja_id -1;
				jobs[ja_id] = new LSFJob();
				jobs[ja_id].job_id = new String(vt.get(0));
				jobs[ja_id].stat = new String(vt.get(2));
				jobs[ja_id].setStatus(jobArrayStatus(vt.get(2)));
				jobs[ja_id].user = new String(vt.get(1));
				jobs[ja_id].queue = new String (vt.get(3));
				jobs[ja_id].exe_host = new String(vt.get(5));
				jobs[ja_id].job_name = new String(vt.get(6));
				jobs[ja_id].Sub_time = new String(vt.get(7));
			}
		}
		
		if (unparse_last == true)
			return elements[elements.length-1];
		else
			return new String("");
	}

	@Override
	public int getJobID(String id) 
	{
		Pattern jobID = Pattern.compile("<[0-9]+>");
		
		Matcher matcher = jobID.matcher(id);
		if (matcher.find())
		{
			String sub = matcher.group(0);
			sub = sub.substring(1,sub.length()-1);
			return Integer.parseInt(sub);
		}
		
		return 0;
	}

	LSFJob createJob()
	{
		return new LSFJob();
	}

	public void setJobStatus(JobStatus [] jb_)
	{
		jb = (LSFJob[]) jb_;
	}
	
	public JobStatus[] getJobStatus()
	{
		return jb;
	}
	
	@Override
	public String Process(String str) 
	{
		System.out.println(str);
		return parseStatus(str,jb);
	}
}
