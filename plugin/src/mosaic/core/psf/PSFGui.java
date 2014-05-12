package mosaic.core.psf;


interface PSFGui
{
	/**
	 * 
	 * Create a GUI to get the parameters
	 * 
	 */
	
	void getParamenters();
	
	
	/**
	 * 
	 * After get Parameters you get a String of the parameters set by 
	 * the user
	 * 
	 * @return String of the parameters
	 */
	
	String getStringParameters();
}
