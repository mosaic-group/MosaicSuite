package mosaic.region_competition.energies;


import java.util.HashMap;

import mosaic.core.imageUtils.Point;
import mosaic.region_competition.RC.ContourParticle;
import mosaic.region_competition.RC.LabelStatistics;


public abstract class Energy {

    /**
     * @param labelMap 
     * @return EnergyResult, entries (energy or merge) are null if not calculated by this energy
     */
    public abstract EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel, HashMap<Integer, LabelStatistics> labelMap);

    public void initEnergy() { /* override if needed */}
    public void updateEnergy() { /*override if needed */}
    
    public static class EnergyResult {
        EnergyResult(Double energy, Boolean merge) {
            this.energyDifference = energy;
            this.merge = merge;
        }
        
        public final Double energyDifference;
        public final Boolean merge;
    }

    /**
     * Responsible for regularization
     */
    public static abstract class InternalEnergy extends Energy {}

    /**
     * Responsible for data fidelity
     */
    public static abstract class ExternalEnergy extends Energy {}
}
