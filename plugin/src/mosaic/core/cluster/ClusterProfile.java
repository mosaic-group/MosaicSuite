package mosaic.core.cluster;

enum hw
{
	CPU,
	GPU,
	MIKE,
	OTHER
}

interface ClusterProfile
{
	public String getUsername();
	public void setUsername(String Username);
	public String getPassword();
	public void setPassword(String Password);
	public void setAcc(hw Acc_);
	public String getAccessAddress();
	public void setAccessAddress(String AccessAddress_);
	public String getProfileName();
	public void setProfileName(String ProfileName_);
	public String getRunningDir();
	public void setRunningDir(String RunningDir_);
	public String getImageJCommand();
	public void setImageJCommand(String ImageJCommand_);
	public String getQueue(double minutes);
	public void setQueue(double minutes, String name);
	public BatchInterface getBatchSystem();
	public void setBatchSystem(BatchInterface bc_);
}