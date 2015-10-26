package mosaic.core.ImagePatcher;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;

import mosaic.core.binarize.BinarizedIntervalLabelImage;
import mosaic.core.image.Connectivity;
import mosaic.core.image.FloodFill;
import mosaic.core.image.LabelImage;
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
    private final Vector<ImagePatch<T, E>> imgagePatches;
    private final long[] dims;

    public ImagePatcher(Img<T> aImage, Img<E> aLabelImage, int margins[]) {
        dims = MosaicUtils.getImageDimensions(aImage);

        // Create a vector of image patches
        imgagePatches = new Vector<ImagePatch<T, E>>();

        // Find connected regions on lbl
        final Connectivity connFG = new Connectivity(aLabelImage.numDimensions(), aLabelImage.numDimensions() - 1);
        final LabelImage labelImg = new LabelImage(aLabelImage);

        // Find connected regions and create statistics
        final HashSet<Integer> oldLabels = new HashSet<Integer>(); // set of the old labels
        final ArrayList<Integer> newLabels = new ArrayList<Integer>(); // set of new labels

        int newLabel = 1;

        final int size = labelImg.getSize();

        // what are the old labels?
        for (int i = 0; i < size; i++) {
            final int l = labelImg.getLabel(i);
            if (l == LabelImage.BGLabel) {
                continue;
            }
            oldLabels.add(l);
        }

        for (int i = 0; i < size; i++) {
            final int l = labelImg.getLabel(i);
            if (l == LabelImage.BGLabel) {
                continue;
            }
            if (oldLabels.contains(l)) {
                // l is an old label
                final BinarizedIntervalLabelImage aMultiThsFunctionPtr = new BinarizedIntervalLabelImage(labelImg);
                aMultiThsFunctionPtr.AddThresholdBetween(l, l);
                final FloodFill ff = new FloodFill(connFG, aMultiThsFunctionPtr, labelImg.iIterator.indexToPoint(i));

                // find a new label
                while (oldLabels.contains(newLabel)) {
                    newLabel++;
                }

                // newLabel is now an unused label
                newLabels.add(newLabel);

                imgagePatches.add(new ImagePatch<T, E>(margins.length));

                final ImagePatch<T, E> ip = imgagePatches.get(imgagePatches.size() - 1);

                // set region to new label
                for (final Point p : ff) {
                    labelImg.setLabel(p, newLabel);

                    // check and extend the border
                    ip.extendPoint(p);
                }
                newLabel++;
            }
        }

        // Add margins for all patches and create patch
        for (int i = 0; i < imgagePatches.size(); i++) {
            imgagePatches.get(i).SubToP1(margins);
            imgagePatches.get(i).AddToP2(margins);
            imgagePatches.get(i).createPatch(aImage, aLabelImage);
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
        final Point p = new Point(new int [img.numDimensions()]);

        while (cur.hasNext()) {
            cur.next();
            cur.localize(p.iCoords);

            final Point psum = offset.add(p);

            randomAccess.setPosition(psum.iCoords);
            randomAccess.get().set(cur.get());
        }
    }

    /**
     * Assemble the final image from the patches
     *
     * @param cls
     * @param start from the patch start until the end
     */
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

        for (int i = start; i < imgagePatches.size(); i++) {
            writeOnImage(img_ass, imgagePatches.get(i));
        }

        return img_ass;
    }

    public Vector<ImagePatch<T, E>> getPathes() {
        return imgagePatches;
    }
}
