package mosaic.ia.gui;


import ij.ImagePlus;
import ij.gui.HistogramWindow;
import ij.process.FloatProcessor;


public class PlotHistogram {

    /**
     * Plots histogram for provided values using aNumOfBins
     * @param aTitle title of diagram
     * @param aValues
     * @param aNumOfBins
     * @return number of elements in each bin
     */
    public static HistogramWindow plot(String aTitle, double[] aValues, int aNumOfBins) {
        final FloatProcessor hist = new FloatProcessor(aValues.length, 1, aValues);
        return new ij.gui.HistogramWindow(aTitle, new ImagePlus(aTitle, hist), aNumOfBins);
    }
}
