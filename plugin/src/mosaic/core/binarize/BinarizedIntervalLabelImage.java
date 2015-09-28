package mosaic.core.binarize;


import mosaic.core.utils.IntervalsListInteger;
import mosaic.core.utils.LabelImage;
import mosaic.core.utils.Point;


/**
 * Basically is a binarized image view (with view we mean that the image is
 * not computed) of an image, based on the definition of intervals of a
 * LabelImage image
 * 1 = in the interval
 * 0 = outside the interval
 *
 * @author Stephan Semmler, refactored Pietro Incardona
 */

public class BinarizedIntervalLabelImage extends IntervalsListInteger implements BinarizedImage {

    private LabelImage labelImage;

    public BinarizedIntervalLabelImage(LabelImage aLabelImage) {
        super();
        SetInputImage(aLabelImage);
    }

    private void SetInputImage(LabelImage labelImage) {
        this.labelImage = labelImage;
    }

    /**
     * With labelImage, it is guaranteed that index does not exceeds bounds
     */
    @Override
    public boolean EvaluateAtIndex(Point p) {
        if (labelImage.isOutOfBound(p) == true) {
            return false;
        }
        final int value = labelImage.getLabel(p);
        return Evaluate(value);
    }
}
