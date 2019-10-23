package mosaic.bregman.segmentation;

import mosaic.utils.Debug;

public class SegmentationParameters {
    public enum NoiseModel {
        POISSON,
        GAUSS
    }
    
    public enum IntensityMode {
        AUTOMATIC,
        LOW,
        MEDIUM,
        HIGH
    }
    
    // Constant segmentation parameters 
    final boolean debug = false;
    final double lambdaData = 1;
    final double defaultBetaMleIn = 1.0;
    final double defaultBetaMleOut = 0.0003;

    // General settings
    final int numOfThreads;
    final int interpolation;
    
    // Segmentation parameters (from segmentation GUI)
    final double lambdaRegularization;
    final double minObjectIntensity;
    final boolean excludeEdgesZ;
    final IntensityMode intensityMode;
    final NoiseModel noiseModel;
    final double sigmaGaussianXY;
    final double sigmaGaussianZ;
    final double minRegionIntensity;
    final int minRegionSize;
    final String mask;
    
    public SegmentationParameters(  int aNumOfThreads,
                                    int aInterpolation,
                                    double aRegularization,
                                    double aMinObjectIntensity,
                                    boolean aExcludeZedges,
                                    IntensityMode aIntensityMode, 
                                    NoiseModel aNoiseModel, 
                                    double aSigmaGaussianXY,
                                    double aSigmaGaussianZ,
                                    double aMinRegionIntensity,
                                    int aMinRegionSize,
                                    String aMask)
    {
        numOfThreads = aNumOfThreads;
        interpolation = aInterpolation;
        
        lambdaRegularization = aRegularization;
        minObjectIntensity = aMinObjectIntensity;
        excludeEdgesZ = aExcludeZedges;
        intensityMode = aIntensityMode;
        noiseModel = aNoiseModel;
        sigmaGaussianXY = aSigmaGaussianXY;
        sigmaGaussianZ = aSigmaGaussianZ;
        minRegionIntensity = aMinRegionIntensity;
        minRegionSize = aMinRegionSize;
        mask = aMask;
    }
    
    @Override
    public String toString() {
        return Debug.getJsonString(this);
    }
}
