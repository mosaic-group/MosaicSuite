package mosaic.core.utils;

import ij.ImagePlus;



public interface Segmentation
{
	String[] getMask(ImagePlus imp);
	String[] getRegionList(ImagePlus imp);
};