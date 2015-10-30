package mosaic.region_competition.initializers;


import mosaic.core.image.LabelImage;


/**
 * Base for Initializers
 */
abstract class Initializer {

    final protected LabelImage iLabelImage;
    final protected int iNumOfDimensions;
    final protected int[] iDimensionsSize;

    public Initializer(LabelImage aLabelImage) {
        this.iLabelImage = aLabelImage;
        this.iNumOfDimensions = iLabelImage.getNumOfDimensions();
        this.iDimensionsSize = iLabelImage.getDimensions();
    }
}
