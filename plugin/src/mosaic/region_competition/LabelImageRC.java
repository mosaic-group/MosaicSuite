package mosaic.region_competition;


import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.GroupedZProjector;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import mosaic.core.binarize.BinarizedIntervalLabelImage;
import mosaic.core.utils.Connectivity;
import mosaic.core.utils.FloodFill;
import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.LabelImage;
import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIterator;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.IntegerType;


public class LabelImageRC extends LabelImage {

    /** Maps the label(-number) to the information of a label */
    public final int forbiddenLabel = Integer.MAX_VALUE; // short
    private HashMap<Integer, LabelInformation> labelMap;

    /**
     * Create a labelImageRC from an imgLib2
     */

    public <T extends IntegerType<T>> LabelImageRC(Img<T> img) {
        super(img);
    }

    /**
     * Create a labelImageRC from another labelImageRC
     *
     * @param l LabelImageRC
     */
    public LabelImageRC(LabelImageRC l) {
        super(l);
    }

    public LabelImageRC(int[] dims) {
        super(dims);
    }

    /**
     * LabelImage loaded from file
     */
    @Override
    public void initWithIP(ImagePlus imagePlus) {
        super.initWithIP(imagePlus);
        initBoundary();
    }

    /**
     * @param stack Stack of Int processors
     */
    @Override
    public void initWithStack(ImageStack stack) {
        super.initWithStack(stack);
        initBoundary();
    }

    @Override
    protected void init(int dims[]) {
        super.init(dims);
        initMembers();
    }

    public void initMembers() {
        labelMap = new HashMap<Integer, LabelInformation>();
    }

    /**
     * Initialize the countor setting it to (-)label
     */

