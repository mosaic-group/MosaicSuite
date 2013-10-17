package mosaic.core.cluster;

import mosaic.core.utils.Compressor;
import mosaic.core.utils.Compressor.Algorithm;

public class MadMaxProfile extends GeneralProfile
{
	MadMaxProfile()
	{
		setAcc(hw.CPU);
		setQueue(60,"short");
		setQueue(480,"medium");
		setQueue(Double.MAX_VALUE,"long");
		
		setAcc(hw.GPU);
		setQueue(720,"gpu");
		setBatchSystem(new LSFBatch(this));
		setAcc(hw.CPU);
	}
	
	@Override
	public String getProfileName() 
	{
		// TODO Auto-generated method stub
		return "Mad Max";
	}

	@Override
	public void setProfileName(String ProfileName_) 
	{
	}

	public String getAccessAddress()
	{
		return "wetcluster";
	}
	
	@Override
	public String getRunningDir() 
	{
		return "/scratch/users/"+UserName + "/";
	}

	@Override
	public void setRunningDir(String RunningDir_) 
	{
	}

	@Override
	public String getImageJCommand() 
	{
		return "fiji";
	}

	@Override
	public void setImageJCommand(String ImageJCommand_)
	{
	}
	
	@Override
	public void setCompressor(Algorithm a)
	{
	}
	
	@Override
	public boolean hasCompressor(Compressor.Algorithm a)
	{
		if (a == null)	return true;
		
		if (a.name.equals("TAR"))
			return true;
		else if (a.name.equals("ZIP"))
			return true;
		
		return false;
	}
}
