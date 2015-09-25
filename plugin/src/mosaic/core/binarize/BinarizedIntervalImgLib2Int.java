package mosaic.core.binarize;


import mosaic.core.utils.IntervalsListInteger;
import mosaic.core.utils.Point;
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

    Img<T> labelImage;
    RandomAccess<T> raLb;

    public BinarizedIntervalImgLib2Int(Img<T> aLabelImage) {
        super();
        SetInputImage(aLabelImage);
        // m_NThresholds = 0;
        // m_Thresholds = new ArrayList<Pair<Double,Double>>();
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
            if (p.x[i] >= labelImage.dimension(i)) {
                return false;
            }
        }

        raLb.localize(p.x);

        final int value = raLb.get().getInteger();
        return Evaluate(value);
    }
}
