package mosaic.region_competition.GUI;

import ij.gui.GenericDialog;
import mosaic.region_competition.Settings;

abstract class GUImeMore
{
	protected GenericDialog gd;
	protected Settings settings;	
	
	protected GUImeMore(Settings settings)
	{
		this.settings = settings;
		gd = new GenericDialog("");
	}
	
	/**
	 * Build here the {@link GenericDialog}
	 */
	public abstract void createDialog();
	
	/**
	 * Read out values from {@link GenericDialog} to {@link Settings} <br>
	 * Always check first for GenericDialog.wasCancled()
	 */
	public final void processDialog()
	{
		if (gd.wasCanceled())
			return;
		else
			process();
	}
	
	/** 
	 * Read values from {@link GenericDialog} (getNext() methods)
	 */
	protected abstract void process();
	
	
	/**
	 * Shows the dialog (waits for confirmation)
	 */
	public void showDialog()
	{
		gd.showDialog();
	}
	
	/**
	 * Gets an empty {@link GenericDialog}
	 * @return
	 */
	public static GenericDialog getNoGUI()
	{
		GenericDialog noGUI;
		noGUI = new GenericDialog("No additional Options");
		String text = "No additional options available";
		noGUI.addMessage(text);
		noGUI.hideCancelButton();
		
		return noGUI;
	}
}

