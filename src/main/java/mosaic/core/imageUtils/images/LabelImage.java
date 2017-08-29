package mosaic.core.imageUtils.images;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.StackStatistics;
import mosaic.core.binarize.BinarizedIntervalLabelImage;
import mosaic.core.imageUtils.Connectivity;
import mosaic.core.imageUtils.FloodFill;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.core.utils.MosaicUtils;
import mosaic.utils.ConvertArray;
import mosaic.utils.ImgUtils;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.IntegerType;


/**
 * LabelImage keeps information about label value assigned to particular point/pixel.
 */
public class LabelImage extends BaseImage
{
    private static final Logger logger = Logger.getLogger(LabelImage.class);
    
    public static final int BGLabel = 0;
    public static final int BorderLabel = Integer.MAX_VALUE;
    
    private int[] iDataLabel;

    private Connectivity iConnectivityFG;
    private Connectivity iConnectivityBG;
    protected Point[] iNeighbourPoints;
    protected int[] iNeighbourIndices;
    protected Point[] iNeighbourBgPoints;
    protected int[] iNeighbourBgIndices;
    
    /**
     * Create a label image from an ImgLib2
     */
    public <T extends IntegerType<T>> LabelImage(Img<T> aLabelImg) {
        super(MosaicUtils.getImageIntDimensions(aLabelImg), 3);
        initConnectivities();
        initDataLabel(aLabelImg);
    }

    /**
     * Create a labelImage from another LabelImage
     */
    public LabelImage(LabelImage aLabelImg) {
        super(aLabelImg.getDimensions(), aLabelImg.getNumOfDimensions());
        initConnectivities();
        iDataLabel = ArrayUtils.clone(aLabelImg.iDataLabel);
    }

    /**
     * Create an empty LabelImage of a given dimension
     * @param aDimensions dimensions of the LabelImage
     */
    public LabelImage(int[] aDimensions) {
        super(aDimensions, 3);
        initConnectivities();
        iDataLabel = new int[getSize()];
    }
    
    /**
     * Create an empty LabelImage of a given dimension and initialize it with a provided data.
     * @param aData - data of image (it is not copied)
     * @param aDimensions dimensions of the LabelImage
     */
    public LabelImage(int[] aData, int[] aDimensions) {
        super(aDimensions, 3);
        if (aData.length != getSize()) {
            throw new RuntimeException("Provided image data size is not compatibile with given dimensions! " + 
                                        aData.length + " vs. " + super.toString() + " (" + getSize() + ")");
        }
        initConnectivities();
        iDataLabel = aData;
    }

    /**
     * Create a labelImage from a short 3D array [z][x][y]
     * @Deprecated
     */
    public LabelImage(short[][][] img) {
        super(new int[] {img[0].length, img[0][0].length, img.length}, 3);
        initConnectivities();
        initDataLabel(img);
    }

    /**
     * Initialize an intensity image from an Image Plus and normalize
     *
     * @param aInputImg
     */
    public LabelImage(ImagePlus aInputImg) {
        this(aInputImg, true);
    }

    /**
     * Initialize an intensity image from an Image Plus
     * choosing is normalizing or not
     *
     * @param aInputImg ImagePlus
     * @param aShouldNormalize true normalize false don' t
     */
    public LabelImage(ImagePlus aInputImg, boolean aShouldNormalize) {
        super(getDimensions(aInputImg), /* max num of dimensions */ 3);
        initConnectivities();
        if (aShouldNormalize == true) {
            aInputImg = ImgUtils.convertToNormalizedGloballyFloatType(aInputImg);
        }
        initWithImg(aInputImg);
    }
    
    /**
     * LabelImage loaded from an imgLib2 image
     */
    private <T extends IntegerType<T>> void initDataLabel(Img<T> aImage) {
        iDataLabel = new int[getSize()];
        SpaceIterator indexIterator = new SpaceIterator(MosaicUtils.getImageIntDimensions(aImage));
        final Iterator<Integer> rg = indexIterator.getIndexIterator();
        final RandomAccess<T> ra = aImage.randomAccess();
    
        while (rg.hasNext()) {
            final int idx = rg.next();
            final Point p = indexIterator.indexToPoint(idx);
            ra.setPosition(p.iCoords);
            iDataLabel[idx] = ra.get().getInteger();
        }
    }

