package mosaic.plugins;

import ij.plugin.PlugIn;
import mosaic.ia.gui.GUIDesign;


/**
 * Interaction Analysis plugin for Fiji/ImageJ. 
 * Based on:
 * 
 * Beyond co-localization: inferring spatial interactions between sub-cellular structures from microscopy images. 
 * J. A. Helmuth, G. Paul, and I. F. Sbalzarini. 
 * BMC Bioinformatics, 11:372, 2010.
 * 
 * MosaicIA: an ImageJ/Fiji plugin for spatial pattern and interaction analysis. 
 * A. Shivanandan, A. Radenovic, and I. F. Sbalzarini. 
 * BMC Bioinformatics, 14:349, 2013.
 * 
 * @author arun.shivanandan@inf.ethz.ch
 */
public class IAPMosaic_ implements PlugIn { // NO_UCD
    
    @Override
    public void run(String arg0) {
        new GUIDesign();
    }
}
