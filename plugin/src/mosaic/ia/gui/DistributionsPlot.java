package mosaic.ia.gui;

import java.awt.Color;
import java.text.DecimalFormat;

import ij.gui.Plot;
import ij.gui.PlotWindow;
import mosaic.ia.PotentialFunctions;
import net.sf.javaml.utils.ArrayUtils;

public class DistributionsPlot {
    private final Plot plot;
    
    public DistributionsPlot(double[] aX, double[] aQ, double[] aObservedDistance) {
        double max = Math.max(ArrayUtils.max(aQ), ArrayUtils.max(aObservedDistance));
        plot = new Plot("Result: Estimated distance distributions", "Distance", "Probability density");
        plot.setLimits(aX[0], aX[aX.length - 1], 0, max);
        plot.setLineWidth(2);
        
        drawObservedDistances(aX, aObservedDistance);
        drawQ(aX, aQ);
    }
    
    public DistributionsPlot(double[] aX, double[] aModelFit, double[] aQ, double[] aObservedDistance, int potentialType, double[][] best, double[] allFitness, int bestFitnessindex) {
        double max = Math.max(ArrayUtils.max(aQ), Math.max(ArrayUtils.max(aObservedDistance), ArrayUtils.max(aModelFit)));
        plot = new Plot("Distance distributions", "Distance", "Probability density");
        plot.setLimits(aX[0], aX[aX.length - 1], 0, max);
        plot.setLineWidth(2);

        drawObservedDistances(aX, aObservedDistance);
        drawQ(aX, aQ);
        drawModelFit(aX, aModelFit);
        addDescription(potentialType, best, allFitness, bestFitnessindex);
    }

    public void show() {
        plot.draw();
        plot.show();
    }
    
    private void addDescription(int potentialType, double[][] best, double[] allFitness, int bestFitnessindex) {
        final DecimalFormat format = new DecimalFormat("#.####E0");
        if (potentialType == PotentialFunctions.STEP) {
            plot.addLabel(.65, .6, "Strength: " + format.format(best[bestFitnessindex][0]));
            plot.addLabel(.65, .7, "Threshold: " + format.format(best[bestFitnessindex][1]));
            plot.addLabel(.65, .8, "Residual: " + format.format(allFitness[bestFitnessindex]));
        }
        else if (potentialType == PotentialFunctions.NONPARAM) {
            plot.addLabel(.65, .6, "Residual: " + format.format(allFitness[bestFitnessindex]));
        }
        else {
            plot.addLabel(.65, .6, "Strength: " + format.format(best[bestFitnessindex][0]));
            plot.addLabel(.65, .7, "Scale: " + format.format(best[bestFitnessindex][1]));
            plot.addLabel(.65, .8, "Residual: " + format.format(allFitness[bestFitnessindex]));
        }
    }

    private void drawModelFit(double[] aX, double[] aModelFit) {
        plot.setColor(Color.green);
        plot.addPoints(aX, aModelFit, PlotWindow.LINE);
        plot.addLabel(.65, .4, "----  ");
        plot.setColor(Color.black);
        plot.addLabel(.7, .4, "p(d): Model fit");
    }

    private void drawQ(double[] aX, double[] aQ) {
        plot.setColor(Color.red);
        plot.addPoints(aX, aQ, PlotWindow.LINE);
        plot.addLabel(.65, .3, "----  ");
        plot.setColor(Color.black);
        plot.addLabel(.7, .3, "q(d): Context");
    }

    private void drawObservedDistances(double[] aX, double[] aObservedDistance) {
        plot.setColor(Color.BLUE);
        plot.addPoints(aX, aObservedDistance, PlotWindow.LINE);
        plot.addLabel(.65, .2, "----  ");
        plot.setColor(Color.black);
        plot.addLabel(.7, .2, "Observed dist");
    }
}
