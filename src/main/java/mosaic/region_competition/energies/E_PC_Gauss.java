package mosaic.region_competition.energies;

import java.util.HashMap;

import mosaic.core.imageUtils.Point;
import mosaic.region_competition.RC.ContourParticle;
import mosaic.region_competition.RC.LabelStatistics;
import mosaic.region_competition.energies.Energy.ExternalEnergy;

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
    @Override
    public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel, HashMap<Integer, LabelStatistics> labelMap) {
        final int fromLabel = contourParticle.label;
        final float aValue = contourParticle.intensity;
        
        final LabelStatistics totoStats = labelMap.get(toLabel);
        final LabelStatistics fromStats = labelMap.get(fromLabel);
        
        final double newToToMean = (totoStats.iMeanIntensity * totoStats.iLabelCount + aValue) / (totoStats.iLabelCount + 1);
        final double newFromMean = (fromStats.iMeanIntensity * fromStats.iLabelCount - aValue) / (fromStats.iLabelCount - 1);
        
        // Before changing the mean, compute the sum of squares of the samples:
        final double totoCount = totoStats.iLabelCount;
        final double fromCount = fromStats.iLabelCount;
        final double vToToLabelSumOfSq = totoStats.iVarIntensity * (totoCount - 1.0) + totoCount * totoStats.iMeanIntensity * totoStats.iMeanIntensity;
        final double vFromLabelSumOfSq = fromStats.iVarIntensity * (fromCount - 1.0) + fromCount * fromStats.iMeanIntensity * fromStats.iMeanIntensity;
        
        double newToVar =   ((1.0 / (totoCount)) * (vToToLabelSumOfSq + aValue * aValue - 2.0 * newToToMean * (totoStats.iMeanIntensity * totoCount + aValue) + (totoCount + 1.0) * newToToMean * newToToMean));
        double newFromVar = ((1.0 / (fromCount - 2)) * (vFromLabelSumOfSq - aValue * aValue - 2.0 * newFromMean * (fromStats.iMeanIntensity * fromCount - aValue) + (fromCount - 1.0) * newFromMean * newFromMean));
        double oldToVar = totoStats.iVarIntensity;
        double oldFromVar = fromStats.iVarIntensity;
        
        if (newToVar == 0) newToVar = Math.ulp(1.0) * 10;
        if (newFromVar == 0) newFromVar = Math.ulp(1.0) * 10;
        if (oldToVar == 0) oldToVar = Math.ulp(1.0) * 10;
        if (oldFromVar == 0) oldFromVar = Math.ulp(1.0) * 10;
        
        double vOneBySq2Pi = 1.0/Math.sqrt(2.0*Math.PI);
        double first = ((fromCount-1) * Math.log(vOneBySq2Pi/ Math.sqrt(newFromVar)) - (fromCount - 1.0) / 2.0);
        first += -(fromCount * Math.log(vOneBySq2Pi / Math.sqrt(oldFromVar)) - (fromCount) / 2.0);
        first += ((totoCount+1) * Math.log(vOneBySq2Pi/ Math.sqrt(newToVar)) - (totoCount + 1.0) / 2.0);
        first += -(totoCount * Math.log(vOneBySq2Pi / Math.sqrt(oldToVar)) - (totoCount) / 2.0);
        first *= -1.0 * 1;
                
        return new EnergyResult(first, false);
    }
}
