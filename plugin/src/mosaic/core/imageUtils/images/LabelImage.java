package mosaic.core.imageUtils.images;


import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.GroupedZProjector;
import ij.plugin.ZProjector;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import mosaic.core.binarize.BinarizedIntervalLabelImage;
import mosaic.core.imageUtils.Connectivity;
import mosaic.core.imageUtils.FloodFill;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.core.utils.MosaicUtils;
import mosaic.region_competition.utils.IntConverter;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.IntegerType;


/**
 * LabelImage keeps information about label value assigned to particular point/pixel.
 */
public class LabelImage extends BaseImage
{
    public static final int BGLabel = 0;
    protected static final int ForbiddenLabel = Integer.MAX_VALUE;
    
    private int[] iDataLabel;

    private Connectivity iConnectivityFG;
    private Connectivity iConnectivityBG;
    protected int[] iNeighbourIndexes;
    
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
        
        iNeighbourIndexes = new int[iConnectivityFG.getNumOfNeighbors()];
        int idx = 0;
        for (Point p : iConnectivityFG.iterator()) {
            iNeighbourIndexes[idx++] = pointToIndex(p);
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
     * Is aLabel forbidden?
     */
    public boolean isForbiddenLabel(int aLabel) {
        return (aLabel == ForbiddenLabel);
    }
    
    /**
     * @return True if aLable is not inner label
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
        if (aLabel == BGLabel || aLabel == ForbiddenLabel) {
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
     */
    public void connectedComponents() {
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

        // relabel connected components
        final BinarizedIntervalLabelImage aMultiThsFunctionPtr = new BinarizedIntervalLabelImage(this);
        aMultiThsFunctionPtr.AddThresholdBetween(minLabel, maxLabel);
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
                
                ++newLabel;
            }
        }
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
     * sets the outermost pixels of the LabelImage to the forbidden label
     */
    public void initBoundary() {
        for (final int idx : iIterator.getIndexIterable()) {
            final Point p = indexToPoint(idx);
            final int xs[] = p.iCoords;
            for (int d = 0; d < getNumOfDimensions(); d++) {
                final int x = xs[d];
                if (x == 0 || x == getDimension(d) - 1) {
                    setLabel(idx, ForbiddenLabel);
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
     * 
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
        
        NeighbourConnIterator(Integer aIndex) {
            inputIndex = aIndex;
        }
        
        @Override
        public boolean hasNext() {
            return (cursor < iNeighbourIndexes.length);
        }

        @Override
        public Integer next() {
            return inputIndex + iNeighbourIndexes[cursor++];
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
        iDataLabel = IntConverter.toIntArray(aImagePlus);
        initBoundary();
    }

    /**
     * Save the LabelImage as tiff
     * @param aFileName where to save (full or relative path)
     */
    public void save(String aFileName) {
        final ImagePlus ip = convert("save", 256);
        IJ.save(ip, aFileName);
        ip.close();
    }

    /**
     * Shows LabelImage
     */
    public ImagePlus show(String aTitle, int aMaxValue) {
        final ImagePlus imp = convert(aTitle, aMaxValue);
        imp.show();
        return imp;
    }
    
    /**
     * Converts LabelImage to ImagePlus (ShortProcessor)
     */
    public ImagePlus convert(String aTitle, int aMaxValue) {
        final String title = "ResultWindow " + aTitle;
        final ImagePlus imp;
        
        if (getNumOfDimensions() == 3) {
            imp = new ImagePlus(title, getShortStack(true));
        }
        else {
            // convert it to absolute shorts
            final short[] shorts = (short[]) getImagePlus(false).getProcessor().getPixels();
            for (int i = 0; i < shorts.length; i++) {
                shorts[i] = (short) Math.abs(shorts[i]);
            }
            
            // Create ImagePlus with data
            final ShortProcessor shortProc = new ShortProcessor(getWidth(), getHeight(), shorts, null);
            imp = new ImagePlus(WindowManager.getUniqueName(title), shortProc);
            IJ.setMinAndMax(imp, 0, aMaxValue);
            IJ.run(imp, "3-3-2 RGB", null);
        }
        return imp;
    }

    /**
     * Add labels (=1) to LabelImage where in aRoi regions.
     * TODO: Should be done in nicer way...
     */
    public void initLabelsWithRoi(Roi aRoi) {
        ImageProcessor ip = null;
        if (getNumOfDimensions() == 3) {
            ip = getImagePlus(true).getProcessor();
        }
        else {
            ip = new ColorProcessor(getWidth(), getHeight(), iDataLabel);
        }
        ip.setValue(1);
        ip.fill(aRoi);
    }

    /**
     * Returns representation of LableImage as a ImagePlus. In case of 3D data all pixels are projected along z-axis to
     * its maximum.
     * @param aClean it true all values are: absolute, in +/- short range and Short.MAX_VALUE is set to 0
     */
    private ImagePlus getImagePlus(boolean aClean) {
        return new GroupedZProjector().groupZProject(new ImagePlus("Projection stack ", getShortStack(aClean)), 
                                                     ZProjector.MAX_METHOD, 
                                                     getNumOfSlices());
    }
    
    /**
     * Converts LabelImage to a stack of ShortProcessors
     */
    public ImageStack getShortStack(boolean clean) {
        return IntConverter.intArrayToShortStack(iDataLabel, getWidth(), getHeight(), getNumOfSlices(), clean);
    }

    /**
     * Returns internal data structure keeping labels
     */
    public int[] getDataLabel() {
        return iDataLabel;
    }
}
