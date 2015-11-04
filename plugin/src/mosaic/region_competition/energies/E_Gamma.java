package mosaic.region_competition.energies;


import java.util.HashMap;

import mosaic.core.imageUtils.Connectivity;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.LabelStatistics;
import mosaic.region_competition.energies.Energy.InternalEnergy;


public class E_Gamma extends InternalEnergy {

    protected final LabelImage labelImage;
    
    public E_Gamma(LabelImage labelImage) {
        this.labelImage = labelImage;
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
