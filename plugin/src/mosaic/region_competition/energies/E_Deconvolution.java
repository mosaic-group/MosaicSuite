package mosaic.region_competition.energies;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.Point;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.LabelImageRC;
import mosaic.region_competition.LabelInformation;
import mosaic.region_competition.energies.Energy.ExternalEnergy;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;


public class E_Deconvolution extends ExternalEnergy {

    final private Img<FloatType> DevImage;
    private final RandomAccessible<FloatType> infDevAccess;
    private final RandomAccess<FloatType> infDevAccessIt;

    private Img<FloatType> m_PSF;
    private final HashMap<Integer, LabelInformation> labelMap;
    private final IntensityImage aDataImage;

    public E_Deconvolution(IntensityImage aDI, HashMap<Integer, LabelInformation> labelMap, ImgFactory<FloatType> imgFactory, int dim[]) {
        super(null, null);
        DevImage = imgFactory.create(dim, new FloatType());

        /* Create boundary strategy 0.0 outside the image */

        infDevAccess = Views.extendPeriodic(DevImage);
        infDevAccessIt = infDevAccess.randomAccess();

        m_PSF = null;

        this.labelMap = labelMap;
        aDataImage = aDI;
    }

    public void setPSF(Img<FloatType> psfImg) {
        m_PSF = psfImg;
    }

    @Override
    public Object atStart() {
        return null;
    }

    private static float Median(ArrayList<Float> values) {
        Collections.sort(values);

        if (values.size() % 2 == 1) {
            final int mid = (values.size() + 1) / 2 - 1;

            if (Float.isInfinite(values.get(mid))) {
                int i = mid;
                for (; i >= 0; i--) {
                    if (Float.isInfinite(values.get(i)) == false) {
                        break;
                    }
                }
                return values.get(i);
            }

            return values.get((values.size() + 1) / 2 - 1);
        }
        else {
            final float lower = values.get(values.size() / 2 - 1);
            final float upper = values.get(values.size() / 2);

            if (Float.isInfinite(lower) || Float.isInfinite(upper)) {
                int i = values.size() / 2;
                for (; i >= 0; i--) {
                    if (Float.isInfinite(values.get(i)) == false) {
                        break;
                    }
                }
                return values.get(i);
            }
            else {
                return (lower + upper) / 2.0f;
            }
        }
    }

    public void GenerateModelImage(LabelImageRC aLabelImage, HashMap<Integer, LabelInformation> labelMap) {
        final Cursor<FloatType> cVModelImage = DevImage.cursor();
        final int size = aLabelImage.getSize();
        for (int i = 0; i < size && cVModelImage.hasNext(); i++) {
            cVModelImage.fwd();
            int vLabel = aLabelImage.getLabelAbs(i);
            if (vLabel == aLabelImage.forbiddenLabel) {
                vLabel = 0; // Set Background value ??
            }

            if (Float.isInfinite((float) labelMap.get(vLabel).median)) {
                // int debug = 0;
                // debug++;
            }

            cVModelImage.get().set((float) labelMap.get(vLabel).median);

        }

        // Convolve

        new FFTConvolution<FloatType>(DevImage, m_PSF).convolve();

    }

