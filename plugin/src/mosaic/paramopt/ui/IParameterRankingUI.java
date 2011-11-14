package mosaic.paramopt.ui;

import ij.ImagePlus;

public interface IParameterRankingUI {
	
	/**
	 * Sets the maximum for the slice number and updates the components which
	 * represent the slice number.
	 * 
	 * @param max the number of slices which are available
	 */
	public void setSliceMaximum(int max);
	
	/**
	 * Sets the ImagePlus resource of the specified image.
	 * 
	 * @param index
	 *            the index of the image whose resource is to be set
	 * @param img the resource which is to be set
	 */
	public void setImage(int index, ImagePlus imp);
	
	/**
	 * Sets the progress of the optimization step.
	 * 
	 * @param progress
	 *            the current progress between 0 and the number of images in
	 *            this UI
	 */
	public void setProgress(int progress);

	public void setOverlay(ImagePlus imp);

	public void setVisible(boolean b);

	/**
	 * Closes the ranking UI.
	 */
	void close();
}

