package mosaic.filamentSegmentation;

import static org.junit.Assert.*;
import mosaic.math.Matlab;
import mosaic.math.Matrix;
import mosaic.nurbs.BSplineSurface;
import mosaic.nurbs.BSplineSurfaceFactory;
import mosaic.nurbs.Function;

import org.junit.Test;

public class SegmentationFunctionsTest {

    @Test
    public void testCalculateRegularizerEnergy() {
        double expected = 20.649;
        Matrix input = new Matrix(new double[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
        
        // Tested function
        double result = SegmentationFunctions.calculateRegularizerEnergy(input);
        
        assertEquals("Regularizer energy", expected, result, 0.001);
    }
    
    @Test
    public void testCalculateDirac() {
        // Expected matrix is taken from Matlab.
        Matrix expected = new Matrix(new double [][] {{1.592, 0.637, 0.318},{0.187, 0.122, 0.086},{0.064, 0.049, 0.039}});
        
        Matrix input = new Matrix(new double[][] {{0.1, 0.2, 0.3}, {0.4, 0.5, 0.6}, {0.7, 0.8, 0.9}});
        
        // Tested function
        Matrix result = SegmentationFunctions.calculateDirac(input);
        
        assertTrue("Output should match matlab: ", expected.compare(result, 0.001));
    }

    @Test
    public void testCalculateHeavySide() {
        // Expected matrix is taken from Matlab.
        Matrix expected = new Matrix(new double [][] {{2.0612e-09, 4.5398e-05, 0.017986},
                                                      {0.45017, 0.5, 0.54983},
                                                      {0.98201, 0.99995, 1}});
        
        Matrix input = new Matrix(new double[][] {{-1, -0.5, -0.2}, {-0.01, 0.0, 0.01}, {0.2, 0.5, 1}});
        
        // Tested function
        Matrix result = SegmentationFunctions.calculateHeavySide(input);

        assertTrue("Output should match matlab: ", expected.compare(result, 0.001));
    }
    
    @Test
    public void testGenerateMask() {
        // Expected matrix is taken from Matlab.
        Matrix expected = new Matrix(new double [][] {{1, 0.230, 0.086},
                                                      {0.036, 0.012, 0}});
        
        Matrix inputPhi = new Matrix(new double[][] {{1, 2, 3}, {4, 5, 6}});
        Matrix inputPsi = new Matrix(new double[][] {{7, 8, 9}, {10, 11, 12}});
        
        // Tested function
        Matrix result = SegmentationFunctions.generateMask(inputPhi, inputPsi);
 
        assertTrue("Output should match matlab: ", expected.compare(result, 0.001));
    }
    
    @Test
    public void testCalculateDiffDirac() {
        // Expected matrix is taken from Matlab.
        Matrix expected = new Matrix(new double [][] {{0.0, -50.661, -16.211}, {-6.079, -2.805, -1.499}});
        
        Matrix input = new Matrix(new double[][] {{0.0, 0.1, 0.2}, {0.3, 0.4, 0.5}});
        
        // Tested function
        Matrix result = SegmentationFunctions.calculateDiffDirac(input);

        assertTrue("Output should match matlab: ", expected.compare(result, 0.001));
    }

}
