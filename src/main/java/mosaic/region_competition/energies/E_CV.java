package mosaic.region_competition.energies;


import java.util.HashMap;

import mosaic.core.imageUtils.Point;
import mosaic.region_competition.RC.ContourParticle;
import mosaic.region_competition.energies.Energy.ExternalEnergy;
import mosaic.region_competition.utils.LabelStatistics;


public class E_CV extends ExternalEnergy {

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
    public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel, HashMap<Integer, LabelStatistics> labelMap) {
        final int fromLabel = contourParticle.label;
        final float aValue = contourParticle.intensity;
        final LabelStatistics toStats = labelMap.get(toLabel);
        final LabelStatistics fromStats = labelMap.get(fromLabel);
        final double newToMean = (toStats.iSum + aValue) / (toStats.iLabelCount + 1);
        final double energy = (aValue - newToMean) * (aValue - newToMean) - (aValue - fromStats.iMeanIntensity) * (aValue - fromStats.iMeanIntensity);
        return new EnergyResult(energy, false);
    }
}
