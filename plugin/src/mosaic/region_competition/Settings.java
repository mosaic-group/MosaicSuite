package mosaic.region_competition;

public class Settings
{
	
    private void initMembers() 
	{
	
		m_OscillationHistoryLength = 10;
		
		/**
		 * Initialize control members
		 */
		m_MaxNbIterations = 600;
		m_AreaThreshold = 2; ///TODO find a good heuristic or stat test.
	
		/**
		 * Initialize energy related parameters
		 */
		// m_ContourLengthFunction =
		// ContourLengthApproxImageFunction<LabelImageType>::New();
	
		m_EnergyFunctional = EnergyFunctionalType.e_CV;
		m_EnergyUseCurvatureRegularization = true;
		m_EnergyContourLengthCoeff = 0.04f; // 0.04;//0.003f;
		m_EnergyRegionCoeff = 1.0f;
	
		m_EnergyEdgeAttractionCoeff = 0; ///EXPERIMENTAL!
		m_SigmaOfLoGFilter = 2.f;
	
		m_EnergySphericityCoeff = 0; ///EXPERIMENTAL!
		m_EnergyShapePriorCoeff = 0.0f;
	
		m_BalloonForceCoeff = 0.0f; /// Experimental
		m_ConstantOutwardFlow = 0.0f;
	
		m_AllowFusion = true;
		m_AllowFission = true;
		m_AllowHandles = true;
	
		m_RemoveNonSignificantRegions = true;
		m_UseForbiddenRegion = false;
		m_UseShapePrior = false;
		m_UseGaussianPSF = true;
		m_UseFastEvolution = false;
		// m_SigmaPSF.Fill(1);
	
		///use competing regions istead the concept of DT if fusion is
		// disallowed.
		m_UseRegionCompetition = true; // still has errors.
	
		// m_ForbiddenRegionLabel = NumericTraits<LabelPixelType>::max();
	
		// for(int vD = 0; vD < m_Dim; vD++) {
		// m_LocalCVEnergyRadius[vD] = 1;
		// }
		m_LocalLiEnergySigma = 5;
		m_CurvatureMaskRadius = 8;
		m_RegionMergingThreshold = 0.1f;
	
		m_AcceptedPointsFactor = 1;
		m_AcceptedPointsReductionFactor = 0.5f;
	}


	public Settings() 
	{
		initMembers();
	}

	int m_OscillationHistoryLength;
	
    float m_AcceptedPointsReductionFactor;
    float m_AcceptedPointsFactor;
    
    float m_EnergyContourLengthCoeff;
    float m_SigmaOfLoGFilter;
    /// Pushes the curve towards edges in the image
    float m_EnergyEdgeAttractionCoeff;
    float m_EnergyShapePriorCoeff;
    float m_EnergyRegionCoeff;
    float m_EnergySphericityCoeff;
    
    float m_BalloonForceCoeff;
	float m_ConstantOutwardFlow;
    
	public boolean m_EnergyUseCurvatureRegularization;

    float m_RegionMergingThreshold;
//    ArrayType m_SigmaPSF;
    boolean m_AllowFusion;
    boolean m_AllowFission;
    boolean m_AllowHandles;
    boolean m_UseRegionCompetition; // if fusion is disallowed, else digital topo is used.
    boolean m_RemoveNonSignificantRegions;
    boolean m_UseForbiddenRegion;
    boolean m_UseGaussianPSF;
    boolean m_UseShapePrior;
    boolean m_UseFastEvolution;
    int m_AreaThreshold;
//    ArrayType m_LocalCVEnergyRadius;
    int m_LocalLiEnergySigma;
    float m_CurvatureMaskRadius;
//    int m_ForbiddenRegionLabel;
//    typedef GaussianImageSource<InternalImageType> GaussianImageSourceType;
//    typename GaussianImageSourceType::Pointer m_GaussianImageSource;

	//Private:
    
	int m_MaxNbIterations;
	boolean m_InitializeFromRegion;
	EnergyFunctionalType m_EnergyFunctional;
	
	
}
