package mosaic.core.cluster;


interface BatchInterface
{
	public String runCommand();
	public String getScript(String script_, String session_id, int njob);
}