package mosaic.filamentSegmentation;

import static org.junit.Assert.*;
import mosaic.math.Matrix;

import org.junit.Test;

public class EnergyGriadientsOfBasesFunctionsTest {

    @Test
    public void testEnergySum() {

        // Tested method
        for (int i = 0; i <= 4; ++i) {
            Matrix result = EnergyGriadientsOfBasesFunctions.getEnergyGradients(i);

            assertEquals("Output sum should be equal to 2^coefficient", Math.pow(2, i), result.sum(), 0.001);
        }
    }

    /**
     * Only coefficient step with values in range 0..4 is supported.
     */
    @Test
    public void testEnergySumForNotSupportedValues() {
        Matrix expected = new Matrix(new double[][] {{0}});
        
        {
            Matrix result = EnergyGriadientsOfBasesFunctions.getEnergyGradients(-1);
            assertTrue("Output for unsupported values should be [0]", expected.compare(result, 0.0));
        }
        
        {
            Matrix result = EnergyGriadientsOfBasesFunctions.getEnergyGradients(5);
            assertTrue("Output for unsupported values should be [0]", expected.compare(result, 0.0));
    
        }
    }
}
