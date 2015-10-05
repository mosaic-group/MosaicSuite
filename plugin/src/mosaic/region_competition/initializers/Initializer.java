package mosaic.region_competition.initializers;


import mosaic.region_competition.LabelImageRC;


/**
 * Abstract Initializer class that do not depend on input image
 */
abstract class Initializer {

    final protected LabelImageRC labelImage;
    final protected int dim;
    final protected int[] dimensions;

    public Initializer(LabelImageRC labelImage) {
        this.labelImage = labelImage;
        this.dim = labelImage.getDim();
        this.dimensions = labelImage.getDimensions();
    }
}
