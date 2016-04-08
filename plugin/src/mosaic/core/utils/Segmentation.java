package mosaic.core.utils;


import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;


/**
 * This interface define the function that a segmentation algorithm should expose
 *
 * @author Pietro Incardona
 */

public interface Segmentation extends PlugInFilter {

    /**
     * Get Mask images name output
     * @param aImp image
     * @return set of possible output
     */
    String[] getMask(ImagePlus imp);

    /**
     * Get CSV regions list name output
     * @param aImp image
     * @return set of possible output
     */
    String[] getRegionList(ImagePlus imp);

    /**
     * Get the name of the segmentation plugin
     * @return the name of the segmentation plugin
     */
    String getName();
}
