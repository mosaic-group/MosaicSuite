package mosaic.core.cluster;


interface BatchInterface
{
	public boolean runCommand();
	public boolean transfertFile();
	public boolean transfertDirectory();
}