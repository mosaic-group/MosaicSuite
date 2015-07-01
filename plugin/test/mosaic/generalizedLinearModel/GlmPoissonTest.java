package mosaic.generalizedLinearModel;

import static org.junit.Assert.*;
import mosaic.math.Matrix;
import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class GlmPoissonTest extends CommonBase {

    @Test
    public void testLink() {
        Matrix expected = new Matrix(new double[][] {{18, 17}, {15, 14}, {12, 11}});
        Matrix input    = new Matrix(new double[][] {{18, 17}, {15, 14}, {12, 11}});
        
        Glm glm = new GlmPoisson();
        Matrix result = glm.link(input);
        
        assertEquals("Output should match", expected, result);
    }
    
    @Test
    public void testLinkDerivative() {
        double expected = 1.0;
        Matrix input    = new Matrix(new double[][] {{18, 17, 16}, {15, 14, 13}});
        
        Glm glm = new GlmPoisson();
        double result = glm.linkDerivative(input);
        
        assertEquals("Output should match", expected, result, 0.001);
    }

    @Test
    public void testLinkInverse() {
        Matrix expected = new Matrix(new double[][] {{1, 1}, {2, 3}});
        Matrix input    = new Matrix(new double[][] {{1, 1}, {2, 3}});
        
        Glm glm = new GlmPoisson();
        Matrix result = glm.linkInverse(input);
        
        assertEquals("Output should match", expected, result);
    }
    
    @Test
    public void testPriorWeights() {
        Matrix expected = new Matrix(5,2).ones();
        Matrix input    = new Matrix(5,2);
        
        Glm glm = new GlmPoisson();
        Matrix result = glm.priorWeights(input);
        
        assertEquals("Output should match", expected, result);
    }
    
    @Test
    public void testVarFunction() {
        Matrix expected = new Matrix(8,9).ones();
        Matrix input    = new Matrix(8,9).ones();
        
        Glm glm = new GlmPoisson();
        Matrix result = glm.varFunction(input);
        
        assertEquals("Output should match", expected, result);
    }
    
    @Test
    public void testNllMean() {
        // Output from Matlab from nllMeanPoisson
        double expected = 5.7944;
        
        Matrix inputImage = new Matrix(new double[][] {{1}});
        Matrix inputMu    = new Matrix(new double[][] {{0.5}});
        Matrix inputWeight= new Matrix(new double[][] {{30}});
        
        Glm glm = new GlmPoisson();
        double result = glm.nllMean(inputImage, inputMu, inputWeight);
        
        assertEquals("Output should match", expected, result, 0.0001);
    }

}
