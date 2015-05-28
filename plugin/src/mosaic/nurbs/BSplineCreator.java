package mosaic.nurbs;

import javax.vecmath.GMatrix;
import javax.vecmath.GVector;
import javax.vecmath.Point3f;
import javax.vecmath.SingularMatrixException;

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
public class BSplineCreator {

	public static BasicNurbsSurface globalSurfaceInterpolation(Point3f points[][], int p, int q)
	        throws InterpolationException {
	    int n = points.length - 1;
	    int m = points[0].length - 1;
	    
	    Point3f[][] cloned = new Point3f[n+1][m+1];
	    for (int x = 0; x <=n; x++) for (int y = 0; y <= m; y++) {
	    	Point3f t = new Point3f(points[x][y]);
	    	t.z = 0;
	    	cloned[x][y] = t;
	    }
	    float uv[][] = surfaceMeshParameters(cloned, n, m); 
	    KnotVector u = averaging(uv[0], p); 
	    KnotVector v = averaging(uv[1], q); 
	
	    ControlPoint4f r[][] = new ControlPoint4f[m + 1][n + 1]; 
	    for (int l = 0; l <= m; l++) {
	        Point3f tmp[] = new Point3f[n + 1]; 
	        for (int i = 0; i <= n; i++) {
	            tmp[i] = points[i][l];
	        }   
	        try {
	            NurbsCurve curve = globalCurveInterpolation(tmp, p, u, 0); 
	            r[l] = curve.getControlPoints();
	        } catch (InterpolationException ex) {
	            for (int i = 0; i < tmp.length; i++) {
	                r[l][i] = new ControlPoint4f(tmp[i], 1); 
	            }   
	        }   
	
	    }   
	
	    ControlPoint4f cp[][] = new ControlPoint4f[n + 1][m + 1]; 
	    for (int i = 0; i <= n; i++) {
	        Point3f tmp[] = new Point3f[m + 1]; 
	        for (int j = 0; j <= m; j++) {
	            tmp[j] = r[j][i].getPoint3f();
	        }   
	        try {
	            NurbsCurve curve = globalCurveInterpolation(tmp, q, v, 1); 
	            cp[i] = curve.getControlPoints();
	        } catch (InterpolationException ex) {
	            for (int j = 0; j < tmp.length; j++) {
	                cp[i][j] = new ControlPoint4f(tmp[j], 1); 
	            }   
	        }   
	    }   
	
	    return new BasicNurbsSurface(new ControlNet(cp), u, v); 
	}

	private static float[][] surfaceMeshParameters(Point3f points[][], int n, int m) {
	    float res[][] = new float[2][];
	    int num = m + 1;
	    float cds[] = new float[(n + 1) * (m + 1)];
	
	    float uk[] = new float[n + 1];
	    uk[n] = 1;
	    for (int l = 0; l <= m; l++) {
	        float total = 0;
	        for (int k = 1; k <= n; k++) {
	            cds[k] = points[k][l].distance(points[k - 1][l]);
	            total += cds[k];
	        }
	        if (total == 0) {
	            num = num - 1;
	        } else {
	            float d = 0;
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
	    float vk[] = new float[m + 1];
	    vk[m] = 1;
	    for (int l = 0; l <= n; l++) {
	        float total = 0;
	        for (int k = 1; k <= m; k++) {
	            cds[k] = points[l][k].distance(points[l][k - 1]);
	            total += cds[k];
	        }
	        if (total == 0) {
	            num = num - 1;
	        } else {
	            float d = 0;
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

	private static KnotVector averaging(float uk[], int p) {
	    int m = uk.length + p;
	    int n = uk.length - 1;
	    float u[] = new float[m + 1];
	    for (int i = 0; i <= p; i++) {
	        u[i] = 0;
	        u[u.length - 1 - i] = 1;
	    }
	    for (int j = 1; j <= n - p; j++) {
	        float sum = 0;
	        for (int i = j; i <= j + p - 1; i++) {
	            sum += uk[i];
	        }
	        u[j + p] = sum / p;
	    }
	    return new KnotVector(u, p);
	}

	private static NurbsCurve globalCurveInterpolation(Point3f points[], int p, KnotVector kv, int axe) throws InterpolationException {
	        try {
	
	            int n = points.length - 1;
	            double A[] = new double[(n + 1) * (n + 1)];
 
	            float uk[] = new float[points.length];
	            for (int i = 0; i < points.length; i++) {
	            	if (axe == 0) uk[i] = (points[i].x - points[0].x)/(points[points.length-1].x - points[0].x);
	            	else uk[i] = (points[i].y - points[0].y)/(points[points.length-1].y - points[0].y);
	            }

	            KnotVector uKnots = kv;
	            
	            for (int i = 0; i <= n; i++) {
	                int span = uKnots.findSpan(uk[i]);
	                double tmp[] = uKnots.basisFunctions(span, uk[i]);
	                System.arraycopy(tmp, 0, A, i * (n + 1) + span - p, tmp.length);
	            }   
	            GMatrix a = new GMatrix(n + 1, n + 1, A); 
	            GVector perm = new GVector(n + 1); 
	            GMatrix lu = new GMatrix(n + 1, n + 1); 
	            a.LUD(lu, perm);
	
	            ControlPoint4f cps[] = new ControlPoint4f[n + 1]; 
	            for (int i = 0; i < cps.length; i++) {
	                cps[i] = new ControlPoint4f(0, 0, 0, 1.0f); 
	            }   
	
	            // Calculate z-ccordinate
	            GVector b = new GVector(n + 1); 
	            for (int j = 0; j <= n; j++) {
	            	b.setElement(j, points[j].z);
	            }   
	            GVector sol = new GVector(n + 1); 
	            sol.LUDBackSolve(lu, b, perm);
	            
	            for (int j = 0; j <= n; j++) {
	                cps[j].x = points[j].x;
	                cps[j].y = points[j].y;
	                cps[j].z = (float) sol.getElement(j);
	            }   
	
	            return new BasicNurbsCurve(cps, uKnots);
	        } catch (SingularMatrixException ex) {
	            throw new InterpolationException(ex);
	        }   
	
	    }  
}
