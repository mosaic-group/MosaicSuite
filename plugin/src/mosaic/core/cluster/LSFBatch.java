package mosaic.core.cluster;


class LSFBatch implements BatchInterface
{
	String script;
	ClusterProfile cp;
	
	public LSFBatch(ClusterProfile cp_)
	{
		cp = cp_;
	}
	
	@Override
	public String getScript(String script_, String session_id, int njob) 
	{
		script = script_;
		return new String("#!/bin/bash \n" +
		"#BSUB -q short \n" +
		"#BSUB -n 1 \n" +
		"#BSUB -J \"" + session_id + "[1-" + njob  + "]\" \n" +
		"#BSUB -o " + session_id + ".out.%J \n" +
		"\n" +
		"echo \"running " + script + " on index $LSB_JOBINDEX\" \n" +
		cp.getRunningDir() + "Fiji.app/ImageJ-linux64" + " --headless -batch " + script + " $LSB_JOBINDEX");
		
		// TODO Auto-generated method stub
	}
	
	@Override
	public String runCommand()
	{
		return new String("bsub < " + script);
	}
	
}