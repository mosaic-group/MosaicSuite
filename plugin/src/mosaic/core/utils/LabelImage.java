package mosaic.core.utils;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.ArrayList;
import java.util.HashSet;

import mosaic.core.binarize.BinarizedIntervalLabelImage;
import mosaic.region_competition.utils.IntConverter;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;

/*
 //TODO TODOs
 - refactor LabelImage, extract what's not supposed to be there (eg energy calc)
 - does merging criterion have to be tested multiple times?
 */

public class LabelImage
{
    protected ImageProcessor labelIP; // map positions -> labels
    private ImagePlus labelPlus;
    public int[] dataLabel;

    private int size;
    protected int dim; // number of dimension
    protected int[] dimensions; // dimensions (width, height, depth, ...)
    private int width = 0;
    private int height = 0;
    public IndexIterator iterator; // iterates over the labelImage

    public final int bgLabel = 0;

    protected Connectivity connFG;
    private Connectivity connBG;

    /**
     * Create a label image from an ImgLib2 image
     * use always native type for computation are
     * much faster than imgLib2
     */
    public <T extends IntegerType<T>> LabelImage(Img<T> lbl) {
        // Get the image dimensions
        final int dimensions[] = MosaicUtils.getImageIntDimensions(lbl);

        // get int dimension
        init(dimensions);
        initImgLib2(lbl);
        iterator = new IndexIterator(dimensions);
    }

    /**
     * Create a labelImage from another label Image
     *
     * @param l LabelImage
     */
    public LabelImage(LabelImage l) {
        init(l.getDimensions());
        initWithIP(l.labelPlus);
        iterator = new IndexIterator(l.getDimensions());
    }

    /**
     * Create a labelImage from a short 3D array
     *
     * @param img short array
     */
    // @Deprecated
    public LabelImage(short[][][] img) {
        final int dims[] = new int[3];
        dims[2] = img.length;
        dims[0] = img[0].length;
        dims[1] = img[0][0].length;
        init(dims);
        initWith3DArray(img);
        iterator = new IndexIterator(dims);
    }

    /**
     * Create an empty label image of a given dimension
     *
     * @param dims dimensions of the LabelImage
     */

    public LabelImage(int dims[]) {
        init(dims);
    }

    /**
     * Check of p is inside the label image
     *
     * @param p Point
     * @return true if is inside
     */

    public boolean isOutOfBound(Point p) {
        for (int i = 0; i < p.iCoords.length; i++) {
            if (p.iCoords[i] < 0) {
                return true;
            }
            if (p.iCoords[i] >= dimensions[i]) {
                return true;
            }
        }
        return false;
    }

    protected void init(int dims[]) {
        initDimensions(dims);
        initConnectivities(dim);
        iterator = new IndexIterator(dims);
        initLabelData();
    }

    private void initDimensions(int[] dims) {
        this.dimensions = dims;
        this.dim = dimensions.length;
        if (dim > 3) {
            throw new RuntimeException("Dim > 3 not supported");
        }

        this.width = dims[0];
        this.height = dims[1];

        size = 1;
        for (int i = 0; i < dim; i++) {
            size *= dimensions[i];
        }
    }

    private void initLabelData() {
        if (dim == 3) {
            labelPlus = null;
            labelIP = null;
            dataLabel = new int[size];
        }
        else {
            labelIP = new ColorProcessor(width, height);
            dataLabel = (int[]) labelIP.getPixels();
            labelPlus = new ImagePlus("LabelImage", labelIP);
        }
    }

    private void initConnectivities(int d) {
        connFG = new Connectivity(d, d - 1);
        connBG = connFG.getComplementaryConnectivity();
    }

    public ImagePlus getLabelPlus() {
        return labelPlus;
    }

    /////////////////////////////////////////////////////////////////

    /**
     * Initializes label image data to all zero
     */
    public void initZero() {
        for (int i = 0; i < size; i++) {
            setLabel(i, 0);
        }
    }

    /**
     * Only 2D
     * Initializes label image with a predefined IP (by copying it)
     * ip: without contour pixels/boundary
     * (invoke initBoundary() and generateContour();
     */
    @Deprecated
    private void initWithImageProc(ImageProcessor ip) {
        // TODO check for dimensions etc
        this.labelIP = IntConverter.procToIntProc(ip);
        this.dataLabel = (int[]) labelIP.getPixels();
        this.labelPlus = new ImagePlus("labelImage", labelIP);
    }

    /**
     * LabelImage loaded from an imgLib2 image
     *
     * @param imgLib2
     */
    private <T extends IntegerType<T>> void initImgLib2(Img<T> img) {
        final RandomAccess<T> ra = img.randomAccess();

        // Create a region iterator

        final RegionIterator rg = new RegionIterator(MosaicUtils.getImageIntDimensions(img));

        // load the image

        while (rg.hasNext()) {
            final Point p = rg.getPoint();
            final int id = rg.next();

            ra.setPosition(p.iCoords);
            dataLabel[id] = ra.get().getInteger();
        }
    }

