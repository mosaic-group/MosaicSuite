package mosaic.core.ImagePatcher;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;

import mosaic.core.binarize.BinarizedIntervalLabelImage;
import mosaic.core.utils.Connectivity;
import mosaic.core.utils.FloodFill;
import mosaic.core.utils.LabelImage;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Point;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;


/**
 * This class take two images one representing the image and one
 * representing the label, it find the connected regions, and create
 * patches.
 *
 * @author Pietro Incardona
 * @param <E>
 */

public class ImagePatcher<T extends NativeType<T> & NumericType<T>, E extends NativeType<E> & IntegerType<E>> {

    private final @SuppressWarnings("rawtypes")
    ImagePatch[] img_p;
    private final long[] dims;

    public ImagePatcher(Img<T> img, Img<E> lbl, int margins[]) {
        dims = MosaicUtils.getImageLongDimensions(img);

        // Create a vector of image patches

        final Vector<ImagePatch<T, E>> img_pt = new Vector<ImagePatch<T, E>>();

        // Find connected regions on lbl

        final Connectivity connFG = new Connectivity(lbl.numDimensions(), lbl.numDimensions() - 1);
        final LabelImage lbl_t = new LabelImage(lbl);

        // Find connected regions and create statistics

        final HashSet<Integer> oldLabels = new HashSet<Integer>(); // set of the old labels
        final ArrayList<Integer> newLabels = new ArrayList<Integer>(); // set of new labels

        int newLabel = 1;

        final int size = lbl_t.getSize();

        // what are the old labels?
        for (int i = 0; i < size; i++) {
            final int l = lbl_t.getLabel(i);
            if (l == lbl_t.bgLabel) {
                continue;
            }
            oldLabels.add(l);
        }

        for (int i = 0; i < size; i++) {
            final int l = lbl_t.getLabel(i);
            if (l == lbl_t.bgLabel) {
                continue;
            }
            if (oldLabels.contains(l)) {
                // l is an old label
                final BinarizedIntervalLabelImage aMultiThsFunctionPtr = new BinarizedIntervalLabelImage(lbl_t);
                aMultiThsFunctionPtr.AddThresholdBetween(l, l);
                final FloodFill ff = new FloodFill(connFG, aMultiThsFunctionPtr, lbl_t.iterator.indexToPoint(i));

                // find a new label
                while (oldLabels.contains(newLabel)) {
                    newLabel++;
                }

                // newLabel is now an unused label
                newLabels.add(newLabel);

                img_pt.add(new ImagePatch<T, E>(margins.length));

                final ImagePatch<T, E> ip = img_pt.get(img_pt.size() - 1);

                // set region to new label
                for (final Point p : ff) {
                    lbl_t.setLabel(p, newLabel);

                    // check and extend the border

                    ip.extendPoint(p);
                }
                // next new label
                newLabel++;
            }
        }

        // Add margins for all patches and create patch

        for (int i = 0; i < img_pt.size(); i++) {
            img_pt.get(i).SubToP1(margins);
            img_pt.get(i).AddToP2(margins);
            img_pt.get(i).createPatch(img, lbl);
        }

        // create an array

        img_p = new ImagePatch<?, ?>[img_pt.size()];

        for (int i = 0; i < img_p.length; i++) {
            img_p[i] = img_pt.get(i);
        }
    }

    /**
     * Write the patch on the image
     *
     * @param img Image
     * @param pt Patch
     */

    private void writeOnImage(Img<E> img, ImagePatch<T, E> pt) {
        final RandomAccess<E> randomAccess = img.randomAccess();

        final Point offset = pt.getP1();
        final Cursor<E> cur = pt.getResult().cursor();
        final Point p = new Point(img.numDimensions());

        while (cur.hasNext()) {
            cur.next();
            cur.localize(p.x);

            final Point psum = offset.add(p);

            randomAccess.setPosition(psum.x);
            randomAccess.get().set(cur.get());
        }
    }

    /**
     * Assemble the final image fromthe patches
     *
     * @param cls
     * @param start from the patch start untill the end
     */

    @SuppressWarnings("unchecked")
    public Img<E> assemble(Class<E> cls, int start) {
        final ImgFactory<E> imgFactory_lbl = new ArrayImgFactory<E>();
        
        Img<E> img_ass = null;

        try {
            img_ass = imgFactory_lbl.create(dims, cls.newInstance());
        }
        catch (final InstantiationException e) {
            e.printStackTrace();
        }
        catch (final IllegalAccessException e) {
            e.printStackTrace();
        }

        for (int i = start; i < img_p.length; i++) {
            writeOnImage(img_ass, img_p[i]);
        }

        return img_ass;
    }

    @SuppressWarnings("rawtypes")
    public ImagePatch[] getPathes() {
        // Get the patches back

        return img_p;

    }
}
