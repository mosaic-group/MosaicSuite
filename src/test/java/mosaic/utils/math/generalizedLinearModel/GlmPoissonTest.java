package mosaic.utils.math.generalizedLinearModel;

import static org.junit.Assert.assertEquals;
import mosaic.test.framework.CommonBase;
import mosaic.utils.math.Matrix;

import org.junit.Test;

public class GlmPoissonTest extends CommonBase {

    @Test
    public void testLink() {
        final Matrix expected = new Matrix(new double[][] {{18, 17}, {15, 14}, {12, 11}});
        final Matrix input    = new Matrix(new double[][] {{18, 17}, {15, 14}, {12, 11}});

        final Glm glm = new GlmPoisson();
        final Matrix result = glm.link(input);

        assertEquals("Output should match", expected, result);
    }

    @Test
    public void testLinkDerivative() {
        final double expected = 1.0;
        final Matrix input    = new Matrix(new double[][] {{18, 17, 16}, {15, 14, 13}});

        final Glm glm = new GlmPoisson();
        final double result = glm.linkDerivative(input);

        assertEquals("Output should match", expected, result, 0.001);
    }

    @Test
    public void testLinkInverse() {
        final Matrix expected = new Matrix(new double[][] {{1, 1}, {2, 3}});
        final Matrix input    = new Matrix(new double[][] {{1, 1}, {2, 3}});

        final Glm glm = new GlmPoisson();
        final Matrix result = glm.linkInverse(input);

        assertEquals("Output should match", expected, result);
    }

    @Test
    public void testPriorWeights() {
        final Matrix expected = new Matrix(5,2).ones();
        final Matrix input    = new Matrix(5,2);

        final Glm glm = new GlmPoisson();
        final Matrix result = glm.priorWeights(input);

        assertEquals("Output should match", expected, result);
    }

    @Test
    public void testVarFunction() {
        final Matrix expected = new Matrix(8,9).ones();
        final Matrix input    = new Matrix(8,9).ones();

        final Glm glm = new GlmPoisson();
        final Matrix result = glm.varFunction(input);

        assertEquals("Output should match", expected, result);
    }

    @Test
    public void testNllMean() {
        // Output from Matlab from nllMeanPoisson
        final double expected = 5.7944;

        final Matrix inputImage = new Matrix(new double[][] {{1}});
        final Matrix inputMu    = new Matrix(new double[][] {{0.5}});
        final Matrix inputWeight= new Matrix(new double[][] {{30}});

        final Glm glm = new GlmPoisson();
        final double result = glm.nllMean(inputImage, inputMu, inputWeight);

        assertEquals("Output should match", expected, result, 0.0001);
    }

    @Test
    public void testNllMean2() {
        // Output from Matlab from nllMeanPoisson
        final double expected = 3.6294;

        final Matrix inputImage = new Matrix(new double[][] {{0.2, 0.001}, {0.0005, 0}});
        final Matrix inputMu    = new Matrix(new double[][] {{0.000001, 0.9}, {0.5, - 0.0001}});
        final Matrix inputWeight= new Matrix(new double[][] {{1, 1}, {1, 1}});

        final Glm glm = new GlmPoisson();
        final double result = glm.nllMean(inputImage, inputMu, inputWeight);

        assertEquals("Output should match", expected, result, 0.0001);
    }
}
