package mosaic.regions.energies;

import java.util.HashMap;

import mosaic.core.imageUtils.Point;
import mosaic.regions.RC.ContourParticle;
import mosaic.regions.energies.Energy.ExternalEnergy;
import mosaic.regions.utils.LabelStatistics;

/**
 * Description from C++ implementation:
 * Computes energy differences for a pixel change in for a piece-wise 
 * constant image model and with i.i.d. Gaussian noise. 
 * The energy to minimize is \f$E = \sum_i^M(\mu_i - I(x))^2\f$ with M being the 
 * number of regions, I the image and \f$\mu_i\f$ the mean of region i.
 * In case of 2 regions this image model is also called Chan-Vese model. 
 * 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class E_PC_Gauss extends ExternalEnergy {
    double CalculateVariance(double aSumSq, double aMean, int aN) {
        if (aN < 2) return 0;
        return (aSumSq - aN * aMean * aMean)/(aN - 1.0);
    }
    
    @Override
    public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel, HashMap<Integer, LabelStatistics> labelMap) {
        final int fromLabel = contourParticle.label;
        final double aValue = contourParticle.intensity;
        
        final LabelStatistics toStats = labelMap.get(toLabel);
        final LabelStatistics fromStats = labelMap.get(fromLabel);
        
        final int toCount = toStats.iLabelCount;
        final int fromCount = fromStats.iLabelCount;
        
        double newToMean = (toStats.iSum + aValue) / (toCount + 1.0);
        double newFromMean = (fromCount > 0) ? (fromStats.iSum - aValue) / (fromCount - 1.0) : 0.0;
        double newToVar   = CalculateVariance(toStats.iSumOfSq + aValue * aValue, newToMean, toCount + 1);
        double newFromVar = CalculateVariance(fromStats.iSumOfSq - aValue * aValue, newFromMean, fromCount - 1);
        double oldToVar = toStats.iVarIntensity;
        double oldFromVar = fromStats.iVarIntensity;
        
        
        if (newToVar <= 0) newToVar = Math.ulp(1.0) * 10;
        if (newFromVar <= 0) newFromVar = Math.ulp(1.0) * 10;
        if (oldToVar <= 0) oldToVar = Math.ulp(1.0) * 10;
        if (oldFromVar <= 0) oldFromVar = Math.ulp(1.0) * 10;
        
        double vOneBySq2Pi = 1.0/Math.sqrt(2.0 * Math.PI);
        double energy = (fromCount-1.0) * Math.log(vOneBySq2Pi / Math.sqrt(newFromVar)) - (fromCount - 1.0) / 2.0;
        energy -=        fromCount      * Math.log(vOneBySq2Pi / Math.sqrt(oldFromVar)) - (fromCount) / 2.0;
        energy +=       (toCount+1.0)   * Math.log(vOneBySq2Pi / Math.sqrt(newToVar))   - (toCount + 1.0) / 2.0;
        energy -=        toCount        * Math.log(vOneBySq2Pi / Math.sqrt(oldToVar))   - (toCount) / 2.0;
        energy *= -1.0;
        
        return new EnergyResult(energy, false);
    }
}
