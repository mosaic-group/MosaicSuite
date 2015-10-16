package mosaic.region_competition.initializers;


import mosaic.region_competition.LabelImageRC;


/**
 * Base for Initializers
 */
abstract class Initializer {

    final protected LabelImageRC iLabelImage;
    final protected int iNumOfDimensions;
    final protected int[] iDimensionsSize;

    public Initializer(LabelImageRC aLabelImage) {
        this.iLabelImage = aLabelImage;
        this.iNumOfDimensions = iLabelImage.getNumOfDimensions();
        this.iDimensionsSize = iLabelImage.getDimensions();
    }
}
