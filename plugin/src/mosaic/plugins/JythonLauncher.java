package mosaic.plugins;

import ij.plugin.PlugIn;
import Jython.Refresh_Jython_Scripts;
 
public class JythonLauncher implements PlugIn {
    public void run(String arg) {
        new Refresh_Jython_Scripts().runScript(getClass().getResourceAsStream(arg));
    }
}
