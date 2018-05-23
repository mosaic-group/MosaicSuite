package mosaic.ia.gui;


import javax.swing.JOptionPane;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.HistogramWindow;
import ij.process.FloatProcessor;


public class Utils {

    /**
     * Plots histogram for provided values using aNumOfBins
     * @param aTitle title of diagram
     * @param aValues
     * @param aNumOfBins
     * @return number of elements in each bin
     */
    public static HistogramWindow plotHistogram(String aTitle, double[] aValues, int aNumOfBins) {
        final FloatProcessor hist = new FloatProcessor(aValues.length, 1, aValues);
        System.out.println("HISTOGRAM..");
        return new ij.gui.HistogramWindow(aTitle, new ImagePlus(aTitle, hist), aNumOfBins);
    }
    
    /**
     * Shows message - when IJ instance is found then IJ component is used (which is good since it is IJ window
     * and it's managed by IJ), swig component otherwise.
     * @param aTitle
     * @param aMessage
     */
    public static void messageDialog(String aTitle, String aMessage) {
        if (IJ.getInstance() != null) IJ.showMessage(aMessage);
        else JOptionPane.showMessageDialog(null, aMessage, aTitle, JOptionPane.PLAIN_MESSAGE);
    }
}

