package mosaic.plugins;

import ij.IJ;
import ij.plugin.PlugIn;

import java.io.InputStream;

import Jython.Refresh_Jython_Scripts;

public class JythonLauncher implements PlugIn {
    public void run(String arg) {
        InputStream stream = getClass().getResourceAsStream(arg);
        new Refresh_Jython_Scripts().runScript(stream);
    }
}
