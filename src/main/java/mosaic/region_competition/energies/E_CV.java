package mosaic.region_competition.energies;


import java.util.HashMap;

import mosaic.core.imageUtils.Point;
import mosaic.region_competition.RC.ContourParticle;
import mosaic.region_competition.RC.LabelStatistics;
import mosaic.region_competition.energies.Energy.ExternalEnergy;


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
//        System.out.println("label to/from: " + toLabel + "/" + fromLabel);
//        System.out.println("tostats: " + toStats.iMeanIntensity + " " + toStats.iLabelCount);
        final double newToMean = (toStats.iMeanIntensity * toStats.iLabelCount + aValue) / (toStats.iLabelCount + 1);
//        System.out.println("val/newMean/oldMean: " + aValue + " " + newToMean + " " + fromStats.iMeanIntensity);
        final double energy = Math.pow(aValue - newToMean, 2) - Math.pow(aValue - fromStats.iMeanIntensity, 2);
        return new EnergyResult(energy, false);
    }
}
