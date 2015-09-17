package mosaic.utils.math.generalizedLinearModel;

import static org.junit.Assert.assertEquals;
import mosaic.test.framework.CommonBase;
import mosaic.utils.math.Matrix;
import mosaic.utils.math.generalizedLinearModel.Glm;
import mosaic.utils.math.generalizedLinearModel.GlmGaussian;

import org.junit.Test;

public class GlmGaussianTest extends CommonBase {

    @Test
    public void testLink() {
        Matrix expected = new Matrix(new double[][] {{18, 17}, {15, 14}, {12, 11}});
        Matrix input    = new Matrix(new double[][] {{18, 17}, {15, 14}, {12, 11}});
        
        Glm glm = new GlmGaussian();
        Matrix result = glm.link(input);
        
        assertEquals("Output should match", expected, result);
    }
    
    @Test
    public void testLinkDerivative() {
        double expected = 1.0;
        Matrix input    = new Matrix(new double[][] {{18, 17, 16}, {15, 14, 13}});
        
        Glm glm = new GlmGaussian();
        double result = glm.linkDerivative(input);
        
        assertEquals("Output should match", expected, result, 0.001);
    }

    @Test
    public void testLinkInverse() {
        Matrix expected = new Matrix(new double[][] {{1, 1}, {2, 3}});
        Matrix input    = new Matrix(new double[][] {{1, 1}, {2, 3}});
        
        Glm glm = new GlmGaussian();
        Matrix result = glm.linkInverse(input);
        
        assertEquals("Output should match", expected, result);
    }
    
    @Test
    public void testPriorWeights() {
        Matrix expected = new Matrix(5,2).ones();
        Matrix input    = new Matrix(5,2);
        
        Glm glm = new GlmGaussian();
        Matrix result = glm.priorWeights(input);
        
        assertEquals("Output should match", expected, result);
    }
    
    @Test
    public void testVarFunction() {
        Matrix expected = new Matrix(1, 10).ones();
        Matrix input    = new Matrix(1, 10);
        
        Glm glm = new GlmGaussian();
        Matrix result = glm.varFunction(input);
        
        assertEquals("Output should match", expected, result);
    }
    
    @Test
    public void testNllMean() {
        // Output from Matlab from nllMeanGauss
        double expected = 31.1;
        
        Matrix inputImage = new Matrix(new double[][] {{1, 2}, {3, 4}});
        Matrix inputMu    = new Matrix(new double[][] {{-4, 2}, {1, -3}});
        Matrix inputWeight= new Matrix(new double[][] {{0.2, 0.3}, {0.4, 0.5}});
        
        Glm glm = new GlmGaussian();
        double result = glm.nllMean(inputImage, inputMu, inputWeight);
        
        assertEquals("Output should match", expected, result, 0.00001);
    }
    
    @Test
    public void testNllMean2() {
        // Output from Matlab from nllMeanGauss
        double expected = 0.232462490802876;
        
        Matrix inputImage = new Matrix(new double[][] {{0.1111, 0.22222}, {0.33333, 0.44444}});
        Matrix inputMu    = new Matrix(new double[][] {{0.010001, 0.500009}, {0.0, 0.111}});
        Matrix inputWeight= new Matrix(new double[][] {{1, 2}, {0.5, 0.1111111111}});
        
        Glm glm = new GlmGaussian();
        double result = glm.nllMean(inputImage, inputMu, inputWeight);
        
        assertEquals("Output should match", expected, result, 2e-16);
    }

}
