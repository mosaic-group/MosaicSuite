package mosaic.filamentSegmentation;

import mosaic.utils.math.CubicSmoothingSpline;
import mosaic.utils.math.MFunc;
import mosaic.utils.math.Matlab;
import mosaic.utils.math.Matrix;
import mosaic.utils.nurbs.BSplineSurface;
import mosaic.utils.nurbs.BSplineSurfaceFactory;
import mosaic.utils.nurbs.Function;

public class SegmentationFunctions {

    /**
     * Calculates values of given b-spline in range (1 .. aHeight) x (1 .. aWidth) with a step given by
     * aSubPixelStep (by default value 1 should be used) and put values in a Matrix.
     * @param aWidth
     * @param aHeight
     * @param aSubPixelStep
     * @param aBSpline
     * @return
     */
    static Matrix calculateBSplinePoints(int aWidth, int aHeight, final double aSubPixelStep, final BSplineSurface aBSpline) {
        final int sizeX = (int)((aWidth - 1)/aSubPixelStep) + 1;
        final int sizeY = (int)((aHeight - 1)/aSubPixelStep) + 1;

        final MFunc func = new MFunc() {
            @Override
            public double f(double aElement, int r, int c) {
                // All values are calculated in Matlab style - that is why "+ 1" which
                // follows indexing in Matlab
                return aBSpline.getValue(c * aSubPixelStep + 1, r * aSubPixelStep + 1);
            }
        };

        return new Matrix(sizeY, sizeX).process(func);
    }

    /**
     * Generate Phi function needed for segmentation
     * @param aX
     * @param aY
     * @param aScale
     * @return
     */
    static BSplineSurface generatePhi(int aX, int aY, double aScale) {
        final double min = (aX < aY) ? aX : aY ;
        final double uMax = aX;
        final double vMax = aY;

        // In original code sample in Matlab beginning coefficients are normalized in range -2..2
        // Not as stated in paper to -1..1. This code follows Matlab implementation.
        return BSplineSurfaceFactory.generateFromFunction(1.0f, uMax, 1.0f, vMax, aScale, 3, new Function() {
            @Override
            public double getValue(double u, double v) {
                return min/4 - Math.sqrt(Math.pow(v - vMax/2, 2) + Math.pow(u - uMax/2, 2));
            }
        }).normalizeCoefficients(2.0);
    }

    /**
     * Generate Psi function needed for segmentation
     * @param aX
     * @param aY
     * @param aScale
     * @return
     */
    static BSplineSurface generatePsi(int aX, int aY, double aScale) {
        final double min = (aX < aY) ? aX : aY ;
        final double uMax = aX;
        final double vMax = aY;

        // In original code sample in Matlab beginning coefficients are normalized in range -2..2
        // Not as stated in paper to -1..1. This code follows Matlab implementation.
        return BSplineSurfaceFactory.generateFromFunction(1.0f, uMax, 1.0f, vMax, aScale, 3, new Function() {
            @Override
            public double getValue(double u, double v) {
                return min/3 - Math.sqrt(Math.pow(v - vMax/3, 2) + Math.pow(u - uMax/3, 2));
            }
        }).normalizeCoefficients(2.0);
    }

    /**
     * Computes dirac(PhiValues)*heavyside(PsiValues) and normalize it in 0..1 range
     * @param aPhiValues
     * @param aPsiValues
     * @return computed mask
     */
    static Matrix generateNormalizedMask(Matrix aPhiValues, Matrix aPsiValues) {
        final Matrix mask = calculateDirac(aPhiValues).elementMult(calculateHeavySide(aPsiValues));

        // scale mask to 0..1 range
        mask.normalizeInRange0to1();

        return mask;
    }

    /**
     * Computes the regularized dirac function on each element of matrix
     * @param aM input image
     * @return
     */
    static Matrix calculateDirac(Matrix aM) {
        final double epsilon = 0.1;

        //Matlab code: y = (1/pi)*(epsilon./(x.^2 + epsilon^2));
        final Matrix result= aM.copy().pow2().add(Math.pow(epsilon, 2)).scale(Math.PI/epsilon).inv();

        return result;
    }

    /**
     * Computes the regularized heaviside function on each element of matrix
     * @param aM input image
     * @return
     */
    static Matrix calculateHeavySide(Matrix aM) {
        //Matlab code: y = 1./(1 + exp(-20*x))

        final MFunc denominatorFunc = new MFunc() {

            @Override
            public double f(double aElement, int aRow, int aCol) {
                return 1 + Math.exp(-20 * aElement);
            }
        };

        final Matrix result= aM.copy().process(denominatorFunc).inv();

        return result;
    }