    /**
     * Initialize data from 3D array [z][x][y]
     */
    private void initDataLabel(short[][][] aArray) {
        iDataLabel = new int[getSize()];

        final int width = getWidth();
        final int height = getHeight();
        for (int z = 0; z < aArray.length; z++) {
            for (int x = 0; x < aArray[0].length; x++) {
                for (int y = 0; y < aArray[0][0].length; y++) {
                    iDataLabel[x + y * width + z * height * width] = aArray[z][x][y];
                }
            }
        }
    }

    /**
     * Initialize both - FG and BG - connectivities
     */
    private void initConnectivities() {
        int numOfDimensions = getNumOfDimensions();
        iConnectivityFG = new Connectivity(numOfDimensions, numOfDimensions - 1);
        iConnectivityBG = iConnectivityFG.getComplementaryConnectivity();
        
        iNeighbourPoints = new Point[iConnectivityFG.getNumOfNeighbors()];
        iNeighbourIndices = new int[iConnectivityFG.getNumOfNeighbors()];
        int idx = 0;
        for (Point p : iConnectivityFG.iterator()) {
            iNeighbourIndices[idx] = pointToIndex(p);
            iNeighbourPoints[idx++] = p;
        }

        iNeighbourBgPoints = new Point[iConnectivityBG.getNumOfNeighbors()];
        iNeighbourBgIndices = new int[iConnectivityBG.getNumOfNeighbors()];
        idx = 0;
        for (Point p : iConnectivityBG.iterator()) {
            iNeighbourBgIndices[idx] = pointToIndex(p);
            iNeighbourBgPoints[idx++] = p;
        }
    }

    /**
     * Initializes LabelImage data to zeros
     */
    public void initZero() {
        for (int i = 0; i < iDataLabel.length; i++) {
            setLabel(i, 0);
        }
    }
    
    /**
     *  Initializes LabelImage data to consecutive numbers
     */
    public void deleteParticles() {
        for (int i = 0; i < iDataLabel.length; i++) {
            setLabel(i, getLabelAbs(i));
        }
    }
    
    /**
     * Sets the LabelImage at given aIndex to aLabel
     */
    public void setLabel(int aIndex, int aLabel) {
        iDataLabel[aIndex] = aLabel;
    }

    /**
     * Sets the LabelImage at given aPoint to aLabel
     */
    public void setLabel(Point aPoint, int aLabel) {
        iDataLabel[pointToIndex(aPoint)] = aLabel;
    }
    
    /**
     * Returns the label at the position aIndex
     */
    public int getLabel(int aIndex) {
        return iDataLabel[aIndex];
    }

    /**
     * Returns the label at the position aIndex
     */
    public int getLabel(Point aPoint) {
        return iDataLabel[pointToIndex(aPoint)];
    }
    
    /**
     * @return absolute (no contour information) label at aPoint
     */
    public int getLabelAbs(Point aPoint) {
        return Math.abs(iDataLabel[pointToIndex(aPoint)]);
    }

    /**
     * @return absolute (no contour information) label at aIndex
     */
    public int getLabelAbs(int aIndex) {
        return Math.abs(iDataLabel[aIndex]);
    }
    
    /**
     * Returns internal data structure keeping labels
     */
    public int[] getDataLabel() {
        return iDataLabel;
    }

    /**
     * Is aLabel forbidden?
     */
    public boolean isBorderLabel(int aLabel) {
        return (aLabel == BorderLabel);
    }
    
    /**
     * @return True if aLable is inner label
     */
    public boolean isInnerLabel(int aLabel) {
        if (isSpecialLabel(aLabel) || isContourLabel(aLabel)) {
            return false;
        }
        return true;
    }

    /**
     * @return true if label is Special (background, forbidden)
     */
    public boolean isSpecialLabel(int aLabel) {
        if (aLabel == BGLabel || aLabel == BorderLabel) {
            return true;
        }
        return false;
    }
    
    /**
     * @param aLabel - input label
     * @return true, if aLabel is a contour label
     */
    public boolean isContourLabel(int aLabel) {
        return (aLabel < 0);
    }

    /**
     * @param aLabel - input label
     * @return if label was a contour label, get the absolute/inner label
     */
    public int labelToAbs(int aLabel) {
        return Math.abs(aLabel);
    }