    /**
     * LabelImage loaded from file
     */
    public void initWithIP(ImagePlus imagePlus) {
        final ImagePlus ip = IntConverter.IPtoInt(imagePlus);

        if (dim == 3) {
            this.labelPlus = ip;
            final ImageStack stack = ip.getImageStack();
            this.dataLabel = IntConverter.intStackToArray(stack);
            this.labelIP = null;
        }
        if (dim == 2) {
            initWithImageProc(ip.getProcessor());
        }
    }

    /**
     * LabelImage loaded from 3D array
     */
    private void initWith3DArray(short[][][] ar) {
        for (int i = 0; i < ar.length; i++) {
            for (int j = 0; j < ar[0].length; j++) {
                for (int k = 0; k < ar[0][0].length; k++) {
                    dataLabel[j + k * dimensions[0] + i * dimensions[1] * dimensions[0]] = ar[i][j][k];
                }
            }
        }
    }

    /**
     * Close all the images
     */

    public void close() {
        if (labelPlus != null) {
            labelPlus.close();
        }
    }

    /**
     * @param stack Stack of Int processors
     */
    public void initWithStack(ImageStack stack) {
        this.dataLabel = IntConverter.intStackToArray(stack);
    }

    /**
     * Save the label image as tiff
     *
     * @param file where to save (full or relative path)
     */

    public void save(String file) {
        // Remove eventually the "file:" string

        if (file.indexOf("file:") >= 0) {
            file = file.substring(file.indexOf("file:") + 5);
        }

        final ImagePlus ip = convert("save", 256);
        IJ.save(ip, file);
        ip.close();
    }

    public ImageProcessor getLabelImageProcessor() {
        return labelIP;
    }

    /**
     * Gets a copy of the labelImage as a short array.
     *
     * @return short[] representation of the labelImage
     */
    public short[] getShortCopy() {
        final int n = dataLabel.length;

        final short[] shortData = new short[n];
        for (int i = 0; i < n; i++) {
            shortData[i] = (short) dataLabel[i];
        }
        return shortData;
    }

