package mosaic.filamentSegmentation;

import mosaic.math.CubicSmoothingSpline;
import mosaic.math.MFunc;
import mosaic.math.Matlab;
import mosaic.math.Matrix;
import mosaic.nurbs.BSplineSurface;
import mosaic.nurbs.BSplineSurfaceFactory;
import mosaic.nurbs.Function;

public class SegmentationFunctions {


    static Matrix calculateBSplinePoints(int aWidth, int aHeight, final double aSubPixelStep, final BSplineSurface aBSpline) {
        int sizeX = (int)((aWidth - 1)/aSubPixelStep) + 1;
        int sizeY = (int)((aHeight - 1)/aSubPixelStep) + 1;
        
        MFunc func = new MFunc() {
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
            
        return BSplineSurfaceFactory.generateFromFunction(1.0f, uMax, 1.0f, vMax, aScale, 3, new Function() {
                    @Override
                    public double getValue(double u, double v) {
                        return min/3 - Math.sqrt(Math.pow(v - vMax/3, 2) + Math.pow(u - uMax/3, 2));
                    }
                }).normalizeCoefficients(2.0);
    }
    
    static Matrix generateNormalizedMask(Matrix aPhiValues, Matrix aPsiValues) {
        Matrix mask = calculateDirac(aPhiValues).elementMult(calculateHeavySide(aPsiValues));

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
        double epsilon = 0.1;
        
        //Matlab code: y = (1/pi)*(epsilon./(x.^2 + epsilon^2));        
        Matrix result= aM.copy().pow2().add(Math.pow(epsilon, 2)).scale(Math.PI/epsilon).inv();
        
        return result;
    }
    
    /**
     * Computes the regularized heaviside function on each element of matrix
     * @param aM input image
     * @return
     */
    static Matrix calculateHeavySide(Matrix aM) {
        //Matlab code: y = 1./(1 + exp(-20*x))  
        
        MFunc denominatorFunc = new MFunc() {

            @Override
            public double f(double aElement, int aRow, int aCol) {
                return 1 + Math.exp(-20 * aElement);
            }
        };
        
        Matrix result= aM.copy().process(denominatorFunc).inv();
        
        return result;
    }
    
    /**
     * Computes the regularized heaviside function on each element of matrix
     * @param aM input image
     * @return
     */
    static Matrix calculateDiffDirac(Matrix aM) { 
        double epsilon = 0.1;
        
        //Matlab code: y = (-1/pi^2)* (2*x./(x.^2 + epsilon^2).^2);   
        Matrix result= aM.copy().scale(-1*2/Math.pow(Math.PI, 2)).elementDiv( aM.copy().pow2().add(Math.pow(epsilon, 2)).pow2() );
        
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
        Matrix regEnergy = calculateRegularizerEnergyMatrix(aM, aWeights, aIsMatrixLogical);
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
        Matrix stencilx = Matrix.mkRowVector(new double[] {0, -1, 1});
        Matrix gradX = Matlab.imfilterSymmetric(aM, stencilx);
        Matrix stencily = stencilx.transpose();
        Matrix gradY = Matlab.imfilterSymmetric(aM, stencily);
        
        if (aIsMatrixLogical) {
            MFunc make1and0 = new MFunc() {
                
                @Override
                public double f(double aElement, int aRow, int aCol) {
                    if (aElement > 0) return 1;
                    return 0;
                }
            };
            
            gradX.process(make1and0);
            gradY.process(make1and0);
        }
        
        // Calculate the regularizer energy
        Matrix regEnergy = gradX.pow2().add(gradY.pow2()).sqrt().elementMult(aWeights);

        return regEnergy;
    }

    static public double calcualteFilamentLenght(final CubicSmoothingSpline css) {
        double start = css.getKnot(0);
        double stop = css.getKnot(css.getNumberOfKNots() - 1);
        
        final Matrix x  = Matlab.linspace(start, stop, 1000);
        Matrix y = x.copy().process(new MFunc() { 
            @Override
            public double f(double aElement, int aRow, int aCol) {
                return css.getValue(x.get(aRow, aCol));
            }
        });
        
        double prevX = x.get(0);
        double prevY = y.get(0);
        double length = 0.0;
        
        for (int i = 0; i < x.size(); ++i) {
            length += Math.sqrt(Math.pow(x.get(i) - prevX,2) + Math.pow(y.get(i) - prevY,2));
            prevX = x.get(i);
            prevY = y.get(i);
        }
        
        
        return length;
    }

}
