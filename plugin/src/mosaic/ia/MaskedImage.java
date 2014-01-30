package mosaic.ia;

import ij.ImagePlus;

public class MaskedImage {
	private ImagePlus maskedImage;
	private int onPixels; // #=255
	
	public MaskedImage(ImagePlus maskedImage, int onPixels) {
		super();
		this.maskedImage = maskedImage;
		this.onPixels = onPixels;
	}

	public ImagePlus getMaskedImage() {
		return maskedImage;
	}

	public int getOnPixels() {
		return onPixels;
	}

}
