package mosaic.plugins;


import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.PlugInInterpreter;
import ij.plugin.PluginInstaller;

public class JythonLauncher implements PlugIn {
    
    private static boolean downloadJar(String url) {
        String name = url.substring(url.lastIndexOf("/")+1);
        boolean ok = false;
        String msg = name+" was not found in the plugins\nfolder or it is outdated. Click \"OK\" to download\nit from the ImageJ website.";
        GenericDialog gd = new GenericDialog("Download "+name+"?");
        gd.addMessage(msg);
        gd.showDialog();
        if (!gd.wasCanceled()) {
                ok = (new PluginInstaller()).install(IJ.URL+url);
                if (!ok) IJ.error("Unable to download "+name+" from "+IJ.URL+url);
        }
        return ok;
    }   
    
    private static void runScript(Object plugin, String script, String arg) {
        if (plugin instanceof PlugInInterpreter) {
                PlugInInterpreter interp = (PlugInInterpreter)plugin;
                interp.run(script, arg);
        }
    }
    
    private static void runPython(String script, String arg) {
        if (arg==null) arg = ""; 
        Object jython = IJ.runPlugIn("Jython", "");
        if (jython==null) {
                boolean ok = downloadJar("/plugins/jython/Jython.jar");
                if (ok) jython = IJ.runPlugIn("Jython", "");
        }
        if (jython != null) runScript(jython, script, arg);
    }   
    
    @Override
    public void run(String arg) {
        
        InputStream stream = getClass().getResourceAsStream(arg);
        String theString  = null;
        try {
            theString = IOUtils.toString(stream);
            runPython(theString, "");
        } catch (IOException e) {
            e.printStackTrace();
        } 
        try {
            stream.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
