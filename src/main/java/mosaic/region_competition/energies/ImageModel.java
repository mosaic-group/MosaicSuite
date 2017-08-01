package mosaic.region_competition.energies;


import java.util.HashMap;

import mosaic.core.imageUtils.Point;
import mosaic.plugins.Region_Competition.EnergyFunctionalType;
import mosaic.region_competition.RC.ContourParticle;
import mosaic.region_competition.RC.LabelStatistics;
import mosaic.region_competition.RC.Settings;
import mosaic.region_competition.energies.Energy.EnergyResult;
import mosaic.region_competition.energies.Energy.ExternalEnergy;
import mosaic.region_competition.energies.Energy.InternalEnergy;


public class ImageModel {
    
    // Settings
    private static final float EnergyRegionCoeff = 1.0f;
    private static final float ConstantOutwardFlow = 0.0f; 
    
    
    private final ExternalEnergy iEnergyData;
    private final InternalEnergy iEnergyLength;
    private ExternalEnergy iEnergyMerge;
    private final Settings iSettings;

    public ImageModel(ExternalEnergy aEnergyData, InternalEnergy aEnergyLength, ExternalEnergy aEnergyMerge, Settings aSettings) {
        iEnergyData = aEnergyData;
        iEnergyLength = aEnergyLength;
        iEnergyMerge = aEnergyMerge;
        iSettings = aSettings;
    }

    public void removeMergeEnergy() {iEnergyMerge = null;}
    
    public Energy getEdata() {
        return iEnergyData;
    }

    public EnergyFunctionalType getEdataType() {
        return iSettings.m_EnergyFunctional;
    }

    public EnergyResult calculateDeltaEnergy(Point aContourIndex, ContourParticle aContourPointPtr, int aToLabel, HashMap<Integer, LabelStatistics> aLabelMap) {
        Double vEnergy = 0.0;
        Boolean vMerge = false;

        // Calculate the change in energy due to the change of intensity when changing
        // from one label 'from' to another 'to'.
        if (EnergyRegionCoeff != 0) {
            EnergyResult vV = iEnergyData.CalculateEnergyDifference(aContourIndex, aContourPointPtr, aToLabel, aLabelMap);
//            System.out.println("ExtEn: " + EnergyRegionCoeff * vV.energyDifference + " " + vV.energyDifference);
            vEnergy += EnergyRegionCoeff * vV.energyDifference ;//* 0; //FAKE
            // vMerge may be null here and will be set below at the merge energy.
            vMerge = vV.merge;
        }

        // Contour Length (Regularization)
        final float m_EnergyContourLengthCoeff = iSettings.m_EnergyContourLengthCoeff;
        if (m_EnergyContourLengthCoeff != 0 && iEnergyLength != null) {
            EnergyResult vV = iEnergyLength.CalculateEnergyDifference(aContourIndex, aContourPointPtr, aToLabel, aLabelMap);
//            System.out.println("IntEn: " + m_EnergyContourLengthCoeff * vV.energyDifference + " " + vV.energyDifference);
            vEnergy += m_EnergyContourLengthCoeff * vV.energyDifference;
        }

        // add a balloon force and a constant outward flow. If fronts were
        // touching, no constant flow is imposed (cancels out).
        if (aContourPointPtr.label == 0) // growing
        {
            final float vCurrentImageValue = aContourPointPtr.intensity;
            vEnergy -= ConstantOutwardFlow;
            if (iSettings.m_EnergyFunctional == EnergyFunctionalType.e_PS) {
                if (iSettings.m_BalloonForceCoeff > 0) { // outward flow
                    vEnergy -= iSettings.m_BalloonForceCoeff * vCurrentImageValue;
                }
                else {
                    vEnergy -= -iSettings.m_BalloonForceCoeff * (1 - vCurrentImageValue);
                }
            }
        }
        else if (aToLabel == 0) // shrinking
        {
            vEnergy += ConstantOutwardFlow;
        }

        // For the full-region based energy models, register competing regions undergo a merge.
        if (iEnergyMerge != null) // use e_merge explicitly
        {
            EnergyResult vV = iEnergyMerge.CalculateEnergyDifference(aContourIndex, aContourPointPtr, aToLabel, aLabelMap);
//            System.out.println("MergeEn: " + vV.energyDifference);
            vMerge = vV.merge;
        }

        return new EnergyResult(vEnergy, vMerge);
    }
}
