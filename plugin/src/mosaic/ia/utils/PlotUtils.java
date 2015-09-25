package mosaic.ia.utils;


import ij.ImagePlus;
import ij.process.FloatProcessor;


public class PlotUtils {

    public static void histPlotDoubleArray_imageJ(String title, double[] array, int bins) {
        final float floatArray[][] = new float[array.length][1];
        for (int i = 0; i < array.length; i++) {
            floatArray[i][0] = (float) array[i];

        }
        final FloatProcessor hist = new FloatProcessor(floatArray);
        new ij.gui.HistogramWindow(title, new ImagePlus(title, hist), bins);
    }
}
