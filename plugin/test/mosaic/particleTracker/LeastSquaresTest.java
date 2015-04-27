package mosaic.particleTracker;

import static org.junit.Assert.assertEquals;
import mosaic.test.framework.CommonBase;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;


/** 
 * This class is responsible for testing {@link LeastSquares} class. 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LeastSquaresTest extends CommonBase {
    private LeastSquares iLeastSquares;
   
    @Before
    public void setUp() {
        iLeastSquares = new LeastSquares();
    }
    
    /** 
     * Tests situation when it is not possible to find line ideally going through
     * given points
     */
    @Test
    public void testNotPerfectMatch() { 
        final double slope = 0.0;
        final double y0 = 1.5;
        double[] xValues = {-2.0, -1.0, 1.0, 2.0};
        double[] yValues = {2.0, 1.0, 1.0, 2.0};
        
        iLeastSquares.calculate(xValues, yValues);
        
        testExpectations(iLeastSquares, slope, y0);
    }
    
    /** 
     * Tests situation when it is possible to find line ideally going through
     * given points
     */
    @Test
    public void testPerfectMatch10Points() { 
        // y = 2.5*x - 3.2
        final double slope = 2.5;
        final double y0 = -3.2;
        final int noOfPoints = 10;
        
        double[] xValues = new double[noOfPoints];
        double[] yValues = new double[noOfPoints];
        
        for (int i = 0; i < noOfPoints; ++i) {
            double x = 1.0 * i;
            xValues[i] = x;
            yValues[i] = slope * x + y0;
        }
        
        iLeastSquares.calculate(xValues, yValues);
        
        testExpectations(iLeastSquares, slope, y0);
    }
    
    /** 
     * Tests situation when it is possible to find line ideally going through
     * given points
     */
    @Test
    public void testPerfectMatch2Points() { 
        // y = 2*x + 1
        final double slope = 2.0;
        final double y0 = 1.0;
        final double[] xValues = {1.0, 2.0};
        final double[] yValues = {3.0, 4.0};
        
        iLeastSquares.calculate(xValues, yValues);
        
        testExpectations(iLeastSquares, slope, y0);
    }
    
    /**
     * Helper method for checking calculated slopes and y-axis intercept 
     * @param aLS linear squares object containing result
     * @param aSlope expected slope
     * @param aYaxisIntercept expected y-axis intercept
     */
    private void testExpectations(LeastSquares aLS, double aSlope, double aYaxisIntercept) {
        // Set some tolerance on double numbers comparisons
        double epsilon = 0.000001;
        assertEquals("Slope should be equal", aSlope, aLS.getBeta(), epsilon);
        assertEquals("y-axis intercept should be equal", aYaxisIntercept, aLS.getAlpha(), epsilon);
    }
}
