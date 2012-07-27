package mosaic.region_competition;

import java.io.Serializable;

public class Settings implements Serializable
{
	private static final long serialVersionUID = 1526978719506239136L;
	
	
	
	public EnergyFunctionalType m_EnergyFunctional = EnergyFunctionalType.e_CV;
	public int m_MaxNbIterations = 300;

	public float m_RegionMergingThreshold = 0.02f;

	public boolean m_RemoveNonSignificantRegions = true;
	public 	int m_AreaThreshold = 2;

	public float m_BalloonForceCoeff = 0.0f;
	public float m_ConstantOutwardFlow = 0.0f;

	public boolean m_EnergyUseCurvatureRegularization = true;
	public float m_CurvatureMaskRadius = 8;
	public float m_EnergyContourLengthCoeff = 0.04f; // 0.04;//0.003f;

	
	public int m_GaussPSEnergyRadius=16;
	
	public int m_OscillationHistoryLength = 20;

	public float m_AcceptedPointsFactor = 1;
	public float m_AcceptedPointsReductionFactor = 0.5f;

//	private float m_SigmaOfLoGFilter = 2.f;
	// / Pushes the curve towards edges in the image
//	private float m_EnergyEdgeAttractionCoeff = 0; // /EXPERIMENTAL!;
//	private float m_EnergyShapePriorCoeff = 0;
	public float m_EnergyRegionCoeff = 1.0f;
//	private float m_EnergySphericityCoeff = 0;

	public boolean m_AllowFusion = true;
	public boolean m_AllowFission = true;
	public boolean m_AllowHandles = true;
//	private boolean m_UseRegionCompetition = true;
//	private boolean m_UseForbiddenRegion = false;
//	private boolean m_UseGaussianPSF = true;
	public boolean m_UseShapePrior = false;
//	private boolean m_UseFastEvolution = false;
//	private int m_LocalLiEnergySigma = 5;

}
