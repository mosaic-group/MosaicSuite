package mosaic.utils.nurbs;

import java.util.Arrays;

import javax.vecmath.Point3d;

import net.jgeom.nurbs.BasicNurbsSurface;
import net.jgeom.nurbs.ControlNet;
import net.jgeom.nurbs.ControlPoint4f;
import net.jgeom.nurbs.util.InterpolationException;

/**
 * Class responsible for creating B-spline surfaces and operating on them 
 * (like changing coefficients values).
 *
 * @author Krzysztof Gonciarz

 */
public class BSplineSurface {
	private int iDegreeInUdir;
	private int iDegreeInVdir;
	private int iOriginalU;
	private int iOriginalV;
	double uMin;
	double vMin;
	double uMax;
	double vMax;

	private Point3d[][] iPoints;

	private BasicNurbsSurface iSurface;
	private int iCoeffLenU;
	private int iCoeffLenV;
	private ControlNet iCtrlNet;

	
	/**
	 * Create B-Spline surface from provided data.
	 * @param aPoints - 2D array of values of surface
	 * @param aUmin - minimum value for 'u' direction
	 * @param aUmax - maximum value for 'u' direction
	 * @param aVmin - minimum value for 'v' direction
	 * @param aVmax - maximum value for 'v' direction
	 * @param aDegreeUdir - degree in 'u' direction
	 * @param aDegreeVdir - degree in 'v' direction
	 * @param aScaleU - scale in 'u' direction
	 * @param aScaleV - scale in 'v' direction
	 * @throws InterpolationException
	 */
	public BSplineSurface(double[][] aPoints, double aUmin, double aUmax, double aVmin, double aVmax, int aDegreeUdir, int aDegreeVdir, double aScaleU, double aScaleV) {
		iDegreeInUdir = aDegreeUdir;
		iDegreeInVdir = aDegreeVdir;
		iOriginalU = aPoints.length;
		iOriginalV = aPoints[0].length;
		uMin = aUmin;
		vMin = aVmin;
		uMax = aUmax;
		vMax = aVmax;
		
		// TODO: add some check for provided parameters or decide not to :-)

		double iOriginalStepU = (uMax - uMin) / (iOriginalU - 1);
		double iOriginalStepV = (vMax - vMin) / (iOriginalV - 1);
		
		int noOfStepsU = (int)(iOriginalU / aScaleU);		
		int noOfStepsV = (int)(iOriginalV / aScaleV);
		
		double iStepU = (uMax - uMin) / (noOfStepsU - 1);
		double iStepV = (vMax - vMin) / (noOfStepsV - 1);

		iPoints = new Point3d[noOfStepsU][noOfStepsV];
		for (int u = 0; u < noOfStepsU; ++u) {
			for (int v = 0; v < noOfStepsV; ++v) {

				double uVal = u * iStepU + uMin;
				
				int maxu=0;
				int minu=0;
				for (int w = 0 ; w < iOriginalU; ++w) {
					if (((w+1) * iOriginalStepU + uMin) > uVal) {
						minu = w;
					}
					if ((w * iOriginalStepU + uMin) >= uVal) {
						maxu = w;
						break;
					}
				}
				double shiftu = uVal - (minu * iStepU + uMin);
				
				double vVal = v * iStepV + vMin;
				int maxv = 0;
				int minv = 0;
				for (int w = 0 ; w < iOriginalV; ++w) {
					if (((w+1) * iOriginalStepV + vMin) > vVal) {
						minv = w;
					}
					if ((w * iOriginalStepV + vMin) >= vVal) {
						maxv = w;
						break;
					}
				}
				double shiftv = vVal - (minv * iStepV + vMin);
				
				double u11 = aPoints[minu][minv];
				double u12 = aPoints[minu][maxv];
				double u21 = aPoints[maxu][minv];
				double u22 = aPoints[maxu][maxv];
				// u11 u21
				// u12 u22
				double realV = (u21 * shiftu + u11 * (1-shiftu)) * (1 - shiftv) + (u22 * shiftu + u12 * (1-shiftu)) * shiftv;
				
				iPoints[u][v] = new Point3d(u * iStepU + uMin, v * iStepV + vMin, realV);
			}
		}

		generateSurface();
	}
	
	public double getValue(double u, double v) {
		
		double us =  (u - uMin)/(uMax - uMin);
		double vs =  (v - vMin)/(vMax - vMin);
		
		return iSurface.pointOnSurface(us, vs).z;
	}

	/**
	 * Normalize coefficients to be in range -1..1 and multiply by aMultiplier
	 * @param aMultiplier
	 * @return 
	 */
	public BSplineSurface normalizeCoefficients(double aMultiplier) {
		double[][] coeff = getCoefficients();
		
		// Find maximum (absolute) value of coefficient
		double max = 0;
		for (int u = 0; u < iCoeffLenU; ++u) {
			for (int v = 0; v < iCoeffLenV; ++v) {
				double val = Math.abs(coeff[u][v]);
				if (val > max) max = val;
			}
		}
	
		// Normalize all coefficient with found value
		if (max != 0) {
			for (int u = 0; u < iCoeffLenU; ++u) {
				for (int v = 0; v < iCoeffLenV; ++v) {
					coeff[u][v] = 2*coeff[u][v] / max;
				}
			}
			setCoefficients(coeff);
		}
		
		return this;
	}

