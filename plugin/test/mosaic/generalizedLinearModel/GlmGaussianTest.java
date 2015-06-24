package mosaic.generalizedLinearModel;

import static org.junit.Assert.assertEquals;
import mosaic.math.Matrix;

import org.junit.Test;

public class GlmGaussianTest {

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
        Matrix expected = new Matrix(new double[][] {{1, 1, 1}, {1, 1, 1}});
        Matrix input    = new Matrix(new double[][] {{18, 17, 16}, {15, 14, 13}});
        
        Glm glm = new GlmGaussian();
        Matrix result = glm.linkDerivative(input);
        
        assertEquals("Output should match", expected, result);
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

}
