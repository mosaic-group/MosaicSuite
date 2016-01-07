package mosaic.bregman;


import mosaic.utils.math.MathOps;


public class GaussianPSFModel {

    private final double NA, r, n;
    private final double kex, kem;
    private final double lex, lem;
    private final double c1, c2;
    private final double airy_unit;

    public GaussianPSFModel(double vlem, double vlex, double vna, double vr, double vn) {
        this.lem = vlem / 1000;
        this.lex = vlex / 1000;
        this.NA = vna;
        this.airy_unit = 1.22 * lex / NA;

        this.r = vr * airy_unit;
        this.n = vn;

        // kex and kem (same l for both)
        this.kex = 2 * Math.PI / lex;
        this.kem = 2 * Math.PI / lem;

        this.c1 = kex * r * NA;
        this.c2 = kem * r * NA;
    }

    public double lateral_WFFM() {
        double res;
        res = Math.sqrt(2) / (kem * NA);
        return res;
    }

    public double axial_WFFM() {
        double res;
        res = 2 * Math.sqrt(6) * n / (kem * NA * NA);
        return res;
    }

    public double lateral_LSCM() {
        double res;
        final double num = 4 * c2 * MathOps.bessel0(c2) * MathOps.bessel1(c2) - 8 * Math.pow(MathOps.bessel1(c2), 2);
        final double den = Math.pow(r, 2) * (Math.pow(MathOps.bessel0(c2), 2) + Math.pow(MathOps.bessel1(c2), 2) - 1);

        res = Math.sqrt(2) / Math.sqrt((c1 * c1 / (r * r)) + num / den);
        return res;
    }

    public double axial_LSCM() {
        double res;

        final double num = 48 * c2 * c2 * (Math.pow(MathOps.bessel0(c2), 2) + Math.pow(MathOps.bessel1(c2), 2)) - 192 * Math.pow(MathOps.bessel1(c2), 2);
        final double den = Math.pow(n, 2) * Math.pow(kem, 2) * Math.pow(r, 4) * (Math.pow(MathOps.bessel0(c2), 2) + Math.pow(MathOps.bessel1(c2), 2) - 1);

        res = 2 * Math.sqrt(6) / Math.sqrt((c1 * c1 * NA * NA / (r * r * n * n)) - num / den);
        return res;
    }
}
