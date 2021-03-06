package mosaic.ia.gui;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import ij.gui.HistogramWindow;
import mosaic.ia.gui.Utils;


public class PlotHistogramTest {

    @Test
    public void testPlotHistogram() {
        HistogramWindow hist = Utils.plotHistogram("Hello", /* input values */ new double[] {1, 2, 10, 19}, /* num of bins */ 3);
        assertArrayEquals(new int[] {2, 1, 1}, hist.getHistogram());
        // Expected ranges are: [1, 7) [7, 13) [13, 19)
        assertArrayEquals(new double[] {1, 7, 13}, hist.getXValues(), 0.01);
    }
}
