package mosaic.region_competition;


import java.io.Serializable;

import mosaic.plugins.Region_Competition.EnergyFunctionalType;
import mosaic.plugins.Region_Competition.InitializationType;
import mosaic.plugins.Region_Competition.RegularizationType;


public class Settings implements Serializable {
    private static final long serialVersionUID = 1777976540627904860L;

    public int m_MaxNbIterations = 300;
    public EnergyFunctionalType m_EnergyFunctional = EnergyFunctionalType.e_PC;
    public int m_GaussPSEnergyRadius = 8;
    public float m_RegionMergingThreshold = 0.02f;
    public float m_BalloonForceCoeff = 0.0f;
    public RegularizationType regularizationType = RegularizationType.Sphere_Regularization;
    public float m_CurvatureMaskRadius = 8;
    public float m_EnergyContourLengthCoeff = 0.04f;
    public InitializationType labelImageInitType = InitializationType.Bubbles;
    public boolean m_AllowFusion = true;
    public boolean m_AllowFission = true;
    public boolean m_AllowHandles = true;
    public double m_OscillationThreshold = 0.02;
    public int m_BubblesRadius = 10;
    public int m_BubblesDispl = 10;
    public int l_BubblesRadius = 5;
    public double l_Sigma = 2;
    public double l_Tolerance = 0.005; // 0 - 1.0
    public int l_RegionTolerance = 4;
    public double l_BoxRatio = 0.8;
    

    public void copy(Settings s) {
        m_MaxNbIterations = s.m_MaxNbIterations;
        m_EnergyFunctional = s.m_EnergyFunctional;
        m_GaussPSEnergyRadius = s.m_GaussPSEnergyRadius;
        m_RegionMergingThreshold = s.m_RegionMergingThreshold;
        m_BalloonForceCoeff = s.m_BalloonForceCoeff;
        regularizationType = s.regularizationType;
        m_CurvatureMaskRadius = s.m_CurvatureMaskRadius;
        m_EnergyContourLengthCoeff = s.m_EnergyContourLengthCoeff;
        labelImageInitType = s.labelImageInitType;
        m_AllowFusion = s.m_AllowFusion;
        m_AllowFission = s.m_AllowFission;
        m_AllowHandles = s.m_AllowHandles;
        m_OscillationThreshold = s.m_OscillationThreshold;
        m_BubblesRadius = s.m_BubblesRadius;
        m_BubblesDispl = s.m_BubblesDispl;
        l_BubblesRadius = s.l_BubblesRadius;
        l_Sigma = s.l_Sigma;
        l_Tolerance = s.l_Tolerance;
        l_RegionTolerance = s.l_RegionTolerance;
        l_BoxRatio = s.l_BoxRatio;
    }

    public Settings() {}

    public Settings(Settings s) { copy(s); }
}
