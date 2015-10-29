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
        int nOtherNeighbors = 0;
        for (final int neighbor : labelImage.iterateNeighbours(pIndex)) {
            final int neighborLabel = labelImage.getLabelAbs(neighbor);
            if (neighborLabel == pLabel) {
                nSameNeighbors++;
            }
            else {
                nOtherNeighbors++;
            }
        }

        // TODO is this true? conn.getNNeighbors
        final double dGamma = (nOtherNeighbors - nSameNeighbors) / (double) conn.getNeighborhoodSize();
        return new EnergyResult(dGamma, false);
    }

}