    /**
     * @param aLabel a label
     * @return the contour form of the label
     */
    public int labelToNeg(int aLabel) {
        if (!isInnerLabel(aLabel)) {
            return aLabel;
        }
        return -aLabel;
    }

    /**
     * @return Connectivity of the foreground
     */
    public Connectivity getConnFG() {
        return iConnectivityFG;
    }

    /**
     * @return Connectivity of the background
     */
    public Connectivity getConnBG() {
        return iConnectivityBG;
    }
    
    /**
     * Makes disconnected components to have different labels. All new components will have new (positive), different 
     * label values than old one.
     * @return 
     */
    public Set<Integer> connectedComponents() {
        final HashSet<Integer> oldLabels = new HashSet<Integer>();
        final int size = getSize();
        int minLabel = Integer.MAX_VALUE;
        int maxLabel = Integer.MIN_VALUE;
        // what are the old labels?
        for (int i = 0; i < size; ++i) {
            final int l = getLabel(i);
            if (isSpecialLabel(l)) {
                continue;
            }
            if (l < minLabel) minLabel = l;
            if (l > maxLabel) maxLabel = l;
            oldLabels.add(l);
        }

        final Set<Integer> newLabels = new HashSet<Integer>();
        // relabel connected components
        final BinarizedIntervalLabelImage aMultiThsFunctionPtr = new BinarizedIntervalLabelImage(this);
        if (minLabel < BGLabel && maxLabel > BGLabel) {
            // Case when we initialize with previous segmentation result (it contains +/- label values and we cannot
            // take BGLabel into range of threshold).
            aMultiThsFunctionPtr.AddThresholdBetween(BGLabel + 1, maxLabel);
            aMultiThsFunctionPtr.AddThresholdBetween(minLabel, BGLabel - 1);
        }
        else {
            aMultiThsFunctionPtr.AddThresholdBetween(minLabel, maxLabel);
        }
        // labels can be also negative, in such case start from 1
        int newLabel = Math.max(maxLabel + 1, 1);
        for (int idx = 0; idx < size; ++idx) {
            final int label = getLabel(idx);
            if (oldLabels.contains(label)) {
                final FloodFill ff = new FloodFill(this, aMultiThsFunctionPtr, indexToPoint(idx));
                // set region to new label
                for (final int p : ff) {
                    setLabel(p, newLabel);
                }
                newLabels.add(newLabel);
                ++newLabel;
            }
        }
        return newLabels;
    }

    /**
     * Is the point at the boundary
     * @param aPoint point to be checked
     * @return true if is at the boundary false otherwise
     */
    public boolean isBoundaryPoint(Point aPoint) {
        final int inLabel = getLabel(aPoint);
        return !isEnclosedByLabel(aPoint, inLabel);
    }

    /**
     * Is the point at the boundary
     * @param aPoint point to be checked
     * @return true if is at the boundary false otherwise
     */
    public boolean isBoundaryPoint(int aIndex) {
        final int inLabel = getLabel(aIndex);
        return !isEnclosedByLabel(aIndex, inLabel);
    }

    /**
     * Is aPoint surrounded by points of the given aLabel
     * @return true if yes
     */
    public boolean isEnclosedByLabel(Point aPoint, int aLabel) {
        return isEnclosedByLabel(pointToIndex(aPoint), aLabel);
    }

    /**
     * Is point with aIndex surrounded by points of the given aLabel
     * @return true if yes
     */
    public boolean isEnclosedByLabel(Integer aIndex, int aLabel) {
        final int absLabel = labelToAbs(aLabel);
        for (final int idx : iterateNeighbours(aIndex)) {
            if (getLabelAbs(idx) != absLabel) {
                return false;
            }
        }
        return true;
    }

