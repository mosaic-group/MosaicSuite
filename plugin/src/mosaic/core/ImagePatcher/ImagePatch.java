package mosaic.core.ImagePatcher;


import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.iterators.RegionIterator;
import mosaic.core.utils.MosaicUtils;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;


/**
 * This class store the patch of an image
 *
 * @author Pietro Incardona
 */

public class ImagePatch<T extends NativeType<T> & NumericType<T>, E extends NativeType<E> & IntegerType<E>> {

    private Img<T> it;
    private Img<E> lb;
    private Img<E> rs;

    private final Point p1;
    private final Point p2;

    /**
     * Create an image patch
     *
     * @param margins
     */

    ImagePatch(int dim) {
        p1 = new Point(new int[dim]);
        p2 = new Point(new int[dim]);

        // Initialize point

        for (int i = 0; i < dim; i++) {
            p1.iCoords[i] = Integer.MAX_VALUE;
            p2.iCoords[i] = Integer.MIN_VALUE;
        }
    }

    /**
     * Extends the patch to include the point
     *
     * @param Point p
     */

    void extendPoint(Point p) {
        // check if lower bound respected

        for (int i = 0; i < p.getNumOfDimensions(); i++) {
            // lower bound

            if (p.iCoords[i] < p1.iCoords[i]) {
                p1.iCoords[i] = p.iCoords[i];
            }

            // upper bound

            if (p.iCoords[i] > p2.iCoords[i]) {
                p2.iCoords[i] = p.iCoords[i];
            }
        }
    }

    /**
     * Add point to P1
     *
     * @param p
     */

    void SubToP1(int p[]) {
        for (int i = 0; i < p.length; i++) {
            p1.iCoords[i] -= p[i];
        }
    }

    /**
     * Add Point to P2
     *
     * @param p
     */

    void AddToP2(int p[]) {
        for (int i = 0; i < p.length; i++) {
            p2.iCoords[i] += p[i];
        }
    }

    /**
     * Create the patch from an image (It copy the portion of the region)
     *
     * @param img source image
     * @param lbl optionally a label image
     */

    void createPatch(Img<T> img, Img<E> lbl) {
        final RandomAccess<T> randomAccess = img.randomAccess();
        RandomAccess<E> randomAccess_lb = null;
        if (lbl != null) {
            randomAccess_lb = lbl.randomAccess();
        }

        // Get the image dimensions

        final int[] dimensions = MosaicUtils.getImageIntDimensions(img);

        // Crop p1 and p2 to remain internally

        for (int i = 0; i < p1.iCoords.length; i++) {
            if (p1.iCoords[i] < 0) {
                p1.iCoords[i] = 0;
            }
            if (p2.iCoords[i] > dimensions[i]) {
                p2.iCoords[i] = dimensions[i];
            }
        }

        // create region iterators and patch image

        final Point sz = p2.sub(p1);

        final ImgFactory<T> imgFactory = new ArrayImgFactory<T>();
        final ImgFactory<E> imgFactory_lbl = new ArrayImgFactory<E>();

        // create an Img of the same type of T and create the patch

        it = imgFactory.create(sz.iCoords, img.firstElement());
        final RandomAccess<T> randomAccess_it = it.randomAccess();

        RandomAccess<E> randomAccess_it_lb = null;
        if (lbl != null) {
            lb = imgFactory_lbl.create(sz.iCoords, lbl.firstElement());
            randomAccess_it_lb = lb.randomAccess();
        }

        final RegionIterator rg_b = new RegionIterator(sz.iCoords);
        final RegionIterator rg = new RegionIterator(dimensions, sz.iCoords, p1.iCoords);
        while (rg.hasNext()) {
            rg.next();
            rg_b.next();
            final Point p = rg.getPoint();
            final Point pp = rg_b.getPoint();

            randomAccess.setPosition(p.iCoords);
            if (randomAccess_lb != null) {
                randomAccess_lb.setPosition(p.iCoords);
            }

            randomAccess_it.setPosition(pp.iCoords);
            if (randomAccess_it_lb != null) {
                randomAccess_it_lb.setPosition(pp.iCoords);
            }

            randomAccess_it.get().set(randomAccess.get());

            if (randomAccess_it_lb != null && randomAccess_lb != null ) {
                randomAccess_it_lb.get().set(randomAccess_lb.get());
            }
        }
    }

    /**
     * Set the image result for the patch
     *
     * @param img
     */

    public void setResult(Img<E> img) {
        rs = img;
    }

    /**
     * Show the patch
     */

    public void show() {
        ImageJFunctions.show(it);
    }

    /**
     * Get the result image
     *
     * @return the result image
     */

    public Img<E> getResult() {
        return rs;
    }

    /**
     * Get the patch image
     *
     * @return the patch image
     */

    public Img<T> getImage() {
        return it;
    }

    /**
     * Get the label image
     *
     * @return the label image patch
     */

    public Img<E> getLabelImage() {
        return lb;
    }

    /**
     * Show the label image result
     */

    public void showResult() {
        ImageJFunctions.show(rs);
    }

    /**
     * Return P1
     *
     * @return the point p1
     */

    public Point getP1() {
        return p1;
    }
}