	/**
	 * Get coefficients from b-spline
	 * @return 2D array [u][v]-directions
	 */
	public double[][] getCoefficients() {
		double[][] coeff = new double[iCoeffLenU][iCoeffLenV];
		
		for (int u = 0; u < iCoeffLenU; u++) {
 		    for (int v = 0; v < iCoeffLenV; v++) {
				coeff[u][v] = iCtrlNet.get(u, v).z;
			}
		}
		
		return coeff;
	}
	
	/**
	 * Set coefficients for b-spline
	 * @param aCoefficients 2D array of coefficients with [u][v] directions
	 */
	public void setCoefficients(double[][] aCoefficients) {
	    for (int u = 0; u < iCoeffLenU; u++) {
  		    for (int v = 0; v < iCoeffLenV; v++) {
				ControlPoint4f cp = iCtrlNet.get(u, v);
				cp.z =  aCoefficients[u][v];
				iCtrlNet.set(u, v, cp);
			}
		}
	}
	
	/**
	 * Debug method. It prints out information about knots and coefficients. 
	 * @return 
	 */
	public BSplineSurface showDebugInfo() { // NO_UCD (unused code)
		System.out.println("------------------------------");
		System.out.println("Degree in direction u: " + iSurface.getUDegree() + " v: " + iSurface.getVDegree());
		
		
		System.out.println("------------------------------");
		ControlNet cn = iSurface.getControlNet();
		int uLength = cn.uLength();
		int vLength = cn.vLength();
		
		System.out.println("Number coefficients in direction u:" + uLength + " v: " + vLength);
		
		double[] coefs = new double[uLength];	
		for (int v = 0; v < vLength; v++) {
			for (int u = 0; u < uLength; u++) {
					coefs[u] = cn.get(u, v).z;
			}
			System.out.println(Arrays.toString(coefs));
		}
		
		System.out.println("------------------------------");
		double[] uKnots = iSurface.getUKnots();
		double[] uk = new double[uKnots.length];
		for (int i = 0; i < uKnots.length; ++i) {
			uk[i] = uKnots[i] * (uMax - uMin) + uMin;
		}
		System.out.println("u-knots[" + uk.length + "] = " + Arrays.toString(uk));

		double[] vKnots = iSurface.getVKnots();
		double[] vk = new double[vKnots.length];
		for (int i = 0; i < vKnots.length; ++i) {
			vk[i] = vKnots[i] * (vMax - vMin) + vMin;
		}
		System.out.println("v-knots[" + vk.length + "] = " + Arrays.toString(vk));
		System.out.println("------------------------------");
		
		return this;
	}
	
	/**
	 * Debug method. It generates to standard output ready to paste matlab commands.
	 * With a scale set to values > 1 (for both: u and v direction) it generates intermediate points
	 * comparing to input data for surface. This scaling values are trimmed to step sizes.
	 * @param aScaleU - scale in direction u
	 * @param aScaleV - scale in direction v
	 * @return 
	 */
	public BSplineSurface showMatlabCode(double aScaleU, double aScaleV) { // NO_UCD (unused code)
		int noOfStepsU = (int)(iOriginalU / aScaleU);		
		int noOfStepsV = (int)(iOriginalV / aScaleV);
		
		double iStepU = (uMax - uMin) / (noOfStepsU - 1);
		double iStepV = (vMax - vMin) / (noOfStepsV - 1);
		
		String xv = "\nx = [ ";
		for (int u = 0; u < noOfStepsU; ++u) {
			xv += uMin + u * iStepU;
			xv += " ";
		}
		xv += "];";
		System.out.println(xv);

		String yv = "y = [ ";
		for (int v = 0; v < noOfStepsV; ++v) {
			yv += vMin + v * iStepV;
			yv += " ";
		}
		yv += "];";
		System.out.println(yv);

		String zv = "z = [ ";
		for (int v = 0; v < noOfStepsV; ++v) {
			for (int u = 0; u < noOfStepsU; ++u) {
				double us = uMin + u * iStepU;
				double vs = vMin + v * iStepV;
				zv += getValue(us, vs);
				zv += " ";
			}
			zv += "; ";
		}
		zv += "];";
		System.out.println(zv);

		System.out.println("surf(x,y,z);\n");
		
		return this;
	}

	private void generateSurface()  {
		try {
			iSurface = BSplineCreator.globalSurfaceInterpolation(iPoints, iDegreeInUdir, iDegreeInVdir);
		} catch (InterpolationException e) {
			iSurface = null;
			iCtrlNet = null;
			iCoeffLenU = 0;
			iCoeffLenV = 0;
			e.printStackTrace();
			return;
		}
		
		iCtrlNet = iSurface.getControlNet();
		iCoeffLenU = iCtrlNet.uLength();
		iCoeffLenV = iCtrlNet.vLength();
	}   

}
