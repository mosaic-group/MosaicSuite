package mosaic.bregman.GUI;


import mosaic.utils.math.MathOps;

/**
 * Calculations done according to to paper:
 * Gaussian approximations of fluorescence microscope point-spread function models.
 * Zhang B1, Zerubia J, Olivo-Marin JC.
 *
 */
class GaussianPSFModel {

    private final double NA, r, n;
    private final double kem;
    private final double c1, c2;

    GaussianPSFModel(double vlem, double vlex, double vna, double vr, double vn) {
        NA = vna;

        // kex and kem (same l for both)
        double lex = vlex / 1000;
        double airy_unit = 1.22 * lex / NA;
        r = vr * airy_unit;
        
        double kex = 2 * Math.PI / lex;
        c1 = kex * r * NA;
        
        double lem = vlem / 1000;
        kem = 2 * Math.PI / lem;
        c2 = kem * r * NA;

        n = vn;
    }
    
    /**
     * @return Table 2 "Paraxial WFFM" from papaer
     */
    double lateral_WFFM() {
        return Math.sqrt(2) / (kem * NA);
    }
    
    /**
     * @return Table 3 "Paraxial WFFM" from papaer
     */
    double axial_WFFM() {
        return 2 * Math.sqrt(6) * n / (kem * Math.pow(NA, 2));
    }

    /**
     * @return Table 2 "Paraxial LSCM and DSCM" from papaer
     */
    double lateral_LSCM() {
        final double num = 4 * c2 * MathOps.bessel0(c2) * MathOps.bessel1(c2) - 8 * Math.pow(MathOps.bessel1(c2), 2);
        final double den = Math.pow(r, 2) * (Math.pow(MathOps.bessel0(c2), 2) + Math.pow(MathOps.bessel1(c2), 2) - 1);

        return Math.sqrt(2) / Math.sqrt((c1 * c1 / (r * r)) + num / den);
    }
    
    /**
     * @return Table 3 "Paraxial LSCM and DSCM" from papaer
     */
    double axial_LSCM() {
        final double num = 48 * c2 * c2 * (Math.pow(MathOps.bessel0(c2), 2) + Math.pow(MathOps.bessel1(c2), 2)) - 192 * Math.pow(MathOps.bessel1(c2), 2);
        final double den = Math.pow(n, 2) * Math.pow(kem, 2) * Math.pow(r, 4) * (Math.pow(MathOps.bessel0(c2), 2) + Math.pow(MathOps.bessel1(c2), 2) - 1);

        return 2 * Math.sqrt(6) / Math.sqrt((c1 * c1 * NA * NA / (r * r * n * n)) - num / den);
    }
}
