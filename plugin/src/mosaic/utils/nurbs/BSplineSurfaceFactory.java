package mosaic.utils.nurbs;


/**
 * Class with utility functions for generating B-Spline surfaces
 *
 * @author Krzysztof Gonciarz
 */
public class BSplineSurfaceFactory {

    /**
     * Generates surface in given range in u/v directions basing on provided function and scale.
     * @param aUmin
     * @param aUmax
     * @param aVmin
     * @param aVmax
     * @param aScale
     * @param aDegree
     * @param aFunc
     * @return
     */
    public static BSplineSurface generateFromFunction(double aUmin, double aUmax, double aVmin, double aVmax, double aScale, int aDegree, Function aFunc) {
        // Calculate number of steps in each directions and create container for function values
        final int noOfStepsU = (int)((aUmax - aUmin) / aScale) + 1;
        final int noOfStepsV = (int)((aVmax - aVmin) / aScale) + 1;

        return generateFromFunction(aUmin, aUmax, aVmin, aVmax, noOfStepsU, noOfStepsV, aDegree, aDegree, aFunc);
    }

    /**
     * Generates surface in given range in u/v directions basing on provided function and numbers of steps. Degree and steps can
     * be configured independently in each direction.
     * @return B-Spline surface
     */
    static BSplineSurface generateFromFunction(double aUmin, double aUmax, double aVmin, double aVmax, int aNoOfStepsInUdir, int aNoOfStepsInVdir, int aDegreeInUdir, int aDegreeInVdir, Function aFunc) {
        final double[][] func = new double[aNoOfStepsInUdir][aNoOfStepsInVdir];

        // Calculate step value in each direction
        final double iStepU = (aUmax - aUmin) / (aNoOfStepsInUdir - 1);
        final double iStepV = (aVmax - aVmin) / (aNoOfStepsInVdir - 1);

        // Calculate values of function in for all requested points
        for (int u = 0; u < aNoOfStepsInUdir; ++u) {
            for (int v = 0; v < aNoOfStepsInVdir; ++v) {
                final double uVal = u * iStepU + aUmin;
                final double vVal = v * iStepV + aVmin;
                final double val = aFunc.getValue(uVal, vVal);

                func[u][v] = val;
            }
        }

        // Generate B-Spline surface
        final BSplineSurface funcSurface = new BSplineSurface(func, aUmin, aUmax, aVmin, aVmax, aDegreeInUdir, aDegreeInVdir, 1, 1);

        return funcSurface;
    }

}
