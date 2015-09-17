package mosaic.plugins.utils;

import ij.plugin.filter.PlugInFilter;


/**
 *
 * It is just the ImageJ PlugInFilter with an extended functionality
 *
 * @author Pietro Incardona
 *
 */

public interface PlugInFilterExt extends PlugInFilter
{
    /**
     *
     * This function close all the images processed and the visualized result produced by the plugins.
     * It is a function useful in case of unit tests. When the test is terminated all the images are
     *  closed
     *
     */

    void closeAll();

    /**
     *
     * Set if that the plugin is on test
     *
     * @param test test mode
     *
     */

    void setIsOnTest(boolean test);

    /**
     *
     * Check if the plugin is running under Junit
     *
     */

    boolean isOnTest();
}