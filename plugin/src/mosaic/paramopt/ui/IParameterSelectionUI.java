package mosaic.paramopt.ui;

public interface IParameterSelectionUI {
	
	/**
	 * Sets the index of the strategy which is currently selected.
	 * 
	 * @param strategy
	 *            the index of the strategy which has to be selected
	 */
	public void setStrategy(int strategy);
	
	/**
	 * Sets the flag of whether the option to export a history file is active
	 * or not.
	 * 
	 * @param enabled
	 *            the new state of the history export flag 
	 */
	public void setExportHistory(boolean enabled);
	
	/**
	 * Notifies the ui about a change to the parameters.
	 */
	public void notifyParameterChange();
	
	/**
	 * Sets the visibility of the ui.
	 * 
	 * @param visible
	 *            the new visibility of the ui
	 */
	public void setVisible(boolean visible);
	
	/**
	 * Notifies the ui that about the status of the plug-in and if optimization
	 * can be started or not.
	 * 
	 * @param ready
	 *            a flag denoting if the optimization can be started or not
	 */
	public void notifyOptimizationready(boolean ready);

	/**
	 * Closes the selection UI.
	 */
	public void close();

}
