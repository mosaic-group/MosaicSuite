package mosaic.filamentSegmentation.GUI;


import java.awt.Color;
import java.util.List;
import java.util.Map;

import ij.gui.Plot;
import ij.gui.PlotWindow;
import mosaic.filamentSegmentation.SegmentationFunctions;
import mosaic.filamentSegmentation.SegmentationFunctions.FilamentXyCoordinates;
import mosaic.utils.math.CubicSmoothingSpline;


/**
 * Class responsible for createing plot for all provided filaments
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class PlotDialog {
    final private String iTitle;
    final private int iXrange;
    final private int iYrange;
    Plot iPlot = null;
    
    /**
     * @param aTitle - Title of a plot window
     * @param aXrange - 0..aXrange for x-axis
     * @param aYrange - 0..aYrange for y-axis
     */
    public PlotDialog(String aTitle, int aXrange, int aYrange) {
        iTitle = aTitle;
        iXrange = aXrange;
        iYrange = aYrange;
    }
    
    /**
     * Creates plot with all splines provided in input data
     * @param aFilamentsData input data structure 
     * {@link mosaic.plugins.FilamentSegmentation#iFilamentsData check here for details of aFilamentsData structure}
     * @return
     */
    public PlotDialog createPlotWithAllCalculetedSplines(final Map<Integer, Map<Integer, List<CubicSmoothingSpline>>> aFilamentsData) {
        PlotWindow.noGridLines = false; // draw grid lines
        final Plot plot = new Plot(iTitle, "X", "Y");
        plot.setLimits(0, iXrange, iYrange, 0);
        
        // Plot data
        plot.setColor(Color.blue);
        for (final Map<Integer, List<CubicSmoothingSpline>> ms : aFilamentsData.values()) {
            for (final List<CubicSmoothingSpline> ps : ms.values()) {
                int count = 0;
                for (final CubicSmoothingSpline css : ps) {
                    // Mix colors - it is good for spotting changes for single filament
                    switch(count) {
                        case 0: plot.setColor(Color.BLUE);break;
                        case 1: plot.setColor(Color.RED);break;
                        case 2: plot.setColor(Color.GREEN);break;
                        case 3: plot.setColor(Color.BLACK);break;
                        case 4: plot.setColor(Color.CYAN);break;
                        default:plot.setColor(Color.MAGENTA);break;
                    }
                    count = (++count % 5); // Keeps all values in 0-5 range
                    
                    // Put stuff on plot
                    FilamentXyCoordinates coordinates = SegmentationFunctions.generateAdjustedXyCoordinatesForFilament(css);
                    plot.addPoints(coordinates.x.getData(), coordinates.y.getData(), PlotWindow.LINE);
                }
            }
        }

        iPlot = plot;
        
        return this;
    }
    
    /**
     * Shows created earlier plot. If it is not created shows nothing.
     */
    public void show() {
        if (iPlot != null) iPlot.show();
    }
    
}
