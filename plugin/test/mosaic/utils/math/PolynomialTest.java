package mosaic.utils.math;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PolynomialTest {

    @Test
    public void testRemovingLeading0() {
        {
            final Polynomial f = new Polynomial(0.0, 0.0, 2.0, 3.0);
            assertEquals("Leading zeros", new Polynomial(2.0, 3.0), f);
        }
        {
            final Polynomial f = new Polynomial(0.0, 0.0, 0.0);
            assertEquals("Leading zeros", new Polynomial(0.0), f);
        }
        {
            final Polynomial f = new Polynomial(1.0, 0.0, 0.0);
            assertEquals("Leading zeros", new Polynomial(1.0, 0.0, 0.0), f);

        }
    }

    @Test
    public void test2ndDegree() {
        final Polynomial f = new Polynomial(1.0, 2.0, 3.0);

        assertEquals("Value", 3.0, f.getValue(0), 0.0);
        assertEquals("Value", 18.0, f.getValue(3), 0.0);

        assertEquals("Str", "f(x) = 1.00000*x^2 + 2.00000*x^1 + 3.00000", f.toString());
    }

    @Test
    public void test1stDegree() {
        final Polynomial f = new Polynomial(3.0, 2.0);

        assertEquals("Value", 5.0, f.getValue(1), 0.0);
        assertEquals("Value", 2.0, f.getValue(0), 0.0);
    }

    @Test
    public void test0Degree() {
        final Polynomial f = new Polynomial(5.0);

        assertEquals("Value", 5.0, f.getValue(1), 0.0);
        assertEquals("Value", 5.0, f.getValue(0), 0.0);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetDerivativeIllegal() {
        final Polynomial f = new Polynomial(4.0, 5.0, 3.0, -2.0);

        // Tested method
        f.getDerivative(-1);
    }

    @Test
    public void testGetCoefficient() {
        final Polynomial f = new Polynomial(4.0, 5.0, 3.0, -2.0);

        assertEquals("coeffs", 4.0, f.getCoefficient(3), 0.0);
        assertEquals("coeffs", 5.0, f.getCoefficient(2), 0.0);
        assertEquals("coeffs", 3.0, f.getCoefficient(1), 0.0);
        assertEquals("coeffs",-2.0, f.getCoefficient(0), 0.0);

        // Out of range cases
        assertEquals("coeffs", 0.0, f.getCoefficient(4), 0.0);
        assertEquals("coeffs", 0.0, f.getCoefficient(-1), 0.0);
    }

    @Test
    public void testGetCoefficients() {
        final double[] expected = new double[] {4.0, 5.0, 3.0, -2.0};

        final Polynomial f = new Polynomial(expected);

        assertArrayEquals("coeffs", expected, f.getCoefficients(), 0.0);
    }

    @Test
    public void testGetDegree() {
        final Polynomial f = new Polynomial(4.0, 5.0, 3.0, -2.0);

        assertEquals("coeffs", 3, f.getDegree());
    }

    @Test
    public void testGetDerivativeOfOrder0() {
        final Polynomial f = new Polynomial(4.0, 5.0, 3.0, -2.0);

        // Tested method
        final Polynomial df = f.getDerivative(0);

        assertEquals("0-order derivative", f, df);
    }

    @Test
    public void testGetDerivativeOfOrder1() {
        final Polynomial f = new Polynomial(4.0, 5.0, 3.0, -2.0);

        // Tested method
        final Polynomial df = f.getDerivative(1);

        assertEquals("1-order derivative", new Polynomial(12.0, 10.0, 3.0), df);
    }

    @Test
    public void testGetDerivativeOfOrder2() {
        final Polynomial f = new Polynomial(4.0, 5.0, 3.0, -2.0);

        // Tested method
        final Polynomial df = f.getDerivative(2);

        assertEquals("2-order derivative", new Polynomial(24.0, 10.0), df);
    }

    @Test
    public void testGetDerivativeOfOrder3() {
        final Polynomial f = new Polynomial(4.0, 5.0, 3.0, -2.0);

        // Tested method
        final Polynomial df = f.getDerivative(3);

        assertEquals("3-order derivative", new Polynomial(24.0), df);
    }

    @Test
    public void testGetDerivativeOfOrder4() {
        final Polynomial f = new Polynomial(4.0, 5.0, 3.0, -2.0);

        // Tested method
        final Polynomial df = f.getDerivative(4);

        assertEquals("4-order derivative", new Polynomial(0.0), df);
    }

    @Test
    public void testGetDerivativeOfOrder5() {
        final Polynomial f = new Polynomial(4.0, 5.0, 3.0, -2.0);

        // Tested method
        final Polynomial df = f.getDerivative(5);

        assertEquals("5-order derivative", new Polynomial(0.0), df);
    }

    @Test
    public void testGetIntegral() {
        final Polynomial f = new Polynomial(12.0, 0.0, -2.0, 0.0, 1.0, 4.0);

        // Tested method
        final Polynomial intf = f.getIntegral(2.0);

        assertEquals("Integral", new Polynomial(2.0, 0.0, -0.5, 0.0, 0.5, 4.0, 2.0), intf);
    }

    @Test
    public void testGetIntegralOf0() {
        final Polynomial f = new Polynomial(0.0);

        // Tested method
        final Polynomial intf = f.getIntegral(3.0);

        assertEquals("Integral", new Polynomial(3.0), intf);
    }
}
