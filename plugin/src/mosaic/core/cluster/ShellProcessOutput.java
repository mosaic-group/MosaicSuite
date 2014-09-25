package mosaic.core.cluster;


/**
 * 
 * Interface used to parse the output of the shell, any classes that want to parse some output of a shell
 * must implement this interface
 * 
 * @author Pietro Incardona
 *
 */

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
