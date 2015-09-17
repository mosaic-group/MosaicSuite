package mosaic.utils.math;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CubicSmoothingSplineTest {

    @Test
    public void testExactInterpolationSmall() {

        // y = 2 * x^2 + 3
        double[] inputX = new double[] {-2, 0};
        double[] inputY = new double[] {-4, -4};
        double smoothingParameter = 1; // exact interpolation

        CubicSmoothingSpline s = new CubicSmoothingSpline(inputX, inputY, smoothingParameter);

        for (int i  = 0; i < inputX.length; ++i) {
            assertEquals(inputY[i], s.getValue(inputX[i]), 0.00000001);
        }
    }

    @Test
    public void testExactInterpolation() {
        // f=csaps([0, 1, 2, 4], [3, 5, 11, 35], 1)
        // y = 2 * x^2 + 3
        double[] inputX = new double[] {0, 1, 2, 4};
        double[] inputY = new double[] {3, 5, 11, 35};
        double smoothingParameter = 1; // exact interpolation

        CubicSmoothingSpline s = new CubicSmoothingSpline(inputX, inputY, smoothingParameter);

        for (int i  = 0; i < inputX.length; ++i) {
            assertEquals(inputY[i], s.getValue(inputX[i]), 0.00000001);
        }
    }

    @Test
    public void testSmoothing0point5() {
        // Expected values from Matlab
        // f=csaps([0 1 2], [3 5 11], 0.5);
        // expectedValues = fnval(f, 0:0.5:2)
        double[] expectedValues = new double[] {2.4, 4.2625, 6.2, 8.2625, 10.4};

        // y = 2 * x^2 + 3
        double[] inputX = new double[] {0, 1, 2};
        double[] inputY = new double[] {3, 5, 11};
        double smoothingParameter = 0.5; // smoothing...

        CubicSmoothingSpline s = new CubicSmoothingSpline(inputX, inputY, smoothingParameter);

        for (int i  = 0; i < expectedValues.length; ++i) {
            assertEquals(expectedValues[i], s.getValue((double)i/2), 0.00000001);
        }
    }

    @Test
    public void testSmoothing0point5_2() {
        // Expected values from Matlab
        // f=csaps([0 0.5 1 1.5 2], [-100 -30 0 30 100], 0.5)
        // expectedValues = fnval(f, 0:0.5:2)
        double[] expectedValues = new double[] {-92.131147540983605, -45.737704918032790, 0.000000000000001, 45.737704918032790, 92.131147540983605};

        // y = 2 * x^2 + 3
        double[] inputX = new double[] {0, 0.5, 1, 1.5, 2};
        double[] inputY = new double[] {-100, -30, 0, 30, 100};
        double smoothingParameter = 0.5; // smoothing...

        CubicSmoothingSpline s = new CubicSmoothingSpline(inputX, inputY, smoothingParameter);

        for (int i  = 0; i < expectedValues.length; ++i) {
            assertEquals(expectedValues[i], s.getValue((double)i/2), 0.00000001);
        }
    }

    @Test
    public void testSmoothing001() {
        // Expected values from Matlab
        // f=csaps([0 1 2], [3 5 11], 0.01);
        // expectedValues = fnval(f, 0:0.5:2)
        double[] expectedValues = new double[] {2.334080717488789, 4.332539237668161, 6.331838565022421, 8.332539237668161,10.334080717488789};

        // y = 2 * x^2 + 3
        double[] inputX = new double[] {0, 1, 2};
        double[] inputY = new double[] {3, 5, 11};
        double smoothingParameter = 0.01; // smoothing...

        CubicSmoothingSpline s = new CubicSmoothingSpline(inputX, inputY, smoothingParameter);

        for (int i  = 0; i < expectedValues.length; ++i) {
            assertEquals(expectedValues[i], s.getValue((double)i/2), 0.00000001);
        }
    }

    @Test
    public void testExtrapolation() {

        // y = 2 * x^2 + 3
        double[] inputX = new double[] {-1, 0, 1};
        double[] inputY = new double[] {1, -1, 1};
        double smoothingParameter = 1; // exact interpolation

        CubicSmoothingSpline s = new CubicSmoothingSpline(inputX, inputY, smoothingParameter);

        assertEquals(-17, s.getValue(4), 0.000001);
        assertEquals(-51, s.getValue(-5), 0.000001);
    }

    @Test
    public void testWeights() {
        double [][] expected = new double[][]
                {{-0.125,      0, 0.375, 0.75},
                { 0.125, -0.375,     0, 1.00}};

        double[] inputX = new double[] {0, 1, 2};
        double[] inputY = new double[] {0, 1.5, 0};
        double[] weights = new double[] {1, 3, 1};
        double smoothingParameter = 0.5; // exact interpolation

        CubicSmoothingSpline s = new CubicSmoothingSpline(inputX, inputY, smoothingParameter, weights);
        double[][] test = s.getCoefficients();

        assertEquals(expected.length, test.length);
        for (int i = 0; i < test.length; ++i) {
            assertArrayEquals(expected[i], test[i], 0.0001);
        }
    }

    @Test
    public void testGetters() {
        double[] inputX = new double[] {-2, -1, 0, 1};
        double[] inputY = new double[] {-33, -11, -3, 3};
        double smoothingParameter = 1; // exact interpolation

        CubicSmoothingSpline s = new CubicSmoothingSpline(inputX, inputY, smoothingParameter);

        assertArrayEquals("Knots", inputX, s.getKnots(), 0.0);
        assertEquals("Number of knots", inputX.length, s.getNumberOfKNots());
        for (int i = 0; i < inputX.length; ++i) {
            assertEquals(inputX[i], s.getKnot(i), 0.0001);
        }

        assertArrayEquals("Values", inputY, s.getValues(), 0.0);
        assertArrayEquals("Weights", new double[] {1, 1, 1, 1} /* default values */, s.getWeights(), 0.0);
    }

    @Test
    public void testToString() {
        // f = csaps([-2.0, -1.0, 0.0, 1.0], [-33.0, -11.0, -3.0, 3.0], 1)
        double[] inputX = new double[] {-2, -1, 0, 1};
        double[] inputY = new double[] {-33, -11, -3, 3};
        double smoothingParameter = 1; // exact interpolation

        CubicSmoothingSpline s = new CubicSmoothingSpline(inputX, inputY, smoothingParameter);

        String expected =
                "--------------- Cubic smoothing splines ----------------------\n" +
                        "Knots: (4)\n" +
                        "[-2.0, -1.0, 0.0, 1.0]\n" +
                        "\n" +
                        "Values: (4)\n" +
                        "[-33.0, -11.0, -3.0, 3.0]\n" +
                        "\n" +
                        "Weights: (4)\n" +
                        "[1.0, 1.0, 1.0, 1.0]\n" +
                        "\n" +
                        "Polynomials: \n" +
                        "1: f(x) =  - 3.60000*x^3 + 0.00000*x^2 + 25.6000*x^1 - 33.0000\n" +
                        "2: f(x) = 4.00000*x^3 - 10.8000*x^2 + 14.8000*x^1 - 11.0000\n" +
                        "3: f(x) =  - 0.400000*x^3 + 1.20000*x^2 + 5.20000*x^1 - 3.00000\n" +
                        "--------------------------------------------------------------\n";
        assertEquals(expected, s.toString());
    }

}