    public ImagePlus convert(Object title, int maxl) {
        if (getDim() == 3) {
            final ImagePlus imp = new ImagePlus("ResultWindow " + title, this.get3DShortStack(true));
            return imp;
        }

        final ImageProcessor imProc = getLabelImageProcessor();

        // convert it to short
        final short[] shorts = getShortCopy();
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) Math.abs(shorts[i]);
        }
        final ShortProcessor shortProc = new ShortProcessor(imProc.getWidth(), imProc.getHeight());
        shortProc.setPixels(shorts);

        // TODO !!!! imProc.convertToShort() does not work, first converts to
        // byte, then to short...
        final String s = "ResultWindow " + title;
        final String titleUnique = WindowManager.getUniqueName(s);

        final ImagePlus imp = new ImagePlus(titleUnique, shortProc);
        IJ.setMinAndMax(imp, 0, maxl);
        IJ.run(imp, "3-3-2 RGB", null);
        return imp;
    }

    public ImagePlus show(Object title, int maxl) {
        final ImagePlus imp = convert(title, maxl);
        imp.show();
        return imp;
    }

    public void deleteParticles() {
        for (int i = 0; i < size; i++) {
            setLabel(i, getLabelAbs(i));
        }
    }

    /**
     * Get an ImgLib2 from a intensity image
     *
     * @return an ImgLib2 image
     */
    public <T extends NativeType<T> & IntegerType<T>> Img<T> getImgLib2(Class<T> cls) {
        final long lg[] = new long[getDim()];

        // Take the size
        final ImgFactory<T> imgFactory = new ArrayImgFactory<T>();

        for (int i = 0; i < getDim(); i++) {
            lg[i] = getDimensions()[i];
        }

        // create an Img of the same type of T and size of the imageLabel
        Img<T> it = null;
        try {
            it = imgFactory.create(lg, cls.newInstance());
        }
        catch (final InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        catch (final IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        final RandomAccess<T> randomAccess_it = it.randomAccess();

        // Region iterator
        final RegionIterator ri = new RegionIterator(getDimensions());

        while (ri.hasNext()) {
            final Point p = ri.getPoint();
            final int id = ri.next();

            randomAccess_it.setPosition(p.iCoords);
            randomAccess_it.get().setInteger(dataLabel[id]);
        }

        return it;
    }

    /**
     * Gives disconnected components in a labelImage distinct labels
     * (eg. to process user input for region guesses)
     *
     * @param li LabelImage
     */
    public void connectedComponents() {
        final HashSet<Integer> oldLabels = new HashSet<Integer>(); // set of the old
        // labels
        final ArrayList<Integer> newLabels = new ArrayList<Integer>(); // set of new
        // labels

        int newLabel = 1;

        final int size = iterator.getSize();

        // what are the old labels?
        for (int i = 0; i < size; i++) {
            final int l = getLabel(i);
            if (l == bgLabel) {
                continue;
            }
            oldLabels.add(l);
        }

        for (int i = 0; i < size; i++) {
            final int l = getLabel(i);
            if (l == bgLabel) {
                continue;
            }
            if (oldLabels.contains(l)) {
                // l is an old label
                final BinarizedIntervalLabelImage aMultiThsFunctionPtr = new BinarizedIntervalLabelImage(this);
                aMultiThsFunctionPtr.AddThresholdBetween(l, l);
                final FloodFill ff = new FloodFill(connFG, aMultiThsFunctionPtr, iterator.indexToPoint(i));

                // find a new label
                while (oldLabels.contains(newLabel)) {
                    newLabel++;
                }

                // newLabel is now an unused label
                newLabels.add(newLabel);

                // set region to new label
                for (final Point p : ff) {
                    setLabel(p, newLabel);
                }
                // next new label
                newLabel++;
            }
        }

    }

    public void initContour() {
        final Connectivity conn = connFG;

        for (final int i : iterator.getIndexIterable()) {
            final int label = getLabelAbs(i);
            if (label != bgLabel) // region pixel
            {
                final Point p = iterator.indexToPoint(i);
                for (final Point neighbor : conn.iterateNeighbors(p)) {
                    final int neighborLabel = getLabelAbs(neighbor);
                    if (neighborLabel != label) {
                        setLabel(p, labelToNeg(label));

                        break;
                    }
                }

            } // if region pixel
        }
    }

    /**
     * Is the point at the boundary
     *
     * @param aIndex Point
     * @return true if is at the boundary false otherwise
     */

    public boolean isBoundaryPoint(Point aIndex) {
        final int vLabelAbs = getLabelAbs(aIndex);
        for (final Point q : connFG.iterateNeighbors(aIndex)) {
            if (getLabelAbs(q) != vLabelAbs) {
                return true;
            }
        }

        return false;
    }

    /**
     * is point surrounded by points of the same (abs) label
     *
     * @param aIndex
     * @return
     */
    public boolean isEnclosedByLabel(Point pIndex, int pLabel) {
        final int absLabel = labelToAbs(pLabel);
        final Connectivity conn = connFG;
        for (final Point qIndex : conn.iterateNeighbors(pIndex)) {
            if (labelToAbs(getLabel(qIndex)) != absLabel) {
                return false;
            }
        }
        return true;
    }

    protected boolean isInnerLabel(int label) {
        if (label == bgLabel || isContourLabel(label)) {
            return false;
        }
        else {
            return true;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @param label
     * @return true, if label is a contour label
     *         ofset version
     */
    public boolean isContourLabel(int label) {
        return (label < 0);
    }

    /**
     * return the label at the position index (linearized)
     *
     * @param index position
     * @return the label value
     */

    public int getLabel(int index) {
        return dataLabel[index];
        // return labelIP.get(index);
    }

    /**
     * @param p
     * @return Returns the (raw; contour information) value of the LabelImage at
     *         Point p.
     */
    public int getLabel(Point p) {
        final int idx = iterator.pointToIndex(p);
        return dataLabel[idx];
        // return getLabel(idx);
    }

    /**
     * @return the abs (no contour information) label at Point p
     */
    public int getLabelAbs(Point p) {
        final int idx = iterator.pointToIndex(p);

        return Math.abs(dataLabel[idx]);
    }

    public int getLabelAbs(int idx) {

        return Math.abs(dataLabel[idx]);
    }

    /**
     * sets the labelImage to val at point x,y
     */
    public void setLabel(int idx, int label) {
        dataLabel[idx] = label;
    }

    /**
     * sets the labelImage to val at Point p
     */
    public void setLabel(Point p, int label) {
        final int idx = iterator.pointToIndex(p);
        dataLabel[idx] = label;
    }

    /**
     * @param label a label
     * @return if label was a contour label, get the absolute/inner label
     */
    public int labelToAbs(int label) {
        return Math.abs(label);
    }

    /**
     * @param label a label
     * @return the contour form of the label
     */
    protected int labelToNeg(int label) {
        if (label == bgLabel || isContourLabel(label)) {
            return label;
        }
        else {
            return -label;
        }
    }

    /**
     * @return The number of dimensions of this LabelImage
     */
    public int getDim() {
        return this.dim;
    }

    /**
     * The size of each dimension of this LabelImage as an int array
     *
     * @return Reference to the dimensions
     */
    public int[] getDimensions() {
        return this.dimensions;
    }

    /**
     * @return The number of pixels of this LabelImage
     */
    public int getSize() {
        return this.size;
    }

    /**
     * @return Connectivity of the foreground
     */
    public Connectivity getConnFG() {
        return connFG;
    }

    /**
     * @return Connectivity of the background
     */
    public Connectivity getConnBG() {
        return connBG;
    }

    /**
     * if 3D image, converts to a stack of ShortProcessors
     *
     * @return
     */
    public ImageStack get3DShortStack(boolean clean) {
        final int dims[] = getDimensions();
        final int labeldata[] = dataLabel;

        final ImageStack stack = IntConverter.intArrayToShortStack(labeldata, dims, clean);

        return stack;
    }

}
