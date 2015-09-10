package mosaic.bregman.output;

import java.util.Vector;

import mosaic.bregman.Region;
import mosaic.core.GUI.GUIOutputChoose;
import mosaic.core.ipc.InterPluginCSV;


public class SquasshOutputChoose extends GUIOutputChoose
{
	/* Class to produce InterPluginsCSV of internal type */
	
	public Class< InterPluginCSV< ? extends Outdata<Region>> > InterPluginCSVFactory;
	
	/* Class to produce vactor of internal type */
	
	public Class<Vector<? extends Outdata<Region>>> vectorFactory;
	
	/* Internal type class factory */
	public Class<? extends Outdata<Region>> classFactory;
	
	public ConvertAndWrite<? extends Outdata<Region>> converter;
	
	char delimiter;
}