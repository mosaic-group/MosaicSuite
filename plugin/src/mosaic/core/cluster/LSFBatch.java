package mosaic.core.cluster;


class LSFBatch implements BatchInterface
{
	String script;
	ClusterProfile cp;
	
	public void setClusterProfile(ClusterProfile cp_)
	{
		cp = cp_;
	}
	
	@Override
	public String getScript(String script_) 
	{
		script = script_;
		return new String("#!/bin/bash \n" +
		"#BSUB -q short \n" +
		"#BSUB -W 00:02 \n" +
		"#BSUB -n 1 \n" +
		"#BSUB -J \"madmax_example[1-27]\" /n" +
		"#BSUB -o madmax_example.out.%J /n" +
		"/n" +
		"echo \"running " + script + "on index $LSB_JOBINDEX\" /n" +
		cp.getRunningDir() + "Fiji.app/ImageJ-linux64" + " --headless -batch  $LSB_JOBINDEX");
		
		// TODO Auto-generated method stub
	}
	
	@Override
	public String runCommand()
	{
		return new String("bsub < " + script);
	}
	
}