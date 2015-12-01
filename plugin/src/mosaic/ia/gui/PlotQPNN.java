package mosaic.ia.gui;

import java.awt.Color;
import java.text.DecimalFormat;

import ij.gui.Plot;
import ij.gui.PlotWindow;
import mosaic.ia.PotentialFunctions;

public class PlotQPNN {
    public static void plot(String aName, double[] d, double[] p, double[] q, double[] nn, int potentialType, double[][] best, double[] allFitness, int bestFitnessindex) {
        final Plot plot = new Plot("Distance distributions", aName, "Probability density", d, nn);
    
        double max = 0;
        for (int i = 0; i < q.length; i++) {
            if (q[i] > max) {
                max = q[i];
            }
        }
        for (int i = 0; i < nn.length; i++) {
            if (nn[i] > max) {
                max = nn[i];
            }
        }
        for (int i = 0; i < p.length; i++) {
            if (p[i] > max) {
                max = p[i];
            }
        }
        plot.setLimits(d[0], d[d.length - 1], 0, max);
        plot.setColor(Color.BLUE);
        plot.setLineWidth(2);
        plot.addLabel(.65, .2, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.addLabel(.7, .2, "Observed dist");
        plot.draw();
    
        plot.setColor(Color.red);
        plot.addPoints(d, q, PlotWindow.LINE);
        plot.addLabel(.65, .3, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.draw();
        plot.addLabel(.7, .3, "q(d): Context");
    
        plot.setColor(Color.green);
        plot.addPoints(d, p, PlotWindow.LINE);
        plot.addLabel(.65, .4, "----  ");
        plot.setColor(Color.black);
        plot.draw();
        plot.addLabel(.7, .4, "p(d): Model fit");
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
    
        plot.show();
    }
}
