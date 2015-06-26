package mosaic.filamentSegmentation;

import mosaic.math.MFunc;
import mosaic.math.Matlab;
import mosaic.math.Matrix;
import mosaic.nurbs.BSplineSurface;
import mosaic.nurbs.BSplineSurfaceFactory;
import mosaic.nurbs.Function;
import mosaic.plugins.utils.ImgUtils;

class SegmentationFunctions {

    // TOOD: Is this needed???
    static Matrix calculateBSplinePoints(int aWidth, int aHeight, final double aSubPixelStep, final BSplineSurface aBSpline) {
        Matrix dx = Matlab.regularySpacedVector(1, aSubPixelStep, aWidth);
        Matrix dy = Matlab.regularySpacedVector(1, aSubPixelStep, aHeight);
        
        
        MFunc phi = new MFunc() {
            @Override
            public double f(double aElement, int r, int c) {
                //System.out.println((float)((c)* aSubPixelStep + 1) + "," + (float)((r) * aSubPixelStep + 1));
                return aBSpline.getValue((float)((c)* aSubPixelStep + 1), (float)((r) * aSubPixelStep + 1));
            }
        };
        
        return new Matrix(dy.numCols(), dx.numCols()).process(phi);
    }
    
    /**
     * Generate Phi function needed for segmentation
     * @param aX
     * @param aY
     * @param aScale
     * @return
     */
    static BSplineSurface generatePhi(int aX, int aY, float aScale) {
        final float min = (aX < aY) ? aX : aY ;
        final float uMax = aX;
        final float vMax = aY;
            
        return BSplineSurfaceFactory.generateFromFunction(1.0f, uMax, 1.0f, vMax, aScale, 3, new Function() {
                    @Override
                    public float getValue(float u, float v) {
                        return min/4 - (float) Math.sqrt(Math.pow(v - vMax/2, 2) + Math.pow(u - uMax/2, 2));
                    }
                }).normalizeCoefficients();
    }
    
    /**
     * Generate Psi function needed for segmentation
     * @param aX
     * @param aY
     * @param aScale
     * @return
     */
    static BSplineSurface generatePsi(int aX, int aY, float aScale) {
        final float min = (aX < aY) ? aX : aY ;
        final float uMax = aX;
        final float vMax = aY;
            
        return BSplineSurfaceFactory.generateFromFunction(1.0f, uMax, 1.0f, vMax, aScale, 3, new Function() {
                    @Override
                    public float getValue(float u, float v) {
                        return min/3 - (float)Math.sqrt(Math.pow(v - vMax/3, 2) + Math.pow(u - uMax/3, 2));
                    }
                }).normalizeCoefficients();
    }
    
    static Matrix generateMask(Matrix aPhiValues, Matrix aPsiValues) {
        Matrix mask = calculateDirac(aPhiValues).elementMult(calculateHeavySide(aPsiValues));
        
        // scale mask to 0..1 range
        double[][] values = mask.getArrayYX();
        ImgUtils.normalize(values);
        mask = new Matrix(values);
        
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
                return Math.exp(-20 * aElement) + 1;
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
        Matrix result= aM.copy().scale(2).elementDiv( aM.copy().pow2().add(Math.pow(epsilon, 2)).pow2() ).scale( -1/Math.pow(Math.PI, 2) );
        
        return result;
    }
    
    /**
     * Calculate regularizer energy
     * @param aM - input image
     * @return - calculated energy
     */
    static double calculateRegularizerEnergy(Matrix aM) {
        Matrix regEnergy = calculateRegularizerEnergyMatrix(aM);
        
        return regEnergy.sum();
    }

    /**
     * Calculate regularizer energy from matrix basing on 
     * finite difference with Neumann Boundary Conditions
     * 
     * @param aM - input Matrix
     * @return
     */
    static Matrix calculateRegularizerEnergyMatrix(Matrix aM) {
        // Calculate forward finite difference with Neumann Boundary Conditions
        Matrix stencilx = Matrix.mkRowVector(new double[] {0, -1, 1});
        Matrix stencily = stencilx.copy().transpose();
        Matrix gradX = Matlab.imfilterSymmetric(aM, stencilx);
        Matrix gradY = Matlab.imfilterSymmetric(aM, stencily);
        
        // Calculate the regularizer energy
        Matrix regEnergy = gradX.pow2().add(gradY.pow2()).sqrt();
        
        return regEnergy;
    }

}
