package mosaic.filamentSegmentation;

import org.apache.commons.math3.special.BesselJ;

import mosaic.utils.math.Matlab;
import mosaic.utils.math.Matrix;
import mosaic.utils.math.Matrix.MFunc;

class PhaseContrastPsf {
    static Matrix generate(double aR, double aW, double aM) {
        // Comments show equivalent in Matlab code:
        // --------------------------------------------------------------------
        // r = M;
        // [xx,yy] = meshgrid(linspace(-r,r,2*M+1),linspace(-r,r,2*M+1));
        // Z = sqrt(xx.^2 + yy.^2);
        //
        final Matrix[] meshgrid = Matlab.meshgrid(Matlab.linspace(-aM, aM,(int)(2*aM+1)), Matlab.linspace(-aM,aM,(int)(2*aM+1)));
        final Matrix m1 = meshgrid[0].pow2();
        final Matrix m2 = meshgrid[1].pow2();
        final Matrix Z = m1.add(m2).sqrt();

        // --------------------------------------------------------------------
        // epsilon = 0.1;
        // Dlt = (1/pi)*(epsilon./(Z.^2 + epsilon^2));
        final double epsilon = 0.1;
        final Matrix tmp = new Matrix(Z);
        tmp.elementMult(tmp).add(epsilon * epsilon);

        final Matrix Dlt = Z.newSameSize();
        Dlt.fill(epsilon * (1/Math.PI)).elementDiv(tmp);

        // --------------------------------------------------------------------
        // Airy =       R*besselj(1,2*pi*R*Z)./Z     - (R-W)*besselj(1,2*pi*(R-W)*Z)./Z;
        final MFunc bessel = new MFunc() {
            @Override
            public double f(double aElement, int r, int c) {
                return BesselJ.value(1, aElement);
            }
        };
        final Matrix RZ = new Matrix(Z);
        RZ.scale(2 * Math.PI * aR).process(bessel).scale(aR).elementDiv(Z);

        final Matrix RZW = new Matrix(Z);
        RZW.scale(2 * Math.PI * (aR-aW)).process(bessel).scale(aR-aW).elementDiv(Z);

        final Matrix Airy = RZ.sub(RZW);

        // --------------------------------------------------------------------
        // Airy(Z==0) = R*besselj(1,2*pi*R*eps)./eps - (R-W)*besselj(1,2*pi*(R-W)*eps)./eps;

        // BesselJ of first order cannot be calculated for arguments less then zero.
        // Calculate it then for absolute value and then change sign (it match results from Matlab).
        double valForZero= aR*BesselJ.value(1, 2*Math.PI*aR*Math.ulp(1.0))/Math.ulp(1.0);
        final double RsubW = Math.abs(aR - aW);
        final double subValue = (aR-aW)*BesselJ.value(1,2*Math.PI*(RsubW)*Math.ulp(1.0))/Math.ulp(1.0);
        valForZero += (aR - aW < 0) ? subValue : -subValue;

        for (int i = 0; i < RZW.numRows(); ++i) {
            for (int j = 0; j < RZW.numCols(); ++j) {
                if (Z.get(i, j) == 0.0f) {
                    Airy.set(i, j, valForZero);
                }
            }
        }

        // --------------------------------------------------------------------
        // PSF = Dlt - Airy;
        final Matrix PSF = Dlt.sub(Airy);

        return PSF;
    }

}
