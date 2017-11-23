package mosaic.utils.math;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import mosaic.utils.math.StatisticsUtils.MinMaxMean;


public class StatisticsUtilsTest {

    @Test
    public void testGetMinMaxMean() {
        double[] values = {1, 2, 3, 2, 2};
        MinMaxMean mmm = StatisticsUtils.getMinMaxMean(values);
        
        double epsilon = 0.000001;
        assertEquals(1, mmm.min, epsilon);
        assertEquals(3, mmm.max, epsilon);
        assertEquals(2, mmm.mean, epsilon);
    }
    
    @Test
    public void testCalculateCdf() {
        {
            double[] values = {0, 0.1, 0.5, 0.3, 0.1};
            double[] expected = {0, 0.1, 0.6, 0.9, 1.0};

            double[] cdf = StatisticsUtils.calculateCdfFromPmf(values, false);
            assertArrayEquals(expected, cdf, 0.000001);
        }
        {
            double[] values = {0, 0.2, 1.0, 0.6, 0.2};
            double[] expected = {0, 0.1, 0.6, 0.9, 1.0};

            double[] cdf = StatisticsUtils.calculateCdfFromPmf(values, true);
            assertArrayEquals(expected, cdf, 0.000001);
        }
    }
    
    @Test
    public void testNormalizeCdf() {
        {
            double[] values = {0, 0.3, 1.8, 2.7, 3.0};
            double[] expected = {0, 0.1, 0.6, 0.9, 1.0};
    
            double[] cdf = StatisticsUtils.normalizeDiscreteCdf(values, true);
            assertArrayEquals(expected, cdf, 0.000001);
        }
        {
            double[] values = {0, 0.3, 1.8, 2.7, 3.0};
            double[] expected = {0, 0.1, 0.6, 0.9, 1.0};
    
            StatisticsUtils.normalizeDiscreteCdf(values, false);
            assertArrayEquals(expected, values, 0.000001);
        }
    }
    
    @Test
    public void testNormalizePmf() {
        {
            double[] values = {0, 0.2, 1.0, 0.6, 0.2};
            double[] expected = {0, 0.1, 0.5, 0.3, 0.1};
    
            double[] pdf = StatisticsUtils.normalizePmf(values, true);
            assertArrayEquals(expected, pdf, 0.000001);
        }
        {
            double[] values = {0, 0.2, 1.0, 0.6, 0.2};
            double[] expected = {0, 0.1, 0.5, 0.3, 0.1};
    
            StatisticsUtils.normalizePmf(values, false);
            assertArrayEquals(expected, values, 0.000001);
        }
    }
    
    @Test
    public void testCalcSampleVariance() {
        double[] values = {1, 2, 2, 2, 3};
        double expectedVar = 0.5;

        double var = StatisticsUtils.calcSampleVariance(values);
        assertEquals(expectedVar, var, 0.000001);
    }
    
    @Test
    public void testCalcStandardDev() {
        double[] values = {1, 2, 2, 2, 3};
        double expectedVar = Math.sqrt(0.5);

        double var = StatisticsUtils.calcStandardDev(values);
        assertEquals(expectedVar, var, 0.000001);
    }
    
    @Test
    public void testGetPercentile() {
        {
            // Generate array with values 100..1
            int num = 100;
            double[] values = new double[num];
            for (int i = 0; i < num; ++i) values[i] = num - i;
            
            double epsilon = 0.000001;
            assertEquals(25.75, StatisticsUtils.getPercentile(values, 0.25), epsilon);
            assertEquals(50.5, StatisticsUtils.getPercentile(values, 0.5), epsilon);
            assertEquals(100, StatisticsUtils.getPercentile(values, 1), epsilon);
           
            // percentile = 0, special case: return first element (as Matlab does)
            assertEquals(1, StatisticsUtils.getPercentile(values, 0), epsilon);
            
            // getPercentile should not change (sort) original array
            assertEquals(num, values[0], epsilon);
        }
        {
            // one value array
            assertEquals(3, StatisticsUtils.getPercentile(new double[] {3}, 0.5), 0.00001);
        }
        {
            // empty array
            assertEquals(Double.NaN, StatisticsUtils.getPercentile(new double[] {}, 0.5), 0.00001);
        }
        {
            // percentile > 1
            try {
            assertEquals(Double.NaN, StatisticsUtils.getPercentile(new double[] {}, 1.01), 0.00001);
            fail("Should not reach this statement");
            }
            catch (Exception e) {
            }
        }
    }
    
