package mosaic.paramopt;

import java.io.File;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public interface IParameterSelectionController {
	/**
	 * Selects the file from which the macro is to be loaded.
	 * 
	 * @param file
	 *            the file from which the macro is to be read
	 */
	public void selectMacroFile(File file);
	
	/**
	 * Selects a file as the file from which the macro configuration is to be
	 * read.
	 * 
	 * @param file
	 *            the file from which the configuration is to be read
	 */
	public void selectMacroConfigurationFile(File file);
	
	/**
	 * Aborts the selection of parameters and quits the plug-in.
	 */
	public void abortSelection();

	/**
	 * Starts the optimization process.
	 */
	public void startOptimization();
	
	/**
	 * Returns an array of with the names of the available strategies.
	 * 
	 * @return an array with names of strategies
	 */
	public List<String> getStrategies();
	
	/**
	 * Sets the index of the selected strategy.
	 * 
	 * @param strategy
	 *            the index of the selected strategy 
	 */
	public void selectStrategy(int strategy);
	
	/**
	 * Sets the flag for the export of the evolution history.
	 * 
	 * @param enabled
	 *            the flag for whether the history shall be exported or not
	 */
	public void setHistoryEnabled(boolean enabled);
	
	/**
	 * Returns an abstract table model for access to the parameter settings.
	 * 
	 * @return an abstract table model for access to the parameter settings
	 */
	public AbstractTableModel getParameterTableModel();

	/**
	 * Returns the line number of the specified parameter.
	 * 
	 * @param selected
	 *            the index of the selected parameter
	 * @return the line number of the selected parameter
	 */
	public int getLineNumberOfParameter(int selected);

	/**
	 * Returns the code of the current macro.
	 * 
	 * @return the current macro code.
	 */
	public String getMacro();

	/**
	 * Stores the current configuration in a configuration file.
	 * 
	 * @param selectedFile
	 *            the file to which the configuration is to be written.
	 */
	public void saveMacroConfigurationFile(File selectedFile);
}
