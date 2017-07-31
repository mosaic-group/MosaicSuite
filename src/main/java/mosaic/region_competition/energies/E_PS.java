package mosaic.region_competition.energies;


import java.util.HashMap;

import mosaic.core.imageUtils.MaskOnSpaceMapper;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.masks.BallMask;
import mosaic.region_competition.RC.ContourParticle;
import mosaic.region_competition.RC.LabelStatistics;
import mosaic.region_competition.energies.Energy.ExternalEnergy;


public class E_PS extends ExternalEnergy {

    private final int iBgLabel;

    private final IntensityImage iIntensityImage;
    private final LabelImage iLabelImage;

    private final float iRegionMergingThreshold;
    private final MaskOnSpaceMapper iSphereIt;
    
    public E_PS(LabelImage aLabelImage, IntensityImage aIntensityImage, int aPsEnergyRadius, float aRegionMergingThreshold) {
        iBgLabel = LabelImage.BGLabel;
        iIntensityImage = aIntensityImage;
        iLabelImage = aLabelImage;
        iRegionMergingThreshold = aRegionMergingThreshold;
        iSphereIt = new MaskOnSpaceMapper(new BallMask(aPsEnergyRadius, iLabelImage.getNumOfDimensions()), iLabelImage.getDimensions());
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
    public EnergyResult CalculateEnergyDifference(Point aContourPoint, ContourParticle aContourParticle, int aToLabel, HashMap<Integer, LabelStatistics> aLabelStats) {
        final double value = aContourParticle.intensity;
        final int fromLabel = aContourParticle.label;

        iSphereIt.setMiddlePoint(aContourPoint);

        double sumFrom = -value; // we ignore the value of the center point
        double sumSquaredFrom = -value * value; // ignore the value of the center point.
        int cntFrom = -1;
        
        double sumTo = 0;
        double sumSquaredTo = 0.0;
        int cntTo = 0;

        while (iSphereIt.hasNext()) {
            final int labelIdx = iSphereIt.next();
            final int absLabel = iLabelImage.getLabelAbs(labelIdx);
            
            if (absLabel == fromLabel) {
                final double data = iIntensityImage.get(labelIdx);
                sumFrom += data;
                sumSquaredFrom += data * data;
                cntFrom++;
            }
            else if (absLabel == aToLabel) {
                final double data = iIntensityImage.get(labelIdx);
                sumTo += data;
                sumSquaredTo += data * data;
                cntTo++;
            }
        }

        double meanTo;
        double varTo;
        if (cntTo == 0) {
            // this 'if' should only happen with the BG label
            final LabelStatistics info = aLabelStats.get(aToLabel);
            meanTo = info.iMeanIntensity;
            varTo = info.iVarIntensity;
//            mosaic.utils.Debug.print("DEBUG varTo if: ", aToLabel, info, meanTo, varTo);
        }
        else {
            meanTo = sumTo / cntTo;
            varTo = (sumSquaredTo - sumTo * sumTo / cntTo) / (cntTo);
//            mosaic.utils.Debug.print("DEBUG varTo", sumSquaredTo, sumSquaredTo, cntTo);
            // varTo = (sumSquaredTo - sumTo * sumTo / cntTo) / (cntTo - 1);
        }

        double meanFrom;
        double varFrom;
        if (cntFrom == 0) {
            final LabelStatistics info = aLabelStats.get(fromLabel);
            meanFrom = info.iMeanIntensity;
            varFrom = info.iVarIntensity;
        }
        else {
            meanFrom = sumFrom / cntFrom;
            varFrom = (sumSquaredFrom - sumFrom * sumFrom / cntFrom) / (cntFrom);
//            mosaic.utils.Debug.print("DEBUG varFrom", sumSquaredFrom, sumFrom, cntFrom);
            // varFrom = (sumSquaredFrom - sumFrom * sumFrom / cntFrom) / (cntFrom - 1);
        }

        boolean shouldMerge = false;
        if (fromLabel != iBgLabel && aToLabel != iBgLabel) {
            if (E_KLMergingCriterion.calc(meanFrom, meanTo, varFrom, varTo, cntFrom, cntTo) < iRegionMergingThreshold) {
                shouldMerge = true;
            }
        }

        final double energyDiff = Math.pow(value - meanTo, 2) - Math.pow(value - meanFrom, 2);

        return new EnergyResult(energyDiff, shouldMerge);
    }
}
