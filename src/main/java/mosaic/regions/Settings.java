package mosaic.regions;


import java.io.Serializable;

import mosaic.regions.RegionsUtils.EnergyFunctionalType;
import mosaic.regions.RegionsUtils.InitializationType;
import mosaic.regions.RegionsUtils.RegularizationType;


public class Settings implements Serializable {
    private static final long serialVersionUID = 1777976540627904860L;
    
    // Init Label Image ---------------------------------------------------------------------------
    // Init Type
    public InitializationType initType = InitializationType.Bubbles;
    
    // Box Init / Init Label Image
    public double initBoxRatio = 0.8;
    
    // Bubbles Init / Label Image
    public int initBubblesRadius = 10;
    public int initBubblesDisplacement = 10;
    
    // Local Max / Init Label Image
    public double initLocalMaxGaussBlurSigma = 2;
    public double initLocalMaxTolerance = 0.005; // 0 - 1.0
    public int initLocalMaxMinimumRegionSize = 4;
    public int initLocalMaxBubblesRadius = 5;

    // Init Energies ------------------------------------------------------------------------------
    // Curvature Flow Energy (internal)
    public float energyCurvatureMaskRadius = 8;

    // E_PS Energy (external)
    public int energyPsGaussEnergyRadius = 8;
    public float energyPsBalloonForceCoeff = 0.0f;

    // Other --------------------------------------------------------------------------------------
    // Image Model
    public EnergyFunctionalType energyFunctional = EnergyFunctionalType.e_PC;
    public RegularizationType regularizationType = RegularizationType.Sphere_Regularization;
    public float energyContourLengthCoeff = 0.04f;
    
    // RC and DRS
    public boolean allowFusion = true;
    public boolean allowFission = true;
    public boolean allowHandles = true;
    public int maxNumOfIterations = 300;
    
    public void copy(Settings s) {
        initType = s.initType;
        
        initBoxRatio = s.initBoxRatio;
        
        initBubblesRadius = s.initBubblesRadius;
        initBubblesDisplacement = s.initBubblesDisplacement;
        
        initLocalMaxGaussBlurSigma = s.initLocalMaxGaussBlurSigma;
        initLocalMaxTolerance = s.initLocalMaxTolerance;
        initLocalMaxMinimumRegionSize = s.initLocalMaxMinimumRegionSize;
        initLocalMaxBubblesRadius = s.initLocalMaxBubblesRadius;
        
        energyCurvatureMaskRadius = s.energyCurvatureMaskRadius;
        
        energyPsGaussEnergyRadius = s.energyPsGaussEnergyRadius;
        energyPsBalloonForceCoeff = s.energyPsBalloonForceCoeff;
        
        energyFunctional = s.energyFunctional;
        regularizationType = s.regularizationType;
        energyContourLengthCoeff = s.energyContourLengthCoeff;
        
        allowFusion = s.allowFusion;
        allowFission = s.allowFission;
        allowHandles = s.allowHandles;
        maxNumOfIterations = s.maxNumOfIterations;
    }

    public Settings() {}

    public Settings(Settings s) { copy(s); }
}
