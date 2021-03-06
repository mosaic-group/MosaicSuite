package mosaic.filamentSegmentation;

import static org.junit.Assert.assertTrue;
import mosaic.test.framework.CommonBase;
import mosaic.utils.math.Matrix;

import org.junit.Test;

public class GaussPsfTest extends CommonBase {

    @Test
    public void testGauss3by3Matrix() {
        // Expected values generated in Matlab with 'fspecial'
        final Matrix expected = new Matrix(new double[][] {{0.0113, 0.0838, 0.0113},
                {0.0838, 0.6193, 0.0838},
                {0.0113, 0.0838, 0.0113}});

        // Tested method
        final Matrix result = GaussPsf.generate(3, 3, 0.5);

        assertTrue("Output should match", expected.compare(result, 0.001));
    }

    @Test
    public void testGaussOneValue() {
        // Expected values generated in Matlab
        final Matrix expected = new Matrix(new double[][] {{1.000}});

        // Tested method
        final Matrix result = GaussPsf.generate(1, 1, 0.5);

        assertTrue("Output should match", expected.compare(result, 0.001));
    }

    @Test
    public void testGauss1by4Matrix() {
        // Expected values generated in Matlab
        final Matrix expected = new Matrix(new double[][] {{0.134, 0.365, 0.365, 0.134}});
        // Tested method
        final Matrix result = GaussPsf.generate(1, 4, 1);

        assertTrue("Output should match", expected.compare(result, 0.001));
    }

    @Test
    public void testGauss4by1Matrix() {
        // Expected values generated in Matlab
        final Matrix expected = new Matrix(new double[][] {{0.134, 0.365, 0.365, 0.134}});
        // Tested method
        final Matrix result = GaussPsf.generate(4, 1, 1);

        assertTrue("Output should match", expected.transpose().compare(result, 0.001));
    }
}