    /**
     * Computes the regularized heaviside function on each element of matrix
     * @param aM input image
     * @return
     */
    static Matrix calculateDiffDirac(Matrix aM) {
        final double epsilon = 0.1;

        //Matlab code: y = (-1/pi^2)* (2*x./(x.^2 + epsilon^2).^2);
        final Matrix result= aM.copy().scale(-1*2/Math.pow(Math.PI, 2)).elementDiv( aM.copy().pow2().add(Math.pow(epsilon, 2)).pow2() );

        return result;
    }

    /**
     * Calculate regularizer energy
     * @param aM - input image
     * @param aWeights - input weights
     * @param aIsMatrixLogical - is aM a bool matrix? (0, 1 values only)
     * @return - calculated energy
     */
    static double calculateRegularizerEnergy(Matrix aM, Matrix aWeights, boolean aIsMatrixLogical) {
        final Matrix regEnergy = calculateRegularizerEnergyMatrix(aM, aWeights, aIsMatrixLogical);
        return regEnergy.sum();
    }

    /**
     * Calculate regularizer energy from matrix basing on
     * finite difference with Neumann Boundary Conditions
     *
     * @param aM - input Matrix
     * @return
     */
    static Matrix calculateRegularizerEnergyMatrix(Matrix aM, Matrix aWeights, boolean aIsMatrixLogical) {
        // Calculate forward finite difference with Neumann Boundary Conditions
        final Matrix stencilx = Matrix.mkRowVector(new double[] { -1, 1});
        Matrix gradX = Matlab.imfilterSymmetric(aM, stencilx);
        final Matrix stencily = stencilx.transpose();
        Matrix gradY = Matlab.imfilterSymmetric(aM, stencily);

        if (aIsMatrixLogical) {
            gradX = Matlab.logical(gradX, 0);
            gradY = Matlab.logical(gradY, 0);
        }
        
        // Calculate the regularizer energy
        final Matrix regEnergy = gradX.pow2().add(gradY.pow2()).sqrt().elementMult(aWeights);

        return regEnergy;
    }

    /**
     * Calcualtes filament length from given cubic smoothing spline
     * @param aCubicSmoothingSpline
     * @return length of filament
     */
    static public double calcualteFilamentLenght(final CubicSmoothingSpline aCubicSmoothingSpline) {
        FilamentXyCoordinates coordinates = generateXyCoordinatesForFilament(aCubicSmoothingSpline);
        final Matrix x = coordinates.x;
        final Matrix y = coordinates.y;
        
        double prevX = x.get(0);
        double prevY = y.get(0);
        double length = 0.0;

        for (int i = 1; i < x.size(); ++i) {
            length += Math.sqrt(Math.pow(x.get(i) - prevX,2) + Math.pow(y.get(i) - prevY,2));
            prevX = x.get(i);
            prevY = y.get(i);
        }

        return length;
    }
    
    /**
     * Internal class used to pass generated coordinates output
     */
    static public class FilamentXyCoordinates {
        public Matrix x;
        public Matrix y;
        
        protected FilamentXyCoordinates(Matrix aXvalues, Matrix aYvalues) {x = aXvalues; y = aYvalues;}
    }
    
    /**
     * Generates (x,y) coordinates from given cubic smoothing spline
     * @param css - input spline
     * @return 
     */
    static public FilamentXyCoordinates generateXyCoordinatesForFilament(final CubicSmoothingSpline css) {
        // Generate x,y coordinates for current filament
        double start = css.getKnot(0);
        double stop = css.getKnot(css.getNumberOfKNots() - 1);

        final Matrix x = Matlab.linspace(start, stop, 1000);
        Matrix y = x.copy().process(new MFunc() {
            @Override
            public double f(double aElement, int aRow, int aCol) {
                return css.getValue(x.get(aRow, aCol));
            }
        });
        
        return new FilamentXyCoordinates(x, y);
    }
    
    /**
     * Generates (x,y) coordinates from given cubic smoothing spline
     * @param css - input spline
     * @return 
     */
    static public FilamentXyCoordinates generateAdjustedXyCoordinatesForFilament(final CubicSmoothingSpline css) {
        FilamentXyCoordinates coordinates = generateXyCoordinatesForFilament(css);

        // Adjust from 1..n range (used to be compatibilt wiht matlab code) to 0..n-1 as used for 
        // images in fiji. Additionally x should point to middle of a pixel (currently segmentation 
        // can found only integer values on x axis) so additional correction by 0.5 value.
        coordinates.x.sub(1 - 0.5);
        coordinates.y.sub(1);
        
        return coordinates;
    }

}
