package mosaic.region_competition.energies;


import java.util.HashMap;

import mosaic.core.imageUtils.Point;
import mosaic.plugins.Region_Competition.EnergyFunctionalType;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.LabelStatistics;
import mosaic.region_competition.Settings;
import mosaic.region_competition.energies.Energy.EnergyResult;
import mosaic.region_competition.energies.Energy.ExternalEnergy;
import mosaic.region_competition.energies.Energy.InternalEnergy;


public class ImageModel {
    
    // Settings
    private static final float EnergyRegionCoeff = 1.0f;
    private static final float ConstantOutwardFlow = 0.0f; 
    
    private final Settings settings;
    
    private final ExternalEnergy e_data;
    private final InternalEnergy e_length;
    private final ExternalEnergy e_merge;

    public ImageModel(ExternalEnergy e_data, InternalEnergy e_length, ExternalEnergy e_merge, Settings settings) {
        this.e_data = e_data;
        this.e_length = e_length;
        this.e_merge = e_merge;
        this.settings = settings;
    }

    public Energy getEdata() {
        return e_data;
    }

    public EnergyFunctionalType getEdataType() {
        return settings.m_EnergyFunctional;
    }

    public EnergyResult calculateDeltaEnergy(Point aContourIndex, ContourParticle aContourPointPtr, int aToLabel, HashMap<Integer, LabelStatistics> labelMap) {
        Double vEnergy = 0.0;
        Boolean vMerge = false;

        // Calculate the change in energy due to the change of intensity when changing
        // from one label 'from' to another 'to'.
        if (EnergyRegionCoeff != 0) {
            EnergyResult vV = e_data.CalculateEnergyDifference(aContourIndex, aContourPointPtr, aToLabel, labelMap);
            vEnergy += EnergyRegionCoeff * vV.energyDifference;
            // vMerge may be null here and will be set below at the merge energy.
            vMerge = vV.merge;
        }

        // Contour Length (Regularization)
        final float m_EnergyContourLengthCoeff = settings.m_EnergyContourLengthCoeff;
        if (m_EnergyContourLengthCoeff != 0 && e_length != null) {

            EnergyResult vV = e_length.CalculateEnergyDifference(aContourIndex, aContourPointPtr, aToLabel, labelMap);
            final Double eCurv = vV.energyDifference;
            vEnergy += m_EnergyContourLengthCoeff * eCurv;
        }

        // add a balloon force and a constant outward flow. If fronts were
        // touching, no constant flow is imposed (cancels out).
        // currentLabel == 0
        if ( aContourPointPtr.label == 0) // growing
        {
            final float vCurrentImageValue = aContourPointPtr.intensity;
            vEnergy -= ConstantOutwardFlow;
            if (settings.m_EnergyFunctional == EnergyFunctionalType.e_PS) {
                if (settings.m_BalloonForceCoeff > 0) { // outward flow
                    vEnergy -= settings.m_BalloonForceCoeff * vCurrentImageValue;
                }
                else {
                    vEnergy -= -settings.m_BalloonForceCoeff * (1 - vCurrentImageValue);
                }
            }
        }
        else if (aToLabel == 0) // shrinking
        {
            vEnergy += ConstantOutwardFlow;
        }

        // For the full-region based energy models, register competing regions undergo a merge.
        if (e_merge != null) // use e_merge explicitly
        {
            EnergyResult vV = e_merge.CalculateEnergyDifference(aContourIndex, aContourPointPtr, aToLabel, labelMap);
            vMerge = vV.merge;
        }

        return new EnergyResult(vEnergy, vMerge);
    }
}
