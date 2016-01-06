package mosaic.psf2d;


/**
 * <p>
 * Copyright � 1999 CERN - European Organization for Nuclear Research. Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose is hereby granted without fee, provided that the above copyright notice appear in all copies and that both that copyright notice and this permission notice appear in supporting documentation. CERN makes no representations
 * about the suitability of this software for any purpose. It is provided "as is" without expressed or implied warranty.
 * <p>
 * Bessel and Airy functions.
 */
public class PsfBessel {
    /**
     * Returns the Bessel function of the first kind of order 0 of the argument.
     * @param x the value to compute the bessel function of.
     */
    static public double j0(double x) throws ArithmeticException {
        double ax;

        if ((ax = Math.abs(x)) < 8.0) {
            final double y = x * x;
            final double ans1 = 57568490574.0 + y * (-13362590354.0 + y * (651619640.7 + y * (-11214424.18 + y * (77392.33017 + y * (-184.9052456)))));
            final double ans2 = 57568490411.0 + y * (1029532985.0 + y * (9494680.718 + y * (59272.64853 + y * (267.8532712 + y * 1.0))));

            return ans1 / ans2;

        }
        else {
            final double z = 8.0 / ax;
            final double y = z * z;
            final double xx = ax - 0.785398164;
            final double ans1 = 1.0 + y * (-0.1098628627e-2 + y * (0.2734510407e-4 + y * (-0.2073370639e-5 + y * 0.2093887211e-6)));
            final double ans2 = -0.1562499995e-1 + y * (0.1430488765e-3 + y * (-0.6911147651e-5 + y * (0.7621095161e-6 - y * 0.934935152e-7)));

            return Math.sqrt(0.636619772 / ax) * (Math.cos(xx) * ans1 - z * Math.sin(xx) * ans2);
        }
    }

    /**
     * Returns the Bessel function of the first kind of order 1 of the argument.
     * @param x the value to compute the bessel function of.
     */
    static public double j1(double x) throws ArithmeticException {
        double ax;
        double y;
        double ans1, ans2;

        if ((ax = Math.abs(x)) < 8.0) {
            y = x * x;
            ans1 = x * (72362614232.0 + y * (-7895059235.0 + y * (242396853.1 + y * (-2972611.439 + y * (15704.48260 + y * (-30.16036606))))));
            ans2 = 144725228442.0 + y * (2300535178.0 + y * (18583304.74 + y * (99447.43394 + y * (376.9991397 + y * 1.0))));
            return ans1 / ans2;
        }
        else {
            final double z = 8.0 / ax;
            final double xx = ax - 2.356194491;
            y = z * z;

            ans1 = 1.0 + y * (0.183105e-2 + y * (-0.3516396496e-4 + y * (0.2457520174e-5 + y * (-0.240337019e-6))));
            ans2 = 0.04687499995 + y * (-0.2002690873e-3 + y * (0.8449199096e-5 + y * (-0.88228987e-6 + y * 0.105787412e-6)));
            double ans = Math.sqrt(0.636619772 / ax) * (Math.cos(xx) * ans1 - z * Math.sin(xx) * ans2);
            if (x < 0.0) {
                ans = -ans;
            }
            return ans;
        }
    }
}
