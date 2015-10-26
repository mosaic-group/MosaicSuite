package mosaic.region_competition.energies;


import java.util.HashMap;

import mosaic.core.image.Connectivity;
import mosaic.core.image.LabelImage;
import mosaic.core.image.Point;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.LabelStatistics;
import mosaic.region_competition.energies.Energy.InternalEnergy;


public class E_Gamma extends InternalEnergy {

    public E_Gamma(LabelImage labelImage) {
        super(labelImage);
    }

    @Override
    public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel, HashMap<Integer, LabelStatistics> labelMap) {
        final Point pIndex = contourPoint;
        final int pLabel = contourParticle.candidateLabel;

        final Connectivity conn = labelImage.getConnFG();

        int nSameNeighbors = 0;
        for (final Point neighbor : conn.iterateNeighbors(pIndex)) {
            final int neighborLabel = labelImage.getLabelAbs(neighbor);
            if (neighborLabel == pLabel) {
                nSameNeighbors++;
            }
        }

        // TODO is this true? conn.getNNeighbors
        final int nOtherNeighbors = conn.getNNeighbors() - nSameNeighbors;
        final double dGamma = (nOtherNeighbors - nSameNeighbors) / (double) conn.GetNeighborhoodSize();
        return new EnergyResult(dGamma, false);
    }

}
