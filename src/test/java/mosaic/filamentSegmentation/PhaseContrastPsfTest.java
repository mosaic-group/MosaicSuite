package mosaic.filamentSegmentation;

import static org.junit.Assert.assertTrue;
import mosaic.test.framework.CommonBase;
import mosaic.utils.math.Matrix;

import org.junit.Test;

public class PhaseContrastPsfTest extends CommonBase {

    @Test
    public void testPcp3by3Matrix() {
        // Expected values generated in Matlab with genPahsecontrastPSF with input values R/W/M as below.
        final double R = 4;
        final double W = 2;
        final double M = 1;
        final Matrix expected = new Matrix(new double[][] {{-0.163, 0.166, -0.163}, {0.166, -34.516, 0.166}, {-0.163, 0.166, -0.163}});

        final Matrix result = PhaseContrastPsf.generate(R, W, M);

        assertTrue("Output should match", expected.compare(result, 0.001));
    }

    /*
     * This test is checking behaviour in "special" case when R is smaller than W.
     * It seems that Java's BesselJ cannot calculate values when they are < 0. It is handled in
     * code and should give proper result.
     */
    @Test
    public void testPcpSingleValue() {
        // Expected values generated in Matlab with genPahsecontrastPSF with input values R/W/M as below.
        final double R = 2;
        final double W = 9;
        final double M = 0;
        final Matrix expected = new Matrix(new double[][] {{144.55}});

        final Matrix result = PhaseContrastPsf.generate(R, W, M);

        assertTrue("Output should match", expected.compare(result, 0.01));
    }

    @Test
    public void testPcpSingleValue2() {
        // Expected values generated in Matlab with genPahsecontrastPSF with input values R/W/M as below.
        final double R = 9;
        final double W = 2;
        final double M = 0;
        final Matrix expected = new Matrix(new double[][] {{-97.348}});

        final Matrix result = PhaseContrastPsf.generate(R, W, M);

        assertTrue("Output should match", expected.compare(result, 0.01));
    }
}
