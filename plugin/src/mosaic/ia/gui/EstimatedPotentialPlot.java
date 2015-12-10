package mosaic.ia.gui;

import java.awt.Color;

import ij.gui.Plot;
import mosaic.ia.Potentials.Potential;
import mosaic.ia.Potentials.PotentialNoParam;
import mosaic.ia.Potentials.PotentialType;
import mosaic.utils.math.StatisticsUtils;
import mosaic.utils.math.StatisticsUtils.MinMaxMean;

public class EstimatedPotentialPlot extends BasePlot {
    
    public EstimatedPotentialPlot(final double[] aDistances, Potential aPotential, double[] aBestPointFound, double aBestFunctionValue) {
        aPotential.calculate(aDistances, aBestPointFound);
        double[] potentialValues = aPotential.getPotential();
        plot = new Plot("Estimated potential", "distance", "Potential value", aDistances, potentialValues);
        MinMaxMean mmm = StatisticsUtils.getMinMaxMean(potentialValues);
        plot.setLimits(aDistances[0] - 1, aDistances[aDistances.length - 1], mmm.min, mmm.max);
        plot.setColor(Color.BLUE);
        plot.setLineWidth(2);
        addDescription(aPotential, aBestPointFound, aBestFunctionValue);
        
        // In case of non parametric potential, create additional plot with weights of points
        if (aPotential.getType() == PotentialType.NONPARAM) {
            PotentialNoParam noParam = (PotentialNoParam) aPotential;
            double[] dp = noParam.getSupportPoints();
            final Plot plotWeight = new Plot("Estimated Nonparam weights for best fitness:", "Support", "Weight");
            mmm = StatisticsUtils.getMinMaxMean(aBestPointFound);
            plotWeight.setLimits(dp[0], dp[noParam.numOfDimensions() - 1], mmm.min, mmm.max);
            plotWeight.addPoints(dp, aBestPointFound, Plot.CROSS);
            plotWeight.setColor(Color.RED);
            plotWeight.setLineWidth(2);
            plotWeight.show();
        }
    }
}
