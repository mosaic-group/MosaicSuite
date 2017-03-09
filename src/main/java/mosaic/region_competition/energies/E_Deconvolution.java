package mosaic.region_competition.energies;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.utils.MosaicUtils;
import mosaic.region_competition.RC.ContourParticle;
import mosaic.region_competition.RC.LabelStatistics;
import mosaic.region_competition.energies.Energy.ExternalEnergy;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;


public class E_Deconvolution extends ExternalEnergy {

    final private Img<FloatType> DevImage;
    final private RandomAccess<FloatType> infDevAccessIt;
    final private IntensityImage iImage;
    final private Img<FloatType> iPsf;
    final private Point iMiddlePointPsf;

    public E_Deconvolution(IntensityImage aImage, Img<FloatType> aPsf) {
        DevImage = new ArrayImgFactory<FloatType>().create(aImage.getDimensions(), new FloatType());
        infDevAccessIt = Views.extendPeriodic(DevImage).randomAccess();
        iImage = aImage;
        iPsf = aPsf;
        int[] psfDims = MosaicUtils.getImageIntDimensions(iPsf);
        for (int i = 0; i < psfDims.length; ++i) {
            psfDims[i] = psfDims[i] / 2;
        }
        iMiddlePointPsf = new Point(psfDims);
    }

    @Override
    public EnergyResult CalculateEnergyDifference(Point aIndex, ContourParticle contourParticle, int aToLabel, HashMap<Integer, LabelStatistics> labelMap) {
        final int aFromLabel = contourParticle.label;
        final float intensityDelta = (float)(labelMap.get(aToLabel).median - labelMap.get(aFromLabel).median);
        
        final Point LowerCorner = aIndex.sub(iMiddlePointPsf);
    
        final int loc[] = new int[iPsf.numDimensions()];
        final Cursor<FloatType> vPSF = iPsf.localizingCursor();
        double energyDifference = 0.0;
        while (vPSF.hasNext()) {
            vPSF.fwd();
            vPSF.localize(loc);
            Point pos = LowerCorner.add(loc);
            infDevAccessIt.setPosition(pos.iCoords);
    
            float vEOld = infDevAccessIt.get().get() - iImage.getSafe(pos);
            float vENew = vEOld + intensityDelta * vPSF.get().get();
            energyDifference += vENew * vENew - vEOld * vEOld;
        }
    
        return new EnergyResult(energyDifference, false);
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
        return (lower + upper) / 2.0f;
    }

    public void GenerateModelImage(LabelImage aLabelImage, HashMap<Integer, LabelStatistics> labelMap) {
        final Cursor<FloatType> cVModelImage = DevImage.cursor();
        final int size = aLabelImage.getSize();
        for (int i = 0; i < size && cVModelImage.hasNext(); i++) {
            cVModelImage.fwd();
            int vLabel = aLabelImage.getLabelAbs(i);
            if (aLabelImage.isBorderLabel(vLabel)) {
                vLabel = 0; // Set Background value ??
            }

            cVModelImage.get().set((float) labelMap.get(vLabel).median);
        }

        new FFTConvolution<FloatType>(DevImage, iPsf).convolve();

    }

