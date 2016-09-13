package mosaic.utils.nurbs;

import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.GMatrix;
import org.scijava.vecmath.GVector;
import org.scijava.vecmath.SingularMatrixException;

import net.jgeom.nurbs.BasicNurbsCurve;
import net.jgeom.nurbs.BasicNurbsSurface;
import net.jgeom.nurbs.ControlNet;
import net.jgeom.nurbs.ControlPoint4f;
import net.jgeom.nurbs.KnotVector;
import net.jgeom.nurbs.NurbsCurve;
import net.jgeom.nurbs.util.InterpolationException;

/**
 * This class contains methods that were originally part of 'jgeom' library.
 * They were modified so generated b-spline surface has knots/coefficients
 * matching values generated in Matlab by command 'spapi'
 */
class BSplineCreator {

    static BasicNurbsSurface globalSurfaceInterpolation(Point3d points[][], int p, int q) {
        final int n = points.length - 1;
        final int m = points[0].length - 1;

        final Point3d[][] cloned = new Point3d[n+1][m+1];
        for (int x = 0; x <=n; x++) {
            for (int y = 0; y <= m; y++) {
                final Point3d t = new Point3d(points[x][y]);
                t.z = 0;
                cloned[x][y] = t;
            }
        }
        final double uv[][] = surfaceMeshParameters(cloned, n, m);
        final KnotVector u = averaging(uv[0], p);
        final KnotVector v = averaging(uv[1], q);

        final ControlPoint4f r[][] = new ControlPoint4f[m + 1][n + 1];
        for (int l = 0; l <= m; l++) {
            final Point3d tmp[] = new Point3d[n + 1];
            for (int i = 0; i <= n; i++) {
                tmp[i] = points[i][l];
            }
            try {
                final NurbsCurve curve = globalCurveInterpolation(tmp, p, u, 0);
                r[l] = curve.getControlPoints();
            } catch (final InterpolationException ex) {
                for (int i = 0; i < tmp.length; i++) {
                    r[l][i] = new ControlPoint4f(tmp[i], 1);
                }
            }

        }

        final ControlPoint4f cp[][] = new ControlPoint4f[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            final Point3d tmp[] = new Point3d[m + 1];
            for (int j = 0; j <= m; j++) {
                tmp[j] = r[j][i].getPoint3d();
            }
            try {
                final NurbsCurve curve = globalCurveInterpolation(tmp, q, v, 1);
                cp[i] = curve.getControlPoints();
            } catch (final InterpolationException ex) {
                for (int j = 0; j < tmp.length; j++) {
                    cp[i][j] = new ControlPoint4f(tmp[j], 1);
                }
            }
        }

        return new BasicNurbsSurface(new ControlNet(cp), u, v);
    }

    private static double[][] surfaceMeshParameters(Point3d points[][], int n, int m) {
        final double res[][] = new double[2][];
        int num = m + 1;
        final double cds[] = new double[(n + 1) * (m + 1)];

        final double uk[] = new double[n + 1];
        uk[n] = 1;
        for (int l = 0; l <= m; l++) {
            double total = 0;
            for (int k = 1; k <= n; k++) {
                cds[k] = points[k][l].distance(points[k - 1][l]);
                total += cds[k];
            }
            if (total == 0) {
                num = num - 1;
            } else {
                double d = 0;
                for (int k = 1; k <= n; k++) {
                    d += cds[k];
                    uk[k] += d / total;
                }
            }
        }
        if (num == 0) {
            return null;
        }
        for (int k = 1; k < n; k++) {
            uk[k] /= num;
        }

        num = n + 1;
        final double vk[] = new double[m + 1];
        vk[m] = 1;
        for (int l = 0; l <= n; l++) {
            double total = 0;
            for (int k = 1; k <= m; k++) {
                cds[k] = points[l][k].distance(points[l][k - 1]);
                total += cds[k];
            }
            if (total == 0) {
                num = num - 1;
            } else {
                double d = 0;
                for (int k = 1; k <= m; k++) {
                    d += cds[k];
                    vk[k] += d / total;
                }
            }
        }
        if (num == 0) {
            return null;
        }
        for (int k = 1; k < m; k++) {
            vk[k] /= num;
        }

        res[0] = uk;
        res[1] = vk;

        return res;
    }

    private static KnotVector averaging(double uk[], int p) {
        final int m = uk.length + p;
        final int n = uk.length - 1;
        final double u[] = new double[m + 1];
        for (int i = 0; i <= p; i++) {
            u[i] = 0;
            u[u.length - 1 - i] = 1;
        }
        for (int j = 1; j <= n - p; j++) {
            double sum = 0;
            for (int i = j; i <= j + p - 1; i++) {
                sum += uk[i];
            }
            u[j + p] = sum / p;
        }
        return new KnotVector(u, p);
    }

    private static NurbsCurve globalCurveInterpolation(Point3d points[], int p, KnotVector kv, int axe) throws InterpolationException {
        try {

            final int n = points.length - 1;
            final double A[] = new double[(n + 1) * (n + 1)];

            final double uk[] = new double[points.length];
            for (int i = 0; i < points.length; i++) {
                if (axe == 0) {
                    uk[i] = (points[i].x - points[0].x)/(points[points.length-1].x - points[0].x);
                }
                else {
                    uk[i] = (points[i].y - points[0].y)/(points[points.length-1].y - points[0].y);
                }
            }

            final KnotVector uKnots = kv;

            for (int i = 0; i <= n; i++) {
                final int span = uKnots.findSpan(uk[i]);
                final double tmp[] = uKnots.basisFunctions(span, uk[i]);
                System.arraycopy(tmp, 0, A, i * (n + 1) + span - p, tmp.length);
            }
            final GMatrix a = new GMatrix(n + 1, n + 1, A);
            final GVector perm = new GVector(n + 1);
            final GMatrix lu = new GMatrix(n + 1, n + 1);
            a.LUD(lu, perm);

            final ControlPoint4f cps[] = new ControlPoint4f[n + 1];
            for (int i = 0; i < cps.length; i++) {
                cps[i] = new ControlPoint4f(0, 0, 0, 1.0f);
            }

            // Calculate z-ccordinate
            final GVector b = new GVector(n + 1);
            for (int j = 0; j <= n; j++) {
                b.setElement(j, points[j].z);
            }
            final GVector sol = new GVector(n + 1);
            sol.LUDBackSolve(lu, b, perm);

            for (int j = 0; j <= n; j++) {
                cps[j].x = points[j].x;
                cps[j].y = points[j].y;
                cps[j].z = sol.getElement(j);
            }

            return new BasicNurbsCurve(cps, uKnots);
        } catch (final SingularMatrixException ex) {
            throw new InterpolationException(ex);
        }

    }
}