    @Override
    public void initContour() {
        final Connectivity conn = connFG;

        for (final int i : iterator.getIndexIterable()) {
            final int label = getLabelAbs(i);
            if (label != bgLabel && label != forbiddenLabel) // region pixel
            // && label<negOfs
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
     * Eliminate forbidden region and particles
     */

    public void eliminateForbidden() {
        for (int i = 0; i < getSize(); i++) {
            if (dataLabel[i] == forbiddenLabel) {
                dataLabel[i] = 0;
            }

            if (dataLabel[i] < 0) {
                dataLabel[i] = Math.abs(dataLabel[i]);
            }
        }
    }

    /**
     * @param label a label
     * @return the contour form of the label
     */
    @Override
    protected int labelToNeg(int label) {
        if (label == bgLabel || isForbiddenLabel(label) || isContourLabel(label)) {
            return label;
        }
        else {
            return -label;
            // return label + negOfs;
        }
    }

    /**
     * Gives disconnected components in a labelImage distinct labels
     * bg and forbidden label stay the same
     * contour labels are treated as normal labels,
     * so use this function only for BEFORE contour particles are added to the
     * labelImage
     * (eg. to process user input for region guesses)
     *
     * @param li LabelImage
     */
    @Override
    public void connectedComponents() {
        // TODO ! test this

        final HashSet<Integer> oldLabels = new HashSet<Integer>(); // set of the old
        // labels
        final ArrayList<Integer> newLabels = new ArrayList<Integer>(); // set of new
        // labels

        int newLabel = 1;

        final int size = iterator.getSize();

        // what are the old labels?
        for (int i = 0; i < size; i++) {
            final int l = getLabel(i);
            if (l == forbiddenLabel || l == bgLabel) {
                continue;
            }
            oldLabels.add(l);
        }

        for (int i = 0; i < size; i++) {
            final int l = getLabel(i);
            if (l == forbiddenLabel || l == bgLabel) {
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

    /**
     * Calculate the center of Mass of the regions
     */

    public void calculateRegionsCenterOfMass() {
        // iterate through all the regions and reset mean_pos

        for (final Integer lbl : labelMap.keySet()) {
            for (int i = 0; i < labelMap.get(lbl).mean_pos.length; i++) {
                labelMap.get(lbl).mean_pos[i] = 0.0;
            }
        }

        // Iterate through all the region

        final RegionIterator rc = new RegionIterator(getDimensions());
        while (rc.hasNext()) {
            rc.next();
            final Point p = rc.getPoint();
            final int lbl = getLabelAbs(p);

            final LabelInformation lbi = labelMap.get(lbl);

            // Label information

            if (lbi != null) {
                for (int i = 0; i < p.x.length; i++) {
                    lbi.mean_pos[i] += p.x[i];
                }
            }
        }

        // Iterate through all the regions

        for (final Entry<Integer, LabelInformation> entry : labelMap.entrySet()) {
            for (int i = 0; i < entry.getValue().mean_pos.length; i++) {
                entry.getValue().mean_pos[i] /= entry.getValue().count;
            }
        }
    }

    /**
     * Gets a copy of the labelImage as a short array.
     *
     * @return short[] representation of the labelImage
     */
    @Override
    public short[] getShortCopy() {
        if (dim == 3) {
            return (short[]) getProjected3D(false).getProcessor().getPixels();
        }

        final int n = dataLabel.length;

        final short[] shortData = new short[n];
        for (int i = 0; i < n; i++) {
            shortData[i] = (short) dataLabel[i];
        }
        return shortData;
    }

    public Object getSlice() {
        if (dim == 3) {
            return get3DShortStack(false);
        }
        else {
            return getShortCopy();
        }
    }

    @Override
    public ImageProcessor getLabelImageProcessor() {
        if (dim == 3) {
            return getProjected3D(true).getProcessor();
        }
        return labelIP;
    }

    /**
     * sets the outermost pixels of the labelimage to the forbidden label
     */
    public void initBoundary() {
        for (final int idx : iterator.getIndexIterable()) {
            final Point p = iterator.indexToPoint(idx);
            final int xs[] = p.x;
            for (int d = 0; d < dim; d++) {
                final int x = xs[d];
                if (x == 0 || x == dimensions[d] - 1) {
                    setLabel(idx, forbiddenLabel);
                    break;
                }
            }
        }
    }

    @Override
    protected boolean isInnerLabel(int label) {
        if (label == forbiddenLabel || label == bgLabel || isContourLabel(label)) {
            return false;
        }
        else {
            return true;
        }
    }

    public int createStatistics(IntensityImage intensityImage) {
        getLabelMap().clear();

        final HashSet<Integer> usedLabels = new HashSet<Integer>();

        final int size = iterator.getSize();
        for (int i = 0; i < size; i++) {
            // int label = get(x, y);
            // int absLabel = labelToAbs(label);

            final int absLabel = getLabelAbs(i);

            if (absLabel != forbiddenLabel /* && absLabel != bgLabel */) {
                usedLabels.add(absLabel);

                LabelInformation stats = labelMap.get(absLabel);
                if (stats == null) {
                    stats = new LabelInformation(absLabel, dim);
                    labelMap.put(absLabel, stats);
                }
                final double val = intensityImage.get(i);
                stats.count++;

                stats.mean += val; // only sum up, mean and var are computed
                // below
                stats.var = (stats.var + val * val);
            }
        }

        // if background label do not exist add it

        LabelInformation stats = labelMap.get(0);
        if (stats == null) {
            stats = new LabelInformation(0, dim);
            labelMap.put(0, stats);
        }

        // now we have in all LabelInformation:
        // in mean the sum of the values, in var the sum of val^2
        for (final LabelInformation stat : labelMap.values()) {
            final int n = stat.count;
            if (n > 1) {
                final double var = (stat.var - stat.mean * stat.mean / n) / (n - 1);
                stat.var = (var);
                // stat.var = (stat.var - stat.mean*stat.mean / n) / (n-1);
            }
            else {
                stat.var = 0;
            }

            if (n > 0) {
                stat.mean = stat.mean / n;
            }
            else {
                stat.mean = 0.0;
            }

            // Median on start set equal to mean

            stat.median = stat.mean;
        }
        return usedLabels.size();
    }

    public PointCM[] createCMModel() {
        // TODO ! test this

        final HashMap<Integer, PointCM> Labels = new HashMap<Integer, PointCM>(); // set
        // of
        // the
        // old
        // labels

        final int size = iterator.getSize();

        // what are the old labels?
        for (int i = 0; i < size; i++) {
            final int l = getLabel(i);
            if (l == forbiddenLabel || l == bgLabel) {
                continue;
            }
            if (Labels.get(l) == null) {
                final PointCM tmp = new PointCM();
                tmp.p = new Point(getDimensions().length);
                Labels.put(l, tmp);
            }
        }

        final int[] off = new int[] { 0, 0 };

        final RegionIterator img = new RegionIterator(getDimensions(), getDimensions(), off);

        while (img.hasNext()) {
            final Point p = img.getPoint();
            final int i = img.next();
            if (dataLabel[i] != bgLabel && dataLabel[i] != forbiddenLabel) {
                final int id = Math.abs(dataLabel[i]);

                Labels.get(id).p = Labels.get(id).p.add(p);
                Labels.get(id).count++;

                // Get the module of the curvature flow
            }
        }

        for (final PointCM p : Labels.values()) {
            p.p = p.p.div(p.count);
        }

        // labelDispenser.setLabelsInUse(newLabels);
        // for (int label: oldLabels)
        // {
        // labelDispenser.addFreedUpLabel(label);
        // }

        return Labels.values().toArray(new PointCM[Labels.size()]);
    }

    private boolean isForbiddenLabel(int label) {
        return (label == forbiddenLabel);
    }

    public HashMap<Integer, LabelInformation> getLabelMap() {
        return this.labelMap;
    }

    private ImagePlus getProjected3D(boolean abs) {
        final ImageStack stack = get3DShortStack(abs);
        final int z = getDimensions()[2];

        ImagePlus imp = new ImagePlus("Projection stack ", stack);
        final StackProjector projector = new StackProjector();
        imp = projector.doIt(imp, z);

        return imp;
    }
}

class StackProjector extends GroupedZProjector {

    private final int method = ZProjector.MAX_METHOD;

    public StackProjector() {
        // method = ZProjector.SUM_METHOD;
        // method = ZProjector.AVG_METHOD;
    }

    public ImagePlus doIt(ImagePlus imp, int groupSize) {
        // method = ZProjector.SUM_METHOD;
        // method = ZProjector.AVG_METHOD;
        final ImagePlus imp2 = groupZProject(imp, method, groupSize);

        return imp2;
    }

}