    public void RenewDeconvolution(LabelImage aInitImage, HashMap<Integer, LabelStatistics> aLabelMap) {
        /**
         * Generate a model image using rough estimates of the intensities. Here,
         * we use the old intensity values.
         * For all FG regions (?), find the median of the scaling factor.
         */

        // The BG region is not fitted above(since it may be very large and thus
        // the mean is a good approx), set it to the mean value:
        final double vOldBG = aLabelMap.get(0).median;

        // Time vs. Memory:
        // Memory efficient: iterate the label image: for all new seed points (new label found),
        // iterate through the label using a floodfill iterator. While iterating,
        // read out the data and model image. Put the quotient of those into an
        // 'new' array. Sort the array, read out the median and delete the array.
        //
        // Time efficient: iterate the label image. Store all quotients found in
        // an array corresponding to the label. This is another 32-bit copy of
        // the image.

        // Set up a map datastructure that maps from labels to arrays of data
        // values. These arrays will be sorted to read out the median.

        final HashMap<Integer, Integer> vLabelCounter = new HashMap<Integer, Integer>();
        final HashMap<Integer, Float> vIntensitySum = new HashMap<Integer, Float>();

        final HashMap<Integer, ArrayList<Float>> vScalings3 = new HashMap<Integer, ArrayList<Float>>();

        // For all the active labels, create an entry in the map and initialize
        // an array as the corresponding value.
        final Iterator<Map.Entry<Integer, LabelStatistics>> vActiveLabelsIt = aLabelMap.entrySet().iterator();
        while (vActiveLabelsIt.hasNext()) {
            final Map.Entry<Integer, LabelStatistics> Label = vActiveLabelsIt.next();
            final int vLabel = Label.getKey();
            if (aInitImage.isBorderLabel(vLabel)) {
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
            if (aInitImage.isBorderLabel(vLabelAbs)) {
                continue;
            }
            vLabelCounter.put(vLabelAbs, vLabelCounter.get(vLabelAbs) + 1);

            if (vLabelAbs == 0) {
                final float vBG = iImage.get(i) - (cVDevImage.get().get() - (float) vOldBG);
                final ArrayList<Float> arr = vScalings3.get(vLabelAbs);
                arr.add(vBG);
            }
            else {
                final float vScale = (iImage.get(i) - (float) vOldBG) / (cVDevImage.get().get() - (float) vOldBG);

                final ArrayList<Float> arr = vScalings3.get(vLabelAbs);
                arr.add(vScale);
            }
        }

        // For all the labels (except the BG ?) sort the scalar factors for all
        // the pixel. The median is in the middle of the sorted list.
        // TODO: check if fitting is necessary for the BG.
        // TODO: Depending on the size of the region, sorting takes too long and
        // a median of medians algorithm (O(N)) could provide a good
        // approximation of the median.

        final Iterator<Map.Entry<Integer, ArrayList<Float>>> vScaling3It = vScalings3.entrySet().iterator();
        while (vScaling3It.hasNext()) {
            final Map.Entry<Integer, ArrayList<Float>> vLabel = vScaling3It.next();
            float vMedian;
            if (aLabelMap.get(vLabel.getKey()).count > 2) {
                vMedian = Median(vScalings3.get(vLabel.getKey()));
            }
            else {
                vMedian = (float) aLabelMap.get(vLabel.getKey()).mean;
            }

            // Correct the old intensity values.
            if (vLabel.getKey() == 0) {
                if (vMedian < 0) {
                    vMedian = 0;
                }
                aLabelMap.get(vLabel.getKey()).median = vMedian;
            }
            else {
                // Avoid Nan
                if (Double.isInfinite(vMedian)) {
                    // Search for the first non infinite value
                    vMedian = Median(vScalings3.get(vLabel.getKey()));
                }

                aLabelMap.get(vLabel.getKey()).median = (aLabelMap.get(vLabel.getKey()).median - vOldBG) * vMedian + aLabelMap.get(0).median;
            }
        }

        // The model image has to be renewed as well to match the new statistic values:
        GenerateModelImage(aInitImage, aLabelMap);
    }

    public void UpdateConvolvedImage(Point aIndex, int aFromLabel, int aToLabel, HashMap<Integer, LabelStatistics> aLabelMap) {
        Point currentPos = aIndex.sub(iMiddlePointPsf);
        
        if (aToLabel == 0) { 
            // ...the point is removed and set to BG To avoid the operator map::[] in the loop:
            final float vIntensity_FromLabel = (float) aLabelMap.get(aFromLabel).median;
            final float vIntensity_BGLabel = (float) aLabelMap.get(aToLabel).median;
            subtractPsfFromConvImage(currentPos, vIntensity_FromLabel, vIntensity_BGLabel);
        }
        else {
            final float vIntensity_ToLabel = (float) aLabelMap.get(aToLabel).median;
            final float vIntensity_BGLabel = (float) aLabelMap.get(0).median;
            subtractPsfFromConvImage(currentPos, vIntensity_BGLabel, vIntensity_ToLabel);
        }
    }
    
    private void subtractPsfFromConvImage(Point currentPos, final float fromLabel, final float toLabel) {
        final int loc[] = new int[iPsf.numDimensions()];
        final Cursor<FloatType> vPSF = iPsf.localizingCursor();

        while (vPSF.hasNext()) {
            vPSF.fwd();
            vPSF.localize(loc);
            Point pos = new Point(loc).add(currentPos);
            infDevAccessIt.setPosition(pos.iCoords);
            infDevAccessIt.get().set(infDevAccessIt.get().get() - (fromLabel - toLabel) * vPSF.get().get());
        }
    }
}