    @Test
    public void testSamplePearsonCorrelationCoefficient() {
        double[] a = new double[] { 1, 2 ,3 ,4, 5};
        double[] b = new double[] { 1, 2, 3, 4, 5};
        double[] c = new double[] { 5, 4, 3, 2, 1};
        
        assertEquals(1, StatisticsUtils.samplePearsonCorrelationCoefficient(a, b), 1e-9);
        assertEquals(-1, StatisticsUtils.samplePearsonCorrelationCoefficient(a, c), 1e-9);
    }
    
    @Test
    public void testCalculateCdfAtPoint() {
        double[] pmf =  {0.3, 0.1, 0.3, 0.9};
        double[] grid = {1, 2, 3, 4};
        
        // off boundary values (w.r.t grid)
        assertEquals(1, StatisticsUtils.calculateCdfAtPoint(5, pmf, grid), 0.00001);
        assertEquals(0, StatisticsUtils.calculateCdfAtPoint(0, pmf, grid), 0.00001);
        
        // boundary
        assertEquals(1, StatisticsUtils.calculateCdfAtPoint(4, pmf, grid), 0.00001);
        assertEquals(0, StatisticsUtils.calculateCdfAtPoint(1, pmf, grid), 0.00001);
        
        // exact points
        assertEquals(0.2, StatisticsUtils.calculateCdfAtPoint(2, pmf, grid), 0.00001);
        assertEquals(0.4, StatisticsUtils.calculateCdfAtPoint(3, pmf, grid), 0.00001);        
        
        // non exact
        assertEquals(0.125, StatisticsUtils.calculateCdfAtPoint(1.5, pmf, grid), 0.00001);
        assertEquals(0.275, StatisticsUtils.calculateCdfAtPoint(2.5, pmf, grid), 0.00001);        
        assertEquals(0.625, StatisticsUtils.calculateCdfAtPoint(3.5, pmf, grid), 0.00001);
    }
    
    @Test
    public void testNormalizePdf() {
        double[] pdf =  {3, 1, 3, 9};
        double[] grid = {1, 2, 3, 4};
        
        
        // new container
        double[] expected = {0.3, 0.1, 0.3, 0.9};
        double[] pdfNormalized = StatisticsUtils.normalizePdf(pdf, grid, true);
        assertArrayEquals(expected, pdfNormalized, 0.000001);
        assertArrayEquals(new double[]{3, 1, 3, 9}, pdf, 0.000001); // check if original not changed
        
        // in place normalization
        pdfNormalized = StatisticsUtils.normalizePdf(pdf, grid, false);
        assertArrayEquals(expected, pdfNormalized, 0.000001);
        assertArrayEquals(expected, pdf, 0.000001);
    }
    
    @Test
    public void testCalcuateCdfFromPdf() {
        double[] pdf =  {3, 1, 3, 9};
        double[] grid = {1, 2, 3, 4};
        
        double[] expected = {0, 0.2, 0.4, 1};
        double[] cdf = StatisticsUtils.calculateCdfFromPdf(pdf, grid);
        assertArrayEquals(expected, cdf, 0.000001);
        
        mosaic.utils.Debug.print(StatisticsUtils.normalizePdf(pdf, grid, true));
        mosaic.utils.Debug.print(StatisticsUtils.normalizePmf(pdf, true));
        mosaic.utils.Debug.print(StatisticsUtils.calculateCdfFromPdf(pdf, grid));
        mosaic.utils.Debug.print(StatisticsUtils.calculateCdfFromPmf(pdf, true));
    }
}
