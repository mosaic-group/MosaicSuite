package mosaic.ia.gui;

import java.awt.Color;
import java.text.DecimalFormat;

import ij.gui.Plot;
import ij.gui.PlotWindow;
import mosaic.ia.PotentialFunctions;
import net.sf.javaml.utils.ArrayUtils;

public class DistributionsPlot {
    final Plot plot;
    
    public DistributionsPlot(String aName, double[] aX, double[] aQ, double[] aObservedDistance) {
        double max = Math.max(ArrayUtils.max(aQ), ArrayUtils.max(aObservedDistance));
        plot = new Plot("Result: Estimated distance distributions", aName, "Probability density", aX, aObservedDistance);
        plot.setLimits(aX[0], aX[aX.length - 1], 0, max);
        
        drawObservedDistances();
        drawQ(aX, aQ);
    }
    
    public DistributionsPlot(String aName, double[] aX, double[] aModelFit, double[] aQ, double[] aObservedDistance, int potentialType, double[][] best, double[] allFitness, int bestFitnessindex) {
        double max = Math.max(ArrayUtils.max(aQ), Math.max(ArrayUtils.max(aObservedDistance), ArrayUtils.max(aModelFit)));
        plot = new Plot("Distance distributions", aName, "Probability density", aX, aObservedDistance);
        plot.setLimits(aX[0], aX[aX.length - 1], 0, max);

        drawObservedDistances();
        drawQ(aX, aQ);
        drawModelFit(aX, aModelFit);
        addDescrition(potentialType, best, allFitness, bestFitnessindex);
    }

    private void addDescrition(int potentialType, double[][] best, double[] allFitness, int bestFitnessindex) {
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
        plot.draw();
        plot.addLabel(.7, .4, "p(d): Model fit");
    }

    private void drawQ(double[] aX, double[] aQ) {
        plot.setColor(Color.red);
        plot.addPoints(aX, aQ, PlotWindow.LINE);
        plot.addLabel(.65, .3, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.draw();
        plot.addLabel(.7, .3, "q(d): Context");
    }

    private void drawObservedDistances() {
        plot.setColor(Color.BLUE);
        plot.setLineWidth(2);
        plot.addLabel(.65, .2, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.addLabel(.7, .2, "Observed dist");
        plot.draw();
    }
    
    public void show() {
        plot.show();
    }
}
