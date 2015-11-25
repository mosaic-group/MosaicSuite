package mosaic.ia.utils;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import ij.gui.HistogramWindow;


public class PlotUtilsTest {

    @Test
    public void testPlotHistogram() {
        HistogramWindow hist = PlotUtils.plotHistogram("Hello", /* input values */ new double[] {1, 2, 10, 19}, /* num of bins */ 3);
        assertArrayEquals(new int[] {2, 1, 1}, hist.getHistogram());
        // Expected ranges are: [1, 7) [7, 13) [13, 19)
        assertArrayEquals(new double[] {1, 7, 13}, hist.getXValues(), 0.01);
    }

}
