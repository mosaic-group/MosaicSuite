package mosaic.region_competition.energies;


import java.util.HashMap;

import ij.measure.Calibration;
import mosaic.core.image.LabelImage;
import mosaic.core.utils.Point;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.LabelStatistics;
import mosaic.region_competition.energies.Energy.InternalEnergy;


public class E_CurvatureFlow extends InternalEnergy// implements SettingsListener
{

    private final CurvatureBasedFlow curv;

    public E_CurvatureFlow(LabelImage labelImage, int rad, Calibration cal) {
        super(labelImage);
        curv = new CurvatureBasedFlow(rad, labelImage, cal);
    }

    @Override
    public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel, HashMap<Integer, LabelStatistics> labelMap) {
        final int fromLabel = contourParticle.label;
        final double flow = curv.generateData(contourPoint, fromLabel, toLabel);
        return new EnergyResult(flow, null);
    }
}