    public void RenewDeconvolution(LabelImageRC aInitImage) {
        /**
         * Generate a model image using rough estimates of the intensities. Here,
         * we use the old intensity values.
         * For all FG regions (?), find the median of the scaling factor.
         */

        // / The BG region is not fitted above(since it may be very large and thus
        // / the mean is a good approx), set it to the mean value:
        final double vOldBG = aInitImage.getLabelMap().get(0).median;

        // Time vs. Memory:
        // Memory efficient: iterate the label image: for all new seed points (new label found),
        // iterate through the label using a floodfill iterator. While iterating,
        // read out the data and model image. Put the quotient of those into an
        // 'new' array. Sort the array, read out the median and delete the array.
        //
        // Time efficient: iterate the label image. Store all quotients found in
        // an array corresponding to the label. This is another 32-bit copy of
        // the image.

        // / Set up a map datastructure that maps from labels to arrays of data
        // / values. These arrays will be sorted to read out the median.

        final HashMap<Integer, Integer> vLabelCounter = new HashMap<Integer, Integer>();
        final HashMap<Integer, Float> vIntensitySum = new HashMap<Integer, Float>();

        final HashMap<Integer, ArrayList<Float>> vScalings3 = new HashMap<Integer, ArrayList<Float>>();

        // / For all the active labels, create an entry in the map and initialize
        // / an array as the corresponding value.
        final Iterator<Map.Entry<Integer, LabelInformation>> vActiveLabelsIt = aInitImage.getLabelMap().entrySet().iterator();
        while (vActiveLabelsIt.hasNext()) {
            final Map.Entry<Integer, LabelInformation> Label = vActiveLabelsIt.next();
            final int vLabel = Label.getKey();
            if (vLabel == aInitImage.forbiddenLabel) {
                continue;
            }
            vScalings3.put(vLabel, new ArrayList<Float>());
            vLabelCounter.put(vLabel, 0);
            vIntensitySum.put(vLabel, 0.0f);
        }

        final Cursor<FloatType> cVDevImage = DevImage.cursor();
        final int size = aInitImage.getSize();
        for (int i = 0; i < size && cVDevImage.hasNext(); i++) {
            cVDevImage.fwd();
            final int vLabelAbs = aInitImage.getLabelAbs(i);
            if (vLabelAbs == aInitImage.forbiddenLabel) {
                continue;
            }
            vLabelCounter.put(vLabelAbs, vLabelCounter.get(vLabelAbs) + 1);

            if (vLabelAbs == 0) {
                final float vBG = aDataImage.get(i) - (cVDevImage.get().get() - (float) vOldBG);
                final ArrayList<Float> arr = vScalings3.get(vLabelAbs);
                arr.add(vBG);
            }
            else {
                final float vScale = (aDataImage.get(i) - (float) vOldBG) / (cVDevImage.get().get() - (float) vOldBG);

                final ArrayList<Float> arr = vScalings3.get(vLabelAbs);
                arr.add(vScale);
            }
        }

        // / For all the labels (except the BG ?) sort the scalar factors for all
        // / the pixel. The median is in the middle of the sorted list.
        // / TODO: check if fitting is necessary for the BG.
        // / TODO: Depending on the size of the region, sorting takes too long and
        // / a median of medians algorithm (O(N)) could provide a good
        // / approximation of the median.

        final Iterator<Map.Entry<Integer, ArrayList<Float>>> vScaling3It = vScalings3.entrySet().iterator();
        while (vScaling3It.hasNext()) {
            final Map.Entry<Integer, ArrayList<Float>> vLabel = vScaling3It.next();
            float vMedian;
            if (aInitImage.getLabelMap().get(vLabel.getKey()).count > 2) {
                vMedian = Median(vScalings3.get(vLabel.getKey()));
            }
            else {
                vMedian = (float) aInitImage.getLabelMap().get(vLabel.getKey()).mean;
            }

            // / TESTING: REMOVE THE NEXT LINE
            // vMedian = vIntensitySum[vLabelAbs] / vLabelCounter[vLabelAbs];

            // / Correct the old intensity values.
            if (vLabel.getKey() == 0) {
                if (vMedian < 0) {
                    vMedian = 0;
                }
                aInitImage.getLabelMap().get(vLabel.getKey()).median = vMedian;
            }
            else {
                // Avoid Nan

                if (Double.isInfinite(vMedian)) {
                    // Search for the first non infinite value

                    // int debug = 0;
                    // debug++;
                    vMedian = Median(vScalings3.get(vLabel.getKey()));
                }

                aInitImage.getLabelMap().get(vLabel.getKey()).median = (aInitImage.getLabelMap().get(vLabel.getKey()).median - vOldBG) * vMedian + aInitImage.getLabelMap().get(0).median;
            }
        }

        // The model image has to be renewed as well to match the new statistic values:
        GenerateModelImage(aInitImage, aInitImage.getLabelMap());
    }

