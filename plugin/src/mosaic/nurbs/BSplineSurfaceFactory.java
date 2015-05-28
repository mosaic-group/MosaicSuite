package mosaic.nurbs;

import net.jgeom.nurbs.util.InterpolationException;

/**
 * Class with utility functions for generating B-Spline surfaces
 * 
 * @author Krzysztof Gonciarz
 */
public class BSplineSurfaceFactory {
	
	/**
	 * Generates surface in given range in u/v directions basing on provided function
	 * @param aUmin
	 * @param aUmax
	 * @param aVmin
	 * @param aVmax
	 * @param aScale
	 * @param aDegree
	 * @param aFunc
	 * @return
	 */
	public static BSplineSurface generateFromFunction(float aUmin, float aUmax, float aVmin, float aVmax, float aScale, int aDegree, Function aFunc) {
		
		int noOfStepsU = (int)((aUmax - aUmin) / aScale);		
		int noOfStepsV = (int)((aVmax - aVmin) / aScale);
		float[][] func = new float[noOfStepsU][noOfStepsV];
		float uMax = aUmax;
		float uMin = aUmin;
		float vMax = aVmax;
		float vMin = aVmin;
		float iStepU = (uMax - uMin) / (noOfStepsU - 1);
		float iStepV = (vMax - vMin) / (noOfStepsV - 1);

		for (int u = 0; u < noOfStepsU; ++u) {
			for (int v = 0; v < noOfStepsV; ++v) {

				float uVal = u * iStepU + uMin;
				float vVal = v * iStepV + vMin;
				float val = aFunc.getValue(uVal, vVal);
				
				func[u][v] = val;
			}
		}
		
		BSplineSurface funcSurface = null;
		try {
			funcSurface = new BSplineSurface(func, uMin, uMax, vMin, vMax, aDegree, 1);
		} catch (InterpolationException e) {
			e.printStackTrace();
		}

		return funcSurface;
	}
	
}
