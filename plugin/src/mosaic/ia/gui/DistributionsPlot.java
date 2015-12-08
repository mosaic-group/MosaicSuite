package mosaic.ia.gui;

import java.awt.Color;

import ij.gui.Plot;
import ij.gui.PlotWindow;
import mosaic.ia.Potential.PotentialType;
import net.sf.javaml.utils.ArrayUtils;

/**
 * Class responsible for plotting calculated distributions
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class DistributionsPlot extends BasePlot {
    public DistributionsPlot(double[] aX, double[] aQ, double[] aObservedDistance) {
        double max = Math.max(ArrayUtils.max(aQ), ArrayUtils.max(aObservedDistance));
        plot = new Plot("Result: Estimated distance distributions", "Distance", "Probability density");
        plot.setLimits(aX[0], aX[aX.length - 1], 0, max);
        plot.setLineWidth(2);
        
        drawNearestNeighborDistances(aX, aObservedDistance);
        drawProbabilityOfDistance(aX, aQ);
    }
    
    public DistributionsPlot(double[] aDistances, double[] aModelFit, double[] aProbabilityOfDistance, double[] aNearestNeighborDistance, PotentialType aPotentialType, double[] aBestPointFound, double aBestFunctionValue) {
        double max = Math.max(ArrayUtils.max(aProbabilityOfDistance), Math.max(ArrayUtils.max(aNearestNeighborDistance), ArrayUtils.max(aModelFit)));
        plot = new Plot("Distance distributions", "Distance", "Probability density");
        plot.setLimits(aDistances[0], aDistances[aDistances.length - 1], 0, max);
        plot.setLineWidth(2);

        drawNearestNeighborDistances(aDistances, aNearestNeighborDistance);
        drawProbabilityOfDistance(aDistances, aProbabilityOfDistance);
        drawModelFit(aDistances, aModelFit);
        addDescription(aPotentialType, aBestPointFound, aBestFunctionValue);
    }

    private void drawModelFit(double[] aDistances, double[] aModelFit) {
        plot.setColor(Color.green);
        plot.addPoints(aDistances, aModelFit, PlotWindow.LINE);
        plot.addLabel(.65, .4, "----  ");
        plot.setColor(Color.black);
        plot.addLabel(.7, .4, "p(d): Model fit");
    }

    private void drawProbabilityOfDistance(double[] aDistances, double[] aProbabilityOfDistance) {
        plot.setColor(Color.red);
        plot.addPoints(aDistances, aProbabilityOfDistance, PlotWindow.LINE);
        plot.addLabel(.65, .3, "----  ");
        plot.setColor(Color.black);
        plot.addLabel(.7, .3, "q(d): Context");
    }

    private void drawNearestNeighborDistances(double[] aDistances, double[] aNearestNeighborDistance) {
        plot.setColor(Color.BLUE);
        plot.addPoints(aDistances, aNearestNeighborDistance, PlotWindow.LINE);
        plot.addLabel(.65, .2, "----  ");
        plot.setColor(Color.black);
        plot.addLabel(.7, .2, "Observed dist");
    }
}
