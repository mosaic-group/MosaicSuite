package mosaic.region_competition.energies;


import java.util.HashMap;

import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.LabelImage;
import mosaic.core.utils.Point;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.LabelInformation;


public abstract class Energy {

    /**
     * @param labelMap 
     * @return EnergyResult, entries (energy or merge) are null if not calculated by this energy
     */
    public abstract EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel, HashMap<Integer, LabelInformation> labelMap);

    public static class EnergyResult {

        EnergyResult(Double energy, Boolean merge) {
            this.energyDifference = energy;
            this.merge = merge;
        }

        public Double energyDifference;
        public final Boolean merge;
    }

    /**
     * Responsible for regularization
     * Independent of image I
     */
    static abstract class InternalEnergy extends Energy {

        protected final LabelImage labelImage;

        InternalEnergy(LabelImage labelImage) {
            this.labelImage = labelImage;
        }
    }

    /**
     * Responsible for data fidelity
     */
    static abstract class ExternalEnergy extends Energy {

        protected final IntensityImage intensityImage;
        protected final LabelImage labelImage;

        ExternalEnergy(LabelImage labelImage, IntensityImage intensityImage) {
            this.labelImage = labelImage;
            this.intensityImage = intensityImage;
        }
    }
}
