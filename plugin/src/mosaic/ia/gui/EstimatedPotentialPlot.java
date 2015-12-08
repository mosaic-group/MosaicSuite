package mosaic.ia.gui;

import java.awt.Color;

import ij.gui.Plot;
import mosaic.ia.Potential;
import mosaic.ia.Potential.PotentialType;
import mosaic.utils.math.StatisticsUtils;
import mosaic.utils.math.StatisticsUtils.MinMaxMean;

public class EstimatedPotentialPlot extends BasePlot {
    
    public EstimatedPotentialPlot(final double[] aDistances, final double[] aPotential, PotentialType aPotentialType, double[] aBestPointFound, double aBestFunctionValue) {
        plot = new Plot("Estimated potential", "distance", "Potential value", aDistances, aPotential);
        MinMaxMean mmm = StatisticsUtils.getMinMaxMean(aPotential);
        plot.setLimits(aDistances[0] - 1, aDistances[aDistances.length - 1], mmm.min, mmm.max);
        plot.setColor(Color.BLUE);
        plot.setLineWidth(2);
        addDescription(aPotentialType, aBestPointFound, aBestFunctionValue);
        
        // In case of non parametric potencial, create additional plot
        // with weights of points
        if (aPotentialType == PotentialType.NONPARAM) {
            final Plot plotWeight = new Plot("Estimated Nonparam weights for best fitness:", "Support", "Weight");
            mmm = StatisticsUtils.getMinMaxMean(aBestPointFound);
            plotWeight.setLimits(Potential.dp[0], Potential.dp[Potential.NONPARAM_WEIGHT_SIZE - 1 - 1], mmm.min, mmm.max);
            plotWeight.addPoints(Potential.dp, aBestPointFound, Plot.CROSS);
            plotWeight.setColor(Color.RED);
            plotWeight.setLineWidth(2);
            plotWeight.show();
        }
    }
}
