package mosaic.core.cluster;


public interface ShellProcessOutput
{
	/**
	 * 
	 * Callback to parse the output
	 * 
	 * @param str String of the output
	 * @return the unprocessed string ( new output will be appended on str )
	 */
	
	String Process(String str);
}
