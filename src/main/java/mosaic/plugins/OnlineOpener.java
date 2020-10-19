package mosaic.plugins;


import ij.plugin.PlugIn;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class OnlineOpener implements PlugIn {
    @Override
    public void run(String s) {
        try {
            Desktop.getDesktop().browse(new URI(s));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
