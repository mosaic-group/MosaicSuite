package mosaic.region_competition.initializers;


import mosaic.core.utils.LabelImage;
import mosaic.core.utils.RegionIterator;


/**
 * Initialize label image with a box which size is a ratio of labelImage size.
 */
public class BoxInitializer extends Initializer {

    public BoxInitializer(LabelImage aLabelImage) {
        super(aLabelImage);
    }

    /**
     * creates an initial guess (of the size r*labelImageSize)
     * @param aRatio fraction of sizes of the guess
     */
    public void initialize(double aRatio) {
        // Calculate size of region for iteration and offset in original dimensions "space"
        final int[] regionDimensions = iDimensionsSize.clone();
        final int[] offsets = iDimensionsSize.clone();
        for (int i = 0; i < iNumOfDimensions; i++) {
            regionDimensions[i] = (int) (regionDimensions[i] * aRatio);
            offsets[i] = (iDimensionsSize[i] - regionDimensions[i]) / 2;
        }

        // Mark chosen region with '1' label
        final int label = 1;
        final RegionIterator it = new RegionIterator(iDimensionsSize, regionDimensions, offsets);
        while (it.hasNext()) {
            final int idx = it.next();
            iLabelImage.setLabel(idx, label);
        }
    }
}
