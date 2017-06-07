package mosaic.ia.gui;

import java.awt.Color;

import ij.gui.Plot;
import ij.gui.PlotWindow;
import mosaic.ia.Potentials.Potential;
import net.sf.javaml.utils.ArrayUtils;

/**
 * Class responsible for plotting calculated distributions
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class DistributionsPlot extends BasePlot {
    public DistributionsPlot(double[] aContextQdDistancesGrid, double[] aContextQdPdf, double[] aNearestNeighborDistancesXtoYPdf) {
        double max = Math.max(ArrayUtils.max(aContextQdPdf), ArrayUtils.max(aNearestNeighborDistancesXtoYPdf));
        plot = new Plot("Result: Estimated distance distributions", "Distance", "Probability density");
        plot.setLimits(aContextQdDistancesGrid[0], aContextQdDistancesGrid[aContextQdDistancesGrid.length - 1], 0, max);
        
        drawContextQdPdf(aContextQdDistancesGrid, aContextQdPdf);
        drawNearestNeighborDistancesXtoYPdf(aContextQdDistancesGrid, aNearestNeighborDistancesXtoYPdf);
    }
    
    public DistributionsPlot(double[] aContextQdDistancesGrid, double[] aObservedModelFitPdPdf, double[] aContextQdPdf, double[] aNearestNeighborDistancesXtoYPdf, Potential aPotential, double[] aBestPointFound, double aBestFunctionValue) {
        double max = Math.max(ArrayUtils.max(aContextQdPdf), Math.max(ArrayUtils.max(aNearestNeighborDistancesXtoYPdf), ArrayUtils.max(aObservedModelFitPdPdf)));
        plot = new Plot("Distance distributions", "Distance", "Probability density");
        plot.setLimits(aContextQdDistancesGrid[0], aContextQdDistancesGrid[aContextQdDistancesGrid.length - 1], 0, max);
        plot.setLineWidth(2);

        drawContextQdPdf(aContextQdDistancesGrid, aContextQdPdf);
        drawObservedModelFitPdPdf(aContextQdDistancesGrid, aObservedModelFitPdPdf);
        drawNearestNeighborDistancesXtoYPdf(aContextQdDistancesGrid, aNearestNeighborDistancesXtoYPdf);
        addDescription(aPotential, aBestPointFound, aBestFunctionValue);
    }

    private void drawObservedModelFitPdPdf(double[] aDistances, double[] aModelFit) {
        plot.setColor(Color.green); 
        plot.setLineWidth(2);
        plot.addPoints(aDistances, aModelFit, PlotWindow.LINE);
        plot.addLabel(.65, .4, "----  ");
        plot.setColor(Color.black);
        plot.addLabel(.7, .4, "p(d): Model fit");
    }

    private void drawContextQdPdf(double[] aDistances, double[] aProbabilityOfDistance) {
        plot.setColor(Color.red);
        plot.setLineWidth(4);
        plot.addPoints(aDistances, aProbabilityOfDistance, PlotWindow.LINE);
        plot.addLabel(.65, .3, "----  ");
        plot.setColor(Color.black);
        plot.addLabel(.7, .3, "q(d): Context");
    }

    private void drawNearestNeighborDistancesXtoYPdf(double[] aDistances, double[] aNearestNeighborDistance) {
        plot.setColor(Color.BLUE);
        plot.setLineWidth(1.5f);
        plot.addPoints(aDistances, aNearestNeighborDistance, PlotWindow.LINE);
        plot.addLabel(.65, .2, "----  ");
        plot.setColor(Color.black);
        plot.addLabel(.7, .2, "Observed dist");
    }
}
