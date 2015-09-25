package mosaic.utils.math.generalizedLinearModel;

import static org.junit.Assert.assertEquals;
import mosaic.test.framework.CommonBase;
import mosaic.utils.math.Matrix;

import org.junit.Test;

public class GlmGaussianTest extends CommonBase {

    @Test
    public void testLink() {
        final Matrix expected = new Matrix(new double[][] {{18, 17}, {15, 14}, {12, 11}});
        final Matrix input    = new Matrix(new double[][] {{18, 17}, {15, 14}, {12, 11}});

        final Glm glm = new GlmGaussian();
        final Matrix result = glm.link(input);

        assertEquals("Output should match", expected, result);
    }

    @Test
    public void testLinkDerivative() {
        final double expected = 1.0;
        final Matrix input    = new Matrix(new double[][] {{18, 17, 16}, {15, 14, 13}});

        final Glm glm = new GlmGaussian();
        final double result = glm.linkDerivative(input);

        assertEquals("Output should match", expected, result, 0.001);
    }

    @Test
    public void testLinkInverse() {
        final Matrix expected = new Matrix(new double[][] {{1, 1}, {2, 3}});
        final Matrix input    = new Matrix(new double[][] {{1, 1}, {2, 3}});

        final Glm glm = new GlmGaussian();
        final Matrix result = glm.linkInverse(input);

        assertEquals("Output should match", expected, result);
    }

    @Test
    public void testPriorWeights() {
        final Matrix expected = new Matrix(5,2).ones();
        final Matrix input    = new Matrix(5,2);

        final Glm glm = new GlmGaussian();
        final Matrix result = glm.priorWeights(input);

        assertEquals("Output should match", expected, result);
    }

    @Test
    public void testVarFunction() {
        final Matrix expected = new Matrix(1, 10).ones();
        final Matrix input    = new Matrix(1, 10);

        final Glm glm = new GlmGaussian();
        final Matrix result = glm.varFunction(input);

        assertEquals("Output should match", expected, result);
    }

    @Test
    public void testNllMean() {
        // Output from Matlab from nllMeanGauss
        final double expected = 31.1;

        final Matrix inputImage = new Matrix(new double[][] {{1, 2}, {3, 4}});
        final Matrix inputMu    = new Matrix(new double[][] {{-4, 2}, {1, -3}});
        final Matrix inputWeight= new Matrix(new double[][] {{0.2, 0.3}, {0.4, 0.5}});

        final Glm glm = new GlmGaussian();
        final double result = glm.nllMean(inputImage, inputMu, inputWeight);

        assertEquals("Output should match", expected, result, 0.00001);
    }

    @Test
    public void testNllMean2() {
        // Output from Matlab from nllMeanGauss
        final double expected = 0.232462490802876;

        final Matrix inputImage = new Matrix(new double[][] {{0.1111, 0.22222}, {0.33333, 0.44444}});
        final Matrix inputMu    = new Matrix(new double[][] {{0.010001, 0.500009}, {0.0, 0.111}});
        final Matrix inputWeight= new Matrix(new double[][] {{1, 2}, {0.5, 0.1111111111}});

        final Glm glm = new GlmGaussian();
        final double result = glm.nllMean(inputImage, inputMu, inputWeight);

        assertEquals("Output should match", expected, result, 2e-16);
    }

}