    @Override
    public EnergyResult CalculateEnergyDifference(Point aIndex, ContourParticle contourParticle, int aToLabel) {

        final int aFromLabel = contourParticle.label;
        infDevAccessIt.setPosition(aIndex.x/* pos.x */);

        final EnergyResult vEnergyDiff = new EnergyResult(0.0, false);

        final float vIntensity_FromLabel = (float) labelMap.get(aFromLabel).median;
        final float vIntensity_ToLabel = (float) labelMap.get(aToLabel).median;

        final long dimlen[] = new long[m_PSF.numDimensions()];
        m_PSF.dimensions(dimlen);

        // middle coord

        // Point sizeP = new Point (dimlen);

        for (int i = 0; i < m_PSF.numDimensions(); i++) {
            dimlen[i] = dimlen[i] / 2;
        }
        final Point middle = new Point(dimlen);
        final Point LowerCorner = aIndex.sub(middle);

        Point pos = new Point(dimlen);
        final int loc[] = new int[m_PSF.numDimensions()];
        final Cursor<FloatType> vPSF = m_PSF.localizingCursor();

        // Point UpperCorner = LowerCorner.add(sizeP);

        // if we are

        /*
         * if (aDataImage.isOutOfBound(LowerCorner) == true && aDataImage.isOutOfBound(UpperCorner) == true)
         * {
         */

        while (vPSF.hasNext()) {

            vPSF.fwd();
            // Add aindex to cursor

            pos = new Point(LowerCorner);
            vPSF.localize(loc);
            pos = pos.add(new Point(loc));

            infDevAccessIt.setPosition(pos.x);

            float vEOld = (infDevAccessIt.get().get() - aDataImage.getSafe(pos));
            vEOld = vEOld * vEOld;
            // vEOld = fabs(vEOld);
            float vENew = (infDevAccessIt.get().get() - aDataImage.getSafe(pos) + (vIntensity_ToLabel - vIntensity_FromLabel) * vPSF.get().get());
            vENew = vENew * vENew;

            vEnergyDiff.energyDifference += vENew - vEOld;

        }
        /*
         * }
         * else
         * {
         * while (vPSF.hasNext())
         * {
         * vPSF.fwd();
         * // Add aindex to cursor
         * pos = new Point(middle);
         * vPSF.localize(loc);
         * pos = pos.add(new Point(loc));
         * infDevAccessIt.setPosition(pos.x);
         * float vEOld = (infDevAccessIt.get().get() - aDataImage.get(pos));
         * vEOld = vEOld * vEOld;
         * // vEOld = fabs(vEOld);
         * float vENew = (infDevAccessIt.get().get() - aDataImage.get(pos) + ((float)vIntensity_ToLabel - (float)vIntensity_FromLabel)*vPSF.get().get());
         * vENew = vENew * vENew;
         * vEnergyDiff.energyDifference += vENew - vEOld;
         * }
         * }
         */

        return vEnergyDiff;
    }

    public void UpdateConvolvedImage(Point aIndex, LabelImageRC aLabelImage, int aFromLabel, int aToLabel) {
        /**
         * Subtract a scaled psf from the ideal image
         */

        // / Iterate through the region and subtract the psf from the conv image.

        final long dimlen[] = new long[m_PSF.numDimensions()];
        m_PSF.dimensions(dimlen);

        // middle coord

        for (int i = 0; i < m_PSF.numDimensions(); i++) {
            dimlen[i] = dimlen[i] / 2;
        }
        Point middle = new Point(dimlen);
        middle = aIndex.sub(middle);

        Point pos = new Point(m_PSF.numDimensions());
        final int loc[] = new int[m_PSF.numDimensions()];

        final Cursor<FloatType> vPSF = m_PSF.localizingCursor();

        if (aToLabel == 0) { // ...the point is removed and set to BG
                             // To avoid the operator map::[] in the loop:
            final float vIntensity_FromLabel = (float) aLabelImage.getLabelMap().get(aFromLabel).median;
            final float vIntensity_BGLabel = (float) aLabelImage.getLabelMap().get(aToLabel).median;
            while (vPSF.hasNext()) {
                vPSF.fwd();
                // Add aindex to cursor

                pos.zero();
                vPSF.localize(loc);
                pos = pos.add(new Point(loc));

                pos = pos.add(middle);
                infDevAccessIt.setPosition(pos.x);

                infDevAccessIt.get().set(infDevAccessIt.get().get() - (vIntensity_FromLabel - vIntensity_BGLabel) * vPSF.get().get());
            }
        }
        else {
            final float vIntensity_ToLabel = (float) aLabelImage.getLabelMap().get(aToLabel).median;
            final float vIntensity_BGLabel = (float) aLabelImage.getLabelMap().get(0).median;
            while (vPSF.hasNext()) {
                vPSF.fwd();
                // Add aindex to cursor

                pos.zero();
                vPSF.localize(loc);
                pos = pos.add(new Point(loc));

                pos = pos.add(middle);
                infDevAccessIt.setPosition(pos.x);

                infDevAccessIt.get().set(infDevAccessIt.get().get() + (vIntensity_ToLabel - vIntensity_BGLabel) * vPSF.get().get());
            }
        }
    }

}
