package mosaic.region_competition;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.EventListener;

import mosaic.region_competition.energies.EnergyFunctionalType;
import mosaic.region_competition.energies.RegularizationType;
import mosaic.region_competition.initializers.InitializationType;


public class Settings implements Serializable {

    private static final long serialVersionUID = 1777976540627904860L;

    public int m_MaxNbIterations = 300;
    public boolean shrinkFirstOnly = false;

    public EnergyFunctionalType m_EnergyFunctional = EnergyFunctionalType.e_PC;
    public int m_GaussPSEnergyRadius = 8;

    public float m_RegionMergingThreshold = 0.02f;

    public float m_BalloonForceCoeff = 0.0f;
    public final float default_PSballoonForceCoeff = 0.05f;
    public float m_ConstantOutwardFlow = 0.0f;

    public RegularizationType regularizationType = RegularizationType.Sphere_Regularization;
    public float m_CurvatureMaskRadius = 8;
    public float m_EnergyContourLengthCoeff = 0.04f; // 0.04;//0.003f;
    public boolean m_RemoveNonSignificantRegions = true;
    public int m_AreaThreshold = 1;

    public int m_OscillationHistoryLength = 10;
    public float m_AcceptedPointsFactor = 1;
    public float m_AcceptedPointsReductionFactor = 0.5f;

    public InitializationType labelImageInitType = InitializationType.Bubbles;

    // private float m_SigmaOfLoGFilter = 2.f;
    // / Pushes the curve towards edges in the image
    // private float m_EnergyEdgeAttractionCoeff = 0; // /EXPERIMENTAL!;
    // private float m_EnergyShapePriorCoeff = 0;
    public float m_EnergyRegionCoeff = 1.0f;
    // private float m_EnergySphericityCoeff = 0;

    public boolean m_AllowFusion = true;
    public boolean m_AllowFission = true;
    public boolean m_AllowHandles = true;
    // private boolean m_UseRegionCompetition = true;
    // private boolean m_UseForbiddenRegion = false;
    public boolean m_UseShapePrior = false;
    // private boolean m_UseFastEvolution = false;
    // private int m_LocalLiEnergySigma = 5;
    public double m_OscillationThreshold = 0.02;

    public String m_PSFImg;
    public int m_BubblesRadius = 10;
    public int m_BubblesDispl = 10;

    public int l_BubblesRadius = 5;
    public double l_Sigma = 2;
    public double l_Tolerance = 0.005;
    public int l_RegionTolerance = 4;
    public double l_BoxRatio = 0.8;

    // SettingsListener ////////////////////////////////
    // To update values on the fly

    private static interface SettingsListener extends EventListener {

        public void settingsChanged(Settings settings);
    }

    ArrayList<SettingsListener> listeners = new ArrayList<Settings.SettingsListener>();

    public boolean RC_free;

    public void copy(Settings s) {
        m_MaxNbIterations = s.m_MaxNbIterations;
        shrinkFirstOnly = s.shrinkFirstOnly;

        m_EnergyFunctional = s.m_EnergyFunctional;
        m_GaussPSEnergyRadius = s.m_GaussPSEnergyRadius;

        m_RegionMergingThreshold = s.m_RegionMergingThreshold;

        m_BalloonForceCoeff = s.m_BalloonForceCoeff;
        m_ConstantOutwardFlow = s.m_ConstantOutwardFlow;

        regularizationType = s.regularizationType;
        m_CurvatureMaskRadius = s.m_CurvatureMaskRadius;
        m_EnergyContourLengthCoeff = s.m_EnergyContourLengthCoeff; // 0.04;//0.003f;
        m_RemoveNonSignificantRegions = s.m_RemoveNonSignificantRegions;
        m_AreaThreshold = s.m_AreaThreshold;

        m_OscillationHistoryLength = s.m_OscillationHistoryLength;
        m_AcceptedPointsFactor = s.m_AcceptedPointsFactor;
        m_AcceptedPointsReductionFactor = 0.5f;

        labelImageInitType = s.labelImageInitType;
        m_EnergyRegionCoeff = s.m_EnergyRegionCoeff;
        // private float m_EnergySphericityCoeff = 0;

        m_AllowFusion = s.m_AllowFusion;
        m_AllowFission = s.m_AllowFission;
        m_AllowHandles = s.m_AllowHandles;
        // private boolean m_UseRegionCompetition = true;
        // private boolean m_UseForbiddenRegion = false;
        m_UseShapePrior = s.m_UseShapePrior;
        // private boolean m_UseFastEvolution = false;
        // private int m_LocalLiEnergySigma = 5;
        m_OscillationThreshold = s.m_OscillationThreshold;

        m_PSFImg = s.m_PSFImg;
        m_BubblesRadius = s.m_BubblesRadius;
        m_BubblesDispl = s.m_BubblesDispl;

        l_BubblesRadius = s.l_BubblesRadius;
        l_Sigma = s.l_Sigma;
        l_Tolerance = s.l_Tolerance;
        l_RegionTolerance = s.l_RegionTolerance;
    }

    public Settings() {

    }

    public Settings(Settings s) {
        copy(s);
    }
}
