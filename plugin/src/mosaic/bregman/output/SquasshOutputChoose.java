package mosaic.bregman.output;

import java.util.Vector;

import mosaic.core.GUI.GUIOutputChoose;
import mosaic.core.ipc.InterPluginCSV;
import mosaic.core.ipc.Outdata;


public class SquasshOutputChoose extends GUIOutputChoose
{
	public Class<InterPluginCSV<? extends Outdata<?>>> InterPluginCSVFactory;
	public Class<Vector<? extends Outdata<?>>> vectorFactory;
	public Class<? extends Outdata<?>> classFactory;
}