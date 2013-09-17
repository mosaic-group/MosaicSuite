package mosaic.core.cluster;

import java.util.Vector;


public class GeneralProfile implements ClusterProfile
{
	String ProfileName;
	String RunningDir;
	String ImageJCommand;
	String UserName;
	String AccessAddress;
	hw acc;
	
	class Tqueue
	{
		Tqueue(double minutes_, String name_)
		{
			minutes = minutes_;
			name = name_;
		}
		
		double minutes;
		String name;
	}
	
	Vector<Vector<Tqueue>> cq;
	
	GeneralProfile()
	{
		cq = new Vector<Vector<Tqueue>>();
	}
	
	public void setUserName(String UserName_)
	{
		UserName = UserName_;
	}
	
	public String getUserName()
	{
		return UserName;
	}
	
	@Override
	public String getProfileName() 
	{
		// TODO Auto-generated method stub
		return ProfileName;
	}

	@Override
	public void setProfileName(String ProfileName_) 
	{
		ProfileName = ProfileName_;
	}

	@Override
	public String getRunningDir() 
	{
		return RunningDir;
	}

	@Override
	public void setRunningDir(String RunningDir_) 
	{
		RunningDir = RunningDir_;
	}

	@Override
	public String getImageJCommand() 
	{
		return ImageJCommand;
	}

	@Override
	public void setImageJCommand(String ImageJCommand_)
	{
		ImageJCommand = ImageJCommand_;
	}
	
	public String getQueue(double minutes)
	{
		for (int i = 0 ; i < cq.size() ; i++)
		{
			if (cq.get(acc.ordinal()).get(i).minutes > minutes)
			{
				return cq.get(acc.ordinal()).get(i).name;
			}
		}
		
		return null;
	}
	
	public void setAcc(hw acc_)
	{
		acc = acc_;
	}
	
	public void setQueue(double minutes, String name)
	{		
		if (cq.size() <= acc.ordinal())
			cq.add(new Vector<Tqueue>());
		
		for (int i = 0 ; i < cq.get(acc.ordinal()).size() ; i++)
		{
			if (cq.get(acc.ordinal()).get(i).minutes > minutes)
			{
				cq.get(acc.ordinal()).insertElementAt(new Tqueue(minutes, name), i);
				return;
			}
		}
		
		cq.get(acc.ordinal()).add(new Tqueue(minutes,name));
	}

	@Override
	public String getAccessAddress() 
	{
		return AccessAddress;
	}

	@Override
	public void setAccessAddress(String AccessAddress_) 
	{
		AccessAddress = AccessAddress_;
	}
}
