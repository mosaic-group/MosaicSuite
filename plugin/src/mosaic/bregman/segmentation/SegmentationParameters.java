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
    final boolean debug = true;
    final double lambdaData = 1;
    final double gamma = 1;
    final int energyEvaluationModulo = 5;
    final double energySearchThreshold = 1e-7;
    final double defaultBetaMleIn = 1.0;
    final double defaultBetaMleOut = 0.0003;
    final int minRegionSize = 2;

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
    
    public SegmentationParameters(  int aNumOfThreads,
                                    int aInterpolation,
                                    double aRegularization,
                                    double aMinObjectIntensity,
                                    boolean aExcludeZedges,
                                    IntensityMode aIntensityMode, 
                                    NoiseModel aNoiseModel, 
                                    double aSigmaGaussianXY,
                                    double aSigmaGaussianZ,
                                    double aMinRegionIntensity )
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
    }
    
    @Override
    public String toString() {
        return Debug.getJsonString(this);
    }
}
