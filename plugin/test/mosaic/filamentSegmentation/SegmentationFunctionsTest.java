package mosaic.filamentSegmentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import mosaic.test.framework.CommonBase;
import mosaic.utils.math.Matrix;
import mosaic.utils.nurbs.BSplineSurface;
import mosaic.utils.nurbs.BSplineSurfaceFactory;
import mosaic.utils.nurbs.Function;

import org.junit.Test;

public class SegmentationFunctionsTest extends CommonBase {

    @Test 
    public void testCalculateBSplinePoints() {
        Matrix expected = new Matrix (new double[][] {{ 3,  3.5,  4,  4.5,  5 },
                                                      { 4,  4.5,  5,  5.5,  6 },
                                                      { 5,  5.5,  6,  6.5,  7 },
                                                      { 6,  6.5,  7,  7.5,  8 },
                                                      { 7,  7.5,  8,  8.5,  9 },
                                                      { 8,  8.5,  9,  9.5, 10 },
                                                      { 9,  9.5, 10, 10.5, 11 }});
        
        int width = 3;
        int height = 4;

        //Input
        BSplineSurface surf = BSplineSurfaceFactory.generateFromFunction(1.0f, width, 1.0f, height, 1f, 1, new Function() {
            @Override
            public double getValue(double u, double v) {
                return u + 2*v;
            }
        });
        
        // Tested function
        Matrix result = SegmentationFunctions.calculateBSplinePoints(width, height, 0.5, surf);
        
        assertEquals("Rows", expected.numRows(), result.numRows());
        assertEquals("Cols", expected.numCols(), result.numCols());
        assertTrue("Values", expected.compare(result, 1e-15));
    }
    
    @Test 
    public void testGeneratePhi() {
        Matrix expected = new Matrix (new double[][] {
                {-1.381232735728789, -1.231828145241504, -1.093836321356054, -0.969953914547850, -0.862877575292299},
                {-1.231828145241504, -1.071975657783768, -0.923438174694800, -0.788752262282727, -0.670454486855677},
                {-1.093836321356054, -0.923438174694800, -0.762465471457578, -0.614648326852983, -0.483716856089609},
                {-0.969953914547850, -0.788752262282728, -0.614648326852983, -0.452399892358310, -0.306764742898404},
                {-0.862877575292299, -0.670454486855677, -0.483716856089609, -0.306764742898404, -0.143698207186367},
                {-0.775333335732110, -0.571804102904452, -0.373693417736683, -0.182501763571523, 0.000270376248306},
                {-0.710164754679176, -0.498951117650565, -0.289769343805950, -0.084374243470185, 0.115479373031877},
                                                      });
        
        int width = 3;
        int height = 4;

        //Input
        BSplineSurface surf = SegmentationFunctions.generatePhi(10, 10, 1);

        // Tested function
        Matrix result = SegmentationFunctions.calculateBSplinePoints(width, height, 0.5, surf);

        assertEquals("Rows", expected.numRows(), result.numRows());
        assertEquals("Cols", expected.numCols(), result.numCols());
        assertTrue("Values", expected.compare(result, 1e-15));
    }
    
    @Test 
    public void testGeneratePsi() {
        Matrix expected = new Matrix (new double[][] {
                {0.010993608881435, 0.119977324986745, 0.211957285633731, 0.280801315938471, 0.320377241017041},
                {0.119977324986744, 0.248436112262687, 0.346512497336141, 0.414846267954408, 0.454077211864784},
                {0.211957285633731, 0.346512497336141, 0.475069057084843, 0.579789422786368, 0.642836052347247},
                {0.280801315938471, 0.414846267954408, 0.579789422786368, 0.731302521017865, 0.825057303232409},
                {0.320377241017041, 0.454077211864784, 0.642836052347247, 0.825057303232409, 0.939144505288252},
                {0.325842308905545, 0.463663101978931, 0.650383892739107, 0.828395994123403, 0.940090719069144},
                {0.297511459320211, 0.438333651865946, 0.604657847193966, 0.755342754823809, 0.849247084275010}
                                                      });
        
        int width = 3;
        int height = 4;

        //Input
        BSplineSurface surf = SegmentationFunctions.generatePsi(10, 10, 1);

        // Tested function
        Matrix result = SegmentationFunctions.calculateBSplinePoints(width, height, 0.5, surf);

        assertEquals("Rows", expected.numRows(), result.numRows());
        assertEquals("Cols", expected.numCols(), result.numCols());
        assertTrue("Values", expected.compare(result, 1e-15));
    }
    
