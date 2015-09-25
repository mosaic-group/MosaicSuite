package mosaic.plugins;

import ij.plugin.PlugIn;

import java.io.IOException;
import java.io.InputStream;

import Jython.Refresh_Jython_Scripts;

public class JythonLauncher implements PlugIn { // NO_UCD
    @Override
    public void run(String arg) {
        final InputStream stream = getClass().getResourceAsStream(arg);
        new Refresh_Jython_Scripts().runScript(stream);
        try {
            stream.close();
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
