package mosaic.ia.gui;

import java.awt.Color;

import ij.gui.Plot;
import ij.gui.PlotWindow;
import net.sf.javaml.utils.ArrayUtils;

public class PlotQP {
    public static void plot(String aName, double[] aX, double[] aQ, double[] aObservedDistance) {
        final Plot plot = new Plot("Result: Estimated distance distributions", aName, "Probability density", aX, aObservedDistance);
        
        double max = ArrayUtils.max(aQ);
        double max2 = ArrayUtils.max(aObservedDistance);
        max = max2 > max ? max2 : max;
        plot.setLimits(aX[0], aX[aX.length - 1], 0, max);
        
        plot.setColor(Color.BLUE);
        plot.setLineWidth(2);
        plot.addLabel(.7, .2, "----  ");
    
        plot.draw();
        plot.setColor(Color.black);
        plot.addLabel(.75, .2, "Observed dist");
        plot.draw();
    
        plot.setColor(Color.red);
        plot.addPoints(aX, aQ, PlotWindow.LINE);
        plot.addLabel(.7, .3, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.draw();
        plot.addLabel(.75, .3, "q(d): Context");
    
        plot.show();
    }
}
