package mosaic.region_competition.energies;


import java.util.HashMap;

import mosaic.core.imageUtils.Point;
import mosaic.region_competition.RC.ContourParticle;
import mosaic.region_competition.energies.Energy.EnergyResult;
import mosaic.region_competition.energies.Energy.ExternalEnergy;
import mosaic.region_competition.energies.Energy.InternalEnergy;
import mosaic.region_competition.utils.LabelStatistics;


public class ImageModel {
    
    // Settings
    private static final float EnergyRegionCoeff = 1.0f;
    private static final float ConstantOutwardFlow = 0.0f; 
    
    
    private final ExternalEnergy iEnergyData;
    private final InternalEnergy iEnergyLength;
    private ExternalEnergy iEnergyMerge;
    private final float iEnergyContourLengthCoeff;

    public ImageModel(ExternalEnergy aEnergyData, InternalEnergy aEnergyLength, ExternalEnergy aEnergyMerge, float aEnergyContourLengthCoeff) {
        iEnergyData = aEnergyData;
        iEnergyLength = aEnergyLength;
        iEnergyMerge = aEnergyMerge;
        iEnergyContourLengthCoeff = aEnergyContourLengthCoeff;
    }

    public Energy getEdata() {
        return iEnergyData;
    }

    public EnergyResult calculateDeltaEnergy(Point aContourIndex, ContourParticle aContourPointPtr, int aToLabel, HashMap<Integer, LabelStatistics> aLabelMap) {
        Double energyChange = 0.0;
        Boolean shouldMerge = false;

        // Calculate the change in energy due to the change of intensity when changing from one label 'from' to another 'to'.
        EnergyResult e = iEnergyData.CalculateEnergyDifference(aContourIndex, aContourPointPtr, aToLabel, aLabelMap);
        energyChange += EnergyRegionCoeff * e.energyDifference;
        // vMerge may be null here and will be set below at the merge energy.
        shouldMerge = e.merge;

        // Contour Length (Regularization)
        if (iEnergyContourLengthCoeff != 0 && iEnergyLength != null) {
            e = iEnergyLength.CalculateEnergyDifference(aContourIndex, aContourPointPtr, aToLabel, aLabelMap);
            energyChange += iEnergyContourLengthCoeff * e.energyDifference;
        }

        // add a balloon force and a constant outward flow. If fronts were touching, no constant flow is imposed (cancels out).
        energyChange -= (aContourPointPtr.label == 0) ? ConstantOutwardFlow : -ConstantOutwardFlow;

        // For the full-region based energy models, register competing regions undergo a merge and use e_merge explicitly
        if (iEnergyMerge != null) {
            e = iEnergyMerge.CalculateEnergyDifference(aContourIndex, aContourPointPtr, aToLabel, aLabelMap);
            shouldMerge = e.merge;
        }

        return new EnergyResult(energyChange, shouldMerge);
    }
}
