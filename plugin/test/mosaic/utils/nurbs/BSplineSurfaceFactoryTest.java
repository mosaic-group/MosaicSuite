package mosaic.utils.nurbs;

import static org.junit.Assert.*;
import mosaic.test.framework.CommonBase;
import mosaic.utils.nurbs.BSplineSurface;
import mosaic.utils.nurbs.BSplineSurfaceFactory;
import mosaic.utils.nurbs.Function;

import org.junit.Test;

/**
 * Tests BSplineSurfaceFactory class. Additionally functionality of {@link BSplineSurfaceFactory} class is tested.
 * Should not be done here but... :-)
 * 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 *
 */
public class BSplineSurfaceFactoryTest extends CommonBase {

	@Test
	public void testBSplineSurface1stDegree() {
		// Set some tolerance on float numbers comparisons
        float epsilon = 0.001f;
			// Generate flat surface increasing in u direction
			 BSplineSurface surf = BSplineSurfaceFactory.generateFromFunction(10.0f, 20.0f, 30.0f, 40.0f, 1.0f, 1, new Function() {
						@Override
						public double getValue(double u, double v) {
							return u;
						}
					});
			 // Test values at the beginning, in the middle and in the end of surface 
			 // (should be equal to u value)
			 assertEquals("Value at u = 10 shoul be 10", 10.0f, surf.getValue(10.0f, 30.0f), epsilon);
			 assertEquals("Value at u = 15 shoul be 15", 15.0f, surf.getValue(15.0f, 35.0f), epsilon);
			 assertEquals("Value at u = 20 shoul be 20", 20.0f, surf.getValue(20.0f, 40.0f), epsilon);
	}

	@Test
	public void testBSplineSurface2ndDegreeUdir() {
		// Set some tolerance on float numbers comparisons
        float epsilon = 0.001f;
        
        // Generate square function (u - dir). With u in range [-9, 9] and scale factor 6 there will be only
        // three points generated for function. Still with degree 2 it should give correct values of funciton
        // in whole range.
		BSplineSurface surf = BSplineSurfaceFactory.generateFromFunction(-9.0f, 9.0f, 0.0f, 20.0f, 6.0f, 2, new Function() {
					@Override
					public double getValue(double u, double v) {
						return u*u;
					}
				});
		 
		for (float u = -9; u <= 9f; u += 1.0) {
			assertEquals("Value at u = " + u + " should be " + u*u, u*u, surf.getValue(u, 10.0f), epsilon);
		}
	}
	
	@Test
	public void testBSplineSurface3rdDegreeVdir() {
		// Set some tolerance on float numbers comparisons
        float epsilon = 0.001f;
        
        // Generate square function (v - dir). With u in range [-10, 10] and scale factor 4 there will be only
        // five points generated for function. Still with degree 3 it should give correct values of function
        // in whole range.
		BSplineSurface surf = BSplineSurfaceFactory.generateFromFunction(-9.0f, 9.0f,-10.0f, 10.0f, 4.0f, 3, new Function() {
					@Override
					public double getValue(double u, double v) {
						return -1.5f*v*v;
					}
				});
		 
		for (float v = -10; v <= 10f; v += 1.0) {
			assertEquals("Value at u = " + v + " should be " + -1.5f*v*v, -1.5f*v*v, surf.getValue(12.0f, v), epsilon);
		}
		
	}
	
	@Test
	public void testBSplineSurface2ndDegreeVdirWithFixedNumberOfSteps() {
		// Set some tolerance on float numbers comparisons
        float epsilon = 0.001f;
        
        // Generate square function (v - dir) with 11 steps.
        BSplineSurface surf = BSplineSurfaceFactory.generateFromFunction(-9.0f, 9.0f,-10.0f, 10.0f, 11, 11, 2, 2, new Function() {
					@Override
					public double getValue(double u, double v) {
						return -1.5f*v*v;
					}
				});
		for (float v = -10; v <= 10f; v += 1.0) {
			assertEquals("Value at u = " + v + " should be " + -1.5f*v*v, -1.5f*v*v, surf.getValue(12.0f, v), epsilon);
		}
	}
	
}
