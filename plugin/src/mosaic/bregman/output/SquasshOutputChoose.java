package mosaic.bregman.output;

import java.util.Vector;

import mosaic.core.GUI.GUIOutputChoose;
import mosaic.core.ipc.InterPluginCSV;
import mosaic.core.ipc.Outdata;


public class SquasshOutputChoose extends GUIOutputChoose
{
	public Class<InterPluginCSV<?>> InterPluginCSVFactory;
	public Class<Vector<?>> vectorFactory;
	public Class<?> classFactory;
	char delimiter;
}