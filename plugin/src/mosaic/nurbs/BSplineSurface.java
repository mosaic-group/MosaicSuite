package mosaic.nurbs;

import java.util.Arrays;

import javax.vecmath.Point3f;

import net.jgeom.nurbs.ControlNet;
import net.jgeom.nurbs.NurbsSurface;
import net.jgeom.nurbs.util.InterpolationException;
import net.jgeom.nurbs.util.NurbsCreator;

/**
 * Class responsible for creating B-spline surfaces and operating on them (like changing coefficients values)
 * @author Krzysztof Gonciarz
 *
 */
public class BSplineSurface {
	private Point3f[][] iPoints;
	private int iDegreeInUdir;
	private int iDegreeInVdir;
	private int iStepU;
	private int iStepV;
	private int iOriginalU;
	private int iOriginalV;
	private NurbsSurface iSurface;

	public BSplineSurface(float[][] aPoints, int aDegreeInUdir, int aDegreeInVdir, int aStepU, int aStepV) throws InterpolationException {
		iDegreeInUdir = aDegreeInUdir;
		iDegreeInVdir = aDegreeInVdir;
		iStepU = aStepU;
		iStepV = aStepV;

		// TODO: add some check for provided parameters or decide not to :-)

		iOriginalU = aPoints.length;
		iOriginalV = aPoints[0].length;
		int uNewLen = (iOriginalU - 1) / iStepU + 1;
		int vNewLen = (iOriginalV - 1) / iStepV + 1;
		iPoints = new Point3f[uNewLen][vNewLen];
		for (int u = 0; u < uNewLen; ++u) {
			for (int v = 0; v < vNewLen; ++v) {
				iPoints[u][v] = new Point3f(u, v, aPoints[u * iStepU][v * iStepV]);
			}
		}

		iSurface = NurbsCreator.globalSurfaceInterpolation(iPoints, iDegreeInUdir, iDegreeInVdir);

		showMatlabCode(1, 1);
		showDebugInfo();
	}
	
	/**
	 * Debug method. It prints out information about knots and coefficients. 
	 */
	public void showDebugInfo() {
		System.out.println("------------------------------");
		System.out.println("Degree in direction u: " + iSurface.getUDegree() + " v: " + iSurface.getVDegree());
		
		
		System.out.println("------------------------------");
		ControlNet cn = iSurface.getControlNet();
		int uLength = cn.uLength();
		int vLength = cn.vLength();
		
		System.out.println("Number coefficients in direction u:" + uLength + " v: " + vLength);
		
		float[] coefs = new float[uLength];	
		for (int v = 0; v < vLength; v++) {
			for (int u = 0; u < uLength; u++) {
					coefs[u] = cn.get(u, v).z;
			}
			System.out.println(Arrays.toString(coefs));
		}
		
		System.out.println("------------------------------");
		float[] uKnots = iSurface.getUKnots();
		float[] uk = new float[uKnots.length];
		for (int i = 0; i < uKnots.length; ++i) {
			uk[i] = iSurface.pointOnSurface(uKnots[i], 0).x * iStepU;
		}
		System.out.println("u-knots[" + uk.length + "] = " + Arrays.toString(uk));

		float[] vKnots = iSurface.getVKnots();
		float[] vk = new float[vKnots.length];
		for (int i = 0; i < vKnots.length; ++i) {
			vk[i] = iSurface.pointOnSurface(0, vKnots[i]).y * iStepV;
		}
		System.out.println("v-knots[" + vk.length + "] = " + Arrays.toString(vk));
	}
	
	/**
	 * Debug method. It generates to standard output ready to paste matlab commands.
	 * With a scale set to values > 1 (for both: u and v direction) it generates intermediate points
	 * comparing to input data for surface. This scaling values are trimmed to step sizes.
	 * @param aScaleU - scale in direction u
	 * @param aScaleV - scale in direction v
	 */
	public void showMatlabCode(int aScaleU, int aScaleV) {
		if (aScaleU > iStepU) aScaleU = iStepU;
		if (aScaleV > iStepV) aScaleV = iStepV;
		
		int ul = (iOriginalU * aScaleU - 1) / iStepU + 1; 
		int vl = (iOriginalV * aScaleV - 1) / iStepV + 1; 

		String xv = "\nx = [ ";
		for (int u = 0; u < ul; ++u) {
			xv += u * iStepU / aScaleU;
			xv += " ";
		}
		xv += "];";
		System.out.println(xv);

		String yv = "y = [ ";
		for (int v = 0; v < vl; ++v) {
			yv += v * iStepV / aScaleV;
			yv += " ";
		}
		yv += "];";
		System.out.println(yv);

		String zv = "z = [ ";
		for (int v = 0; v < vl; ++v) {
			for (int u = 0; u < ul; ++u) {
				float us = (float) u / (ul - 1);
				float vs = (float) v / (vl - 1);
				zv += iSurface.pointOnSurface(us, vs).z;
				zv += " ";
			}
			zv += "; ";
		}

		zv += "];";
		System.out.println(zv);

		System.out.println("surf(x,y,z);\n");
	}
}
