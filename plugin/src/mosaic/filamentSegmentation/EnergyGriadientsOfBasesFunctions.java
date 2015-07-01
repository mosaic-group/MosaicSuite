package mosaic.filamentSegmentation;

import mosaic.math.Matrix;

/**
 * This class provides energy griadients from B-Spline coeffcients.
 * Values provided here were precomputed in Matlab and are just copy-pased here.
 * (see xx_init.m for filament segmentation Matlab code)
 * 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
class EnergyGriadientsOfBasesFunctions {

    /**
     * Returns Matrix with energy gradients
     * @param aCoefficientStep coefficient in range 0..4
     * @return
     */
    static Matrix getEnergyGradients(int aCoefficientStep) {
        Matrix result = null;
        
        switch (aCoefficientStep) {
            case 0: 
                result = getGradientsFor0();
                break;
            case 1: 
                result = getGradientsFor1();
                break;
            case 2: 
                result = getGradientsFor2();
                break;
            case 3: 
                result = getGradientsFor3();
                break;
            case 4: 
                result = getGradientsFor4();
                break;
            default:
                // Not handled case - return zero
                result = new Matrix(new double [][] {{0}});
                break;    
        }
        return result;
    }
    
    // ===============================================
    // Below are values precomputed in Matlab code
    //
    // NoOfValues = 4 * 2^coeff - 1;
    // Calculated values closed to: 
    //      fspecial('gaussian',[1,4*2^coeff-1], coeff^2*0.59933) * coeff^2
    //
    // ===============================================
    
    private static Matrix getGradientsFor0() {        
        // 3 values 
        return new Matrix(new double [][] {{0.1667, 0.6667, 0.1667}});
    }
    
    private static Matrix getGradientsFor1() {
        // 7 values
        return new Matrix(new double [][] {{0.0208, 0.1667, 0.4792, 0.6667, 0.4792,
                                            0.1667, 0.0208}});
    }
    
    private static Matrix getGradientsFor2() {
        // 15 values
        return new Matrix(new double [][] {{0.0026, 0.0208, 0.0703, 0.1667, 0.3151,
                                            0.4792, 0.612,  0.6667, 0.612,  0.4792,
                                            0.3151, 0.1667, 0.0703, 0.0208, 0.0026}});
    }
    
    private static Matrix getGradientsFor3() {
        // 31 values
        return new Matrix(new double [][] {{0.00032552, 0.0026, 0.0088, 0.0208, 0.0407,
                                            0.0703, 0.1117, 0.1667, 0.236, 0.3151,
                                            0.3981, 0.4792, 0.5524, 0.612, 0.652,
                                            0.6667, 0.652,  0.612,  0.5524, 0.4792,
                                            0.3981, 0.3151, 0.236,  0.1667, 0.1117,
                                            0.0703, 0.0407, 0.0208, 0.0088, 0.0026,
                                            0.00032552}});
    }
    
    private static Matrix getGradientsFor4() {
        // 63 values
        return new Matrix(new double [][] {{4.069e-05, 0.00032552, 0.0011, 0.0026, 0.0051,
                                            0.0088, 0.014, 0.0208, 0.0297, 0.0407, 
                                            0.0542, 0.0703, 0.0894, 0.1117, 0.1373, 
                                            0.1667, 0.1997, 0.236, 0.2747, 0.3151, 
                                            0.3565, 0.3981, 0.4392, 0.4792, 0.5171, 
                                            0.5524, 0.5843, 0.612, 0.6348, 0.652, 
                                            0.6629, 0.6667, 0.6629, 0.652, 0.6348, 
                                            0.612, 0.5843, 0.5524, 0.5171, 0.4792, 
                                            0.4392, 0.3981, 0.3565, 0.3151, 0.2747, 
                                            0.236, 0.1997, 0.1667, 0.1373, 0.1117, 
                                            0.0894, 0.0703, 0.0542, 0.0407, 0.0297, 
                                            0.0208, 0.014, 0.0088, 0.0051, 0.0026, 
                                            0.0011, 0.00032552, 4.069e-05}});
    }
}