    /**
     * Is point with aIndex single (not having FG neighbour with same abs label)
     * @return true if yes
     */
    public boolean isSingleFgPoint(Integer aIndex, int aLabel) {
        final int absLabel = labelToAbs(aLabel);
        for (final int idx : iterateNeighbours(aIndex)) {
            if (getLabelAbs(idx) == absLabel) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Is point with aIndex surrounded with BG connectivity by points of the given aLabel 
     * @return true if yes
     */
    public boolean isEnclosedByLabelBgConnectivity(Integer aIndex, int aLabel) {
        final int absLabel = labelToAbs(aLabel);
        for (final int idx : iterateBgNeighbours(aIndex)) {
            if (getLabelAbs(idx) != absLabel) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * sets the outermost pixels of the LabelImage to the forbidden label
     */
    public void initBorder() {
        for (final int idx : iIterator.getIndexIterable()) {
            final Point p = indexToPoint(idx);
            final int xs[] = p.iCoords;
            for (int d = 0; d < getNumOfDimensions(); d++) {
                final int x = xs[d];
                if (x == 0 || x == getDimension(d) - 1) {
                    setLabel(idx, BorderLabel);
                    break;
                }
            }
        }
    }
    
    /**
     * Initialize the contour by setting it to (-)label value.
     * @return List of contour points.
     */
    public List<Point> initContour() {
        List<Point> contourPoints = new LinkedList<Point>();
        for (final int i : iIterator.getIndexIterable()) {
            final int label = getLabelAbs(i);
            if (!isSpecialLabel(label)) {
                final Point p = indexToPoint(i);
                for (final Integer neighbor : iterateNeighbours(p)) {
                    final int neighborLabel = getLabelAbs(neighbor);
                    if (neighborLabel != label) {
                        setLabel(p, labelToNeg(label));
                        contourPoints.add(p);
                        break;
                    }
                }
            }
        }
        
        return contourPoints;
    }
    
    /**
     * @param aPoint
     * @return
     */
    public Iterable<Integer> iterateNeighbours(final Point aPoint) {
        return new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new NeighbourConnIterator(pointToIndex(aPoint));
            }
        };
    }
    
    public Iterable<Integer> iterateNeighbours(final Integer aIndex) {
        return new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new NeighbourConnIterator(aIndex);
            }
        };
    }
    
    private class NeighbourConnIterator implements Iterator<Integer> {
        private int cursor = 0;
        private final int inputIndex;
        int max = 0;
        int[] idxs = null;
        NeighbourConnIterator(Integer aIndex) {
            inputIndex = aIndex;
            Point start = indexToPoint(aIndex);
            max = 0;
            idxs = new int[iNeighbourPoints.length];
            if (isInBound(start)) {
                for (int i = 0; i < iNeighbourPoints.length; i++) { 
                    if (isInBound(start.add(iNeighbourPoints[i]))) {
                        idxs[max++] = iNeighbourIndices[i] + inputIndex;
                    }
                }
            }
        }
        
        @Override
        public boolean hasNext() {
              return cursor < max;
        }

        @Override
        public Integer next() {
            return idxs[cursor++];
        }
        
        @Override
        public void remove() {
            // do nothing
        }
    }
    
