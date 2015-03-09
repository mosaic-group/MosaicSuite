package mosaic.bregman.output;

import java.util.Vector;

import mosaic.core.GUI.GUIOutputChoose;
import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.ipc.InterPluginCSV;


public class SquasshOutputChoose extends GUIOutputChoose
{
	/* Class to produce InterPluginsCSV of internal type */
	
	public Class<InterPluginCSV<? extends ICSVGeneral>> InterPluginCSVFactory;
	
	/* Class to produce vactor of internal type */
	
	public Class<Vector<? extends ICSVGeneral>> vectorFactory;
	
	/* Internal type class factory */
	public Class<? extends ICSVGeneral> classFactory;
	char delimiter;
}