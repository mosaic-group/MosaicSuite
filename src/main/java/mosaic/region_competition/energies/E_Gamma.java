package mosaic.region_competition.energies;


import java.util.HashMap;

import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.region_competition.RC.ContourParticle;
import mosaic.region_competition.energies.Energy.InternalEnergy;
import mosaic.region_competition.utils.LabelStatistics;


public class E_Gamma extends InternalEnergy {

    protected final LabelImage iLabelImage;
    
    public E_Gamma(LabelImage aLabelImage) {
        iLabelImage = aLabelImage;
    }

    @Override
    public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel, HashMap<Integer, LabelStatistics> labelMap) {
        final int pLabel = contourParticle.candidateLabel;

        int nSameNeighbors = 0;
        int nOtherNeighbors = 0;
        for (final int neighbor : iLabelImage.iterateNeighbours(contourPoint)) {
            final int neighborLabel = iLabelImage.getLabelAbs(neighbor);
            if (neighborLabel == pLabel) {
                nSameNeighbors++;
            }
            else {
                nOtherNeighbors++;
            }
        }

        // TODO is this true? conn.getNNeighbors
        final double dGamma = (nOtherNeighbors - nSameNeighbors) / (double) iLabelImage.getConnFG().getNeighborhoodSize();
        return new EnergyResult(dGamma, false);
    }

}