    public Iterable<Integer> iterateBgNeighbours(final Integer aIndex) {
        return new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new BgNeighbourConnIterator(aIndex);
            }
        };
    }
    
    private class BgNeighbourConnIterator implements Iterator<Integer> {
        private int cursor = 0;
        private final int inputIndex;
        int max = 0;
        int[] idxs = null;
        BgNeighbourConnIterator(Integer aIndex) {
            inputIndex = aIndex;
            Point start = indexToPoint(aIndex);
            max = 0;
            idxs = new int[iNeighbourBgPoints.length];
            if (isInBound(start)) {
                for (int i = 0; i < iNeighbourBgPoints.length; i++) { 
                    if (isInBound(start.add(iNeighbourBgPoints[i]))) {
                        idxs[max++] = iNeighbourBgIndices[i] + inputIndex;
                    }
                }
            }
        }
        
        @Override
        public boolean hasNext() {
            return cursor < max;
        }

        @Override
        public Integer next() {
            return idxs[cursor++];
        }
        
        @Override
        public void remove() {
            // do nothing
        }
    }
    
    //
    // Below are all function dependent on ImageJ implementation (ImagePlus, ImageProcessor, Roi...)
    // TODO: It should be verify if this is the best place for them after ImageLabelRC is cleaned up
    //
    
    /**
     * LabelImage loaded from file
     */
    public void initWithImg(ImagePlus aImagePlus) {
        iDataLabel = imgToIntArray(aImagePlus);
    }

    /**
     * Add labels (=1) to LabelImage where in aRoi regions.
     * Works only for 2D and in case of 3D just do nothing.
     */
    public void initLabelsWithRoi(Roi aRoi) {
        if (getNumOfDimensions() == 2) {
            ImageProcessor ip = new ColorProcessor(getWidth(), getHeight(), iDataLabel);
            ip.setValue(1);
            ip.fill(aRoi);
        }
    }
    
    public int getMax() {
        int max = Integer.MIN_VALUE;
        for (int v : iDataLabel) {
            if (max < v) max = v;
        }
        return max;
    }
    
    /**
     * Converts LabelImage to ImagePlus (ShortProcessor)
     * @param aTitle - title of created image
     */
    @Override
    public ImagePlus convertToImg(String aTitle) {
        final ImagePlus imp = new ImagePlus(aTitle, getShortStack(true, true, true));
        StackStatistics stackStats = new StackStatistics(imp);
        imp.setDisplayRange(stackStats.min, stackStats.max);
        IJ.run(imp, "3-3-2 RGB", null);
        return imp;
    }

    /**
     * Converts LabelImage to a stack of ShortProcessors
     */
    public ImageStack getShortStack(boolean aUseAbsValue, boolean aBorderRemove, boolean aClampValues) {
        final short shortData[] = intToShort(iDataLabel, aUseAbsValue, aBorderRemove, aClampValues);
        int w = getWidth();
        int h = getHeight();
        final int area = w * h;
        
        final ImageStack stack = new ImageStack(w, h);
        for (int i = 0; i < getNumOfSlices(); ++i) {
            stack.addSlice("", Arrays.copyOfRange(shortData, i * area, (i + 1) * area));
        } 
        
        return stack;
    }
    
    /**
     * @param aInputArr - input int[] array
     * @param aUseAbsValue - Math.abs() the array
     * @param aBorderRemove - Short.MAX_VALUE to Zero
     * @param aClampValues- values > Short.MAX_VALUE to Short.MAX_VALUE (same for MIN_VALUE)
     * @return short[] array with wanted operations executed
     */
    private static short[] intToShort(int[] aInputArr, boolean aUseAbsValue, boolean aBorderRemove, boolean aClampValues) {
        final int len = aInputArr.length;
        final short[] shorts = new short[len];
        boolean foundFirstErrorMax = false;
        boolean foundFirstErrorMin = false;
        
        for (int i = 0; i < len; ++i) {
            int val = aInputArr[i];
            if (aBorderRemove && val == BorderLabel) val = BGLabel;
            if (aClampValues) {
                if (val > Short.MAX_VALUE) {
                    val = Short.MAX_VALUE;
                    if (!foundFirstErrorMax) {
                        foundFirstErrorMax = true;
                        logger.error("Found value=" + val + "at idx=" + i + " which is too big for short type!");
                    }
                }
                else if (val < Short.MIN_VALUE) {
                    if (!foundFirstErrorMin) {
                        foundFirstErrorMin = true;
                        logger.error("Found value=" + val + "at idx=" + i + " which is too small for short type!");
                    }                    
                    val = Short.MIN_VALUE;
                }
            }
            shorts[i] = aUseAbsValue ? (short) Math.abs((short) val) : (short) val;
        }

        return shorts;
    }
    
    /**
     * Converts aImagePlus to int array with all slices
     */
    private static int[] imgToIntArray(ImagePlus aImagePlus) {
        final ImageStack stack = aImagePlus.getStack();
        final int imgArea = aImagePlus.getWidth() * aImagePlus.getHeight();
        final int numOfSlices = stack.getSize();
        
        int[] result = new int[numOfSlices * imgArea];
        for (int i = 0; i < numOfSlices; ++i) {
            int[] intArray = getIntArray(stack.getProcessor(i + 1));
            for (int j = 0; j < imgArea; ++j) {
                result[i * imgArea + j] = intArray[j];
            }
        }
        
        return result;
    }
    
    /**
     * Converts pixels from provided ImageProcessor to int[] array.
     */
    private static int[] getIntArray(ImageProcessor aImgProcessor) {
        int[] intArray = null;
        
        final Object pixels = aImgProcessor.getPixels();
        if (pixels instanceof int[]) {
            intArray = ((int[]) pixels).clone();
        }
        else if (pixels instanceof float[]) {
            intArray = ConvertArray.toInt((float[]) pixels);
        }
        else if (pixels instanceof byte[]) {
            intArray = ConvertArray.toInt((byte[]) pixels);
        }
        else if (pixels instanceof short[]) {
            intArray = ConvertArray.toInt((short[]) pixels);
        }
        else {
            throw new RuntimeException("Not supported conversion");
        }
        
        return intArray;
    }
}
