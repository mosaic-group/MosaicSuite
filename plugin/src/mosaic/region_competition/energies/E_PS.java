package mosaic.region_competition.energies;


import java.util.HashMap;

import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.LabelImage;
import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIteratorMask;
import mosaic.core.utils.SphereMask;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.LabelInformation;
import mosaic.region_competition.energies.Energy.ExternalEnergy;


public class E_PS extends ExternalEnergy {

    private final int[] dimensions;
    private final int bgLabel;

    private final float regionMergingThreshold;
    private final SphereMask sphere;
    private final RegionIteratorMask sphereIt;

    public E_PS(LabelImage labelImage, IntensityImage intensityImage, int PSenergyRadius, float regionMergingThreshold) {
        super(labelImage, intensityImage);
        this.dimensions = labelImage.getDimensions();
        this.bgLabel = LabelImage.BGLabel;

        this.regionMergingThreshold = regionMergingThreshold;
        final int rad = PSenergyRadius;
        sphere = new SphereMask(rad, 2 * rad + 1, labelImage.getNumOfDimensions());

        // sphereIt is slower than separate version

        sphereIt = new RegionIteratorMask(sphere, dimensions);
    }

    /**
     * Here we have the possibility to either put the current pixel
     * value to the BG, calculate the BG-mean and then calculate the
     * squared distance of the pixel to both means, BG and the mean
     * of the region (where the pixel currently still belongs to).
     * The second option is to remove the pixel from the region and
     * calculate the new mean of this region. Then compare the squared
     * distance to both means. This option needs a region to be larger
     * than 1 pixel/voxel.
     */
    @Override
    public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel, HashMap<Integer, LabelInformation> labelMap) {
        final double value = contourParticle.intensity;
        final int fromLabel = contourParticle.label;

        // read out the size of the mask
        // vRegion is the size of our temporary window

        sphereIt.setMidPoint(contourPoint);

        // vOffset is basically the difference of the center and the start of the window

        double vSumFrom = -value; // we ignore the value of the center point
        double vSumTo = 0;
        double vSumOfSqFrom = -value * value; // ignore the value of the center point.
        double vSumOfSqTo = 0.0;
        int vNFrom = -1;
        int vNTo = 0;

        while (sphereIt.hasNext()) {
            final int labelIdx = sphereIt.next();

            final int dataIdx = labelIdx;
            final int absLabel = labelImage.getLabelAbs(labelIdx);
            if (absLabel == fromLabel) {
                final double data = intensityImage.get(dataIdx);
                vSumFrom += data;
                vSumOfSqFrom += data * data;
                vNFrom++;
            }
            else if (absLabel == toLabel) {
                final double data = intensityImage.get(dataIdx);
                vSumTo += data;
                vSumOfSqTo += data * data;
                vNTo++;
            }
        }

        double vMeanTo;
        double vVarTo;
        double vMeanFrom;
        double vVarFrom;
        if (vNTo == 0) // this should only happen with the BG label
        {
            final LabelInformation info = labelMap.get(toLabel);
            vMeanTo = info.mean;
            vVarTo = info.var;
        }
        else {
            vMeanTo = vSumTo / vNTo;
            // vVarTo = (vSumOfSqTo - vSumTo * vSumTo / vNTo) / (vNTo - 1);
            vVarTo = (vSumOfSqTo - vSumTo * vSumTo / vNTo) / (vNTo);
        }

        if (vNFrom == 0) {
            final LabelInformation info = labelMap.get(fromLabel);
            vMeanFrom = info.mean;
            vVarFrom = info.var;
        }
        else {
            vMeanFrom = vSumFrom / vNFrom;
            vVarFrom = (vSumOfSqFrom - vSumFrom * vSumFrom / vNFrom) / (vNFrom);
        }

        // double vVarFrom = (vSumOfSqFrom - vSumFrom * vSumFrom / vNFrom) / (vNFrom - 1);

        boolean vMerge = false;

        if (fromLabel != bgLabel && toLabel != bgLabel) {
            final double e = E_KLMergingCriterion.CalculateKLMergingCriterion(vMeanFrom, vMeanTo, vVarFrom, vVarTo, vNFrom, vNTo);
            if (e < regionMergingThreshold) {
                vMerge = true;
            }
        }

        final double vEnergyDiff = (value - vMeanTo) * (value - vMeanTo) - (value - vMeanFrom) * (value - vMeanFrom);

        return new EnergyResult(vEnergyDiff, vMerge);// <Double, Boolean>(vEnergyDiff, vMerge);
    }
}