    @Test
    public void testCalculateRegularizerEnergy() {
        double expected = 20.649110640673520;
        Matrix input = new Matrix(new double[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
        
        // Tested function
        double result = SegmentationFunctions.calculateRegularizerEnergy(input, input.copy().ones(), false);
        
        assertEquals("Regularizer energy", expected, result, 1e-15);
    }
    
    @Test
    public void testCalculateDirac() {
        // Expected matrix is taken from Matlab.
        Matrix expected = new Matrix(new double [][] {
                                        {1.591549430918953, 0.636619772367581, 0.318309886183791},
                                        {0.187241109519877, 0.122426879301458, 0.086029698968592},
                                        {0.063661977236758, 0.048970751720583, 0.038818278802901},
                                    });
        
        Matrix input = new Matrix(new double[][] {{0.1, 0.2, 0.3}, {0.4, 0.5, 0.6}, {0.7, 0.8, 0.9}});
        
        // Tested function
        Matrix result = SegmentationFunctions.calculateDirac(input);
        
        assertTrue("Output should match matlab: ", expected.compare(result, 1e-15));
    }

    @Test
    public void testCalculateHeavySide() {
        // Expected matrix is taken from Matlab.
        Matrix expected = new Matrix(new double [][] {
                                        {0.000000002061154, 0.000045397868702, 0.017986209962092},
                                        {0.450166002687522, 0.500000000000000, 0.549833997312478},
                                        {0.982013790037908, 0.999954602131298, 0.999999997938846},
                                     });
        
        Matrix input = new Matrix(new double[][] {{-1, -0.5, -0.2}, {-0.01, 0.0, 0.01}, {0.2, 0.5, 1}});
        
        // Tested function
        Matrix result = SegmentationFunctions.calculateHeavySide(input);

        assertTrue("Output should match matlab: ", expected.compare(result, 1e-15));
    }
    
    @Test
    public void testGenerateMask() {
        // Expected matrix is taken from Matlab.
        Matrix expected = new Matrix(new double [][] {{1, 0.230, 0.086},
                                                      {0.036, 0.012, 0}});
        
        Matrix inputPhi = new Matrix(new double[][] {{1, 2, 3}, {4, 5, 6}});
        Matrix inputPsi = new Matrix(new double[][] {{7, 8, 9}, {10, 11, 12}});
        
        // Tested function
        Matrix result = SegmentationFunctions.generateNormalizedMask(inputPhi, inputPsi);
        
        assertTrue("Output should match matlab: ", expected.compare(result, 0.001));
    }
    
    @Test
    public void testGenerateMask2() {
        // Expected matrix is taken from Matlab.
        Matrix expected = new Matrix(new double [][] {
                                        {0.490196084992651, 0, 1.000000000000000},
                                        {0.490196086043435, 0.495293440509157, 0.468137282596113},
                                    });
        
        Matrix inputPhi = new Matrix(new double[][] {{0.1, -0.5, 0}, {0, 0.0001, 0.02}});
        Matrix inputPsi = new Matrix(new double[][] {{1, 0, 0.9}, {0, 0.001, -0.0005}});
        
        // Tested function
        Matrix result = SegmentationFunctions.generateNormalizedMask(inputPhi, inputPsi);
        
        assertTrue("Output should match matlab: ", expected.compare(result, 1e-15));
    }
    
    @Test
    public void testCalculateDiffDirac() {
        // Expected matrix is taken from Matlab.
        Matrix expected = new Matrix(new double [][] {
                                            {0, -50.660591821168865, -16.211389382774037},
                                            {-6.079271018540265, -2.804738647538761, -1.498834077549375},
                                            {50.660591821168865, 2.026018448896791, 0.020264236323183}
                                     });
        
        Matrix input = new Matrix(new double[][] {{0.0, 0.1, 0.2}, {0.3, 0.4, 0.5}, {-0.1, -0.001, -0.00001}});
        
        // Tested function
        Matrix result = SegmentationFunctions.calculateDiffDirac(input);

        assertTrue("Output should match matlab: ", expected.compare(result, 1e-15));
    }

}
