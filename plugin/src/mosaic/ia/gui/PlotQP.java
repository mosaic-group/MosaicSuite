package mosaic.ia.gui;

import java.awt.Color;

import ij.gui.Plot;
import ij.gui.PlotWindow;

public class PlotQP {
    public static void plot(String aName, double[] d, double[] q, double[] nn) {
        final Plot plot = new Plot("Result: Estimated distance distributions", aName, "Probability density", d, nn);
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
        plot.setLimits(d[0], d[d.length - 1], 0, max);
        plot.setColor(Color.BLUE);
        plot.setLineWidth(2);
        plot.addLabel(.7, .2, "----  ");
    
        plot.draw();
        plot.setColor(Color.black);
        plot.addLabel(.75, .2, "Observed dist");
        plot.draw();
    
        plot.setColor(Color.red);
        plot.addPoints(d, q, PlotWindow.LINE);
        plot.addLabel(.7, .3, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.draw();
        plot.addLabel(.75, .3, "q(d): Context");
    
        plot.show();
    }
}
