package mosaic.ia.gui;

import java.text.DecimalFormat;

import ij.gui.Plot;
import mosaic.ia.PotentialFunctions;

abstract public class BasePlot {
    protected Plot plot;
    
    public void show() {
        plot.draw();
        plot.show();
    }
    
    protected void addDescription(int aPotentialType, double[] aBestPointFound, double aBestFunctionValue) {
        final DecimalFormat format = new DecimalFormat("#.####E0");
        if (aPotentialType == PotentialFunctions.STEP) {
            plot.addLabel(.65, .6, "Strength: " + format.format(aBestPointFound[0]));
            plot.addLabel(.65, .7, "Threshold: " + format.format(aBestPointFound[1]));
            plot.addLabel(.65, .8, "Residual: " + format.format(aBestFunctionValue));
        }
        else if (aPotentialType == PotentialFunctions.NONPARAM) {
            plot.addLabel(.65, .6, "Residual: " + format.format(aBestFunctionValue));
        }
        else {
            plot.addLabel(.65, .6, "Strength: " + format.format(aBestPointFound[0]));
            plot.addLabel(.65, .7, "Scale: " + format.format(aBestPointFound[1]));
            plot.addLabel(.65, .8, "Residual: " + format.format(aBestFunctionValue));
        }
    }
}
