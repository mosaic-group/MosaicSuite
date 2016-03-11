package mosaic.bregman.segmentation.solver;

import mosaic.utils.Debug;

public class SolverParameters {
    public enum NoiseModel {
        POISSON,
        GAUSS
    }
    
    // Constant segmentation parameters 
    final boolean debug = false;
    final double lambdaData = 1;
    final double gamma = 1;
    final int energyEvaluationModulo = 5;
    final double energySearchThreshold = 1e-7;

    // General settings
    final int numOfThreads;

    // Segmentation parameters (from segmentation GUI)
    final NoiseModel noiseModel;
    final double betaMleIn;
    final double betaMleOut;
    final double lambdaRegularization;
    
    public SolverParameters( int aNumOfThreads, 
                             NoiseModel aNoiseModel,
                             double aBetaMleIn,
                             double aBetaMleOut,
                             double aRegularization ) 
    {
        numOfThreads = aNumOfThreads;
        noiseModel = aNoiseModel;
        betaMleIn = aBetaMleIn;
        betaMleOut = aBetaMleOut;
        lambdaRegularization = aRegularization;
    }
    
    @Override
    public String toString() {
        return Debug.getJsonString(this);
    }
}
