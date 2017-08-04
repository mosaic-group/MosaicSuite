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
    double CalculateVariance(double aSumSq, double aMean, double aN) {
        if (aN < 2) return 0; //TODO: what would be appropriate?
        return (aSumSq - aN * aMean * aMean)/(aN - 1.0);
    }
    @Override
    public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel, HashMap<Integer, LabelStatistics> labelMap) {
        final int fromLabel = contourParticle.label;
        final float aValue = contourParticle.intensity;
        
        final LabelStatistics totoStats = labelMap.get(toLabel);
        final LabelStatistics fromStats = labelMap.get(fromLabel);
        
        final double totoCount = totoStats.iLabelCount;
        final double fromCount = fromStats.iLabelCount;
        
        double newToToMean = (totoStats.iSum + aValue) / (totoCount + 1.0);
        double newFromMean = (fromStats.iSum - aValue) / (fromCount - 1.0);
        double newToVar   = CalculateVariance(totoStats.iSumOfSq + aValue * aValue, newToToMean, totoCount + 1.0);
        double newFromVar = CalculateVariance(fromStats.iSumOfSq - aValue * aValue, newFromMean, fromCount - 1.0);
        double oldToVar = CalculateVariance(totoStats.iSumOfSq, totoStats.iSum / totoCount, totoCount);
        double oldFromVar = CalculateVariance(fromStats.iSumOfSq, fromStats.iSum / fromCount, fromCount);
        
//        System.out.println(totoStats.iLabelCount + " " + fromStats.iLabelCount + " " + totoStats.iMeanIntensity + " " + fromStats.iMeanIntensity + " " + oldFromVar + " " + oldToVar);
//        System.out.println("NewMeans/NewVars: " + newToToMean + " " + newFromMean + " " + newToVar + " " + newFromVar);
        
        if (newToVar <= 0) newToVar = Math.ulp(1.0) * 10;
        if (newFromVar <= 0) newFromVar = Math.ulp(1.0) * 10;
        if (oldToVar <= 0) oldToVar = Math.ulp(1.0) * 10;
        if (oldFromVar <= 0) oldFromVar = Math.ulp(1.0) * 10;
        
        double vOneBySq2Pi = 1.0/Math.sqrt(2.0*Math.PI);
        double first = ((fromCount-1) * Math.log(vOneBySq2Pi / Math.sqrt(newFromVar)) - (fromCount - 1.0) / 2.0);
        first +=       -(fromCount    * Math.log(vOneBySq2Pi / Math.sqrt(oldFromVar)) - (fromCount) / 2.0);
        first +=       ((totoCount+1) * Math.log(vOneBySq2Pi / Math.sqrt(newToVar))   - (totoCount + 1.0) / 2.0);
        first +=          -(totoCount * Math.log(vOneBySq2Pi / Math.sqrt(oldToVar))   - (totoCount) / 2.0);
        first *= -1.0 * 1;
        return new EnergyResult(first, false);
    }
}
