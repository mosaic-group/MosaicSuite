package mosaic.core.cluster;


interface BatchInterface
{
	public String runCommand();
	public String getScript(String macro);
	public void setClusterProfile(ClusterProfile cp_);
}