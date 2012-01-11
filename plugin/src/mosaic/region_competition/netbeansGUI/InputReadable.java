package mosaic.region_competition.netbeansGUI;

public interface InputReadable
{
	
	/**
	 * Reads the input values into the correspondent data structures
	 * @return true on success, false on error
	 */
	public boolean processInput();
	
	// Input
	
	public LabelImageInput getLabelImageInput();
	public String getLabelImageFilename();
	
	/**
	 * @return The filepath as String or empty String if no file was chosen.
	 */
	public String getInputImageFilename();
	
	// UI
	
	public boolean useStack();
	public boolean showStatistics();

	
	// Debugging
	
	public boolean useOldRegionIterator();
	public int getKBest();

	public boolean useRegularization();
	
	public enum LabelImageInput
	{
		Rectangle, Ellipses, UserDefinedROI, File, Bubbles
	}
	
}


