package mosaic.region_competition.energies;


import java.util.HashMap;

import ij.measure.Calibration;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.region_competition.RC.ContourParticle;
import mosaic.region_competition.RC.LabelStatistics;
import mosaic.region_competition.energies.Energy.InternalEnergy;


public class E_CurvatureFlow extends InternalEnergy
{

    private final CurvatureBasedFlow curv;

    public E_CurvatureFlow(LabelImage labelImage, int rad, Calibration cal) {
        curv = new CurvatureBasedFlow(rad, labelImage, cal);
    }

    @Override
    public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel, HashMap<Integer, LabelStatistics> labelMap) {
        final int fromLabel = contourParticle.label;
        final double flow = curv.generateData(contourPoint, fromLabel, toLabel);
        return new EnergyResult(flow, null);
    }
}
