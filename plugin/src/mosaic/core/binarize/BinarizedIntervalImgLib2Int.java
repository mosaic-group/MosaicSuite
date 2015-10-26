package mosaic.core.binarize;


import mosaic.core.image.Point;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.IntegerType;


/**
 * Basically is a binarized image view (with view we mean that the image is
 * not computed) of an image, based on the definition of intervals of an Integer
 * imgLib2 image
 * 1 = in the interval
 * 0 = outside the interval
 *
 * @author Pietro Incardona
 */

public class BinarizedIntervalImgLib2Int<T extends IntegerType<T>> extends IntervalsListInteger implements BinarizedImage {

    private Img<T> labelImage;
    // TODO: Never crated but used later in code.
    private final RandomAccess<T> raLb = null;

    public BinarizedIntervalImgLib2Int(Img<T> aLabelImage) {
        super();
        SetInputImage(aLabelImage);
    }

    private void SetInputImage(Img<T> labelImage) {
        this.labelImage = labelImage;
    }

    /**
     * Evaluate the binarized image at Point p
     *
     * @param p Point where to evaluate
     * @return true or false
     */
    @Override
    public boolean EvaluateAtIndex(Point p) {
        for (int i = 0; i < labelImage.numDimensions(); i++) {
            if (p.iCoords[i] >= labelImage.dimension(i)) {
                return false;
            }
        }

        raLb.localize(p.iCoords);

        final int value = raLb.get().getInteger();
        return Evaluate(value);
    }
}
