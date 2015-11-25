package mosaic.ia.utils;


import ij.ImagePlus;
import ij.gui.HistogramWindow;
import ij.process.FloatProcessor;


public class PlotUtils {

    /**
     * Plots histogram for provided values using aNumOfBins
     * @param aTitle title of diagram
     * @param aValues
     * @param aNumOfBins
     * @return number of elements in each bin
     */
    public static HistogramWindow plotHistogram(String aTitle, double[] aValues, int aNumOfBins) {
        final FloatProcessor hist = new FloatProcessor(aValues.length, 1, aValues);
        return new ij.gui.HistogramWindow(aTitle, new ImagePlus(aTitle, hist), aNumOfBins);
    }
}
