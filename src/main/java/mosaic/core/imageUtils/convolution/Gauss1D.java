package mosaic.core.imageUtils.convolution;

import mosaic.filamentSegmentation.GaussPsf;

/**
 * Generates normalized 1D Gassian blur kernel
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class Gauss1D extends Kernel1D {
    public Gauss1D(double aStdDev, int aKernelLen) {
        generateKernel(aStdDev, aKernelLen);
    }
    
    public Gauss1D(double aStdDev) {
        // 4 * stdDev on both sides of Gaussian distr. covers 99,73% of it - good enough
        int kernelLen =  (int) (2 * 3 * aStdDev);
        // make sure it's odd number
        if (kernelLen % 2 == 0) kernelLen += 1;
        
        generateKernel(aStdDev, kernelLen);
    }

    private void generateKernel(double aSigma, int aKernelLen) {
        k = GaussPsf.generateKernel(aKernelLen, 1 /* size in y direction */, aSigma)[0];
        iHalfWidth = aKernelLen/2;
    }
}
