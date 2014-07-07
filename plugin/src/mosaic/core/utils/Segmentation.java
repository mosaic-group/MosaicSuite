package mosaic.core.utils;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;



public interface Segmentation extends PlugInFilter
{
	/**
	 * 
	 * Get Mask images name output
	 * 
	 * @param aImp image
	 * @return set of possible output
	 */
	
	String[] getMask(ImagePlus imp);
	
	/**
	 * 
	 * Get CSV regions list name output
	 * 
	 * @param aImp image
	 * @return set of possible output
	 */
	
	String[] getRegionList(ImagePlus imp);
	
	/**
	 * 
	 * Close all windows and images produced
	 * 
	 */
	
	void closeAll();
};